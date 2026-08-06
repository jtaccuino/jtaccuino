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
package org.jtaccuino.core.ui.controls;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.LineEnding;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

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

    private final Type type;

    private final CodeArea input;
    private final int cellNumber;
    private final SimpleBooleanProperty editorFocussedProperty = new SimpleBooleanProperty();
    private final ReadOnlyObjectWrapper<Point2D> caretRowColumn = new ReadOnlyObjectWrapper<>(this, "caretRowColumn", Point2D.ZERO);
    private final Label placeholder;

    @SuppressWarnings("this-escape")
    public InputControl(int cellNumber, Type type) {
        this.cellNumber = cellNumber;
        this.type = type;
        input = new CodeArea();
        placeholder = createPlaceholder(type.promptText);
        setup();
        getChildren().addAll(placeholder, input);

        editorFocussedProperty.bind(input.focusedProperty());
        getStyleClass().add(type.styleClassPrefix + "-cell-input");

        input.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if (event.getSource() instanceof CodeArea c && !c.isFocused()) {
                requestFocus();
            }
        });
        input.caretPositionProperty().addListener((observable, oldValue, newValue) -> updateCaretRowColumn(newValue));
    }

    protected int getCellNumber() {
        return cellNumber;
    }

    @Override
    public void requestFocus() {
        getInput().requestFocus();
    }

    public ReadOnlyBooleanProperty codeEditorFocussed() {
        return editorFocussedProperty;
    }

    public ReadOnlyObjectProperty<Point2D> caretRowColumnProperty() {
        return caretRowColumn.getReadOnlyProperty();
    }

    public CodeArea getInput() {
        return input;
    }

    public void openDocument(String text) {
        input.setText(text);
        input.select(input.getDocumentEnd());
        updatePlaceholder();
    }

    private Label createPlaceholder(String promptText) {
        var label = new Label(promptText);
        label.setMouseTransparent(true);
        label.setTranslateX(padding);
        label.setTranslateY(padding);
        label.getStyleClass().add("editor-placeholder");
        return label;
    }

    private void setup() {
        input.setLineNumbersEnabled(true);
        input.setUseContentHeight(true);
        input.setTabSize(4);
        input.setLineEnding(LineEnding.LF);
        input.setId("input_" + cellNumber);
        input.getStyleClass().add(type.styleClassPrefix + "-editor");
        input.setTranslateX(padding);
        input.setTranslateY(padding);
        input.setLineSpacing(5);

        AnchorPane.setLeftAnchor(input, padding);
        AnchorPane.setRightAnchor(input, padding);
        placeholder.setTranslateX(padding);
        placeholder.setTranslateY(padding);

        input.modelProperty().addListener((observable, oldValue, newValue) -> subscribeToModel(newValue));
        subscribeToModel(input.getModel());
        input.focusedProperty().addListener((observable, oldValue, newValue) -> updatePlaceholder());
        input.widthProperty().addListener((observable, oldValue, newValue) -> updatePlaceholder());
    }

    private void subscribeToModel(StyledTextModel model) {
        if (null != model) {
            model.addListener((StyledTextModel.Listener) change -> updatePlaceholder());
        }
    }

    private void updatePlaceholder() {
        placeholder.setVisible(input.getText().isEmpty() && !input.isFocused());
    }

    private void updateCaretRowColumn(TextPos pos) {
        if (null != pos) {
            caretRowColumn.set(new Point2D(pos.offset(), pos.index()));
        }
    }
}
