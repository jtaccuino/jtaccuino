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
package org.jtaccuino.cheatsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheatSheetGeneratorTest {

    @Test
    void rendersMarkdownToAPdf(@TempDir Path dir) throws IOException {
        var markdown = dir.resolve("in.md");
        Files.writeString(markdown, """
                # Title

                Some **bold** text with `code`.

                | a | b |
                | --- | --- |
                | 1 | 2 |
                """, StandardCharsets.UTF_8);
        var css = dir.resolve("in.css");
        Files.writeString(css, "@page { size: A4; margin: 10mm; }", StandardCharsets.UTF_8);
        var pdf = dir.resolve("out.pdf");

        CheatSheetGenerator.main(new String[] {
            markdown.toString(), css.toString(), pdf.toString()
        });

        assertTrue(Files.size(pdf) > 0, "the PDF should not be empty");
        try (var bytes = Files.newInputStream(pdf)) {
            assertEquals("%PDF", new String(bytes.readNBytes(4), StandardCharsets.US_ASCII));
        }
        try (var document = PDDocument.load(pdf.toFile())) {
            assertTrue(document.getNumberOfPages() >= 1, "the PDF should have at least one page");
        }
    }
}
