/*
 * Copyright 2026 JTaccuino Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jtaccuino.core.ui.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.chart.NumberAxis;
import javax.imageio.ImageIO;
import org.jtaccuino.core.ui.FxTestRuntime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the premise the whole export feature rests on: a node can be
 * rasterised to a real PNG with no display, no window system and no GPU.
 *
 * <p>The CLI depends on this. If {@code Node.snapshot()} silently produced a
 * blank or zero-sized image under the headless Glass toolkit, every exported
 * chart would be a silent data loss rather than a visible bug, so the
 * assertions below check actual pixel content instead of just "no exception".
 */
class HeadlessSnapshotTest {

    /** Result of one snapshot, carried back off the FX thread. */
    private record Capture(BufferedImage image, ByteArrayOutputStream png) {
    }

    @BeforeAll
    static void startToolkit() {
        FxTestRuntime.start();
    }

    @Test
    void snapshotRendersRealPixelsUnderHeadlessGlass() throws Exception {
        var label = new Label("headless snapshot");
        label.setStyle("-fx-text-fill: black; -fx-background-color: white;");

        var capture = snapshot(label, 400, 200, true);
        var image = capture.image();

        assertTrue(image.getWidth() > 0 && image.getHeight() > 0,
                "blank dimensions: " + image.getWidth() + "x" + image.getHeight());
        assertTrue(darkPixelCount(image) > 0,
                "snapshot contains no dark pixels, so the label text was not rendered");
    }

    @Test
    void writtenPngRoundTrips() throws Exception {
        var label = new Label("decodable");
        label.setStyle("-fx-text-fill: black; -fx-background-color: white;");

        var capture = snapshot(label, 200, 100, false);
        var bytes = capture.png().toByteArray();
        assertTrue(bytes.length > 0, "no bytes were written");
        assertTrue(bytes[0] == (byte) 0x89 && bytes[1] == 'P', "output is not a PNG, signature missing");

        var decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(decoded, "PNG bytes could not be decoded");
        assertTrue(decoded.getWidth() > 0 && decoded.getHeight() > 0, "decoded image is empty");
    }

    @Test
    void lineChartSnapshotRendersAxesAndData() throws Exception {
        // The headline export case: ChartsFx hands display() a plain Node, so
        // this is the same snapshot path, but it is by far the largest render
        // tree involved (axes, ticks, legend, and a lot of font work).
        var xAxis = new NumberAxis();
        xAxis.setLabel("x");
        var yAxis = new NumberAxis();
        yAxis.setLabel("y");
        var chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        var series = new XYChart.Series<Number, Number>();
        series.setName("data");
        for (int i = 0; i <= 10; i++) {
            series.getData().add(new XYChart.Data<>(i, Math.sin(i)));
        }
        chart.getData().add(series);

        var image = onFxThread(() -> {
            new Scene(chart, 600, 400);
            chart.applyCss();
            chart.layout();
            return SwingFXUtils.fromFXImage(chart.snapshot(new SnapshotParameters(), null), null);
        });

        assertTrue(image.getWidth() > 0 && image.getHeight() > 0,
                "blank dimensions: " + image.getWidth() + "x" + image.getHeight());
        assertTrue(darkPixelCount(image) > 0, "chart snapshot has no dark pixels");
        // The exact count depends on the platform's fonts and anti-aliasing:
        // Linux software Prism draws the axes and ticks with noticeably fewer
        // pixels below the dark threshold than macOS does (around 190 vs 240).
        // A blank snapshot yields 0, so 100 still separates a rendered chart
        // from an empty one without pinning a platform-specific value.
        assertTrue(darkPixelCount(image) > 100,
                "chart snapshot looks empty: only " + darkPixelCount(image) + " dark pixels");
    }

    private static Capture snapshot(Label node, int width, int height, boolean fillWhite) throws Exception {
        var capture = new AtomicReference<Capture>();
        var failure = new AtomicReference<Throwable>();
        var latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                var params = new SnapshotParameters();
                if (fillWhite) {
                    params.setFill(Color.WHITE);
                }
                // A node only lays out once it belongs to a scene, which is what
                // DisplayExtension relies on by adding the node to a live VBox.
                new Scene(node, width, height);

                var image = node.snapshot(params, null);
                var buffered = SwingFXUtils.fromFXImage(image, null);
                var png = new ByteArrayOutputStream();
                ImageIO.write(buffered, "png", png);
                capture.set(new Capture(buffered, png));
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(60, TimeUnit.SECONDS), "FX task did not complete");
        if (failure.get() != null) {
            throw new AssertionError("snapshot failed on the FX thread", failure.get());
        }
        return capture.get();
    }

    @Test
    void pixelReaderConversionMatchesSwingFxUtils() throws Exception {
        // DisplayExtension used to rasterise with SwingFXUtils, which lives in
        // javafx.embed.swing and would have forced a Swing dependency on every
        // consumer of the notebook module. It now copies pixels through
        // PixelReader instead. That swap is only safe if it produces the very
        // same image, so compare the two encoders byte for byte.
        var label = new Label("conversion");
        label.setStyle("-fx-text-fill: black; -fx-background-color: white;");

        var params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        var image = onFxThread(() -> {
            new Scene(label, 320, 120);
            return label.snapshot(params, null);
        });

        var viaSwing = new ByteArrayOutputStream();
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", viaSwing);

        var viaPixelReader = new ByteArrayOutputStream();
        ImageIO.write(toBufferedImage(image), "png", viaPixelReader);

        assertEquals(dump(viaSwing), dump(viaPixelReader), "the two conversions produced different PNGs");
    }

    /** Mirrors DisplayExtension.toBufferedImage, which is private. */
    private static BufferedImage toBufferedImage(WritableImage image) {
        var width = (int) image.getWidth();
        var height = (int) image.getHeight();
        var reader = image.getPixelReader();
        var buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    private static List<String> dump(ByteArrayOutputStream png) throws Exception {
        // compare dimensions and every pixel rather than the compressed bytes,
        // so a difference in encoder tuning cannot mask a real colour change
        var image = ImageIO.read(new ByteArrayInputStream(png.toByteArray()));
        var pixels = new ArrayList<String>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels.add(Integer.toHexString(image.getRGB(x, y)));
            }
        }
        return List.of(image.getWidth() + "x" + image.getHeight(), pixels.toString());
    }

    private static <T> T onFxThread(Supplier<T> action) throws Exception {
        var result = new AtomicReference<T>();
        var failure = new AtomicReference<Throwable>();
        var latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(60, TimeUnit.SECONDS), "FX task did not complete");
        if (failure.get() != null) {
            throw new AssertionError("failed on the FX thread", failure.get());
        }
        return result.get();
    }

    private static int darkPixelCount(BufferedImage image) {
        var dark = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                var rgb = image.getRGB(x, y) & 0xFFFFFF;
                var r = (rgb >> 16) & 0xFF;
                var g = (rgb >> 8) & 0xFF;
                var b = rgb & 0xFF;
                if (r < 100 && g < 100 && b < 100) {
                    dark++;
                }
            }
        }
        return dark;
    }
}
