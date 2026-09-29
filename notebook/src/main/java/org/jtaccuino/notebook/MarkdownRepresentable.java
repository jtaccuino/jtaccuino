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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

/**
 * Offers a markdown rendering of an object that would otherwise only be
 * available as a picture.
 *
 * <p>This is the text counterpart of {@link NodeRenderer}, and it is a service
 * provider interface for the same reason: a renderer for collections has no
 * business living in the notebook model, so implementations are discovered at
 * runtime and a consumer without them simply produces no markdown.
 *
 * <h2>Why this exists</h2>
 * A {@code display(collection)} output is rasterised to a PNG so the IDE can
 * show it. In an exported document that PNG is a picture of a table: it cannot
 * be searched, copied or read by a screen reader, and it does not reflow. Where
 * a renderer can also express the object as markdown, the exporter prefers that
 * and the picture is not used.
 *
 * <h2>Relationship to the NodeRenderer SPI</h2>
 * The two are independent. An object typically has both: the node is what the
 * IDE draws, the markdown is what an export writes. Implementing only one is
 * valid. Returning an empty {@link Optional} means "not mine after all", which
 * lets a renderer decline a value rather than forcing a fallback on it.
 */
public interface MarkdownRepresentable<T> {

    /**
     * Renders the object as markdown.
     *
     * @param object never {@code null}
     * @return the markdown, or empty if this renderer does not handle this
     * particular value
     */
    Optional<String> toMarkdown(T object);

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Descriptor {

        Class<?> type();
    }
}
