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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import javafx.scene.Group;
import javafx.scene.Scene;
import org.jtaccuino.core.ui.FxTestRuntime;
import org.jtaccuino.notebook.DisplayResult;
import org.jtaccuino.notebook.DisplaySink;
import org.jtaccuino.notebook.ExecutionOptions;
import org.jtaccuino.notebook.ExportOptions;
import org.jtaccuino.notebook.MarkdownNotebookExporter;
import org.jtaccuino.notebook.Notebooks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Executes a fixture notebook end to end and compares the whole exported
 * document against a committed reference.
 *
 * <p>The unit tests around it each pin one representation in isolation. This
 * one exists so there is a single readable markdown file showing what a real
 * export looks like — a printed stream, a result value, a collection and an
 * array as tables, and a plain node as an image — and so that a change to any
 * part of the chain has to be an intentional change to that document.
 *
 * <p>The PNG payload is the one thing that cannot be part of a committed
 * reference: the bytes a snapshot produces are not stable across platforms and
 * fonts. Only the base64 body is redacted; the placement, alt text and mime
 * type are still asserted, so the image branch cannot silently disappear.
 *
 * <p>{@code representations.expected.md} is intentionally the same file the CLI
 * writes: this test drives the same {@code Notebooks.execute} and
 * {@code MarkdownNotebookExporter} the command line tool does.
 */
class MarkdownExportGoldenTest {

    private static final Pattern PNG_DATA_URI = Pattern.compile("data:image/png;base64,[A-Za-z0-9+/=]+");

    @TempDir
    private File tempDir;

    @BeforeAll
    static void startToolkit() {
        FxTestRuntime.start();
    }

    @Test
    void theFixtureNotebookExportsToTheCommittedReference() throws Exception {
        var notebook = Notebooks.read(resourcePath("representations.ipynb"));

        // display() snapshots the node it is handed, and a node only lays out
        // once it belongs to a scene, so the headless sink gives it a hidden one
        var root = new Group();
        var scene = new Scene(root);
        DisplaySink sink = result -> {
            if (result instanceof DisplayResult.Graphical graphical) {
                root.getChildren().setAll(graphical.node());
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            }
        };

        var result = Notebooks.execute(notebook, ExecutionOptions.defaults().withDisplaySink(sink));
        assertTrue(result.success(), result.toMarkdown());

        var target = new File(tempDir, "representations.md");
        MarkdownNotebookExporter.export(notebook.getCells(), target, result.displayRegistry(),
                ExportOptions.defaults());

        var actual = normalize(Files.readString(target.toPath(), StandardCharsets.UTF_8));
        var expected = normalize(resourceText("representations.expected.md"));
        assertEquals(expected, actual);
    }

    private static Path resourcePath(String name) throws Exception {
        return Path.of(MarkdownExportGoldenTest.class.getResource(name).toURI());
    }

    private static String resourceText(String name) throws Exception {
        try (var stream = MarkdownExportGoldenTest.class.getResourceAsStream(name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String redact(String markdown) {
        return PNG_DATA_URI.matcher(markdown).replaceAll("data:image/png;base64,<redacted>");
    }

    /**
     * Makes the comparison independent of how git checked the reference out.
     *
     * <p>The generated markdown always uses {@code \n}, but on Windows the
     * committed resource is checked out with {@code \r\n}, which would make the
     * two look identical in a failure message yet compare unequal.
     */
    private static String normalize(String markdown) {
        return redact(markdown).replace("\r\n", "\n").replace('\r', '\n').strip();
    }
}
