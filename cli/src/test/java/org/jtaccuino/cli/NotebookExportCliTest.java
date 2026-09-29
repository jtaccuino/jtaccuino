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
package org.jtaccuino.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jtaccuino.notebook.CellData;
import org.jtaccuino.notebook.NotebookPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End to end tests of the command line tool: reading a real ipynb, executing it
 * and exporting, with the exit codes the usage promises.
 *
 * <p>These run in the {@code cli} module on purpose. The renderers live in
 * {@code ui}, so this is the closest a test can get to the shipped classpath
 * without forking a process.
 */
class NotebookExportCliTest {

    @TempDir
    private File tempDir;

    @Test
    void noArgumentsIsAUsageError() {
        var result = run();

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("no notebook given"), result.err());
        assertTrue(result.err().contains("Usage:"), result.err());
    }

    @Test
    void helpIsNotAnError() {
        var result = run("--help");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("Usage:"), result.out());
    }

    @Test
    void anUnreadableNotebookIsALoadError() {
        var result = run(tempDir.toPath().resolve("missing.ipynb").toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("Could not read"), result.err());
    }

    @Test
    void anUnknownImageFormatIsAUsageError() {
        var result = run("--image-format", "webp", notebookFile("int a = 1;").toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("unknown image format"), result.err());
    }

    @Test
    void executesAndWritesMarkdown() throws Exception {
        var notebook = notebookFile("println(\"cli hello\");");

        var result = run(notebook.toString());

        assertEquals(0, result.exitCode(), result.err());
        var markdown = Files.readString(tempDir.toPath().resolve("book.md"), StandardCharsets.UTF_8);
        assertTrue(markdown.contains("cli hello"), markdown);
    }

    @Test
    void anExplicitOutputIsUsed() throws Exception {
        var notebook = notebookFile("println(\"moved\");");
        var output = tempDir.toPath().resolve("nested").resolve("custom.md");
        Files.createDirectories(output.getParent());

        var result = run("-o", output.toString(), notebook.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(Files.exists(output), "the requested output was not written");
    }

    @Test
    void aFailingCellExitsWithTwoButStillWritesWhatRan() throws Exception {
        var notebook = notebookFile("throw new RuntimeException(\"boom\");");

        var result = run(notebook.toString());

        assertEquals(2, result.exitCode(), result.err());
        var markdown = Files.readString(tempDir.toPath().resolve("book.md"), StandardCharsets.UTF_8);
        assertTrue(markdown.contains("boom"), markdown);
    }

    @Test
    void aDisplayedCollectionExportsAsATable() throws Exception {
        var notebook = notebookFile("display(java.util.List.of(java.util.Map.entry(\"k\", \"v\")));");

        var result = run(notebook.toString());

        assertEquals(0, result.exitCode(), result.err());
        var markdown = Files.readString(tempDir.toPath().resolve("book.md"), StandardCharsets.UTF_8);
        assertTrue(markdown.contains("| k | v |"), markdown);
        assertFalse(markdown.contains("data:image/png"), markdown);
    }

    @Test
    void extractImagesWritesSidecarsAndReferencesThem() throws Exception {
        var notebook = notebookFile("display(new javafx.scene.control.Label(\"sidecar\"));");

        var result = run("--extract-images", notebook.toString());

        assertEquals(0, result.exitCode(), result.err());
        var sidecarDirectory = tempDir.toPath().resolve("book_files");
        assertTrue(Files.isDirectory(sidecarDirectory), "no sidecar directory was written");
        try (var files = Files.list(sidecarDirectory)) {
            var sidecars = files.filter(path -> path.toString().endsWith(".png")).toList();
            assertFalse(sidecars.isEmpty(), "no png sidecar was written");
        }
        var markdown = Files.readString(tempDir.toPath().resolve("book.md"), StandardCharsets.UTF_8);
        assertTrue(markdown.contains("book_files/"), markdown);
        assertFalse(markdown.contains("data:image/png"), markdown);
    }

    @Test
    void imageFormatSvgWarnsOnStderrWhenItCannotComply() throws Exception {
        var notebook = notebookFile("display(new javafx.scene.control.Label(\"raster\"));");

        var result = run("--image-format", "svg", notebook.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.err().contains("warning:"), "expected a warning, got: " + result.err());
    }

    @Test
    void ipynbOptionWritesTheExecutedNotebookBack() throws Exception {
        var notebook = notebookFile("println(\"persisted output\");");
        var roundTripped = tempDir.toPath().resolve("executed.ipynb");

        var result = run("--ipynb", roundTripped.toString(), notebook.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(Files.exists(roundTripped), "the executed notebook was not written");
        var reloaded = NotebookPersistence.INSTANCE.of(roundTripped.toUri());
        var hasOutput = reloaded.getCells().getFirst().getOutputData().stream()
                .anyMatch(od -> od instanceof CellData.StreamBasedOutputData stream
                && stream.data().contains("persisted output"));
        assertTrue(hasOutput, "the executed output did not survive the round trip");
    }

    private Path notebookFile(String... sources) {
        List<CellData> cells = Arrays.stream(sources)
                .map(source -> CellData.of(CellData.Type.CODE, source, UUID.randomUUID()))
                .toList();
        var file = new File(tempDir, "book.ipynb");
        NotebookPersistence.INSTANCE.toFile(file, cells, false);
        return file.toPath();
    }

    private CliResult run(String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var exitCode = Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new CliResult(exitCode,
                out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    private record CliResult(int exitCode, String out, String err) {
    }
}
