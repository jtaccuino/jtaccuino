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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds markdown tables for the values notebook outputs are made of.
 *
 * <p>Shared by the collection and array renderers, which describe the same
 * shapes — records spread into columns, or a single column of values — so that
 * an exported table looks the same regardless of whether the source was a
 * {@code Collection} or an array.
 */
final class MarkdownTables {

    private static final Logger LOG = Logger.getLogger(MarkdownTables.class.getName());

    private MarkdownTables() {
    }

    /** One column per record component, one row per record. */
    static String ofRecords(List<?> records) {
        var components = recordComponentsOf(records.getFirst());
        var headers = components.stream().map(RecordComponent::getName).toList();

        var rows = new ArrayList<List<String>>();
        for (var record : records) {
            var row = new ArrayList<String>();
            for (var component : components) {
                row.add(stringOf(componentOf(component, record)));
            }
            rows.add(row);
        }
        return of(headers, rows);
    }

    /** A key column followed by one column per record component of the value. */
    static String ofMapEntriesWithRecordValues(List<? extends Map.Entry<?, ?>> entries) {
        var components = recordComponentsOf(entries.getFirst().getValue());
        var headers = new ArrayList<String>();
        headers.add("Key");
        components.stream().map(RecordComponent::getName).forEach(headers::add);

        var rows = new ArrayList<List<String>>();
        for (var entry : entries) {
            var row = new ArrayList<String>();
            row.add(stringOf(entry.getKey()));
            for (var component : components) {
                row.add(stringOf(componentOf(component, entry.getValue())));
            }
            rows.add(row);
        }
        return of(headers, rows);
    }

    /** Key and value columns. */
    static String ofMapEntries(List<? extends Map.Entry<?, ?>> entries) {
        var rows = new ArrayList<List<String>>();
        for (var entry : entries) {
            rows.add(List.of(stringOf(entry.getKey()), stringOf(entry.getValue())));
        }
        return of(List.of("Key", "Value"), rows);
    }

    /** A single value column, for values that have no structure to spread. */
    static String ofValues(List<?> values) {
        var rows = new ArrayList<List<String>>();
        for (var value : values) {
            rows.add(List.of(stringOf(value)));
        }
        return of(List.of("Value"), rows);
    }

    private static List<RecordComponent> recordComponentsOf(Object record) {
        return List.of(record.getClass().getRecordComponents());
    }

    private static Object componentOf(RecordComponent component, Object record) {
        try {
            return component.getAccessor().invoke(record);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            // a record whose accessor throws must not take the whole export
            // down, so the cell renders as empty rather than aborting
            LOG.log(Level.WARNING, "could not read record component " + component.getName(), ex);
            return "";
        }
    }

    private static String of(List<String> headers, List<List<String>> rows) {
        var markdown = new StringBuilder();
        appendRow(markdown, headers);
        appendRow(markdown, headers.stream().map(header -> "---").toList());
        rows.forEach(row -> appendRow(markdown, row));
        return markdown.toString().stripTrailing();
    }

    private static void appendRow(StringBuilder markdown, List<String> cells) {
        markdown.append('|');
        cells.forEach(cell -> markdown.append(' ').append(escape(cell)).append(" |"));
        markdown.append('\n');
    }

    /**
     * A literal pipe or newline in a value would break the table, so both are
     * escaped. Newlines become spaces because a markdown table cell cannot span
     * lines.
     */
    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private static String stringOf(Object value) {
        return String.valueOf(value);
    }
}
