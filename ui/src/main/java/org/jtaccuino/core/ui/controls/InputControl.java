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
package org.jtaccuino.core.ui.controls;

import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.model.Document;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Subscription;

sealed class InputControl extends AnchorPane permits JavaControl, MarkdownControl {

    enum Type {
        JAVA("Type code here", "java"),
        MARKDOWN("Type markdown here", "md");

        private final String promptText;
        private final String styleClassPrefix;

        Type(String promptText, String styleClassPrefix) {
            this.promptText = promptText;
            this.styleClassPrefix = styleClassPrefix;
        }
    }

    private final double padding = 0;
    private final double inputPadding;

    private final Type type;

    private final RichTextArea input;
    private final int cellNumber;
    private final SimpleBooleanProperty rtaFocussedProperty = new SimpleBooleanProperty();

    private Subscription inputHeightSubscription = null;
    private Subscription widthSubscription = null;

    @SuppressWarnings("this-escape")
    public InputControl(int cellNumber, Type type) {
        this.cellNumber = cellNumber;
        this.type = type;
        input = new RichTextArea();
        setup();
        getChildren().add(input);

        // external padding + 2 pixels from border width
        inputPadding = input.getPadding().getTop() + input.getPadding().getBottom() + 2;
        input.setTranslateX(padding);
        input.setTranslateY(padding);
        rtaFocussedProperty.bind(input.focusedProperty());
        getStyleClass().add(type.styleClassPrefix + "-cell-input");

        input.addEventFilter(MouseEvent.MOUSE_CLICKED, (EventHandler<MouseEvent>) event -> {
            if (event.getSource() instanceof RichTextArea r && !r.isFocused()) {
                requestFocus();
            }
        });
        subscribeToInput();
    }

    protected int getCellNumber() {
        return cellNumber;
    }

    @Override
    public void requestFocus() {
        getInput().requestFocus();
    }

    public ReadOnlyBooleanProperty codeEditorFocussed() {
        return rtaFocussedProperty;
    }

    public RichTextArea getInput() {
        return input;
    }

    public void openDocument(Document document) {
        input.getActionFactory().open(document).execute(new ActionEvent());
    }

    private void setup() {
        input.setParagraphGraphicFactory((i, t) -> {
            if (i < 1) {
                return null;
            }
            Label label = new Label("#");
            label.setMouseTransparent(true);
            label.getStyleClass().add("numbered-label");
            return label;
        });

        //input.setPrefRowCount(1);
        input.setPromptText(type.promptText);
        input.setTableAllowed(false);
        input.setId("input_" + cellNumber);
        input.getStyleClass().add(type.styleClassPrefix + "-editor");
        input.setAutoSave(true);

        widthProperty().subscribe(w -> recalculateRTA(w.doubleValue()));
    }

    final protected void subscribeToInput() {
        inputHeightSubscription = input.fullHeightProperty().subscribe((h) -> recalculateRTA(getWidth()));
        widthSubscription = widthProperty().subscribe((w) -> recalculateRTA(w.doubleValue()));
    }

    final protected void unsubscribeFromInput() {
        inputHeightSubscription.unsubscribe();
        widthSubscription.unsubscribe();
    }

    private void recalculateRTA(double width) {
        input.setPrefWidth(width - 2);
        double textAreaHeight = input.getFullHeight() + inputPadding;
        input.setMinHeight(textAreaHeight);
        input.setPrefHeight(textAreaHeight);
        input.setMaxHeight(textAreaHeight);
        input.requestLayout();

        double newHeight = textAreaHeight + 2;
        setMinHeight(newHeight);
        setPrefHeight(newHeight);
        setMaxHeight(newHeight);
    }
}
