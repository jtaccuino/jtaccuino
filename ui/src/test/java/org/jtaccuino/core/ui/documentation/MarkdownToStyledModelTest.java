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

import java.util.ArrayList;
import java.util.List;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarkdownToStyledModelTest {

    @Test
    public void rendersParagraph() {
        var model = MarkdownToStyledModel.render("some text");
        assertNotNull(model);
        assertEquals(List.of("some text"), nonEmptyParagraphs(model));
    }

    @Test
    public void rendersSeparateParagraphs() {
        var model = MarkdownToStyledModel.render("first\n\nsecond");
        assertNotNull(model);
        assertEquals(List.of("first", "second"), nonEmptyParagraphs(model));
    }

    @Test
    public void rendersBoldSectionLabel() {
        var model = MarkdownToStyledModel.render("**Parameters:**");
        assertNotNull(model);
        assertEquals(List.of("Parameters:"), nonEmptyParagraphs(model));
        var label = model.getParagraph(0);
        assertTrue(labelStyle(label).isBold());
        assertNotNull(labelStyle(label).getTextColor());
    }

    @Test
    public void appliesBodyStyleToDescription() {
        var model = MarkdownToStyledModel.render("some text");
        assertNotNull(model);
        var paragraph = model.getParagraph(0);
        assertEquals(JavadocStyles.BODY_FONT_SIZE, firstTextSegment(paragraph).getStyleAttributeMap(null).getFontSize());
        assertNull(firstTextSegment(paragraph).getStyleAttributeMap(null).getTextColor());
        assertNull(paragraph.getParagraphAttributes().getTextColor());
    }

    @Test
    public void rendersBulletItems() {
        var model = MarkdownToStyledModel.render("- one\n- two");
        assertNotNull(model);
        assertEquals(List.of("one", "two"), nonEmptyParagraphs(model));
        var first = model.getParagraph(0);
        var start = first.getSegment(0);
        assertEquals("\u2022", start.getStyleAttributeMap(null).getBullet());
        assertEquals(JavadocStyles.ITEM_INDENT, start.getStyleAttributeMap(null).getSpaceLeft());
    }

    @Test
    public void rendersInlineCode() {
        var model = MarkdownToStyledModel.render("Use `code` here");
        assertNotNull(model);
        assertEquals(List.of("Use code here"), nonEmptyParagraphs(model));
        var paragraph = model.getParagraph(0);
        var code = paragraph.getSegment(1);
        assertEquals("code", code.getText());
        assertEquals(JavadocStyles.MONOSPACE_FAMILY, code.getStyleAttributeMap(null).getFontFamily());
        assertNotNull(code.getStyleAttributeMap(null).getBackground());
    }

    @Test
    public void appliesSpaceAboveToParagraphs() {
        var model = MarkdownToStyledModel.render("first\n\nsecond");
        var first = model.getParagraph(0);
        var second = model.getParagraph(1);
        assertNotNull(first.getParagraphAttributes());
        assertNotNull(second.getParagraphAttributes());
        assertEquals(JavadocStyles.BODY_SPACE_ABOVE,
                second.getParagraphAttributes().getSpaceAbove());
    }

    private static jfx.incubator.scene.control.richtext.model.StyleAttributeMap labelStyle(RichParagraph paragraph) {
        var segment = firstTextSegment(paragraph);
        return segment.getStyleAttributeMap(null);
    }

    private static jfx.incubator.scene.control.richtext.model.StyledSegment firstTextSegment(RichParagraph paragraph) {
        for (int i = 0; i < paragraph.getSegmentCount(); i++) {
            var segment = paragraph.getSegment(i);
            if (segment.getTextLength() > 0) {
                return segment;
            }
        }
        throw new AssertionError("no text segment");
    }

    private static List<String> nonEmptyParagraphs(SimpleViewOnlyStyledModel model) {
        var result = new ArrayList<String>();
        for (int i = 0; i < model.size(); i++) {
            var text = model.getPlainText(i);
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return result;
    }
}
