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

import java.util.List;

/**
 * What an execution produced.
 *
 * <p>The notebook's cells now hold the outputs of the run, and this carries the
 * two things a caller cannot read off the cells: which ones failed, and the
 * display registry, so that an export can render a displayed collection as a
 * table instead of the picture stored for it.
 *
 * @param notebook the executed notebook, with outputs filled in
 * @param failedCells the cells that failed, in execution order; empty on a clean
 * run
 * @param displayRegistry the objects displayed during this run
 */
public record ExecutionResult(Notebook notebook, List<CellData> failedCells, DisplayRegistry displayRegistry) {

    public ExecutionResult {
        failedCells = List.copyOf(failedCells);
    }

    /** True when every cell that ran succeeded. */
    public boolean success() {
        return failedCells.isEmpty();
    }

    /**
     * Renders the executed notebook as markdown, using the registry so that
     * displayed objects export as text where a renderer can express them.
     */
    public String toMarkdown() {
        return MarkdownNotebookExporter.toMarkdown(notebook.getCells(), displayRegistry);
    }
}
