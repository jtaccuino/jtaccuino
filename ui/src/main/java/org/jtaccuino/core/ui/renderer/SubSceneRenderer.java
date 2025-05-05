/*
 * Copyright 2025 JTaccuino Contributors
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
package org.jtaccuino.core.ui.renderer;

import java.util.Optional;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.jtaccuino.core.ui.extensions.NodeRenderer;
import org.jtaccuino.core.ui.extensions.NodeRenderer.Descriptor;

@Descriptor(type = SubScene.class)
public class SubSceneRenderer implements NodeRenderer<SubScene> {

    private double mouseOldX, mouseOldY = 0;
    private double mousePosX, mousePosY = 0;
    private double mouseDeltaX, mouseDeltaY = 0;

    @Override
    public Optional<Node> render(SubScene object) {
        return Optional.of(subSceneHelper(object));
    }

    private Node subSceneHelper(SubScene subScene) {
        // From FXyz3D, org.fxyz3d.utils.CameraTransform is just a Group, with 5 transforms
        Camera camera = subScene.getCamera();
        Group cameraTransform = subScene.getRoot().getChildrenUnmodifiable().stream()
                .filter(Group.class::isInstance)
                .map(Group.class::cast)
                .findFirst()
                .orElse(null);
        if (cameraTransform == null || cameraTransform.getTransforms().size() != 5) {
            System.out.println("Error with CameraTransform");
            return subScene;
        }
        Translate t = (Translate) cameraTransform.getTransforms().get(0);
//        @SuppressWarnings("UnusedVariable") // TODO: Remove if really unused
//        Rotate rz = (Rotate) cameraTransform.getTransforms().get(1);
        Rotate ry = (Rotate) cameraTransform.getTransforms().get(2);
        Rotate rx = (Rotate) cameraTransform.getTransforms().get(3);
//        @SuppressWarnings("UnusedVariable") // TODO: Remove if really unused
//        Scale s = (Scale) cameraTransform.getTransforms().get(4);

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

                    subScene.parentProperty().removeListener(this);
                }
            }
        });
        return subScene;
    }
}
