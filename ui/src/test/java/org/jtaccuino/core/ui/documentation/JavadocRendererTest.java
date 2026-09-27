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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavadocRendererTest {

    @Test
    public void rendersPlainText() {
        var model = JavadocRenderer.render("some documentation text", "", "");
        assertNotNull(model);
        assertEquals(List.of("some documentation text"), nonEmptyParagraphs(model));
    }

    @Test
    public void stripsHtmlTags() {
        var model = JavadocRenderer.render("Returns the <code>length</code> of the <b>string</b>.", "", "");
        assertNotNull(model);
        assertEquals(List.of("Returns the length of the string."), nonEmptyParagraphs(model));
    }

    @Test
    public void decodesEntities() {
        var model = JavadocRenderer.render("a &lt; b &amp;&amp; c", "", "");
        assertNotNull(model);
        assertEquals(List.of("a < b && c"), nonEmptyParagraphs(model));
    }

    @Test
    public void keepsJavadocTagText() {
        var model = JavadocRenderer.render("@param index the index", "", "");
        assertNotNull(model);
        assertEquals(List.of("@param index - the index"), nonEmptyParagraphs(model));
    }

    @Test
    public void highlightsPlainTextJavadocTagsPerLine() {
        var model = JavadocRenderer.render("Returns the string.\n@param a the first\n@return the result", "", "");
        assertNotNull(model);
        assertEquals(List.of("Returns the string.", "@param a - the first", "@return the result"), nonEmptyParagraphs(model));
    }

    @Test
    public void rendersInlineCodeTag() {
        var model = JavadocRenderer.render("Use {@code for (int i = 0; i < n; i++)} to loop.", "", "");
        assertNotNull(model);
        assertEquals(List.of("Use for (int i = 0; i < n; i++) to loop."), nonEmptyParagraphs(model));
    }

    @Test
    public void rendersInlineLinkTag() {
        var model = JavadocRenderer.render("See {@link java.util.List#size} for details.", "", "");
        assertNotNull(model);
        assertEquals(List.of("See java.util.List#size for details."), nonEmptyParagraphs(model));
    }

    @Test
    public void splitsParagraphs() {
        var model = JavadocRenderer.render("<p>first</p><p>second</p>", "", "");
        assertNotNull(model);
        assertEquals(List.of("first", "second"), nonEmptyParagraphs(model));
    }

    @Test
    public void rendersHeader() {
        var model = JavadocRenderer.render("Indicates whether some object is equal to this one.",
                "java.lang.Object", "public boolean equals(Object obj)");
        assertNotNull(model);
        assertEquals(List.of(
                "java.lang.Object",
                "public boolean equals(Object obj)",
                "Indicates whether some object is equal to this one."), nonEmptyParagraphs(model));
        assertEquals(JavadocStyles.MONOSPACE_FAMILY,
                firstTextSegment(model.getParagraph(0)).getStyleAttributeMap(null).getFontFamily());
        assertNotNull(firstTextSegment(model.getParagraph(0)).getStyleAttributeMap(null).getTextColor());
        assertTrue(firstTextSegment(model.getParagraph(1)).getStyleAttributeMap(null).isBold());
    }

    @Test
    public void stylesSectionLabels() {
        var model = JavadocRenderer.render(
                "<p>Description.</p>"
                        + "<dl><dt>Parameters:</dt><dd><code>obj</code> - the object</dd></dl>",
                "java.lang.Object", "public boolean equals(Object obj)");
        assertNotNull(model);
        var labelStyle = firstTextSegment(model.getParagraph(3)).getStyleAttributeMap(null);
        assertTrue(labelStyle.isBold());
        assertNotNull(labelStyle.getTextColor());
        assertEquals(JavadocStyles.ITEM_INDENT,
                model.getParagraph(4).getParagraphAttributes().getSpaceLeft());
    }

    @Test
    public void rendersParamSectionPlainText() {
        var model = JavadocRenderer.render("Description.\n@param obj the object to compare\n@return true if equal", "", "");
        assertNotNull(model);
        assertEquals(List.of(
                "Description.",
                "@param obj - the object to compare",
                "@return true if equal"), nonEmptyParagraphs(model));
    }

    @Test
    public void rendersHtmlSections() {
        var model = JavadocRenderer.render(
                "<p>Description.</p>"
                        + "<dl><dt>Parameters:</dt><dd><code>obj</code> - the object</dd>"
                        + "<dt>Returns:</dt><dd>true if equal</dd></dl>",
                "java.lang.Object", "public boolean equals(Object obj)");
        assertNotNull(model);
        assertEquals(List.of(
                "java.lang.Object",
                "public boolean equals(Object obj)",
                "Description.",
                "Parameters:",
                "obj - the object",
                "Returns:",
                "true if equal"), nonEmptyParagraphs(model));
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
