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
 * Supplies the live objects that were passed to {@code display(...)} during
 * execution.
 *
 * <p>The exporter is handed an object graph that holds base64 strings, because
 * that is what a notebook is. Recovering a markdown table from a picture of a
 * table is impossible, so an export that wants a better representation than the
 * stored one needs the original objects.
 *
 * <p>Keyed by {@link CellData} rather than by position: an output's index within
 * a cell is an accident of how many calls happened, and any change to the stored
 * outputs would silently shift the alignment. Asking "what did this cell
 * display" has no such coupling.
 */
@FunctionalInterface
public interface DisplayRegistry {

    /**
     * The objects displayed in a cell, in the order they were displayed.
     *
     * @param cell never {@code null}
     * @return the displayed objects, empty when the cell displayed nothing or
     * was not executed in this process; never {@code null}
     */
    List<Object> displayedIn(CellData cell);
}
