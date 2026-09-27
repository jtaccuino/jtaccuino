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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

public class JavadocPopupSkin implements Skin<JavadocPopup> {

    private static final Logger LOGGER = Logger.getLogger(JavadocPopupSkin.class.getName());

    private static final String FONT_FAMILY = "Monaspace Argon";
    private static final double BODY_FONT_SIZE = 13;
    private static final double MIN_WIDTH = 280;
    private static final double MAX_WIDTH = 700;
    private static final double MAX_HEIGHT = 420;
    private static final double LINE_SPACING = 3;
    /**
     * Horizontal space reserved for the popup padding, border and a possible
     * vertical scroll bar. The content is measured without wrapping, so a long
     * line is capped at {@link #MAX_WIDTH} and wrapped by the text area.
     */
    private static final double HORIZONTAL_ALLOWANCE = 44;
    /**
     * Vertical space reserved for the popup padding and border.
     */
    private static final double VERTICAL_ALLOWANCE = 26;
    /**
     * Safety margin below {@link #MAX_HEIGHT} used when deciding whether the
     * content still fits without scrolling, compensating for the estimated
     * height being approximate.
     */
    private static final double HEIGHT_SAFETY = 24;

    private final JavadocPopup control;
    private final RichTextArea content;

    @SuppressWarnings("this-escape")
    public JavadocPopupSkin(JavadocPopup control) {
        this.control = control;
        this.content = new RichTextArea(emptyModel());
        this.content.setEditable(false);
        this.content.setFocusTraversable(false);
        this.content.setWrapText(true);
        this.content.setMaxWidth(MAX_WIDTH);
        this.content.setMaxHeight(MAX_HEIGHT);
        this.content.getStyleClass().add("javadoc-popup-content");
        setJavadoc(control.getJavadoc());
        control.javadocProperty().addListener((ov, oldValue, newValue) -> setJavadoc(newValue));
        control.typeNameProperty().addListener((ov, oldValue, newValue) -> setJavadoc(control.getJavadoc()));
        control.signatureProperty().addListener((ov, oldValue, newValue) -> setJavadoc(control.getJavadoc()));
    }

    private static StyledTextModel emptyModel() {
        try {
            return SimpleViewOnlyStyledModel.of("");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create empty javadoc model", ex);
            return new SimpleViewOnlyStyledModel();
        }
    }

    private void setJavadoc(String javadoc) {
        try {
            var model = JavadocRenderer.render(javadoc, control.getTypeName(), control.getSignature());
            double width = preferredWidth(model);
            double neededHeight = estimatedHeight(model, width);
            this.content.setPrefWidth(width);
            if (neededHeight >= MAX_HEIGHT - HEIGHT_SAFETY) {
                // Tall content: cap the height so it scrolls inside the popup.
                this.content.setUseContentHeight(false);
                this.content.setPrefHeight(MAX_HEIGHT);
            } else {
                // Content fits: let the text area size exactly to its content.
                this.content.setUseContentHeight(true);
                this.content.setPrefHeight(Region.USE_COMPUTED_SIZE);
            }
            this.content.setModel(model);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to create javadoc model", ex);
        }
    }

    /**
     * Measures the widest unwrapped line of the rendered javadoc and returns a
     * preferred width that is only as large as the content requires, clamped to
     * {@code [MIN_WIDTH, MAX_WIDTH]}.
     */
    private static double preferredWidth(SimpleViewOnlyStyledModel model) {
        var font = Font.font(FONT_FAMILY, BODY_FONT_SIZE);
        double widest = 0;
        for (int i = 0; i < model.size(); i++) {
            var text = model.getPlainText(i);
            if (text == null || text.isBlank()) {
                continue;
            }
            double lineWidth = measureWrapped(text, font, 0).getWidth();
            var attributes = model.getParagraph(i).getParagraphAttributes();
            if (attributes != null && attributes.getSpaceLeft() != null) {
                lineWidth += attributes.getSpaceLeft();
            }
            widest = Math.max(widest, lineWidth);
        }
        return Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, widest + HORIZONTAL_ALLOWANCE));
    }

    /**
     * Estimates the height of the rendered javadoc at the given width by
     * measuring each paragraph wrapped to the available text width and adding
     * the paragraph spacing. Used to decide whether the content fits without
     * scrolling.
     */
    private static double estimatedHeight(SimpleViewOnlyStyledModel model, double width) {
        var font = Font.font(FONT_FAMILY, BODY_FONT_SIZE);
        double available = Math.max(MIN_WIDTH, width - HORIZONTAL_ALLOWANCE);
        double lineHeight = new Text("Ag").getLayoutBounds().getHeight();
        double total = 0;
        for (int i = 0; i < model.size(); i++) {
            var text = model.getPlainText(i);
            if (text == null || text.isBlank()) {
                continue;
            }
            double textHeight = measureWrapped(text, font, available).getHeight();
            int lines = Math.max(1, (int) Math.round(textHeight / lineHeight));
            var attributes = model.getParagraph(i).getParagraphAttributes();
            if (attributes != null && attributes.getSpaceAbove() != null) {
                total += attributes.getSpaceAbove();
            }
            total += textHeight + lines * LINE_SPACING;
        }
        return total + VERTICAL_ALLOWANCE;
    }

    private static Bounds measureWrapped(String text, Font font, double wrappingWidth) {
        var node = new Text(text);
        node.setFont(font);
        if (wrappingWidth > 0) {
            node.setWrappingWidth(wrappingWidth);
        }
        return node.getLayoutBounds();
    }

    @Override
    public JavadocPopup getSkinnable() {
        return control;
    }

    @Override
    public Node getNode() {
        return content;
    }

    @Override
    public void dispose() {
    }
}
