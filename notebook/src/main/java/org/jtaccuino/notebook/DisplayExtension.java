/*
 * Copyright 2024-2026 JTaccuino Contributors
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
package org.jtaccuino.notebook;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

/**
 * Implements {@code display(...)}, which is not part of JShell.
 *
 * <p>Converting the displayed object and persisting the resulting PNG live
 * here, so that a headless export records the same image the IDE shows.
 * Attaching the node somewhere visible is delegated to a {@link DisplaySink}.
 */
public class DisplayExtension implements JShellExtension {

    @SuppressWarnings("rawtypes")
    private static final Comparator<ServiceLoader.Provider<NodeRenderer>> NODE_RENDERER_COMPARATOR = Comparator.comparing(
            (ServiceLoader.Provider<NodeRenderer> p) -> p.type().getAnnotation(NodeRenderer.Descriptor.class).type(),
            (Class<?> o1, Class<?> o2) -> o1.isAssignableFrom(o2) ? 1 : -1);

    @Descriptor(mode = Mode.SYSTEM, type = DisplayExtension.class)
    public static class Factory implements JShellExtension.Factory {

        @Override
        public DisplayExtension createExtension(ReactiveJShell jshell) {
            return new DisplayExtension(jshell);
        }
    }

    private DisplaySink displaySink;
    private CellData activeCellData;
    @SuppressWarnings("UnusedVariable") // TODO: Remove if really unused
    private final ReactiveJShell reactiveJShell;

    private DisplayExtension(ReactiveJShell reactiveJShell) {
        this.reactiveJShell = reactiveJShell;
    }

    @Override
    public Optional<String> shellVariableName() {
        return Optional.of("displayManager");
    }

    @Override
    public Optional<String> initCodeSnippet() {
        return Optional.of("""
            public void display(Object object) {
                displayManager.display(object, null);
            }
            public void display(Object object, java.util.function.Consumer<Integer> a) {
                displayManager.display(object, a);
            }""");
    }

    public void setDisplaySink(DisplaySink displaySink) {
        this.displaySink = displaySink;
    }

    public void display(Object object, Consumer<Integer> counterConsumer) {
        var result = resolve(object, counterConsumer);
        // ensure fields are transferred on jshell worker thread, so that code executed
        // on FX platform thread use registered "callbacks" from current execution, not from the next..
        final var dS = this.displaySink;
        final var aC = this.activeCellData;
        Platform.runLater(() -> {
            try {
                if (null != dS) {
                    dS.accept(result);
                }
                switch (result) {
                    case DisplayResult.Graphical graphical -> persistPng(graphical.node(), aC);
                    case DisplayResult.Markdown markdown -> persistMarkdown(markdown.markdown(), aC);
                }
            } catch (Throwable t) {
                Logger.getLogger(DisplayExtension.class.getName()).log(Level.SEVERE, null, t);
            }
        });
    }

    /**
     * Renders an object for display, falling back to markdown when it cannot be
     * turned into a node.
     *
     * <p>The fallback matters: an earlier version raised the failure. Rasterising
     * a sentence into a PNG is a poor substitute for storing the sentence, and a
     * headless export has no way to recover the text afterwards.
     */
    private DisplayResult resolve(Object object, Consumer<Integer> counterConsumer) {
        try {
            return new DisplayResult.Graphical(convertToNode(object, counterConsumer), object);
        } catch (RuntimeException failed) {
            Logger.getLogger(DisplayExtension.class.getName())
                    .log(Level.FINE, "no node for " + object.getClass() + ", falling back to markdown", failed);
            return new DisplayResult.Markdown(unsupportedMarkdown(object), object);
        }
    }

    /**
     * The markdown shown when an object has no renderer. Kept as markdown rather
     * than a node so that a headless export stores readable text.
     */
    private static String unsupportedMarkdown(Object object) {
        return "Automatic conversion for type `" + object.getClass().getName()
                + "` to a `javafx.scene.Node` failed somehow, so this is shown as text instead.";
    }

    private static void persistMarkdown(String markdown, CellData cellData) {
        if (null != cellData) {
            cellData.getOutputData().add(CellData.OutputData.of(
                    CellData.OutputData.OutputType.DISPLAY_DATA,
                    Map.of("text/markdown", markdown)));
        }
    }

    private static void persistPng(Node node, CellData cellData) {
        var snapshot = node.snapshot(new SnapshotParameters(), null);
        var baos = new ByteArrayOutputStream();
        var eos = Base64.getEncoder().wrap(baos);
        try {
            ImageIO.write(toBufferedImage(snapshot), "png", eos);
            baos.flush();
            baos.close();
            var displayData = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            if (null != cellData) {
                cellData.getOutputData().add(CellData.OutputData.of(
                        CellData.OutputData.OutputType.DISPLAY_DATA,
                        Map.of("image/png", displayData)));
            }
        } catch (IOException ex) {
            Logger.getLogger(DisplayExtension.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Copies a snapshot into a {@link BufferedImage} pixel by pixel.
     *
     * <p>This is deliberately not {@code SwingFXUtils.fromFXImage}: that lives
     * in {@code javafx.embed.swing} and would force a Swing dependency on
     * every consumer of the notebook module, including the headless CLI. PNG
     * encoding itself is done by {@code javax.imageio}, which is in the JDK.
     */
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

    private long lastEffect;

    @SuppressWarnings({"rawtypes","unchecked"})
    private Optional<Node> convert(ServiceLoader.Provider<NodeRenderer> p, Object object) {
        Class<?> argumentType = p.type().getAnnotation(NodeRenderer.Descriptor.class).type();
        try {
            MethodHandle nodeRenderer = MethodHandles.publicLookup().findVirtual(p.type(), "render", MethodType.methodType(Optional.class, argumentType));
            return (Optional<Node>) nodeRenderer.invoke(p.get(), object);
        } catch (Throwable ex) {
            Logger.getLogger(DisplayExtension.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.empty();
    }

    /**
     * Renders an object to a node using the first matching
     * {@link NodeRenderer} on the classpath.
     *
     * <p>Public because an exporter needs the node for exactly the same reason
     * {@code display} does, without going through a JShell variable.
     *
     * @param counterConsumer when non-null, notified repeatedly while the node
     * is still animating, so a caller can wait for it to settle
     * @throws java.util.NoSuchElementException if no renderer claims the type,
     * or the renderer that claims it cannot handle this particular value;
     * {@link #resolve(Object, Consumer)} turns that into markdown
     * @return the rendered node, never {@code null}
     */
    @SuppressWarnings("rawtypes")
    public Node convertToNode(Object object, Consumer<Integer> counterConsumer) {
        // the inner Optional is empty when a renderer claimed the type but could
        // not handle this particular value; that is a failure too, so it throws
        // rather than yielding null
        var node = ServiceLoader.load(NodeRenderer.class)
                .stream()
                .filter(p
                        -> p.type().getAnnotation(NodeRenderer.Descriptor.class).type().isAssignableFrom(object.getClass()))
                .sorted(NODE_RENDERER_COMPARATOR)
                .findFirst()
                .map(p -> convert(p, object))
                .orElseThrow()
                .orElseThrow();
        if (null != counterConsumer) {
            node.parentProperty().addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    if (node.getParent() != null) {
                        lastEffect = System.nanoTime();
                        AtomicInteger count = new AtomicInteger();
                        AnimationTimer timerEffect = new AnimationTimer() {
                            @Override
                            public void handle(long now) {
                                if (now > lastEffect + 500_000_000L) {
                                    counterConsumer.accept(count.getAndIncrement());
                                    lastEffect = now;
                                }
                            }
                        };
                        timerEffect.start();
                        node.parentProperty()
                                .removeListener(this);
                    }
                }
            });
        }
        return node;
    }

    public void setCurrentCellData(CellData cellData) {
        this.activeCellData = cellData;
    }
}
