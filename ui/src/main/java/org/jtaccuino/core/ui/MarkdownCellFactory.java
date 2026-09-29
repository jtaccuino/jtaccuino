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
package org.jtaccuino.core.ui;

import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.jtaccuino.core.ui.markdown.MarkdownStyle;
import org.jtaccuino.core.ui.markdown.MarkdownStyledModel;
import org.jtaccuino.notebook.CellData;
import org.jtaccuino.core.ui.controls.MarkdownControl;

public class MarkdownCellFactory implements CellFactory {

    @Override
    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet) {
        var cell = new MarkdownCell(cellData, parent, sheet, sheet.getNextId());
        return cell;
    }

    public static class MarkdownCell extends Sheet.Cell {

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

        public final Font getMarkdownEmphasisFont() {
            return markdownEmphasisFont.getValue();
        }

        public final void setMarkdownEmphasisFont(Font markdownEmphasisFont) {
            this.markdownEmphasisFont.setValue(markdownEmphasisFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownEmphasisFont
                = FACTORY.createStyleableFontProperty(this, "markdownEmphasisFont", "-markdown-emphasis-font", f -> f.markdownEmphasisFont);

        // Strong emphasis font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownStrongEmphasisFontProperty() {
            return (ObservableValue<Font>) markdownStrongEmphasisFont;
        }

        public final Font getMarkdownStrongEmphasisFont() {
            return markdownStrongEmphasisFont.getValue();
        }

        public final void setMarkdownStrongEmphasisFont(Font markdownStrongEmphasisFont) {
            this.markdownStrongEmphasisFont.setValue(markdownStrongEmphasisFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> markdownStrongEmphasisFont
                = FACTORY.createStyleableFontProperty(this, "markdownStrongEmphasisFont", "-markdown-strong-emphasis-font", f -> f.markdownStrongEmphasisFont);

        // Strikethrough font styling for markdown rendering
        @SuppressWarnings("unchecked")
        public ObservableValue<Font> markdownStrikethroughFontProperty() {
            return (ObservableValue<Font>) markdownStrikethroughFont;
        }

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
        public Skin<?> createDefaultSkin() {
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

        private final MarkdownCell control;
        private final BorderPane pane;
        private final MarkdownControl inputControl;

        private final InvalidationListener styleListener;

        private MarkdownCellSkin(MarkdownCell markdownCell) {
            super(markdownCell);
            this.control = markdownCell;
            pane = new BorderPane();
            pane.getStyleClass().add("md-cell-meta");

            inputControl = new MarkdownControl(control.cellNumber);
            inputControl.getInput().fontProperty().bind(markdownCell.baseEditorFontProperty());
            caretRowColumnProperty.bind(inputControl.caretRowColumnProperty());
            String source = markdownCell.getCellData().getSource();
            if (null != source) {
                inputControl.openDocument(source);
            } else {
                inputControl.openDocument("");
            }

            subscribeToModel(inputControl.getInput().getModel());
            inputControl.getInput().modelProperty().addListener((ov, oldM, newM) -> subscribeToModel(newM));

            // works around not customizable input map from RTA (e.g. shift-enter for cell execution)
            inputControl.getInput().onKeyPressedProperty().addListener((ov, t, t1) -> {
                if (!Objects.equals(t1, getKeyHandler())) {
                    inputControl.getInput().setOnKeyPressed(getKeyHandler());
                    delegateKeyEvents(t1);
                }
            });

            inputControl.getInput().getInputMap().register(KeyBinding.shift(KeyCode.ENTER), () -> execute());

            inputControl.getInput().addEventFilter(KeyEvent.KEY_PRESSED, t
                    -> {
                Platform.runLater(() -> this.control.getSheet().ensureCellVisible(control));
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

            // Registered once per skin and removed in dispose(), so a cell that
            // is executed repeatedly does not accumulate listeners.
            this.styleListener = observable -> {
                if (!inputControl.isRendered()) {
                    return;
                }
                var style = markdownStyleOf(getSkinnable());
                control.getSheet().executeAsync(
                        () -> MarkdownStyledModel.render(control.getCellData().getSource(), style),
                        model -> Platform.runLater(() -> inputControl.updateRenderedView(model)));
            };
            control.markdownBaseFontProperty().addListener(styleListener);
        }

        /**
         * Snapshots the CSS derived fonts of a cell into a {@link MarkdownStyle}
         * that can be used off the FX thread.
         */
        private static MarkdownStyle markdownStyleOf(MarkdownCell cell) {
            return new MarkdownStyle(
                    cell.getMarkdownBaseFont(),
                    cell.getMarkdownMonospaceFont(),
                    cell.getMarkdownEmphasisFont(),
                    cell.getMarkdownStrongEmphasisFont(),
                    cell.getMarkdownStrikethroughFont(),
                    List.of(
                            cell.getMarkdownHeadingOneFont(),
                            cell.getMarkdownHeadingTwoFont(),
                            cell.getMarkdownHeadingrThreeFont(),
                            cell.getMarkdownHeadingrFourFont(),
                            cell.getMarkdownHeadingFiveFont(),
                            cell.getMarkdownHeadingSixFont()));
        }

        private void subscribeToModel(StyledTextModel model) {
            if (null != model) {
                model.addListener((StyledTextModel.Listener) change -> {
                    this.control.getCellData().sourceProperty().set(inputControl.getInput().getText());
                });
            }
        }

        @Override
        public void execute() {
            var rendered = inputControl.isRendered();
            // Move on only for a cell the user is editing; running the whole
            // notebook also calls execute() on cells that are not focused.
            var wasEditing = inputControl.getInput().isFocused();
            // Read the style on the FX thread, then render off it: the
            // renderer only touches fonts and the AST, but the style comes
            // from styleable properties that are CSS derived.
            var style = markdownStyleOf(getSkinnable());
            this.control.getSheet().executeAsync(
                    () -> MarkdownStyledModel.render(this.control.getCellData().getSource(), style),
                    model -> Platform.runLater(() -> {
                        if (rendered) {
                            inputControl.updateRenderedView(model);
                        } else {
                            inputControl.switchToRenderedView(model);
                            if (wasEditing) {
                                // Focusing the rendered area is what advances to
                                // the next cell.
                                inputControl.requestFocus();
                            }
                        }
                    }));
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
            control.markdownBaseFontProperty().removeListener(styleListener);
        }

        void markAsSelected(boolean isSelected) {
            this.getNode().pseudoClassStateChanged(SELECTED, isSelected);
        }
    }
}
