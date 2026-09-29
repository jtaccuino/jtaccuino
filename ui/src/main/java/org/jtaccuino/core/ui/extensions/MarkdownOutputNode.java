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
package org.jtaccuino.core.ui.extensions;

import java.util.List;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import jfx.incubator.scene.control.richtext.RichTextArea;
import org.jtaccuino.core.ui.markdown.MarkdownStyledModel;
import org.jtaccuino.core.ui.markdown.MarkdownStyle;

/**
 * Shows a markdown string as notebook output, for results that are text rather
 * than something worth rasterising.
 *
 * <p>It reuses the IDE's own markdown renderer, so output looks like a
 * rendered markdown cell rather than like a block of raw text.
 *
 * <h2>Where the fonts come from</h2>
 * The {@code -markdown-*} font properties are declared in CSS on the
 * {@code .markdown-cell} style class, and this node carries that class, so the
 * fonts the user configured apply here too. They cannot be read any earlier than
 * that: CSS is only applied once the node is in a styled scene, which is why the
 * text is rendered from the skin's layout pass rather than in the
 * constructor. Reading the fonts earlier would silently fall back to the JavaFX
 * defaults.
 */
public final class MarkdownOutputNode extends Control {

    // Deliberately an instance field rather than a static one. A static
    // initializer here pulls javafx.css.StyleablePropertyFactory into this
    // class's class initialization, which can interleave badly with the JavaFX
    // thread initializing the same css classes and deadlock. Keeping class init
    // trivial and doing this per instance mirrors what Control and Region do.
    private final StyleablePropertyFactory<MarkdownOutputNode> factory
            = new StyleablePropertyFactory<>(getClassCssMetaData());

    // These mirror the equivalent properties on MarkdownCell so that output and
    // rendered cells are configured from the one .markdown-cell CSS rule. The
    // CSS names have to stay in step with jtaccuino.css.
    private final StyleableProperty<Font> markdownBaseFont
            = factory.createStyleableFontProperty(this, "markdownBaseFont", "-markdown-base-font", f -> f.markdownBaseFont);

    private final StyleableProperty<Font> markdownMonospaceFont
            = factory.createStyleableFontProperty(this, "markdownMonospaceFont", "-markdown-monospace-font", f -> f.markdownMonospaceFont);

    private final StyleableProperty<Font> markdownEmphasisFont
            = factory.createStyleableFontProperty(this, "markdownEmphasisFont", "-markdown-emphasis-font", f -> f.markdownEmphasisFont);

    private final StyleableProperty<Font> markdownStrongEmphasisFont
            = factory.createStyleableFontProperty(this, "markdownStrongEmphasisFont", "-markdown-strong-emphasis-font", f -> f.markdownStrongEmphasisFont);

    private final StyleableProperty<Font> markdownStrikethroughFont
            = factory.createStyleableFontProperty(this, "markdownStrikethroughFont", "-markdown-strikethrough-font", f -> f.markdownStrikethroughFont);

    private final List<StyleableProperty<Font>> headingFonts = List.of(
            factory.createStyleableFontProperty(this, "MarkdownHeadingOneFont", "-markdown-heading-one-font", f -> f.headingFonts.get(0)),
            factory.createStyleableFontProperty(this, "MarkdownHeadingTwoFont", "-markdown-heading-two-font", f -> f.headingFonts.get(1)),
            factory.createStyleableFontProperty(this, "MarkdownHeadingThreeFont", "-markdown-heading-three-font", f -> f.headingFonts.get(2)),
            factory.createStyleableFontProperty(this, "MarkdownHeadingFourFont", "-markdown-heading-four-font", f -> f.headingFonts.get(3)),
            factory.createStyleableFontProperty(this, "MarkdownHeadingFiveFont", "-markdown-heading-five-font", f -> f.headingFonts.get(4)),
            factory.createStyleableFontProperty(this, "MarkdownHeadingSixFont", "-markdown-heading-six-font", f -> f.headingFonts.get(5)));

    private String markdown = "";

    public MarkdownOutputNode(String markdown) {
        getStyleClass().add("markdown-cell");
        getStyleClass().add("markdown-output");
        this.markdown = markdown;
    }

    public final String getMarkdown() {
        return markdown;
    }

    public final void setMarkdown(String value) {
        this.markdown = value;
    }

    /**
     * The fonts CSS resolved for this node, which is what makes output look like
     * a rendered markdown cell.
     */
    public MarkdownStyle resolvedStyle() {
        return new MarkdownStyle(
                markdownBaseFont.getValue(),
                markdownMonospaceFont.getValue(),
                markdownEmphasisFont.getValue(),
                markdownStrongEmphasisFont.getValue(),
                markdownStrikethroughFont.getValue(),
                headingFonts.stream().map(StyleableProperty::getValue).toList());
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new MarkdownOutputSkin(this);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return factory.getCssMetaData();
    }

    /** Hosts the rendered markdown, built once the fonts are known. */
    private static final class MarkdownOutputSkin extends SkinBase<MarkdownOutputNode> {

        private final MarkdownOutputNode owner;
        private RichTextArea renderArea;
        private String rendered;
        private MarkdownStyle renderedStyle;

        MarkdownOutputSkin(MarkdownOutputNode owner) {
            super(owner);
            this.owner = owner;
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            var text = owner.getMarkdown();
            // comparing the style as well as the text means a live theme change
            // re-renders output that is already on screen; MarkdownStyle is a
            // record, so this is a value comparison
            var style = owner.resolvedStyle();
            if (!text.equals(rendered) || !style.equals(renderedStyle)) {
                rendered = text;
                renderedStyle = style;
                render(text, style);
            }
            if (null != renderArea) {
                renderArea.resizeRelocate(0, 0, w, h);
            }
        }

        private void render(String text, MarkdownStyle style) {
            if (null == renderArea) {
                // configured exactly like the rendered view of a markdown cell
                renderArea = new RichTextArea();
                renderArea.getStyleClass().add("markdown-render");
                renderArea.setEditable(false);
                renderArea.setWrapText(true);
                renderArea.setUseContentHeight(true);
                renderArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
                getChildren().add(renderArea);
            }
            renderArea.setModel(MarkdownStyledModel.render(text, style));
            // replacing the model does not invalidate layout by itself, and the
            // preferred height is derived from the content during layout
            renderArea.requestLayout();
        }

        @Override
        public void dispose() {
            getChildren().clear();
            renderArea = null;
            super.dispose();
        }
    }
}
