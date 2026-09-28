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

import java.util.List;
import javafx.scene.text.Font;

/**
 * The fonts a rendered markdown cell uses. Instances are derived from the
 * {@code -markdown-*} styleable properties on the cell, so the look of
 * rendered markdown stays themeable from CSS.
 *
 * @param base the font for ordinary text
 * @param monospace the font for inline and fenced code
 * @param emphasis the font for emphasised text
 * @param strongEmphasis the font for strong text
 * @param strikethrough the font for struck through text
 * @param headings one font per heading level, most significant first
 */
public record MarkdownStyle(
        Font base,
        Font monospace,
        Font emphasis,
        Font strongEmphasis,
        Font strikethrough,
        List<Font> headings) {

    public static final int HEADING_LEVELS = 6;

    public MarkdownStyle {
        headings = List.copyOf(headings);
    }

    /**
     * The font for the given heading level, clamped to the levels this style
     * actually defines so that out-of-range input degrades to the base font.
     */
    public Font heading(int level) {
        var index = Math.clamp(level, 1, HEADING_LEVELS) - 1;
        return index < headings.size() ? headings.get(index) : base;
    }
}
