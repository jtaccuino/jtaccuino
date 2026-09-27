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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavadocHtmlToMarkdownTest {

    @Test
    public void convertsParagraphsToBlankLineSeparatedBlocks() {
        var md = JavadocHtmlToMarkdown.convert("<p>first</p><p>second</p>");
        assertEquals("first\n\nsecond", md);
    }

    @Test
    public void convertsCodeAndBold() {
        var md = JavadocHtmlToMarkdown.convert("Use <code>foo</code> and <b>bar</b>.");
        assertEquals("Use `foo` and **bar**.", md);
    }

    @Test
    public void convertsInlineCodeTag() {
        var md = JavadocHtmlToMarkdown.convert("Use {@code for (int i = 0; i < n; i++)}.");
        assertEquals("Use `for (int i = 0; i < n; i++)`.", md);
    }

    @Test
    public void convertsInlineLinkTagToPlainText() {
        var md = JavadocHtmlToMarkdown.convert("See {@link java.util.List#size}.");
        assertEquals("See java.util.List#size.", md);
    }

    @Test
    public void decodesEntities() {
        var md = JavadocHtmlToMarkdown.convert("a &lt; b &amp;&amp; c");
        assertEquals("a < b && c", md);
    }

    @Test
    public void convertsDtDdToBoldSectionAndBullets() {
        var md = JavadocHtmlToMarkdown.convert(
                "<dl><dt>Parameters:</dt><dd>obj - the object</dd><dt>Returns:</dt><dd>true</dd></dl>");
        assertEquals("**Parameters:**\n\n- obj - the object\n\n**Returns:**\n\n- true", md);
    }

    @Test
    public void highlightsPlainTextJavadocTags() {
        var md = JavadocHtmlToMarkdown.convert("Description.\n@param obj the object\n@return true");
        assertEquals("Description.\n\n**@param** `obj` - the object\n\n**@return** true", md);
    }

    @Test
    public void highlightsJavadocTagsMixedWithHtml() {
        var md = JavadocHtmlToMarkdown.convert(
                "<p>Description.</p>\n@param obj the object\n@see #isAfter");
        assertEquals("Description.\n\n**@param** `obj` - the object\n\n**@see** `#isAfter`", md);
    }

    @Test
    public void highlightsSeeWithCodeFont() {
        var md = JavadocHtmlToMarkdown.convert("@see #isBefore");
        assertEquals("**@see** `#isBefore`", md);
    }
}
