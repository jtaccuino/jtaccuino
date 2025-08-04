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
package org.jtaccuino.core.ui.documentation;

import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.stage.Window;

public class DocumentationPopup extends PopupControl {

    private final ObservableList<DocumentationItem> documentations = FXCollections.observableArrayList();

    private IntegerProperty visibleDocumentations = new SimpleIntegerProperty(this, "visibleDocumentations", 10);

    @SuppressWarnings("this-escape")
    public DocumentationPopup() {
        setAutoFix(true);
        setAutoHide(true);
        setHideOnEscape(true);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DocumentationPopupSkin(this);
    }

    public ObservableList<DocumentationItem> getDocumentations() {
        return documentations;
    }

    public void setDocumentations(List<DocumentationItem> documentations) {
        this.documentations.setAll(documentations);
    }

    public void show(Node node, Point2D caretOrigin) {
        var parent = node.getScene().getWindow();
        show(node, caretOrigin, parent);
    }

    private void show(Node node, Point2D caretOrigin, Window parent) {
        var localToScene = node.localToScene(caretOrigin);
        this.show(
                node,
                parent.getX() + localToScene.getX() + node.getScene().getX(),
                parent.getY() + localToScene.getY() + node.getScene().getY());
        this.getOwnerNode();
    }

    public final void setVisibleDocumentations(int value) {
        visibleDocumentations.set(value);
    }

    public final int getVisibleDocumentations() {
        return visibleDocumentations.get();
    }

    public final IntegerProperty visibleDocumentationsProperty() {
        return visibleDocumentations;
    }
}
