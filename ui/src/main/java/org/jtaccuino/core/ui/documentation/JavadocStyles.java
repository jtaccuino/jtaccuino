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

import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Central style tokens for the javadoc preview popup. The renderers build
 * {@link StyleAttributeMap}s from these constants so the whole popup has a
 * consistent visual hierarchy: a muted enclosing type, a bold member
 * signature, section labels in an accent color, comfortably spaced body
 * paragraphs, and code-like tokens set off with a light background.
 */
final class JavadocStyles {

    static final String MONOSPACE_FAMILY = "Monaspace Argon";

    private static final Color TYPE_COLOR = Color.rgb(0x60, 0x60, 0x60);
    private static final Color SECTION_COLOR = Color.rgb(0x1E, 0x4E, 0x79);
    private static final Color CODE_BACKGROUND = Color.rgb(0xF0, 0xF2, 0xF4);

    static final double TYPE_FONT_SIZE = 11;
    static final double SIGNATURE_FONT_SIZE = 13;
    static final double BODY_FONT_SIZE = 13;
    static final double CODE_FONT_SIZE = 12;

    static final double SECTION_SPACE_ABOVE = 12;
    static final double BODY_SPACE_ABOVE = 8;
    static final double ITEM_SPACE_ABOVE = 2;
    static final double LINE_SPACING = 3;
    static final double ITEM_INDENT = 24;

    private JavadocStyles() {
        // prevent instantiation
    }

    static StyleAttributeMap typeStyle() {
        return StyleAttributeMap.builder()
                .setFontFamily(MONOSPACE_FAMILY)
                .setFontSize(TYPE_FONT_SIZE)
                .setTextColor(TYPE_COLOR)
                .build();
    }

    static StyleAttributeMap signatureStyle() {
        return StyleAttributeMap.builder()
                .setFontSize(SIGNATURE_FONT_SIZE)
                .setBold(true)
                .build();
    }

    static StyleAttributeMap bodyStyle() {
        return StyleAttributeMap.builder().setFontSize(BODY_FONT_SIZE).build();
    }

    static StyleAttributeMap sectionLabelStyle() {
        return StyleAttributeMap.builder()
                .setFontSize(BODY_FONT_SIZE)
                .setBold(true)
                .setTextColor(SECTION_COLOR)
                .build();
    }

    static StyleAttributeMap codeStyle() {
        return StyleAttributeMap.builder()
                .setFontFamily(MONOSPACE_FAMILY)
                .setFontSize(CODE_FONT_SIZE)
                .setBackground(CODE_BACKGROUND)
                .build();
    }

    static StyleAttributeMap bodyParagraphStyle() {
        return StyleAttributeMap.builder()
                .setSpaceAbove(BODY_SPACE_ABOVE)
                .setLineSpacing(LINE_SPACING)
                .build();
    }

    static StyleAttributeMap sectionParagraphStyle() {
        return StyleAttributeMap.builder()
                .setSpaceAbove(SECTION_SPACE_ABOVE)
                .setLineSpacing(LINE_SPACING)
                .build();
    }

    static StyleAttributeMap itemParagraphStyle() {
        return StyleAttributeMap.builder()
                .setSpaceAbove(ITEM_SPACE_ABOVE)
                .setSpaceLeft(ITEM_INDENT)
                .setLineSpacing(LINE_SPACING)
                .build();
    }

    static StyleAttributeMap itemStartStyle() {
        return StyleAttributeMap.builder()
                .setBullet("\u2022")
                .setSpaceLeft(ITEM_INDENT)
                .build();
    }
}
