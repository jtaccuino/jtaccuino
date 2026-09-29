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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.scene.control.TableView;
import org.jtaccuino.core.ui.FxTestRuntime;
import org.jtaccuino.notebook.MarkdownRepresentations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Covers the markdown table rendering of collections, which is what lets an
 * exported notebook show a readable table rather than a picture of one.
 *
 * <p>The assertions deliberately stay close to the markdown structure — the
 * header row, the separator row, and one row per element — because a table that
 * is merely "some text containing the values" would render as a single run-on
 * paragraph and defeat the purpose.
 */
class CollectionMarkdownRendererTest {

    @BeforeAll
    static void startToolkit() {
        // one test builds the TableView renderer to cross-check the columns
        FxTestRuntime.start();
    }

    record TestRecord(String name, LocalDate birthday) {
    }

    @Test
    void aListOfRecordsBecomesATableWithOneColumnPerComponent() {
        var list = List.of(
                new TestRecord("Hallo", LocalDate.of(1969, Month.SEPTEMBER, 1)),
                new TestRecord("Welt", LocalDate.of(1970, Month.JANUARY, 2)));

        var markdown = new CollectionMarkdownRenderer().toMarkdown(list).orElseThrow();

        assertEquals("""
                | name | birthday |
                | --- | --- |
                | Hallo | 1969-09-01 |
                | Welt | 1970-01-02 |""", markdown);
    }

    @Test
    void theColumnsMatchTheTableViewRenderer() {
        // the IDE and the export must describe the same table; a divergence here
        // would mean the exported document no longer matches what was seen
        var list = List.of(new TestRecord("Hallo", LocalDate.of(1969, Month.SEPTEMBER, 1)));
        var nodeRenderer = new CollectionRenderer().render(list).orElseThrow();

        var markdownHeader = new CollectionMarkdownRenderer().toMarkdown(list).orElseThrow().lines().findFirst().orElseThrow();

        var tableView = (TableView<?>) nodeRenderer;
        tableView.getColumns().forEach(column -> assertTrue(markdownHeader.contains(column.getText()),
                "column " + column.getText() + " is missing from " + markdownHeader));
    }

    @Test
    void aMapEntryListUsesKeyAndValueColumns() {
        var list = List.<Map.Entry<?, ?>>of(Map.entry("Hi", 5.0d), Map.entry("ho", 10.0d));

        var markdown = new CollectionMarkdownRenderer().toMarkdown(list).orElseThrow();

        assertEquals("""
                | Key | Value |
                | --- | --- |
                | Hi | 5.0 |
                | ho | 10.0 |""", markdown);
    }

    @Test
    void aMapWithRecordValuesSpreadsTheRecordIntoColumns() {
        var list = List.<Map.Entry<?, ?>>of(Map.entry("a", new TestRecord("Hallo", LocalDate.of(1969, Month.SEPTEMBER, 1))));

        var markdown = new CollectionMarkdownRenderer().toMarkdown(list).orElseThrow();

        assertTrue(markdown.startsWith("| Key | name | birthday |"), markdown);
        assertTrue(markdown.contains("| a | Hallo | 1969-09-01 |"), markdown);
    }

    @Test
    void aPlainListOfValuesBecomesASingleColumn() {
        var markdown = new CollectionMarkdownRenderer().toMarkdown(List.of("a", "b")).orElseThrow();

        assertEquals("""
                | Value |
                | --- |
                | a |
                | b |""", markdown);
    }

    @Test
    void anEmptyCollectionIsDeclinedRatherThanRenderedAsAnEmptyTable() {
        // there is nothing to describe, so the caller should fall back to
        // whatever it has rather than being handed a header with no rows
        assertTrue(new CollectionMarkdownRenderer().toMarkdown(List.of()).isEmpty());
    }

    @Test
    void pipesAndNewlinesInValuesCannotBreakTheTable() {
        // a value containing a pipe would otherwise split the row into extra
        // columns and corrupt every following line
        var markdown = new CollectionMarkdownRenderer().toMarkdown(List.of("a|b", "c\nd")).orElseThrow();

        assertTrue(markdown.contains("| a\\|b |"), markdown);
        assertTrue(markdown.contains("| c d |"), markdown);
        assertEquals(4, markdown.lines().count(), markdown);
    }

    @Test
    void theRendererIsDiscoverableThroughTheServiceLoader() {
        // the point of the SPI: the exporter finds renderers without knowing
        // that this class exists
        var markdown = MarkdownRepresentations.of(List.of("only"));

        assertTrue(markdown.isPresent(), "no renderer was discovered for a List");
        assertTrue(markdown.orElseThrow().contains("only"), markdown.orElseThrow());
    }

    @Test
    void atMostOneRendererHandlesACollectionThroughTheSpi() {
        // matching is by assignability, so a subtype renderer could otherwise
        // shadow the collection renderer or vice versa
        var markdown = MarkdownRepresentations.of(List.of(1, 2, 3)).orElseThrow();

        assertFalse(markdown.contains("| --- |\n| --- |"), "two tables were concatenated: " + markdown);
        // one header, one separator and three value rows
        assertEquals(5, markdown.lines().count(), markdown);
    }

    @Test
    void anObjectWithNoRendererYieldsNothing() {
        assertEquals(Optional.empty(), MarkdownRepresentations.of(new Object()));
    }

    @Test
    void anIntArrayBecomesAOneColumnTable() {
        // mirrors IntArrayRenderer, which shows it as a list
        var markdown = MarkdownRepresentations.of(new int[]{1, 2, 3}).orElseThrow();

        assertEquals("""
                | Value |
                | --- |
                | 1 |
                | 2 |
                | 3 |""", markdown);
    }

    @Test
    void aDoubleArrayBecomesAOneColumnTable() {
        var markdown = MarkdownRepresentations.of(new double[]{1.5, 2.5}).orElseThrow();

        assertTrue(markdown.startsWith("| Value |"), markdown);
        assertTrue(markdown.contains("| 1.5 |"), markdown);
        assertTrue(markdown.contains("| 2.5 |"), markdown);
    }

    @Test
    void aLongArrayBecomesAOneColumnTable() {
        var markdown = MarkdownRepresentations.of(new long[]{7L}).orElseThrow();

        assertEquals("""
                | Value |
                | --- |
                | 7 |""", markdown);
    }

    @Test
    void anObjectArrayOfRecordsSpreadsIntoColumns() {
        // mirrors ObjectArrayRenderer, which spreads records the same way
        var markdown = MarkdownRepresentations.of(new Object[]{new TestRecord("Hallo", LocalDate.of(1969, Month.SEPTEMBER, 1))})
                .orElseThrow();

        assertTrue(markdown.startsWith("| name | birthday |"), markdown);
        assertTrue(markdown.contains("| Hallo | 1969-09-01 |"), markdown);
    }

    @Test
    void anObjectArrayOfValuesBecomesAOneColumnTable() {
        var markdown = MarkdownRepresentations.of(new Object[]{"a", "b"}).orElseThrow();

        assertEquals("""
                | Value |
                | --- |
                | a |
                | b |""", markdown);
    }

    @Test
    void anEmptyArrayIsDeclined() {
        // same reasoning as an empty collection: a header with no rows says
        // nothing, and the caller should fall back instead
        assertTrue(MarkdownRepresentations.of(new int[0]).isEmpty());
        assertTrue(MarkdownRepresentations.of(new Object[0]).isEmpty());
    }

    @Test
    void nothingAtAllYieldsNothing() {
        assertEquals(Optional.empty(), MarkdownRepresentations.of(null));
    }
}
