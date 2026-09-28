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

import com.gluonhq.emoji.EmojiData;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.emoji.Emoji;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TableSeparator;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import java.util.ArrayDeque;
import java.util.Deque;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Renders markdown into a {@link SimpleViewOnlyStyledModel} for the incubator
 * {@code RichTextArea} that backs a rendered notebook cell.
 *
 * <p>The model is a plain text and paragraph model, so its height is derived
 * from its own content: the control can be configured with
 * {@code useContentHeight} and left to size itself, with no measuring of
 * internals.
 */
public final class MarkdownStyledModel {

    /**
     * The background shared by inline code and fenced code blocks. Inline code
     * gets it through the run highlight, fenced code through the paragraph
     * background, because the underline and highlight attributes are the only
     * segment attributes the rich text area draws behind a run.
     */
    private static final Color CODE_BACKGROUND = Color.rgb(0xF0, 0xF2, 0xF4);
    private static final String BULLET = "\u2022";
    /**
     * A list marker is drawn as a decoration to the left of the paragraph, not
     * as part of it. The paragraph's left padding only moves the text, so
     * nesting has to come from the marker itself: leading non-breaking spaces
     * widen the decoration, which shifts both the marker and the text, and
     * keep the wrapped lines of an item aligned under its first line.
     */
    private static final String ITEM_INDENT = "\u00A0\u00A0\u00A0";
    private static final String ITEM_GAP = "\u00A0\u00A0";
    private static final double CODE_INDENT = 16;
    private static final double BLOCK_SPACE = 6;
    private static final double LINE_SPACING = 2;

    private MarkdownStyledModel() {
        // prevent instantiation
    }

    public static SimpleViewOnlyStyledModel render(String markdown, MarkdownStyle style) {
        var model = new SimpleViewOnlyStyledModel();
        new Walker(model, style).walk(MarkdownParser.parse(markdown));
        return model;
    }

    private static final class Walker {

        private final SimpleViewOnlyStyledModel model;
        private final MarkdownStyle style;
        private final Deque<StyleAttributeMap> inlineStyles = new ArrayDeque<>();
        private final Deque<Integer> listDepths = new ArrayDeque<>();
        private final Deque<ListItem> listItems = new ArrayDeque<>();
        private int orderedItemIndex;
        private boolean paragraphPending;

        private Walker(SimpleViewOnlyStyledModel model, MarkdownStyle style) {
            this.model = model;
            this.style = style;
            this.inlineStyles.push(textStyle(style.base()));
        }

        private void walk(Node node) {
            switch (node) {
                case Document d -> walkChildren(d);
                case Heading h -> walkHeading(h);
                case Paragraph p -> walkParagraph(p);
                case FencedCodeBlock fcb -> walkFencedCodeBlock(fcb);
                case BulletList bl -> walkBulletList(bl);
                case OrderedList ol -> walkOrderedList(ol);
                case TableBlock tb -> walkTable(tb);
                case BulletListItem bli -> walkListItem(bli, BULLET);
                case OrderedListItem oli -> walkListItem(oli, ++orderedItemIndex + ".");
                case Code c -> model.addSegment(c.getText().toString(), inlineCodeStyle());
                case Emphasis e -> walkStyled(e,
                        merge(textStyle(style.emphasis()), StyleAttributeMap.builder().setItalic(true).build()));
                case StrongEmphasis e -> walkStyled(e,
                        merge(textStyle(style.strongEmphasis()), StyleAttributeMap.builder().setBold(true).build()));
                case Strikethrough e -> walkStyled(e,
                        merge(textStyle(style.strikethrough()),
                                StyleAttributeMap.builder().setStrikeThrough(true).build()));
                case Link l -> walkStyled(l, StyleAttributeMap.builder().setUnderline(true).build());
                // A soft break is rendered as a space by CommonMark; dropping it
                // would glue the last word of one line to the first of the next.
                case SoftLineBreak slb -> model.addSegment(" ", inline());
                case HardLineBreak hlb -> model.addSegment("\n", inline());
                case Emoji e -> model.addSegment(emojiCharacter(e), inline());
                case Text t -> model.addSegment(t.getChars().toString(), inline());
                default -> walkChildren(node);
            }
        }

        private void walkHeading(Heading h) {
            startParagraph();
            var headingStyle = StyleAttributeMap.builder()
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontFamily(style.heading(h.getLevel()).getFamily())
                    .setFontSize(style.heading(h.getLevel()).getSize())
                    .setBold(true)
                    .build();
            inlineStyles.push(headingStyle);
            walkChildren(h);
            inlineStyles.pop();
            endParagraph(merge(paragraphStyle(0), headingStyle));
        }

        private void walkParagraph(Paragraph p) {
            startParagraph();
            walkChildren(p);
            var paragraphAttributes = paragraphStyle(0);
            if (!listDepths.isEmpty()) {
                paragraphAttributes = merge(paragraphAttributes,
                        StyleAttributeMap.builder().setSpaceAbove(0).build());
                var item = listItems.peek();
                if (item != null && !item.bulletApplied) {
                    item.bulletApplied = true;
                    // The bullet is a paragraph attribute; attached to a segment
                    // it only indents the text and renders no marker.
                    paragraphAttributes = merge(paragraphAttributes,
                            StyleAttributeMap.builder().setBullet(item.bullet).build());
                }
            }
            endParagraph(paragraphAttributes);
        }

        private void walkFencedCodeBlock(FencedCodeBlock fcb) {
            var codeParagraph = merge(paragraphStyle(0),
                    StyleAttributeMap.builder()
                            .setSpaceLeft(CODE_INDENT)
                            .setBackground(CODE_BACKGROUND)
                            .build());
            var content = fcb.getContentChars().toString();
            // The fenced block content ends with the closing newline; without
            // stripping it the block gains an empty trailing line.
            if (content.endsWith("\n")) {
                content = content.substring(0, content.length() - 1);
            }
            if (content.endsWith("\r")) {
                content = content.substring(0, content.length() - 1);
            }
            for (var line : content.split("\n", -1)) {
                startParagraph();
                model.addSegment(line, codeSegmentStyle());
                // Every line is its own paragraph, and the background is a
                // paragraph attribute, so each one needs it.
                endParagraph(codeParagraph);
            }
        }

        private void walkBulletList(BulletList bl) {
            listDepths.push(listDepth() + 1);
            walkChildren(bl);
            listDepths.pop();
        }

        private void walkOrderedList(OrderedList ol) {
            var previous = orderedItemIndex;
            orderedItemIndex = 0;
            listDepths.push(listDepth() + 1);
            walkChildren(ol);
            listDepths.pop();
            orderedItemIndex = previous;
        }

        private void walkListItem(Node item, String bullet) {
            listItems.push(new ListItem(ITEM_INDENT.repeat(listDepth()) + bullet + ITEM_GAP));
            walkChildren(item);
            listItems.pop();
        }

        /**
         * The rich text area is a paragraph model, so a table is flattened to
         * one paragraph per row with the cells separated by a bar. The
         * delimiter row only describes the table and is skipped; without that
         * its dashes would be emitted as ordinary text.
         */
        private void walkTable(TableBlock table) {
            for (var section = table.getFirstChild(); section != null; section = section.getNext()) {
                if (section instanceof TableSeparator) {
                    continue;
                }
                if (section instanceof TableHead || section instanceof TableBody) {
                    var header = section instanceof TableHead;
                    for (var row = section.getFirstChild(); row != null; row = row.getNext()) {
                        if (row instanceof TableRow tableRow) {
                            walkTableRow(tableRow, header);
                        }
                    }
                } else if (section instanceof TableRow tableRow) {
                    walkTableRow(tableRow, false);
                }
            }
        }

        private void walkTableRow(TableRow row, boolean header) {
            startParagraph();
            var first = true;
            for (var cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
                if (!(cell instanceof TableCell)) {
                    continue;
                }
                if (!first) {
                    model.addSegment(" | ", inline());
                }
                first = false;
                if (header) {
                    inlineStyles.push(merge(inline(),
                            StyleAttributeMap.builder().setBold(true).build()));
                }
                walkChildren(cell);
                if (header) {
                    inlineStyles.pop();
                }
            }
            endParagraph(paragraphStyle(0));
        }

        private void walkStyled(Node node, StyleAttributeMap style) {
            inlineStyles.push(merge(inline(), style));
            walkChildren(node);
            inlineStyles.pop();
        }

        private void walkChildren(Node node) {
            var child = node.getFirstChild();
            while (child != null) {
                walk(child);
                child = child.getNext();
            }
        }

        private void endParagraph(StyleAttributeMap paragraphStyle) {
            model.setParagraphAttributes(paragraphStyle);
            // The line break is emitted by the next paragraph instead, so the
            // document does not end with an empty paragraph that the text area
            // would render as a blank line.
            paragraphPending = true;
        }

        private void startParagraph() {
            if (paragraphPending) {
                model.nl();
                paragraphPending = false;
            }
        }

        private StyleAttributeMap inline() {
            return inlineStyles.peek();
        }

        private int listDepth() {
            return listDepths.isEmpty() ? 0 : listDepths.peek();
        }

        private static String emojiCharacter(Emoji e) {
            return EmojiData.emojiFromShortName(e.getText().toString())
                    .map(com.gluonhq.emoji.Emoji::character)
                    .orElse("");
        }

        private static StyleAttributeMap merge(StyleAttributeMap first, StyleAttributeMap second) {
            return StyleAttributeMap.builder().merge(first).merge(second).build();
        }

        private static StyleAttributeMap textStyle(Font font) {
            return StyleAttributeMap.builder()
                    .setFontFamily(font.getFamily())
                    .setFontSize(font.getSize())
                    .build();
        }

        private StyleAttributeMap codeSegmentStyle() {
            return merge(inline(), textStyle(style.monospace()));
        }

        private StyleAttributeMap inlineCodeStyle() {
            // The underline and highlight runs are the only per-segment
            // decorations drawn behind the text; the highlight is styled by the
            // .text-highlight-1 rule in the application stylesheet.
            return merge(codeSegmentStyle(),
                    StyleAttributeMap.builder()
                            .set(StyleAttributeMap.TEXT_HIGHLIGHT_1, Boolean.TRUE)
                            .build());
        }

        private static StyleAttributeMap paragraphStyle(int spaceAbove) {
            return StyleAttributeMap.builder()
                    .setTextAlignment(TextAlignment.LEFT)
                    .setSpaceAbove(spaceAbove == 0 ? BLOCK_SPACE : spaceAbove)
                    .setSpaceBelow(0)
                    .setLineSpacing(LINE_SPACING)
                    .build();
        }

        /**
         * A list item whose bullet is applied to the first paragraph it
         * contains, so that a loose item with several paragraphs is not marked
         * more than once. The bullet string carries its own indentation.
         */
        private static final class ListItem {

            private final String bullet;
            private boolean bulletApplied;

            private ListItem(String bullet) {
                this.bullet = bullet;
            }
        }
    }
}
