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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Reads and executes notebooks without a user interface.
 *
 * <p>This is the entry point a script or command line tool uses:
 *
 * <pre>{@code
 * var notebook = Notebooks.read(Path.of("book.ipynb"));
 * var result = Notebooks.execute(notebook);
 * Files.writeString(Path.of("book.md"), result.toMarkdown());
 * }</pre>
 *
 * <p>The notebook {@code display(...)}s are not attached anywhere unless a
 * {@link DisplaySink} is supplied, but they are still recorded on the cells and
 * remembered in the {@link ExecutionResult}'s registry, so the markdown export
 * can render a table rather than a picture of one.
 */
public final class Notebooks {

    private Notebooks() {
    }

    /** A new, empty notebook with a single code cell. */
    public static Notebook create() {
        return NotebookPersistence.INSTANCE.of();
    }

    /**
     * Reads a notebook from a file.
     *
     * @throws IOException when the file cannot be read or is not a notebook.
     * {@link NotebookPersistence} reports a malformed file by returning
     * {@code null} and logging the cause, so that is turned into an exception
     * here; a caller such as a command line tool has no cell to attach the
     * failure to
     */
    public static Notebook read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        var notebook = NotebookPersistence.INSTANCE.of(path.toAbsolutePath().toUri());
        if (null == notebook) {
            throw new IOException("Could not read notebook " + path);
        }
        return notebook;
    }

    public static ExecutionResult execute(Notebook notebook) {
        return NotebookExecutor.execute(notebook);
    }

    public static ExecutionResult execute(Notebook notebook, ExecutionOptions options) {
        return NotebookExecutor.execute(notebook, options);
    }
}
