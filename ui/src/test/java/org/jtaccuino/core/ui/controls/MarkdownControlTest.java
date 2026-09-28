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
package org.jtaccuino.core.ui.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.jtaccuino.core.ui.FxTestRuntime;
import org.jtaccuino.core.ui.markdown.MarkdownStyle;
import org.jtaccuino.core.ui.markdown.MarkdownStyledModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the layout contract the rendered view relies on: it spans the full
 * width it is given, and its reported height follows its content. This is what
 * replaced measuring the Gluon rich text area internals from the outside.
 */
class MarkdownControlTest {

    private static final double WIDTH = 600;

    private static MarkdownStyle style;

    @BeforeAll
    static void initStyle() {
        FxTestRuntime.start();
        // Font.font must run after the toolkit is up.
        var base = Font.font("Arial", 12);
        var monospace = Font.font("Monospaced", 12);
        style = new MarkdownStyle(base, monospace, base, base, base,
                IntStream.rangeClosed(1, 6).mapToObj(i -> Font.font("Arial", 12 + i)).toList());
    }

    @Test
    void renderedViewFillsTheWidthAndGrowsWithContent() throws InterruptedException {
        var measurements = new AtomicReference<double[]>();
        var failure = new AtomicReference<Throwable>();
        var latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                var control = new MarkdownControl(1);
                control.setPrefWidth(WIDTH);
                // A Group keeps the control at its preferred size instead of
                // stretching it to the scene, so the height can be inspected.
                var root = new Group(control);
                new Scene(root, WIDTH, 400);
                settle(root);

                control.switchToRenderedView(MarkdownStyledModel.render("one short line", style));
                var area = control.getMdRenderArea().orElseThrow();
                settle(root);
                var shortWidth = area.getWidth();
                var shortHeight = control.getHeight();

                var manyLines = IntStream.range(0, 40)
                        .mapToObj(i -> "line " + i)
                        .collect(Collectors.joining("\n\n"));
                control.updateRenderedView(MarkdownStyledModel.render(manyLines, style));
                settle(root);
                var longHeight = control.getHeight();

                measurements.set(new double[] {shortWidth, shortHeight, longHeight});
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task did not complete");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        var measured = measurements.get();
        assertEquals(WIDTH, measured[0], 1d, "the rendered view should span the control width");
        assertTrue(measured[1] < 60,
                "a single line should not carry trailing empty paragraphs: " + measured[1]);
        assertTrue(measured[2] > measured[1],
                "height should grow with content: " + measured[1] + " -> " + measured[2]);
    }

    /**
     * Runs layout a few times. The use-content-height RichTextArea derives its
     * preferred height from an arrangement it computes during layout, so like a
     * sequence of pulses it takes more than one pass to settle.
     */
    private static void settle(javafx.scene.Parent root) {
        for (var i = 0; i < 5; i++) {
            root.applyCss();
            root.layout();
        }
    }

    @Test
    void focusesTheRenderedAreaWhenItIsShowing() throws InterruptedException {
        var result = new AtomicReference<Boolean>();
        var failure = new AtomicReference<Throwable>();
        var latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Stage stage = null;
            try {
                var control = new MarkdownControl(3);
                stage = new Stage();
                stage.setScene(new Scene(new VBox(control), 400, 200));
                stage.show();

                control.getInput().requestFocus();
                control.switchToRenderedView(MarkdownStyledModel.render("text", style));
                control.requestFocus();
                result.set(control.mdRenderAreaFocused().get());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                if (stage != null) {
                    stage.hide();
                }
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task did not complete");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        assertTrue(result.get(),
                "requestFocus must reach the rendered area, not the detached editor");
    }

    @Test
    void tracksWhetherItIsRenderedAndHidesTheEditor() throws InterruptedException {
        var result = new AtomicReference<Boolean>();
        var failure = new AtomicReference<Throwable>();
        var latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                var control = new MarkdownControl(2);
                var renderedState = false;
                if (!control.isRendered() && control.getInput().isVisible()) {
                    control.switchToRenderedView(MarkdownStyledModel.render("text", style));
                    // The editor used to stay visible after being removed,
                    // which made requestFocus target a detached node.
                    renderedState = control.isRendered()
                            && !control.getInput().isVisible()
                            && !control.getChildren().contains(control.getInput());
                }
                control.switchToSourceView();
                result.set(renderedState
                        && !control.isRendered()
                        && control.getInput().isVisible()
                        && control.getChildren().contains(control.getInput()));
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task did not complete");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        assertTrue(result.get(), "view state and editor visibility should track the rendered view");
    }
}
