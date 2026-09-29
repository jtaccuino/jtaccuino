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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the image handling a command line export exposes: which format is
 * chosen, and whether images are inline or sidecar files.
 *
 * <p>The displayed object is deliberately absent here. These are stored
 * representations, which is what a notebook loaded from disk holds, so the tests
 * need no renderers and live in this module.
 */
class MarkdownExporterImageOptionsTest {

    @TempDir
    private File tempDir;

    @Test
    void extractingImagesWritesASidecarAndReferencesIt() throws Exception {
        var pngBytes = new byte[]{9, 8, 7, 6};
        var cell = cellWith(Map.of("image/png", Base64.getEncoder().encodeToString(pngBytes)));
        var target = new File(tempDir, "notebook.md");

        var result = MarkdownNotebookExporter.export(List.of(cell), target, null,
                ExportOptions.defaults().withExtractImages(true));

        var reference = "notebook_files/" + cell.getId() + "-0.png";
        assertTrue(result.markdown().contains("(" + reference + ")"), result.markdown());
        assertFalse(result.markdown().contains("data:image/png"), result.markdown());

        var written = new File(new File(tempDir, "notebook_files"), cell.getId() + "-0.png");
        assertTrue(written.exists(), "the sidecar image was not written");
        assertArrayEquals(pngBytes, Files.readAllBytes(written.toPath()));
    }

    @Test
    void inlineIsTheDefaultAndLeavesNoFiles() throws Exception {
        var cell = cellWith(Map.of("image/png", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})));
        var target = new File(tempDir, "inline.md");

        MarkdownNotebookExporter.export(List.of(cell), target, null, ExportOptions.defaults());

        assertTrue(new File(tempDir, "inline_files").exists() == false, "no sidecar directory should be created");
    }

    @Test
    void imageFormatPngIgnoresAStoredSvg() throws Exception {
        var cell = cellWith(Map.of(
                "image/png", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}),
                "image/svg+xml", Base64.getEncoder().encodeToString("<svg/>".getBytes(StandardCharsets.UTF_8))));

        var result = MarkdownNotebookExporter.render(List.of(cell), null,
                ExportOptions.defaults().withImageFormat(ImageFormat.PNG));

        assertTrue(result.markdown().contains("data:image/png"), result.markdown());
        assertFalse(result.markdown().contains("data:image/svg+xml"), result.markdown());
    }

    @Test
    void imageFormatBestPrefersAStoredSvg() throws Exception {
        var cell = cellWith(Map.of(
                "image/png", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}),
                "image/svg+xml", Base64.getEncoder().encodeToString("<svg/>".getBytes(StandardCharsets.UTF_8))));

        var result = MarkdownNotebookExporter.render(List.of(cell), null,
                ExportOptions.defaults().withImageFormat(ImageFormat.BEST));

        assertTrue(result.markdown().contains("data:image/svg+xml"), result.markdown());
        assertFalse(result.markdown().contains("data:image/png"), result.markdown());
    }

    @Test
    void imageFormatSvgFallsBackToPngWithAWarningNamingTheCell() throws Exception {
        // nothing on this classpath provides SVG, and the notebook holds only a
        // PNG, so the export still completes but says which cell it could not
        // satisfy
        var cell = cellWith(Map.of("image/png", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})));

        var result = MarkdownNotebookExporter.render(List.of(cell), null,
                ExportOptions.defaults().withImageFormat(ImageFormat.SVG));

        assertTrue(result.markdown().contains("data:image/png"), result.markdown());
        assertEquals(1, result.warnings().size(), result.warnings().toString());
        assertTrue(result.warnings().getFirst().contains(cell.getId().toString()), result.warnings().toString());
    }

    @Test
    void imageFormatSvgDoesNotWarnWhenItSucceeds() throws Exception {
        // a bundle that already carries an SVG satisfies the request, so there
        // is nothing to warn about
        var cell = cellWith(Map.of(
                "image/svg+xml", Base64.getEncoder().encodeToString("<svg/>".getBytes(StandardCharsets.UTF_8))));

        var result = MarkdownNotebookExporter.render(List.of(cell), null,
                ExportOptions.defaults().withImageFormat(ImageFormat.SVG));

        assertTrue(result.markdown().contains("data:image/svg+xml"), result.markdown());
        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
    }

    @Test
    void imageFormatSvgDoesNotWarnAboutATextOnlyOutput() throws Exception {
        // a plain result is not an image, so requesting SVG says nothing about
        // it; warning here would flag every cell in a notebook as affected
        var cell = cellWith(Map.of("text/plain", "int: 42"));

        var result = MarkdownNotebookExporter.render(List.of(cell), null,
                ExportOptions.defaults().withImageFormat(ImageFormat.SVG));

        assertTrue(result.markdown().contains("int: 42"), result.markdown());
        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
    }

    private static CellData cellWith(Map<String, String> mimeBundle) {
        return CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, mimeBundle)));
    }
}
