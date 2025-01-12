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
package org.jtaccuino.core.ui;

import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.model.Document;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.jtaccuino.core.ui.api.CellData;
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

        @Override
        public void markAsSelected(boolean isSelected) {
             ((MarkdownCellSkin) getSkin()).markAsSelected(isSelected);
        }
    }

    public static class MarkdownCellSkin extends AbstractCellSkin<MarkdownCell> {

        private static final PseudoClass HIGHLIGHT = PseudoClass.getPseudoClass("highlight");

        private final MarkdownCell control;
        private final BorderPane pane;
        private final TextArea inputArea;
        private final AnchorPane inputControl;
        private RichTextArea rta;
        private final ChangeListener<Boolean> focusChangeListener;

        private MarkdownCellSkin(MarkdownCell markdownCell) {
            super(markdownCell);
            this.control = markdownCell;
            pane = new BorderPane();
            pane.getStyleClass().add("md-cell-meta");
            focusChangeListener = (ov, oldValue, newValue) -> markAsSelected(newValue);

            pane.focusWithinProperty().addListener(focusChangeListener);

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
        }

        private final static double RTA_LINE_HEIGHT = 25;
        private ListView<?> paragraphListView;
        private Group group;

        private void recalculateRTA() {
            double textAreaHeight = resizeToFit() + 2;
            rta.setMinHeight(textAreaHeight);
            rta.setPrefHeight(textAreaHeight);
            rta.setMaxHeight(textAreaHeight);
            rta.requestLayout();

            double newHeight = rta.getHeight();
            inputControl.setMinHeight(newHeight);
            inputControl.setPrefHeight(newHeight);
            inputControl.setMaxHeight(newHeight);
            inputControl.requestLayout();
        }

        private double resizeToFit() {
            if (group == null) {
                group = (Group) control.lookup(".sheet");
                paragraphListView = (ListView<?>) control.lookup(".paragraph-list-view");
            }
            if (null != group) {
                double averageCellHeight = group.getChildren().stream()
                        .filter(ListCell.class::isInstance)
                        .map(ListCell.class::cast)
                        .filter(cell -> cell.getGraphic() != null)
                        .mapToDouble(n -> n.prefHeight(rta.getWidth()))
                        .average()
                        .orElse(RTA_LINE_HEIGHT);
                return Math.max(RTA_LINE_HEIGHT,
                        averageCellHeight * paragraphListView.getItems().size() + 2);
            }
            return RTA_LINE_HEIGHT;
        }

        private void switchToRenderedView(Document doc) {
            rta = new RichTextArea();
            rta.prefWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            rta.maxWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            rta.contentAreaWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            rta.setEditable(false);
            rta.addEventFilter(MouseEvent.MOUSE_CLICKED, t -> {
                if (1 == t.getClickCount() & t.isShiftDown()) {
                    pane.setCenter(inputControl);
                    inputArea.requestFocus();
                    markAsSelected(true);
                    pane.focusWithinProperty().addListener(focusChangeListener);
                    t.consume();
                }
            });
            pane.setCenter(rta);
            pane.focusWithinProperty().removeListener(focusChangeListener);
            markAsSelected(false);
            rta.documentProperty().subscribe((ov, nv) -> {
                if (nv != null) {
                    recalculateRTA();
                }
            });
            rta.getActionFactory().open(doc).execute(new ActionEvent());
        }

        @Override
        public void execute() {
            if (inputArea.isVisible()) {
//                Platform.runLater(() -> markAsSelected(false));
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

        void markAsSelected(boolean isSelected) {
            this.getNode().pseudoClassStateChanged(SELECTED, isSelected);
        }
    }
}
