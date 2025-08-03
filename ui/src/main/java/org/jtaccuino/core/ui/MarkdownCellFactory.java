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

import com.gluonhq.richtextarea.Selection;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextDecoration;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import org.jtaccuino.core.ui.api.CellData;
import org.jtaccuino.core.ui.controls.MarkdownControl;
import org.jtaccuino.rta.MdUtils;

public class MarkdownCellFactory implements CellFactory {

    @Override
    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet) {
        var cell = new MarkdownCell(cellData, parent, sheet, sheet.getNextId());
        return cell;
    }

    public static class MarkdownCell extends Sheet.Cell {

        public MarkdownCell(CellData cellData, VBox parent, Sheet sheet, int cellNumber) {
            super(cellData, sheet, cellNumber);
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            return new MarkdownCellSkin(this);
        }

        @Override
        public void requestFocus() {
            if (null == getSkin()) {
                skinProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        ((MarkdownCellSkin) getSkin()).requestFocus();
                    }
                });
            } else {
                ((MarkdownCellSkin) getSkin()).requestFocus();
            }
        }

        @Override
        public void execute() {
            ((MarkdownCellSkin) getSkin()).execute();
        }

        @Override
        public void markAsSelected(boolean isSelected) {
            ((MarkdownCellSkin) getSkin()).markAsSelected(isSelected);
            super.markAsSelected(isSelected);
        }
    }

    public static class MarkdownCellSkin extends AbstractCellSkin<MarkdownCell> {

        static final TextDecoration presetDecoration = TextDecoration.builder().presets().fontFamily("Monaspace Radon")
                .fontWeight(FontWeight.NORMAL).fontSize(13).build();

        private static final ParagraphDecoration parPreset
                = ParagraphDecoration.builder().presets()
                        .graphicType(ParagraphDecoration.GraphicType.NUMBERED_LIST)
                        .indentationLevel(1)
                        .topInset(3)
                        .build();

        private final MarkdownCell control;
        private final BorderPane pane;
        private final MarkdownControl inputControl;

        private MarkdownCellSkin(MarkdownCell markdownCell) {
            super(markdownCell);
            this.control = markdownCell;
            pane = new BorderPane();
            pane.getStyleClass().add("md-cell-meta");

            inputControl = new MarkdownControl(control.cellNumber);
            caretRowColumnProperty.bind(inputControl.getInput().caretRowColumnProperty());
            String source = markdownCell.getCellData().getSource();
            if (null != source) {
                inputControl.openDocument(
                        new Document(
                                source,
                                List.of(new DecorationModel(0, source.length(), presetDecoration, parPreset)),
                                source.length() - 1
                        ));
            } else {
                inputControl.openDocument(
                        new Document("",
                                List.of(new DecorationModel(0, 0, presetDecoration, parPreset)),
                                0
                        ));
            }

            inputControl.getInput().documentProperty().subscribe(doc -> markdownCell.getCellData().sourceProperty().set(doc.getText()));

            // works around not customizable input map from RTA (e.g. shift-enter for cell execution)
            inputControl.getInput().onKeyPressedProperty().addListener((ov, t, t1) -> {
                if (t1 != getKeyHandler()) {
                    inputControl.getInput().setOnKeyPressed(getKeyHandler());
                    delegateKeyEvents(t1);
                }
            });

            inputControl.getInput().addEventFilter(KeyEvent.KEY_PRESSED, t
                    -> {
                if (KeyCode.BACK_SPACE == t.getCode()) {
                    var column = (int) inputControl.getInput().getCaretRowColumn().getX();
                    var row = (int) inputControl.getInput().getCaretRowColumn().getY();
                    if (0 == column && 0 == row) {
                        // just consume the event to inhibt deletion of paragraph decoration
                        t.consume();
                    } else if (0 == column) {
                        // automatically remove paragraph decoration in case at beginning of line
                        inputControl.getInput().getActionFactory().removeExtremesAndDecorate(
                                new Selection(inputControl.getInput().getCaretPosition() - 1, inputControl.getInput().getCaretPosition()),
                                ParagraphDecoration.builder().build()).execute(new ActionEvent());
                    }
                } else if (KeyCode.ENTER == t.getCode()) {
                    var column = (int) inputControl.getInput().getCaretRowColumn().getX();
                    if (0 == column) {
                        inputControl.getInput().getActionFactory().insertText("\n").execute(new ActionEvent());
                        t.consume();
                    }
                } else {
                    Platform.runLater(() -> this.control.getSheet().ensureCellVisible(control));
                }
            });

            var toolbar = createToolbar();

            toolbar.visibleProperty().bind(Bindings.or(inputControl.getInput().focusedProperty(), toolbar.focusWithinProperty()));
            inputControl.codeEditorFocussed().addListener((observable, oldValue, newValue) -> {
                getSkinnable().markAsSelected(newValue);
            });

            inputControl.getInput().visibleProperty().addListener((ov, t, t1) -> {
                if (t1) {
                    requestFocus();
                }
            });

            inputControl.mdRenderAreaFocused().addListener((ov, t, t1) -> {
                Platform.runLater(() -> {
                    this.control.getSheet().moveFocusToNextCell(control);
                });
            });

            inputControl.getChildren().add(toolbar);
            AnchorPane.setRightAnchor(toolbar, 15d);
            AnchorPane.setTopAnchor(toolbar, 0d);

            pane.setCenter(inputControl);
        }

        @Override
        public void execute() {
            if (inputControl.getInput().isVisible()) {
                this.control.getSheet().executeAsync(
                        () -> MdUtils.render(this.control.getCellData().getSource()),
                        doc -> Platform.runLater(() -> {
                            inputControl.switchToRenderedView(doc);
                        }));
            }
        }

        public void requestFocus() {
            inputControl.requestFocus();
            this.control.getSheet().ensureCellVisible(control);
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
