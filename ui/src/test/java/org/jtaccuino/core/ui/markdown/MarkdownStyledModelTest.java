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
package org.jtaccuino.core.ui.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Font;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import org.jtaccuino.core.ui.FxTestRuntime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the rendered model rather than its pixels: which text lands in which
 * paragraph, and that the spans CSS is meant to influence really pick up the
 * fonts {@link MarkdownStyle} carries.
 */
class MarkdownStyledModelTest {

    // Real font families, with a distinct size per role. Size is the reliable
    // assertion target: Font.font() silently falls back to the "System" family
    // for names that are not installed, so an invented family name would make
    // every family assertion pass for the wrong reason.
    private static Font base;
    private static Font monospace;
    private static Font emphasis;
    private static Font strong;
    private static Font strikethrough;
    private static List<Font> headings;
    private static MarkdownStyle style;

    @BeforeAll
    static void initFonts() {
        FxTestRuntime.start();
        base = Font.font("Arial", 12);
        monospace = Font.font("Monospaced", 13);
        emphasis = Font.font("Arial", 14);
        strong = Font.font("Arial", 15);
        strikethrough = Font.font("Arial", 16);
        headings = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> Font.font("Arial", 20 + i * 2))
                .toList();
        style = new MarkdownStyle(base, monospace, emphasis, strong, strikethrough, headings);
    }

    /**
     * Returns the attribute map the renderer handed to the model, rather than one
     * resolved against the control's stylesheet.
     */
    private static final StyleResolver RAW = new StyleResolver() {
        @Override
        public StyleAttributeMap resolveStyles(StyleAttributeMap styles) {
            return styles;
        }

        @Override
        public WritableImage snapshot(Node node) {
            throw new UnsupportedOperationException();
        }
    };

    private static StyleAttributeMap styleOfText(String markdown, String text) {
        var model = MarkdownStyledModel.render(markdown, style);
        for (var p = 0; p < model.size(); p++) {
            var paragraph = model.getParagraph(p);
            for (var s = 0; s < paragraph.getSegmentCount(); s++) {
                var segment = paragraph.getSegment(s);
                if (Objects.equals(segment.getText(), text)) {
                    return segment.getStyleAttributeMap(RAW);
                }
            }
        }
        throw new AssertionError("no segment with text '" + text + "' in: " + markdown);
    }

    @Test
    void appliesTheBaseFontToPlainText() {
        assertEquals(base.getSize(), styleOfText("just words", "just words").getFontSize());
    }

    @Test
    void appliesTheMonospaceFontToInlineCode() {
        var style = styleOfText("plain `code` plain", "code");
        assertEquals(monospace.getSize(), style.getFontSize());
        assertEquals(monospace.getFamily(), style.getFontFamily());
    }

    @Test
    void highlightsInlineCodeWithTheRunHighlight() {
        var style = styleOfText("plain `code` plain", "code");
        assertTrue(style.getBoolean(StyleAttributeMap.TEXT_HIGHLIGHT_1),
                "inline code background comes from the run highlight");
    }

    @Test
    void appliesTheMonospaceFontAndBackgroundToFencedCode() {
        var model = MarkdownStyledModel.render("```\nint x = 1;\n```", style);
        var paragraph = model.getParagraph(0);
        assertEquals("int x = 1;", paragraph.getPlainText());
        assertEquals(monospace.getSize(),
                paragraph.getSegment(0).getStyleAttributeMap(RAW).getFontSize());
        assertNotNull(paragraph.getParagraphAttributes().getBackground(),
                "the fenced code paragraph should carry the background");
    }

    @Test
    void appliesTheEmphasisFontAndItalic() {
        var style = styleOfText("a *b* c", "b");
        assertEquals(emphasis.getSize(), style.getFontSize());
        assertTrue(style.isItalic());
    }

    @Test
    void appliesTheStrongFontAndBold() {
        var style = styleOfText("a **b** c", "b");
        assertEquals(strong.getSize(), style.getFontSize());
        assertTrue(style.isBold());
    }

    @Test
    void appliesTheStrikethroughFontAndStrikeThrough() {
        var style = styleOfText("a ~~b~~ c", "b");
        assertEquals(strikethrough.getSize(), style.getFontSize());
        assertTrue(style.isStrikeThrough());
    }

    @Test
    void appliesTheHeadingFontPerLevel() {
        assertEquals(headings.get(0).getSize(), styleOfText("# One", "One").getFontSize());
        assertEquals(headings.get(2).getSize(), styleOfText("### Three", "Three").getFontSize());
    }

    @Test
    void underlinesLinks() {
        assertTrue(styleOfText("[text](http://example.com)", "text").isUnderline());
    }

    /**
     * The visible markers of every paragraph that carries one, without the
     * non-breaking spaces that encode the indentation.
     */
    private static List<String> bullets(String markdown) {
        return rawBullets(markdown).stream()
                .map(bullet -> bullet.replace("\u00A0", ""))
                .toList();
    }

    /**
     * The indentation of every list marker, counted as leading non-breaking
     * spaces.
     */
    private static List<Integer> bulletIndents(String markdown) {
        return rawBullets(markdown).stream()
                .map(bullet -> {
                    var indent = 0;
                    while (indent < bullet.length() && bullet.charAt(indent) == '\u00A0') {
                        indent++;
                    }
                    return indent;
                })
                .toList();
    }

    private static List<String> rawBullets(String markdown) {
        var model = MarkdownStyledModel.render(markdown, style);
        return IntStream.range(0, model.size())
                .mapToObj(i -> model.getParagraph(i).getParagraphAttributes())
                .filter(Objects::nonNull)
                .map(StyleAttributeMap::getBullet)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<String> paragraphs(String markdown) {
        var model = MarkdownStyledModel.render(markdown, style);
        return IntStream.range(0, model.size())
                .mapToObj(model::getPlainText)
                .filter(text -> !text.isBlank())
                .collect(Collectors.toList());
    }

    @Test
    void rendersHeadingsAsSeparateParagraphs() {
        assertEquals(List.of("Title", "Subtitle"), paragraphs("# Title\n\n## Subtitle"));
    }

    @Test
    void keepsSeparateSourceLinesAsSeparateParagraphs() {
        assertEquals(List.of("first", "second", "third"), paragraphs("first\n\nsecond\n\nthird"));
    }

    @Test
    void keepsASpaceAtSoftLineBreaks() {
        var model = MarkdownStyledModel.render("first line\nsecond line", style);
        assertEquals("first line second line", model.getPlainText(0));
    }

    @Test
    void keepsAHardLineBreakInsideTheParagraph() {
        var model = MarkdownStyledModel.render("first  \nsecond", style);
        assertEquals("first\nsecond", model.getPlainText(0));
    }

    @Test
    void rendersInlineEmphasisInPlace() {
        assertEquals(List.of("plain bold plain"), paragraphs("plain **bold** plain"));
    }

    @Test
    void rendersStrikethroughInPlace() {
        assertEquals(List.of("gone here there"), paragraphs("gone ~~here~~ there"));
    }

    @Test
    void rendersInlineCodeInPlace() {
        assertEquals(List.of("call now() please"), paragraphs("call `now()` please"));
    }

    @Test
    void rendersEachBulletAsItsOwnParagraph() {
        assertEquals(List.of("one", "two", "three"), paragraphs("- one\n- two\n- three"));
    }

    @Test
    void marksBulletsAsParagraphAttributes() {
        assertEquals(List.of("\u2022", "\u2022", "\u2022"), bullets("- one\n- two\n- three"));
    }

    @Test
    void numbersOrderedItemsInParagraphAttributes() {
        assertEquals(List.of("1.", "2.", "3."), bullets("1. one\n2. two\n3. three"));
    }

    @Test
    void indentsNestedBulletsMoreThanTheirParents() {
        var indents = bulletIndents("- outer\n  - inner\n    - deeper\n- sibling");
        assertEquals(4, indents.size());
        assertTrue(indents.get(1) > indents.get(0), "nested item should be deeper: " + indents);
        assertTrue(indents.get(2) > indents.get(1), "deeper item should be deeper: " + indents);
        assertEquals(indents.get(0), indents.get(3), "siblings should share an indent: " + indents);
    }

    @Test
    void indentsNestedOrderedItems() {
        var indents = bulletIndents("1. one\n   1. one-one\n      1. one-one-one\n2. two");
        assertEquals(4, indents.size());
        assertTrue(indents.get(1) > indents.get(0), "nested item should be deeper: " + indents);
        assertTrue(indents.get(2) > indents.get(1), "deeper item should be deeper: " + indents);
        assertEquals(indents.get(0), indents.get(3), "siblings should share an indent: " + indents);
    }

    @Test
    void rendersEachOrderedItemAsItsOwnParagraph() {
        assertEquals(List.of("first", "second"), paragraphs("1. first\n2. second"));
    }

    @Test
    void restartsNumberingForEachOrderedList() {
        var rendered = paragraphs("1. a\n\n1. b");
        assertEquals(List.of("a", "b"), rendered);
    }

    @Test
    void rendersFencedCodeLinesAsSeparateParagraphs() {
        assertEquals(List.of("int x = 1;", "return x;"), paragraphs("```java\nint x = 1;\nreturn x;\n```"));
    }

    @Test
    void rendersLinkTextInPlace() {
        assertEquals(List.of("see docs"), paragraphs("see [docs](https://example.com)"));
    }

    @Test
    void rendersNestedListItemsAsFlatParagraphs() {
        assertEquals(List.of("outer", "inner", "sibling"), paragraphs("- outer\n  - inner\n- sibling"));
    }

    @Test
    void rendersTableRowsWithoutTheDelimiterRow() {
        assertEquals(List.of("a | b", "1 | 2"),
                paragraphs("| a | b |\n| --- | --- |\n| 1 | 2 |"));
    }

    @Test
    void boldsTableHeaderCells() {
        assertTrue(styleOfText("| a | b |\n| --- | --- |\n| 1 | 2 |", "a").isBold(),
                "table header cells should be bold");
    }

    @Test
    void handlesEmptyAndNullInput() {
        assertTrue(paragraphs(null).isEmpty());
        assertTrue(paragraphs("").isEmpty());
    }

    @Test
    void keepsEverySourceWordAcrossParagraphs() {
        var markdown = """
                # Heading

                A paragraph with **bold**, *italic*, `code` and a [link](https://example.com).

                - bullet one
                - bullet two

                ```java
                int x = 1;
                ```

                1. ordered one
                2. ordered two
                """;
        var joined = String.join(" ", paragraphs(markdown));
        for (var word : List.of("Heading", "paragraph", "bold", "italic", "code", "link",
                "bullet", "one", "two", "int x = 1;", "ordered")) {
            assertTrue(joined.contains(word), () -> "expected rendered text to contain: " + word);
        }
    }
}
