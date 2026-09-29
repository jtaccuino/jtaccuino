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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jtaccuino.notebook.CellData;
import org.jtaccuino.notebook.DisplayRegistry;
import org.jtaccuino.notebook.MarkdownNotebookExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the join between the exporter and a renderer that only exists here.
 *
 * <p>These tests live in {@code ui} rather than next to the exporter because
 * {@code notebook} deliberately has no renderers on its classpath: it is the
 * consumer of the SPI, and {@code CollectionMarkdownRenderer} is a provider.
 * Testing it from the notebook module would either fail or, worse, force a
 * dependency that undoes the separation.
 *
 * <p>What is being established is the whole point of the SPI: a notebook holds a
 * picture of a table, the registry holds the object that was displayed, and the
 * export replaces the picture with a table.
 */
class CollectionMarkdownExportTest {

    @TempDir
    private File tempDir;

    @Test
    void aDisplayedCollectionExportsAsATableWhenTheLiveObjectIsKnown() throws Exception {
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cell = CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png))));
        var displayed = List.of(Map.entry("only", 1.0d));

        var markdown = export(List.of(cell), new FixedRegistry(cell, List.of(displayed)));

        assertTrue(markdown.contains("| Key | Value |"), markdown);
        assertTrue(markdown.contains("| only | 1.0 |"), markdown);
        assertFalse(markdown.contains("data:image/png"),
                "the picture should not be used when the table is available: " + markdown);
    }

    @Test
    void withoutTheRegistryTheStoredRepresentationIsUsed() throws Exception {
        // a notebook loaded from disk, or exported in a process that did not
        // execute it, has no live objects and must still export
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cell = CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png))));

        var markdown = export(List.of(cell), new FixedRegistry(cell, List.of()));

        assertTrue(markdown.contains("data:image/png;base64," + png), markdown);
    }

    @Test
    void aDisplayedObjectNoRendererHandlesFallsBackToTheStoredRepresentation() throws Exception {
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cell = CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png))));

        var markdown = export(List.of(cell), new FixedRegistry(cell, List.of(new Object())));

        assertTrue(markdown.contains("data:image/png;base64," + png), markdown);
    }

    @Test
    void displayedObjectsArePairedWithDisplayOutputsAndNotWithAllOutputs() throws Exception {
        // a cell can print and display; the stream output must not shift the
        // pairing between display outputs and displayed objects
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cell = CellData.of(CellData.Type.CODE, "println(); display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.EXECUTION_DATA, "printed\n"),
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png))));

        var markdown = export(List.of(cell), new FixedRegistry(cell, List.of(List.of(Map.entry("k", "v")))));

        assertTrue(markdown.contains("printed"), markdown);
        assertTrue(markdown.contains("| k | v |"), markdown);
        assertFalse(markdown.contains("data:image/png"), markdown);
    }

    @Test
    void severalDisplayedObjectsEachGetTheirOwnRendering() throws Exception {
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cell = CellData.of(CellData.Type.CODE, "display(a); display(b);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png)),
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png))));

        var markdown = export(List.of(cell),
                new FixedRegistry(cell, List.of(List.of("first"), List.of("second"))));

        assertTrue(markdown.contains("| first |"), markdown);
        assertTrue(markdown.contains("| second |"), markdown);
        assertFalse(markdown.contains("data:image/png"), markdown);
    }

    private String export(List<CellData> cells, DisplayRegistry registry) throws Exception {
        var target = new File(tempDir, "out.md");
        MarkdownNotebookExporter.export(cells, target, registry);
        return Files.readString(target.toPath(), StandardCharsets.UTF_8);
    }

    /** A registry that answers for one cell. */
    private record FixedRegistry(CellData cell, List<Object> displayed) implements DisplayRegistry {

        @Override
        public List<Object> displayedIn(CellData queried) {
            // CellData does not override equals, so this is identity equality,
            // which is what the registry is keyed on
            return Objects.equals(queried, cell) ? displayed : List.of();
        }
    }
}
