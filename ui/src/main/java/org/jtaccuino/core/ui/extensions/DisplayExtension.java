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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;

import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.jtaccuino.core.ui.api.CellData;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class DisplayExtension implements JShellExtension {

    private VBox activeOutput;
    private CellData activeCellData;
    private final ReactiveJShell reactiveJShell;

    public static class Factory implements JShellExtension.Factory<DisplayExtension> {

        public DisplayExtension createExtension(ReactiveJShell jshell) {
            return new DisplayExtension(jshell);
        }
    }

    private DisplayExtension(ReactiveJShell reactiveJShell) {
        this.reactiveJShell = reactiveJShell;
    }

    @Override
    public String shellVariableName() {
        return "displayManager";
    }

    @Override
    public String initCodeSnippet() {
        return """
            public void display(Object object) {
                displayManager.display(object, null);
            }
            public void display(Object object, java.util.function.Consumer<Integer> a) {
                displayManager.display(object, a);
            }""";
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

    private final static double CELL_MAX_HEIGHT = 200;
    private final static double COLUMN_PREF_WIDTH = 300;

    @SuppressWarnings("unchecked")
    private Node convertToNode(Object object, Consumer<Integer> counterConsumer) {
        return switch (object) {
            case SubScene subScene -> {
                subSceneHelper(subScene, counterConsumer);
                yield subScene;
            }
            case Node n ->
                n;
            case BufferedImage bi -> {
                var iv = new ImageView();
                var fxi = SwingFXUtils.toFXImage(bi, null);
                iv.setImage(fxi);
                iv.setPreserveRatio(true);
                iv.setFitHeight(CELL_MAX_HEIGHT);
                yield iv;
            }
            case List<? extends Object> l -> {
                if (!l.isEmpty() && l.getFirst().getClass().isRecord()) {
                    var tv = new TableView<Object>();
                    tv.getColumns().addAll(tableColumnsFromRecordComponents(l.getFirst().getClass()));
                    tv.getItems().addAll(l);
                    tv.getColumns().forEach(c -> c.setPrefWidth(COLUMN_PREF_WIDTH));
                    tv.setMaxHeight(CELL_MAX_HEIGHT);
                    yield tv;
                } else {
                    var observableList = FXCollections.observableList(l.stream().map(String::valueOf).toList());
                    var lv = new ListView<String>(observableList);
                    lv.setMaxHeight(CELL_MAX_HEIGHT);
                    yield lv;
                }
            }
            case Set<? extends Object> s -> {
                if (!s.isEmpty() && s.iterator().next() instanceof Map.Entry<?, ?> me) {
                    var tv = new TableView<Map.Entry<Object, Object>>();
                    var tcKey = new TableColumn<Map.Entry<Object, Object>, String>("Key");
                    tcKey.setCellValueFactory(o -> {
                        var valueProp = new SimpleStringProperty();
                        if (null != o) {
                            valueProp.set(String.valueOf(o.getValue().getKey()));
                        }
                        return valueProp;
                    });
                    tv.getColumns().add(tcKey);
                    if (me.getValue().getClass().isRecord()) {
                        tv.getColumns().addAll(tableColumnsFromRecordComponentsForMapValues(me.getValue().getClass()));
                    } else {
                        var tcValue = new TableColumn<Map.Entry<Object, Object>, String>("Value");
                        tcValue.setCellValueFactory(o -> {
                            var valueProp = new SimpleStringProperty();
                            if (null != o) {
                                valueProp.set(String.valueOf(o.getValue().getValue()));
                            }
                            return valueProp;
                        });
                        tv.getColumns().add(tcValue);
                    }
                    s.stream().map(o -> (Map.Entry<Object, Object>) o).forEach(e -> tv.getItems().add(e));
                    tv.getColumns().forEach(c -> c.setPrefWidth(COLUMN_PREF_WIDTH));
                    tv.setMaxHeight(CELL_MAX_HEIGHT);
                    yield tv;
                } else {
                    var observableList = FXCollections.observableList(s.stream().map(String::valueOf).toList());
                    var lv = new ListView<String>(observableList);
                    lv.setMaxHeight(CELL_MAX_HEIGHT);
                    yield lv;
                }
            }
            case Object[] a -> convertArrayToNode(a);
            case double[] a -> convertArrayToListView(Arrays.stream(a).mapToObj(Double::valueOf).toArray());
            case int[] a -> convertArrayToListView(Arrays.stream(a).mapToObj(Integer::valueOf).toArray());
            case long[] a -> convertArrayToListView(Arrays.stream(a).mapToObj(Long::valueOf).toArray());
            default -> {
                var label = new Label("No automatic conversion for type " + object.getClass() + " + to javafx.scene.Node found!");
                yield label;
            }
        };
    }

    protected <T> Node convertArrayToNode(T[] a) {
        if (a.length > 0 && null != a[0]) {
            if (a[0].getClass().isRecord()) {
                var tv = new TableView<Object>();
                tv.getColumns().addAll(tableColumnsFromRecordComponents(a[0].getClass()));
                tv.getItems().addAll(a);
                tv.getColumns().forEach(c -> c.setPrefWidth(COLUMN_PREF_WIDTH));
                tv.setMaxHeight(CELL_MAX_HEIGHT);
                return tv;
            }
        }
        var observableList = FXCollections.observableList(Arrays.stream(a).map(String::valueOf).toList());
        var lv = new ListView<String>(observableList);
        lv.setMaxHeight(CELL_MAX_HEIGHT);
        return lv;
    }

    protected <T> Node convertArrayToListView(T[] a) {
        var observableList = FXCollections.observableList(Arrays.stream(a).map(String::valueOf).toList());
        var lv = new ListView<String>(observableList);
        lv.setMaxHeight(CELL_MAX_HEIGHT);
        return lv;
    }

    protected List<TableColumn<Map.Entry<Object, Object>, String>> tableColumnsFromRecordComponentsForMapValues(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(rc -> {
                    var tc = new TableColumn<Map.Entry<Object, Object>, String>(rc.getName());
                    tc.setCellValueFactory(o -> {
                        var valueProp = new SimpleStringProperty();
                        if (null != o) {
                            try {
                                var value = rc.getAccessor().invoke(o.getValue().getValue());
                                valueProp.set(String.valueOf(value));
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            } catch (InvocationTargetException ite) {
                                ite.getTargetException().printStackTrace();
                            }
                        }
                        return valueProp;
                    });
                    return tc;
                }).toList();
    }

    protected List<TableColumn<Object, String>> tableColumnsFromRecordComponents(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(rc -> {
                    var tc = new TableColumn<Object, String>(rc.getName());
                    tc.setCellValueFactory(o -> {
                        var valueProp = new SimpleStringProperty();
                        if (null != o) {
                            try {
                                var value = rc.getAccessor().invoke(o.getValue());
                                valueProp.set(String.valueOf(value));
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            } catch (InvocationTargetException ite) {
                                ite.getTargetException().printStackTrace();
                            }
                        }
                        return valueProp;
                    });
                    return tc;
                }).toList();
    }

    public void setCurrentCellData(CellData cellData) {
        this.activeCellData = cellData;
    }

    private double mouseOldX, mouseOldY = 0;
    private double mousePosX, mousePosY = 0;
    private double mouseDeltaX, mouseDeltaY = 0;
    private long lastEffect;

    private void subSceneHelper(SubScene subScene, Consumer<Integer> counterConsumer) {
        // From FXyz3D, org.fxyz3d.utils.CameraTransform is just a Group, with 5 transforms
        Camera camera = subScene.getCamera();
        Group cameraTransform = subScene.getRoot().getChildrenUnmodifiable().stream()
                .filter(Group.class::isInstance)
                .map(Group.class::cast)
                .findFirst()
                .orElse(null);
        if (cameraTransform == null || cameraTransform.getTransforms().size() != 5) {
            System.out.println("Error with CameraTransform");
            return;
        }
        Translate t = (Translate) cameraTransform.getTransforms().get(0);
        Rotate rz = (Rotate) cameraTransform.getTransforms().get(1);
        Rotate ry = (Rotate) cameraTransform.getTransforms().get(2);
        Rotate rx = (Rotate) cameraTransform.getTransforms().get(3);
        Scale s = (Scale) cameraTransform.getTransforms().get(4);

        subScene.parentProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (subScene.getParent() != null) {
                    Parent parent = subScene.getParent();
                    parent.setOnMousePressed((MouseEvent me) -> {
                        mousePosX = me.getSceneX();
                        mousePosY = me.getSceneY();
                        mouseOldX = me.getSceneX();
                        mouseOldY = me.getSceneY();
                    });
                    parent.setOnMouseDragged((MouseEvent me) -> {
                        mouseOldX = mousePosX;
                        mouseOldY = mousePosY;
                        mousePosX = me.getSceneX();
                        mousePosY = me.getSceneY();
                        mouseDeltaX = (mousePosX - mouseOldX);
                        mouseDeltaY = (mousePosY - mouseOldY);

                        double modifier = 10.0;
                        double modifierFactor = 0.1;

                        if (me.isControlDown()) {
                            modifier = 0.1;
                        }
                        if (me.isShiftDown()) {
                            modifier = 50.0;
                        }
                        if (me.isPrimaryButtonDown()) {
                            ry.setAngle(((ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);  // +
                            rx.setAngle(((rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);  // -
                        } else if (me.isSecondaryButtonDown()) {
                            double z = camera.getTranslateZ();
                            double newZ = z + mouseDeltaX * modifierFactor * modifier;
                            camera.setTranslateZ(newZ);
                        } else if (me.isMiddleButtonDown()) {
                            t.setX(t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3);  // -
                            t.setY(t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3);  // -
                        }
                    });

                    lastEffect = System.nanoTime();
                    AtomicInteger count=new AtomicInteger();
                    AnimationTimer timerEffect = new AnimationTimer() {
                        @Override
                        public void handle(long now) {
                            if (now > lastEffect + 500_000_000l) {
                                counterConsumer.accept(count.getAndIncrement());
                                lastEffect = now;
                            }
                        }
                    };
                    if (counterConsumer != null) {
                        timerEffect.start();
                    }
                    subScene.parentProperty().removeListener(this);
                }
            }
        });
    }
}
