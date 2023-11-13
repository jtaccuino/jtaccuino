/*
 * Copyright 2024 JTaccuino Contributors
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
package org.jtaccuino;

import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.model.Document;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.jtaccuino.rta.MdUtils;

public class MarkdownCellFactory implements CellFactory {

    @Override
    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet) {
        var cell = new MarkdownCell(cellData, parent, sheet, sheet.getNextId());
        return cell;
    }

    public static class MarkdownCell extends Sheet.Cell {

        private final int cellNumber;
        private final VBox parentBox;

        public MarkdownCell(CellData cellData, VBox parent, Sheet sheet, int cellNumber) {
            super(cellData, sheet);
            this.cellNumber = cellNumber;
            this.parentBox = parent;
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            return new MarkdownCellSkin(this);
        }

        @Override
        public void requestFocus() {
            ((MarkdownCellSkin) getSkin()).requestFocus();
        }

        @Override
        public void execute() {
            ((MarkdownCellSkin) getSkin()).execute();
        }

    }

    public static class MarkdownCellSkin extends AbstractCellSkin<MarkdownCell> {

        private final MarkdownCell control;
        private final BorderPane pane;
        private final TextArea inputArea;
        private final AnchorPane inputControl;
        private RichTextArea rta;

        private MarkdownCellSkin(MarkdownCell markdownCell) {
            super(markdownCell);
            this.control = markdownCell;
            pane = new BorderPane() {
                @Override
                protected void layoutChildren() {
                    if (null != rta && this.getChildren().contains(rta)) {
                        rta.prefHeightProperty().set(resizeToFit());
                    }
                    super.layoutChildren();
                }
            };

            // You can re-use parser and renderer instances
            inputArea = new SheetSkin.ResizingTextArea();
            inputArea.setPrefRowCount(1);
            inputArea.setPromptText("Type markdown here");
            inputArea.setId("input_" + this.control.cellNumber);
            inputArea.setFont(Font.font("Monaspace Radon"));
            if (null != markdownCell.getCellData().getSource()) {
                inputArea.setText(markdownCell.getCellData().getSource());
            }
            inputArea.prefWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            markdownCell.getCellData().sourceProperty().bind(inputArea.textProperty());
            inputArea.addEventFilter(KeyEvent.KEY_PRESSED, t -> {
                if (KeyCode.ENTER == t.getCode() && t.isShiftDown()) {
                    execute();
                    t.consume();
                }
            });
            var toolbar = createToolbar();
            toolbar.visibleProperty().bind(Bindings.or(inputArea.focusedProperty(), toolbar.focusWithinProperty()));

            inputControl = new AnchorPane(inputArea, toolbar);
            AnchorPane.setLeftAnchor(inputArea, 0d);
            AnchorPane.setTopAnchor(inputArea, 0d);
            AnchorPane.setRightAnchor(toolbar, 15d);
            AnchorPane.setTopAnchor(toolbar, 0d);

            pane.setCenter(inputControl);
            pane.setPadding(new Insets(5));
        }

        private double resizeToFit() {
            var group = (Group) control.lookup(".sheet");
            if (null != group) {
                double sum = group.getChildren().stream()
                        .filter(ListCell.class::isInstance)
                        .map(ListCell.class::cast)
                        .filter(cell -> cell.getGraphic() != null)
                        .mapToDouble(n -> n.prefHeight(rta.getWidth()))
                        .sum();
                return Double.compare(0, sum) == 0 ? rta.getPrefHeight() : sum;
            }
            return rta.getPrefHeight();
        }

        private void switchToRenderedView(Document doc) {
            rta = new RichTextArea();
            rta.setDocument(doc);
            rta.prefWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            rta.maxWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            rta.contentAreaWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            rta.setEditable(false);
            rta.addEventFilter(MouseEvent.MOUSE_CLICKED, t -> {
                if (1 == t.getClickCount() & t.isShiftDown()) {
                    pane.setCenter(inputControl);
                    inputArea.requestFocus();
                }
                t.consume();
            });
            pane.setCenter(rta);
        }

        @Override
        public void execute() {
            if (inputArea.isVisible()) {
                this.control.getSheet().executeAsync(
                        () -> MdUtils.render(this.control.getCellData().getSource()),
                        doc -> Platform.runLater(() -> {
                            switchToRenderedView(doc);
                            Platform.runLater(() -> this.control.getSheet().moveFocusToNextCell(control));
                        }));
            }
        }

        public void requestFocus() {
            if (inputArea.isVisible()) {
                inputArea.requestFocus();
                this.control.getSheet().ensureCellVisible(getNode());
            }
        }

        @Override
        public MarkdownCell getSkinnable() {
            return control;
        }

        @Override
        public Node getNode() {
            return pane;
        }

        @Override
        public void dispose() {
        }

    }

}
