/*
 * Copyright 2024 JTaccuino Contributors
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
package org.jtaccuino.rta;

import com.gluonhq.emoji.EmojiData;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextDecoration;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.emoji.Emoji;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.scene.paint.Color;
import javafx.scene.text.FontPosture;
import static javafx.scene.text.FontWeight.BOLD;
import javafx.scene.text.TextAlignment;

public class MdUtils {

    private MdUtils() {
        // prevent instantiation
    }

    public static Document render(String text) {
        MutableDataSet options = new MutableDataSet();

        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                EmojiExtension.create(),
                AttributesExtension.create()));
        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        Parser parser = Parser.builder(options).build();

        // You can re-use parser and renderer instances
        com.vladsch.flexmark.util.ast.Document document = parser.parse(text);

        final List<DecorationModel> decorationList = new ArrayList<>();
        final StringBuilder theText = new StringBuilder();
        final ParagraphDecoration presetParagraphDecoration = ParagraphDecoration.builder().presets().build();
        final TextDecoration headingOneTextDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontWeight(BOLD).fontSize(20).build();
        final ParagraphDecoration headingOneParagraph = ParagraphDecoration.builder().presets().alignment(TextAlignment.LEFT).topInset(0).bottomInset(4).build();
        final TextDecoration headingTwoTextDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontWeight(BOLD).fontSize(18).build();
        final ParagraphDecoration headingTwoParagraph = ParagraphDecoration.builder().presets().alignment(TextAlignment.LEFT).topInset(5).bottomInset(2).build();
        final TextDecoration headingThreeTextDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontWeight(BOLD).fontSize(16).build();
        final ParagraphDecoration headingThreeParagraph = ParagraphDecoration.builder().presets().alignment(TextAlignment.LEFT).topInset(5).bottomInset(0).build();
        final TextDecoration headingFourTextDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontWeight(BOLD).fontSize(14).build();
        final ParagraphDecoration headingFourParagraph = ParagraphDecoration.builder().presets().alignment(TextAlignment.LEFT).topInset(5).bottomInset(0).build();
        final TextDecoration headingFiveTextDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontWeight(BOLD).fontSize(12).build();
        final ParagraphDecoration headingFiveParagraph = ParagraphDecoration.builder().presets().alignment(TextAlignment.LEFT).topInset(5).bottomInset(0).build();
        final TextDecoration.Builder presetTextDecorationBuilder = TextDecoration.builder().presets().fontFamily("Arial").fontSize(12);
        final TextDecoration presetTextDecoration = presetTextDecorationBuilder.build();
        final TextDecoration monospaceDecoration = TextDecoration.builder().presets().fontFamily("Monospace").background(Color.GAINSBORO.toString()).foreground(Color.BLACK.toString()).build();
        final TextDecoration emphasisDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontPosture(FontPosture.ITALIC).build();
        final TextDecoration strongEmphasisDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontWeight(BOLD).build();
        @SuppressWarnings("UnusedVariable") // TODO: Check if it is used and remove if not
        final TextDecoration bothEmphasisDecoration = TextDecoration.builder().presets().fontFamily("Arial").fontPosture(FontPosture.ITALIC).fontWeight(BOLD).build();
        final TextDecoration strikethroughDecoration = TextDecoration.builder().presets().strikethrough(true).fontFamily("Arial").build();

        final ParagraphDecoration bulletItemLevelOneDecoration = ParagraphDecoration.builder().presets()
                .graphicType(ParagraphDecoration.GraphicType.BULLETED_LIST)
                .indentationLevel(1)
                .build();
        final ParagraphDecoration bulletItemLevelTwoDecoration = ParagraphDecoration.builder().presets()
                .graphicType(ParagraphDecoration.GraphicType.BULLETED_LIST)
                .indentationLevel(2)
                .build();
        final ParagraphDecoration bulletItemLevelThreeDecoration = ParagraphDecoration.builder().presets()
                .graphicType(ParagraphDecoration.GraphicType.BULLETED_LIST)
                .indentationLevel(3)
                .build();
        final ParagraphDecoration orderedItemLevelOneDecoration = ParagraphDecoration.builder().presets()
                .graphicType(ParagraphDecoration.GraphicType.NUMBERED_LIST)
                .indentationLevel(1)
                .build();
        final ParagraphDecoration orderedItemLevelTwoDecoration = ParagraphDecoration.builder().presets()
                .graphicType(ParagraphDecoration.GraphicType.NUMBERED_LIST)
                .indentationLevel(2)
                .build();
        final ParagraphDecoration orderedItemLevelThreeDecoration = ParagraphDecoration.builder().presets()
                .graphicType(ParagraphDecoration.GraphicType.NUMBERED_LIST)
                .indentationLevel(3)
                .build();

        NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
            private int bulletListLevel = 0;
            private int orderedListLevel = 0;

            String indentation = " ";

            Deque<ParagraphDecoration> paragraphDecorations = new ArrayDeque<>();
            Deque<TextDecoration> textDecorations = new ArrayDeque<>();

            @Override
            protected void processNode(com.vladsch.flexmark.util.ast.Node node, boolean withChildren, BiConsumer<com.vladsch.flexmark.util.ast.Node, Visitor<com.vladsch.flexmark.util.ast.Node>> processor) {
                indentation = indentation + " ";
                switch (node) {
                    case com.vladsch.flexmark.util.ast.Document d -> {
                        paragraphDecorations.push(presetParagraphDecoration);
                        textDecorations.push(presetTextDecoration);
                        visitChildren(d);
                    }
                    case Heading h when h.getLevel() == 1 -> {
                        paragraphDecorations.push(headingOneParagraph);
                        textDecorations.push(headingOneTextDecoration);
                        visitChildren(h);
                        textDecorations.pop();
                        paragraphDecorations.pop();
                        var start = theText.length();
                        theText.append("\n");
                        decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                    }
                    case Heading h when h.getLevel() == 2 -> {
                        paragraphDecorations.push(headingTwoParagraph);
                        textDecorations.push(headingTwoTextDecoration);
                        visitChildren(h);
                        paragraphDecorations.pop();
                        textDecorations.pop();
                        var start = theText.length();
                        theText.append("\n");
                        decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                    }
                    case Heading h when h.getLevel() == 3 -> {
                        paragraphDecorations.push(headingThreeParagraph);
                        textDecorations.push(headingThreeTextDecoration);
                        visitChildren(h);
                        paragraphDecorations.pop();
                        textDecorations.pop();
                        var start = theText.length();
                        theText.append("\n");
                        decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                    }
                    case Heading h when h.getLevel() == 4 -> {
                        paragraphDecorations.push(headingFourParagraph);
                        textDecorations.push(headingFourTextDecoration);
                        visitChildren(h);
                        paragraphDecorations.pop();
                        textDecorations.pop();
                        var start = theText.length();
                        theText.append("\n");
                        decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                    }
                    case Heading h when h.getLevel() == 5 -> {
                        paragraphDecorations.push(headingFiveParagraph);
                        textDecorations.push(headingFiveTextDecoration);
                        visitChildren(h);
                        paragraphDecorations.pop();
                        textDecorations.pop();
                        var start = theText.length();
                        theText.append("\n");
                        decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                    }
                    case Paragraph p -> {
                        visitChildren(p);
                        if (!p.getParent().isOrDescendantOfType(com.vladsch.flexmark.util.ast.Document.class) || !p.getParent().getLastChild().equals(p)) {
                            var start = theText.length();
                            theText.append("\n");
                            decorationList.add(new DecorationModel(start, 1, presetTextDecoration, paragraphDecorations.peek()));
                        }
                    }
                    case Code c -> {
                        var start = theText.length();
                        var text = c.getText();
                        var length = text.length();
                        theText.append(text);
                        decorationList.add(new DecorationModel(start, length, monospaceDecoration, presetParagraphDecoration));
                    }
                    case Emphasis e -> {
                        var start = theText.length();
                        var text = e.getText();
                        var length = text.length();
                        theText.append(text);
                        decorationList.add(new DecorationModel(start, length, emphasisDecoration, presetParagraphDecoration));
                    }
                    case StrongEmphasis e -> {
                        var start = theText.length();
                        var text = e.getText();
                        var length = text.length();
                        theText.append(text);
                        decorationList.add(new DecorationModel(start, length, strongEmphasisDecoration, presetParagraphDecoration));
                    }
                    case Strikethrough e -> {
                        var start = theText.length();
                        var text = e.getText();
                        var length = text.length();
                        theText.append(text);
                        decorationList.add(new DecorationModel(start, length, strikethroughDecoration, presetParagraphDecoration));
                    }
                    case Emoji e -> {
                        var start = theText.length();
                        var emojiAsString = EmojiData.emojiFromShortName(e.getText().toString()).map(com.gluonhq.emoji.Emoji::character).orElse("");
                        var length = emojiAsString.length();
                        theText.append(emojiAsString);
                        decorationList.add(new DecorationModel(start, length, presetTextDecoration, presetParagraphDecoration));
                    }
                    case Text t -> {
                        var start = theText.length();
                        var text = t.getChars().toString();
                        var length = text.length();
                        theText.append(text);
                        decorationList.add(new DecorationModel(start, length, textDecorations.peek(), paragraphDecorations.peek()));
                    }
                    case BulletList bl -> {
                        bulletListLevel++;
                        if (!theText.toString().endsWith("\n")) {
                            var start = theText.length();
                            theText.append("\n");
                            decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                        }
                        switch (bulletListLevel) {
                            case 1 ->
                                paragraphDecorations.push(bulletItemLevelOneDecoration);
                            case 2 ->
                                paragraphDecorations.push(bulletItemLevelTwoDecoration);
                            case 3 ->
                                paragraphDecorations.push(bulletItemLevelThreeDecoration);
                            default ->
                                paragraphDecorations.push(bulletItemLevelThreeDecoration);
                        }
                        visitChildren(bl);

                        paragraphDecorations.pop();
                        bulletListLevel--;
                        if (0 == bulletListLevel && !bl.getParent().getLastChild().equals(bl)) {
                            var pStart = theText.length();
                            theText.append("\n");
                            decorationList.add(new DecorationModel(pStart, 1, presetTextDecoration, presetParagraphDecoration));
                        }
                    }
                    case OrderedList ol -> {
                        orderedListLevel++;
                        if (!theText.toString().endsWith("\n")) {
                            var start = theText.length();
                            theText.append("\n");
                            decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                        }
                        switch (orderedListLevel) {
                            case 1 ->
                                paragraphDecorations.push(orderedItemLevelOneDecoration);
                            case 2 ->
                                paragraphDecorations.push(orderedItemLevelTwoDecoration);
                            case 3 ->
                                paragraphDecorations.push(orderedItemLevelThreeDecoration);
                            default ->
                                paragraphDecorations.push(orderedItemLevelThreeDecoration);
                        }
                        visitChildren(ol);

                        paragraphDecorations.pop();
                        orderedListLevel--;
                        if (0 == orderedListLevel && !ol.getParent().getLastChild().equals(ol)) {
                            var pStart = theText.length();
                            theText.append("\n");
                            decorationList.add(new DecorationModel(pStart, 1, presetTextDecoration, presetParagraphDecoration));
                        }
                    }
                    case BulletListItem bli -> {
                        visitChildren(bli);
                        var start = theText.length();
                        if (!bli.getParent().getLastChild().equals(bli)) {
                            theText.append("\n");
                            decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                        }
                    }
                    case OrderedListItem oli -> {
                        visitChildren(oli);
                        var start = theText.length();
                        if (!oli.getParent().getLastChild().equals(oli)) {
                            theText.append("\n");
                            decorationList.add(new DecorationModel(start, 1, presetTextDecoration, presetParagraphDecoration));
                        }
                    }
                    case FencedCodeBlock fcb -> {
                        var pd = ParagraphDecoration.builder().fromDecoration(presetParagraphDecoration).indentationLevel(fcb.getFenceIndent()).build();
                        paragraphDecorations.push(pd);
                        textDecorations.push(monospaceDecoration);
                        visitChildren(fcb);
                        paragraphDecorations.pop();
                        textDecorations.pop();
                    } case Link l -> {
                        var url = l.getUrl();
                        var td = presetTextDecorationBuilder.url(url.toString()).build();
                        textDecorations.push(td);
                        visitChildren(l);
                        textDecorations.pop();
                    }
                    default -> {
                        visitChildren(node);
                    }
                }
                indentation = indentation.substring(0, indentation.length() - 1);
            }
        };
        visitor.visit(document);
        return new Document(theText.toString(), decorationList, text.length());
    }
}
