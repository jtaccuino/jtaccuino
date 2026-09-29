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
import org.jtaccuino.notebook.SvgRepresentations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the SVG branch of the export precedence, using a test-only provider.
 *
 * <p>Nothing in production offers SVG yet, so without a provider here the branch
 * would be untestable until a real charting integration lands. A test provider
 * keeps the precedence — markdown, SVG, then the stored representation — honest
 * in the meantime.
 */
class SvgExportTest {

    @TempDir
    private File tempDir;

    @Test
    void theProviderIsDiscoverableThroughTheServiceLoader() {
        var svg = SvgRepresentations.of(new TestSvgSubject(10));

        assertTrue(svg.isPresent(), "the test provider was not discovered");
        assertTrue(svg.orElseThrow().contains("width=\"10\""), svg.orElseThrow());
    }

    @Test
    void anObjectNothingCanDrawYieldsNothing() {
        assertTrue(SvgRepresentations.of(new Object()).isEmpty());
        assertTrue(SvgRepresentations.of(null).isEmpty());
    }

    @Test
    void svgIsUsedInsteadOfTheStoredPng() throws Exception {
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cell = CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png))));

        var markdown = export(cell, new TestSvgSubject(42));

        assertTrue(markdown.contains("data:image/svg+xml;base64,"), markdown);
        assertFalse(markdown.contains("data:image/png"), "the PNG should have been superseded: " + markdown);
    }

    @Test
    void theSvgIsCarriedAsADataUriSoTheDocumentStaysSelfContained() throws Exception {
        var cell = CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                        Map.of("image/png", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})))));

        var markdown = export(cell, new TestSvgSubject(7));

        var expected = Base64.getEncoder().encodeToString("<svg width=\"7\"></svg>".getBytes(StandardCharsets.UTF_8));
        assertTrue(markdown.contains(expected), "expected the svg base64 to be embedded: " + markdown);
    }

    @Test
    void markdownStillWinsOverSvg() throws Exception {
        // an object that has both must export as text: it stays searchable and
        // reflows, which scaling does not give you
        var cell = CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                        Map.of("image/png", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})))));

        // a collection has markdown but no svg in this test setup
        var markdown = export(cell, List.of("a", "b"));

        assertTrue(markdown.contains("| Value |"), markdown);
        assertFalse(markdown.contains("data:image/svg+xml"), markdown);
        assertFalse(markdown.contains("data:image/png"), markdown);
    }

    private String export(CellData cell, Object displayed) throws Exception {
        var target = new File(tempDir, "out.md");
        MarkdownNotebookExporter.export(List.of(cell), target, registry(cell, List.of(displayed)));
        return Files.readString(target.toPath(), StandardCharsets.UTF_8);
    }

    private static DisplayRegistry registry(CellData cell, List<Object> displayed) {
        return queried -> Objects.equals(queried, cell) ? displayed : List.of();
    }
}
