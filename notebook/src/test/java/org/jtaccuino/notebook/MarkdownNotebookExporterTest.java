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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MarkdownNotebookExporterTest {

    @TempDir
    private File tempDir;

    @Test
    void exportsMarkdownAndPlainTextCells() throws Exception {
        var cells = List.of(
                CellData.of(CellData.Type.MARKDOWN, "# Title"),
                CellData.of(CellData.Type.CODE, "System.out.println(\"hi\");", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.EXECUTION_DATA, Map.of("text/plain", "42"))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("# Title"));
        Assertions.assertTrue(markdown.contains("```java"));
        Assertions.assertTrue(markdown.contains("System.out.println(\"hi\");"));
        Assertions.assertTrue(markdown.contains("```"));
        Assertions.assertTrue(markdown.contains("42"));
    }

    @Test
    void exportsPngAndSvgOutputsAsDataUris() throws Exception {
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var svg = Base64.getEncoder().encodeToString("<svg/>".getBytes(StandardCharsets.UTF_8));
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "display(new Object());", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", png)),
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/svg+xml", svg))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("!["));
        Assertions.assertTrue(markdown.contains("](data:image/png;base64," + png + ")"));
        Assertions.assertTrue(markdown.contains("](data:image/svg+xml;base64," + svg + ")"));
    }

    @Test
    void exportsMarkdownOutputAsMarkdownAndNotAsAFencedBlock() throws Exception {
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                                Map.of("text/markdown", "a **bold** sentence"))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("a **bold** sentence"),
                "markdown output should survive as markdown, was: " + markdown);
        Assertions.assertFalse(markdown.contains("```\na **bold** sentence"),
                "markdown output must not be wrapped in a fenced code block: " + markdown);
    }

    @Test
    void anOutputWithSeveralRepresentationsEmitsOnlyOne() throws Exception {
        // a bundle often carries a picture and a text fallback of the same
        // thing; emitting both would show the output twice
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                                Map.of("image/png", png, "text/plain", "fallback"))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("](data:image/png;base64," + png + ")"), markdown);
        Assertions.assertFalse(markdown.contains("fallback"),
                "the lower ranked representation should not also appear: " + markdown);
    }

    @Test
    void markdownIsPreferredOverAPictureOfTheSameThing() throws Exception {
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                                Map.of("image/png", png, "text/markdown", "| a | b |"))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("| a | b |"), markdown);
        Assertions.assertFalse(markdown.contains("data:image/png"), markdown);
    }

    @Test
    void svgIsPreferredOverPng() throws Exception {
        var png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        var svg = Base64.getEncoder().encodeToString("<svg/>".getBytes(StandardCharsets.UTF_8));
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                                Map.of("image/png", png, "image/svg+xml", svg))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("data:image/svg+xml"), markdown);
        Assertions.assertFalse(markdown.contains("data:image/png"), markdown);
    }

    @Test
    void anUnknownMimeTypeIsStillExported() throws Exception {
        // nothing should be dropped just because it is not one of the known
        // types, otherwise the document silently loses content
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                                Map.of("application/json", "{\"a\":1}"))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("{\"a\":1}"), markdown);
    }

    @Test
    void exportsHtmlOutputAsAFencedHtmlBlock() throws Exception {
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA,
                                Map.of("text/html", "<b>bold</b>"))
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("```html"), markdown);
        Assertions.assertTrue(markdown.contains("<b>bold</b>"), markdown);
    }

    @Test
    void exportsStreamOutputAsCodeBlock() throws Exception {
        var cells = List.of(
                CellData.of(CellData.Type.CODE, "println(\"hello\");", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.EXECUTION_DATA, "hello\n")
                )));

        var markdown = MarkdownNotebookExporter.toMarkdown(cells);

        Assertions.assertTrue(markdown.contains("```"));
        Assertions.assertTrue(markdown.contains("hello\n"));
    }

    @Test
    void exportsToFile() throws Exception {
        var exported = new File(tempDir, "notebook.md");
        var cells = List.of(
                CellData.of(CellData.Type.MARKDOWN, "# Title"),
                CellData.of(CellData.Type.CODE, "int a = 1;", UUID.randomUUID(), List.of(
                        CellData.OutputData.of(CellData.OutputData.OutputType.EXECUTION_DATA, Map.of("text/plain", "1"))
                )));

        MarkdownNotebookExporter.export(cells, exported);

        Assertions.assertTrue(exported.exists());
        var content = Files.readString(exported.toPath(), StandardCharsets.UTF_8);
        Assertions.assertTrue(content.contains("# Title"));
        Assertions.assertTrue(content.contains("```java"));
        Assertions.assertTrue(content.contains("int a = 1;"));
    }
}
