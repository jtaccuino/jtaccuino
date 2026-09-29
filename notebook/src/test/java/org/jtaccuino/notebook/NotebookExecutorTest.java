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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers headless execution: the loop that turns a notebook into executed cells
 * with output an export can use.
 *
 * <p>The display and print extensions persist their output on the JavaFX thread,
 * so a toolkit has to exist even though nothing is ever shown. That is the same
 * premise {@code PrintExtensionHeadlessTest} establishes and the CLI relies on.
 */
class NotebookExecutorTest {

    @TempDir
    private File tempDir;

    @BeforeAll
    static void startToolkit() {
        var latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyRunning) {
            latch.countDown();
        }
        try {
            assertTrue(latch.await(60, TimeUnit.SECONDS), "toolkit did not start");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void printsAreRecordedOnTheExecutedCell() {
        var notebook = notebook(code("println(\"hello from the executor\");"));

        var result = Notebooks.execute(notebook);

        assertTrue(result.success(), "execution failed: " + result.toMarkdown());
        var stream = streamOf(notebook.getCells().getFirst());
        assertEquals("hello from the executor\n", stream.data());
        assertTrue(result.toMarkdown().contains("hello from the executor"), result.toMarkdown());
    }

    @Test
    void theLastValueOfACellIsRecorded() {
        var notebook = notebook(code("2 + 2"));

        var result = Notebooks.execute(notebook);

        assertTrue(result.success(), result.toMarkdown());
        assertTrue(result.toMarkdown().contains("int: 4"), result.toMarkdown());
    }

    @Test
    void aDisplayedObjectIsRememberedForTheExport() {
        var cell = code("display(\"shown\");");
        var notebook = notebook(cell);

        var result = Notebooks.execute(notebook);

        assertTrue(result.success(), result.toMarkdown());
        assertEquals(List.of("shown"), result.displayRegistry().displayedIn(cell));
        assertFalse(cell.getOutputData().isEmpty(), "the display output was not stored on the cell");
    }

    @Test
    void aFailingCellIsReportedAndStopsTheRunByDefault() {
        var failing = code("throw new RuntimeException(\"boom\");");
        var later = code("println(\"should not run\");");
        var notebook = notebook(failing, later);

        var result = Notebooks.execute(notebook);

        assertFalse(result.success());
        assertEquals(List.of(failing), result.failedCells());
        assertTrue(result.toMarkdown().contains("boom"), result.toMarkdown());
        assertTrue(later.getOutputData().isEmpty(),
                "a cell after the failure was executed anyway: " + later.getOutputData());
    }

    @Test
    void continueOnErrorKeepsRunningLaterCells() {
        var failing = code("throw new RuntimeException(\"boom\");");
        var later = code("println(\"carried on\");");
        var notebook = notebook(failing, later);

        var result = Notebooks.execute(notebook, ExecutionOptions.defaults().withContinueOnError(true));

        assertFalse(result.success());
        assertEquals(List.of(failing), result.failedCells());
        assertEquals("carried on\n", streamOf(later).data());
    }

    @Test
    void markdownCellsAreNotExecuted() {
        var markdown = CellData.of(CellData.Type.MARKDOWN, "# A heading", UUID.randomUUID());
        var notebook = notebook(markdown, code("println(\"only code\");"));

        var result = Notebooks.execute(notebook);

        assertTrue(result.success(), result.toMarkdown());
        assertTrue(markdown.getOutputData().isEmpty());
        assertTrue(result.toMarkdown().contains("# A heading"), result.toMarkdown());
    }

    @Test
    void readRoundTripsANotebookWrittenToDisk() throws Exception {
        var original = notebook(code("println(\"persisted\");"));
        var file = new File(tempDir, "roundtrip.ipynb");
        NotebookPersistence.INSTANCE.toFile(file, original.getCells());

        var loaded = Notebooks.read(file.toPath());

        assertNotNull(loaded);
        assertEquals(1, loaded.getCells().size());
        assertEquals("println(\"persisted\");", loaded.getCells().getFirst().getSource());
    }

    @Test
    void readRejectsAFileThatIsNotANotebook() throws Exception {
        Path notANotebook = Files.writeString(tempDir.toPath().resolve("broken.ipynb"), "{ not json",
                StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> Notebooks.read(notANotebook));
    }

    private static Notebook notebook(CellData... cells) {
        var notebook = Notebooks.create();
        notebook.getCells().setAll(cells);
        return notebook;
    }

    private static CellData code(String source) {
        return CellData.of(CellData.Type.CODE, source, UUID.randomUUID());
    }

    private static CellData.StreamBasedOutputData streamOf(CellData cell) {
        return cell.getOutputData().stream()
                .filter(CellData.StreamBasedOutputData.class::isInstance)
                .map(CellData.StreamBasedOutputData.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stream output on " + cell.getOutputData()));
    }
}
