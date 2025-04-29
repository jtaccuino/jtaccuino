/*
 * Copyright 2024-2025 JTaccuino Contributors
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;

import org.jtaccuino.core.ui.api.CellData;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class DisplayExtension implements JShellExtension {

    @SuppressWarnings("rawtypes")
    private static Comparator<ServiceLoader.Provider<NodeRenderer>> NODE_RENDERER_COMPARATOR = Comparator.comparing(
            (ServiceLoader.Provider<NodeRenderer> p) -> p.type().getAnnotation(NodeRenderer.Descriptor.class).type(),
            (Class<?> o1, Class<?> o2) -> o1.isAssignableFrom(o2) ? 1 : -1);

    @Descriptor(mode = Mode.SYSTEM, type = DisplayExtension.class)
    public static class Factory implements JShellExtension.Factory {

        public DisplayExtension createExtension(ReactiveJShell jshell) {
            return new DisplayExtension(jshell);
        }
    }

    private VBox activeOutput;
    private CellData activeCellData;
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

    public void setActiveOutput(VBox vbox) {
        this.activeOutput = vbox;
    }

    public void display(Object object, Consumer<Integer> counterConsumer) {
        var node = convertToNode(object, counterConsumer);
        // ensure fields are transferred on jshell worker thread, so that code executed
        // on FX platform thread use registered "callbacks" from current execution, not from the next..
        final var aO = this.activeOutput;
        final var aC = this.activeCellData;
        if (null != node) {
            Platform.runLater(() -> {
                try {
                    aO.getChildren().add(node);
                    var snapshot = node.snapshot(new SnapshotParameters(), null);
                    var baos = new ByteArrayOutputStream();
                    var eos = Base64.getEncoder().wrap(baos);
                    try {
                        ImageIO.write(
                                SwingFXUtils.fromFXImage(snapshot, null),
                                "png",
                                eos);
                        baos.flush();
                        baos.close();
                        var displayData = new String(baos.toByteArray());
                        aC.getOutputData().add(CellData.OutputData.of(
                                CellData.OutputData.OutputType.DISPLAY_DATA,
                                Map.of("image/png", displayData)
                        ));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }

    private long lastEffect;

    @SuppressWarnings("rawtypes")
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

    @SuppressWarnings("rawtypes")
    Node convertToNode(Object object, Consumer<Integer> counterConsumer) {
        var node = ServiceLoader.load(NodeRenderer.class)
                .stream()
                .filter(p
                        -> p.type().getAnnotation(NodeRenderer.Descriptor.class).type().isAssignableFrom(object.getClass()))
                .sorted(NODE_RENDERER_COMPARATOR)
                .findFirst()
                .map(p -> convert(p, object))
                .orElseThrow()
                .orElseGet(() -> {
                    var label = new Label("Automatic conversion for type " + object.getClass() + " + to javafx.scene.Node failed somehow!");
                    return label;
                });
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
