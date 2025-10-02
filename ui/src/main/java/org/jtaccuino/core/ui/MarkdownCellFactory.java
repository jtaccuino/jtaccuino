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

import org.jtaccuino.rta.StyleableMarkdown;
import com.gluonhq.richtextarea.Selection;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextDecoration;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
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

    public static class MarkdownCell extends Sheet.Cell implements StyleableMarkdown {

        private static final StyleablePropertyFactory<MarkdownCell> FACTORY = new StyleablePropertyFactory<>(MarkdownCell.getClassCssMetaData());

        @SuppressWarnings("this-escape")
        public MarkdownCell(CellData cellData, VBox parent, Sheet sheet, int cellNumber) {
            super(cellData, sheet, cellNumber);
            getStyleClass().add("markdown-cell");
        }

        // Basic font styling for editing
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> baseEditorFontProperty() {
            return (ObservableValue<Font>) baseEditorFont;
        }

        public final Font getBaseEditorFont() {
            return baseEditorFont.getValue();
        }

        public final void setBaseEditorFont(Font baseEditorFont) {
            this.baseEditorFont.setValue(baseEditorFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> baseEditorFont
                = FACTORY.createStyleableFontProperty(this, "baseEditorFont", "-base-editor-font", f -> f.baseEditorFont);

        // Basic font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownBaseFontProperty() {
            return (ObservableValue<Font>) markdownBaseFont;
        }

        @Override
        public final Font getMarkdownBaseFont() {
            return markdownBaseFont.getValue();
        }

        public final void setMarkdownBaseFont(Font markdownBaseFont) {
            this.markdownBaseFont.setValue(markdownBaseFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownBaseFont
                = FACTORY.createStyleableFontProperty(this, "markdownBaseFont", "-markdown-base-font", f -> f.markdownBaseFont);

        // Heading one font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownHeadingOneFontProperty() {
            return (ObservableValue<Font>) markdownHeadingOneFont;
        }

        @Override
        public final Font getMarkdownHeadingOneFont() {
            return markdownHeadingOneFont.getValue();
        }

        public final void setMarkdownHeadingOneFont(Font markdownHeadingOneFont) {
            this.markdownHeadingOneFont.setValue(markdownHeadingOneFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownHeadingOneFont
                = FACTORY.createStyleableFontProperty(this, "markdownHeadingOneFont", "-markdown-heading-one-font", f -> f.markdownHeadingOneFont);

        // Heading two font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownHeadingTwoFontProperty() {
            return (ObservableValue<Font>) markdownHeadingTwoFont;
        }

        @Override
        public final Font getMarkdownHeadingTwoFont() {
            return markdownHeadingTwoFont.getValue();
        }

        public final void setMarkdownHeadingTwoFont(Font markdownHeadingTwoFont) {
            this.markdownHeadingTwoFont.setValue(markdownHeadingTwoFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownHeadingTwoFont
                = FACTORY.createStyleableFontProperty(this, "markdownHeadingTwoFont", "-markdown-heading-two-font", f -> f.markdownHeadingTwoFont);

        // Heading three font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownHeadingThreeFontProperty() {
            return (ObservableValue<Font>) markdownHeadingThreeFont;
        }

        @Override
        public final Font getMarkdownHeadingrThreeFont() {
            return markdownHeadingThreeFont.getValue();
        }

        public final void setMarkdownHeadingThreeFont(Font markdownHeadingThreeFont) {
            this.markdownHeadingThreeFont.setValue(markdownHeadingThreeFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownHeadingThreeFont
                = FACTORY.createStyleableFontProperty(this, "markdownHeadingThreeFont", "-markdown-heading-three-font", f -> f.markdownHeadingThreeFont);

        // Heading four font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownHeadingFourFontProperty() {
            return (ObservableValue<Font>) markdownHeadingFourFont;
        }

        @Override
        public final Font getMarkdownHeadingrFourFont() {
            return markdownHeadingFourFont.getValue();
        }

        public final void setMarkdownHeadingFourFont(Font markdownHeadingFourFont) {
            this.markdownHeadingFourFont.setValue(markdownHeadingFourFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownHeadingFourFont
                = FACTORY.createStyleableFontProperty(this, "markdownHeadingFourFont", "-markdown-heading-four-font", f -> f.markdownHeadingFourFont);

        // Heading five font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownHeadingFiveFontProperty() {
            return (ObservableValue<Font>) markdownHeadingFiveFont;
        }

        @Override
        public final Font getMarkdownHeadingFiveFont() {
            return markdownHeadingFiveFont.getValue();
        }

        public final void setMarkdownHeadingFiveFont(Font markdownHeadingFiveFont) {
            this.markdownHeadingFiveFont.setValue(markdownHeadingFiveFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownHeadingFiveFont
                = FACTORY.createStyleableFontProperty(this, "markdownHeadingFiveFont", "-markdown-heading-five-font", f -> f.markdownHeadingFiveFont);

        // Heading five font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownHeadingSixFontProperty() {
            return (ObservableValue<Font>) markdownHeadingSixFont;
        }

        @Override
        public final Font getMarkdownHeadingSixFont() {
            return markdownHeadingSixFont.getValue();
        }

        public final void setMarkdownHeadingSixFont(Font markdownHeadingSixFont) {
            this.markdownHeadingSixFont.setValue(markdownHeadingSixFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownHeadingSixFont
                = FACTORY.createStyleableFontProperty(this, "markdownHeadingSixFont", "-markdown-heading-six-font", f -> f.markdownHeadingSixFont);

        // Monospace font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownMonospaceFontProperty() {
            return (ObservableValue<Font>) markdownMonospaceFont;
        }

        @Override
        public final Font getMarkdownMonospaceFont() {
            return markdownMonospaceFont.getValue();
        }

        public final void setMarkdownMonospaceFont(Font markdownMonospaceFont) {
            this.markdownMonospaceFont.setValue(markdownMonospaceFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownMonospaceFont
                = FACTORY.createStyleableFontProperty(this, "markdownMonospaceFont", "-markdown-monospace-font", f -> f.markdownMonospaceFont);

        // Emphasis font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownEmphasisFontProperty() {
            return (ObservableValue<Font>) markdownEmphasisFont;
        }

        @Override
        public final Font getMarkdownEmphasisFont() {
            return markdownEmphasisFont.getValue();
        }

        public final void setMarkdownEmphasisFont(Font markdownEmphasisFont) {
            this.markdownEmphasisFont.setValue(markdownEmphasisFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownEmphasisFont
                = FACTORY.createStyleableFontProperty(this, "markdownEmphasisFont", "-markdown-emphasis-font", f -> f.markdownEmphasisFont);

        // Strong Emphasis font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownStrongEmphasisFontProperty() {
            return (ObservableValue<Font>) markdownStrongEmphasisFont;
        }

        @Override
        public final Font getMarkdownStrongEmphasisFont() {
            return markdownStrongEmphasisFont.getValue();
        }

        public final void setMarkdownStrongEmphasisFont(Font markdownStrongEmphasisFont) {
            this.markdownStrongEmphasisFont.setValue(markdownStrongEmphasisFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownStrongEmphasisFont
                = FACTORY.createStyleableFontProperty(this, "markdownStrongEmphasisFont", "-markdown-strong-emphasis-font", f -> f.markdownStrongEmphasisFont);

        // Strong Emphasis font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownStrikethroughFontProperty() {
            return (ObservableValue<Font>) markdownStrikethroughFont;
        }

        @Override
        public final Font getMarkdownStrikethroughFont() {
            return markdownStrikethroughFont.getValue();
        }

        public final void setMarkdownStrikethroughFont(Font markdownStrikethroughFont) {
            this.markdownStrikethroughFont.setValue(markdownStrikethroughFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownStrikethroughFont
                = FACTORY.createStyleableFontProperty(this, "markdownStrikethroughFont", "-markdown-strikethrough-font", f -> f.markdownStrikethroughFont);

        @Override
        protected Skin<?> createDefaultSkin() {
            return new MarkdownCellSkin(this);
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
            return FACTORY.getCssMetaData();
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

        static TextDecoration presetDecoration = TextDecoration.builder().presets().fontFamily("Monaspace Radon")
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
            this.control.baseEditorFontProperty().addListener(new ChangeListener<Font>() {
                @Override
                public void changed(ObservableValue<? extends Font> observable, Font oldValue, Font newValue) {
                    presetDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize()).build();
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
                }
            });
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
                } else if (KeyCode.ENTER == t.getCode() && !t.isShiftDown()) {
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
                System.out.println("ov" + ov + " from " + t + " to " + t1);
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
                this.control.markdownBaseFontProperty().addListener(new InvalidationListener() {
                    @Override
                    public void invalidated(Observable observable) {
                        var doc = MdUtils.render(control.getCellData().getSource(), getSkinnable());
                        inputControl.updateRenderedView(doc);;
                    }
                });

                this.control.getSheet().executeAsync(
                        () -> MdUtils.render(this.control.getCellData().getSource(), this.getSkinnable()),
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
