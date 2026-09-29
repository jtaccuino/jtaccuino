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
package org.jtaccuino.core.ui.renderer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jtaccuino.notebook.MarkdownRepresentable;
import org.jtaccuino.notebook.MarkdownRepresentable.Descriptor;

/**
 * Renders a collection as a markdown table, so that an exported notebook shows
 * a readable table instead of a picture of one. The IDE still shows the
 * {@code TableView} from {@link CollectionRenderer}; this is the text form.
 *
 * <p>The three shapes mirror {@link CollectionRenderer} exactly, so the exported
 * table describes the same rows and columns the IDE displayed.
 */
@Descriptor(type = Collection.class)
public class CollectionMarkdownRenderer implements MarkdownRepresentable<Collection<?>> {

    @Override
    public Optional<String> toMarkdown(Collection<?> collection) {
        if (collection.isEmpty()) {
            // nothing to describe, so decline rather than emit a header with no
            // rows; the caller falls back to the stored representation
            return Optional.empty();
        }
        var first = collection.iterator().next();
        if (first instanceof Map.Entry<?, ?> entry) {
            return Optional.of(mapEntriesToTable(collection, entry));
        }
        if (null != first && first.getClass().isRecord()) {
            return Optional.of(MarkdownTables.ofRecords(List.copyOf(collection)));
        }
        // a plain collection of values has no columns to speak of, so a one
        // column table is the honest rendering
        return Optional.of(MarkdownTables.ofValues(List.copyOf(collection)));
    }

    @SuppressWarnings("unchecked")
    private static String mapEntriesToTable(Collection<?> collection, Map.Entry<?, ?> first) {
        var entries = (List<? extends Map.Entry<?, ?>>) List.copyOf(collection);
        return null != first.getValue() && first.getValue().getClass().isRecord()
                ? MarkdownTables.ofMapEntriesWithRecordValues(entries)
                : MarkdownTables.ofMapEntries(entries);
    }
}
