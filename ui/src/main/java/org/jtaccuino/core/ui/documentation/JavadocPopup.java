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
package org.jtaccuino.core.ui.documentation;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.stage.Window;

/**
 * Lightweight popup displaying the javadoc of the currently focused completion
 * suggestion. It is intentionally non-focusable so that keyboard navigation in
 * the completion popup is unaffected.
 */
public class JavadocPopup extends PopupControl {

    private final StringProperty javadoc = new SimpleStringProperty(this, "javadoc", "");
    private final StringProperty typeName = new SimpleStringProperty(this, "typeName", "");
    private final StringProperty signature = new SimpleStringProperty(this, "signature", "");

    @SuppressWarnings("this-escape")
    public JavadocPopup() {
        setAutoFix(true);
        setAutoHide(true);
        setHideOnEscape(true);
        setConsumeAutoHidingEvents(false);
    }

    public final StringProperty javadocProperty() {
        return javadoc;
    }

    public final String getJavadoc() {
        return javadoc.get();
    }

    public final void setJavadoc(String value) {
        javadoc.set(value);
    }

    public final StringProperty typeNameProperty() {
        return typeName;
    }

    public final String getTypeName() {
        return typeName.get();
    }

    public final void setTypeName(String value) {
        typeName.set(value);
    }

    public final StringProperty signatureProperty() {
        return signature;
    }

    public final String getSignature() {
        return signature.get();
    }

    public final void setSignature(String value) {
        signature.set(value);
    }

    public void show(Node node, double x, double y, Window parent) {
        show(node, x, y);
        syncStylesheets(node.getScene().getStylesheets());
    }

    private void syncStylesheets(javafx.collections.ObservableList<String> ownerStylesheets) {
        var popupScene = getScene();
        if (popupScene == null || ownerStylesheets.isEmpty()) {
            return;
        }
        var popupStylesheets = popupScene.getStylesheets();
        for (var stylesheet : ownerStylesheets) {
            if (!popupStylesheets.contains(stylesheet)) {
                popupStylesheets.add(stylesheet);
            }
        }
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JavadocPopupSkin(this);
    }
}
