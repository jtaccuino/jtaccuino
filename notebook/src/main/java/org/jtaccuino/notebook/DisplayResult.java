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
package org.jtaccuino.notebook;

import java.util.Objects;
import javafx.scene.Node;

/**
 * What a call to {@code display(...)} resolved to.
 *
 * <p>Most objects become a {@linkplain Graphical graphical} result, which is
 * rasterised to PNG exactly as before. An object that cannot be rendered
 * becomes a {@linkplain Markdown markdown} result instead, because
 * rasterising a paragraph of text into a PNG would leave the notebook holding
 * an unsearchable image where a sentence was meant to be.
 *
 * <p>The IDE renders both: a node directly, markdown through its markdown
 * renderer. A headless export rasterises only the graphical ones and keeps the
 * markdown as text.
 */
public sealed interface DisplayResult {

    /** The object that was passed to {@code display}, kept for export-time derivation. */
    Object displayed();

    /**
     * A node the output can be rasterised from.
     *
     * @param node never {@code null}
     */
    record Graphical(Node node, Object displayed) implements DisplayResult {

        public Graphical {
            Objects.requireNonNull(node, "node");
        }
    }

    /**
     * Text the output should be rendered from, as markdown.
     *
     * @param markdown never {@code null}, may be empty
     */
    record Markdown(String markdown, Object displayed) implements DisplayResult {

        public Markdown {
            Objects.requireNonNull(markdown, "markdown");
        }
    }
}
