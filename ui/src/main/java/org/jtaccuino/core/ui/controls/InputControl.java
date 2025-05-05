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
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;

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
    private final double MIN_WIDTH = 100;
    private final double inputPadding;

    private final Type type;

    private final RichTextArea input;
    private final int cellNumber;
    private final SimpleBooleanProperty rtaFocussedProperty = new SimpleBooleanProperty();

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

        input.documentProperty().subscribe(d
                -> Platform.runLater(()
                        -> recalculateRTA(Math.max(MIN_WIDTH, getWidth()))));
        input.heightProperty().subscribe((h0, h) -> {
            double newHeight = h.doubleValue() + 2 * padding;
            setMinHeight(newHeight);
            setPrefHeight(newHeight);
            setMaxHeight(newHeight);
            requestLayout();
        });
        widthProperty().subscribe(w -> recalculateRTA(w.doubleValue()));
    }

    private void recalculateRTA(double width) {
        input.setPrefWidth(width - 2);
        double textAreaHeight = computeRTAPrefHeight(input.getWidth()) + inputPadding;
        input.setMinHeight(textAreaHeight);
        input.setPrefHeight(textAreaHeight);
        input.setMaxHeight(textAreaHeight);
        input.requestLayout();
    }

    private final static double RTA_LINE_HEIGHT = 25;

    private ListView<?> paragraphListView;
    private Group sheet;

    private double computeRTAPrefHeight(double textAreaWidth) {
        if (sheet == null) {
            sheet = (Group) input.lookup(".sheet");
            paragraphListView = (ListView<?>) input.lookup(".paragraph-list-view");
        }
        if (sheet != null) {
            double cellHeight = sheet.getChildren().stream()
                    .filter(ListCell.class::isInstance)
                    .map(ListCell.class::cast)
                    .filter(cell -> cell.getGraphic() != null)
                    .mapToDouble(n -> n.prefHeight(textAreaWidth))
                    .findFirst()
                    .orElse(RTA_LINE_HEIGHT);
            return Math.max(RTA_LINE_HEIGHT,
                    cellHeight * paragraphListView.getItems().size() + 2);
        }
        return RTA_LINE_HEIGHT;
    }
}
