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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * A {@link DisplaySink} and {@link DisplayRegistry} in one, so that an export
 * can still reach the objects the IDE displayed.
 *
 * <p>Wraps another sink rather than replacing it: the IDE's sink still attaches
 * the node, and this remembers the object on the way past. The cell an object
 * belongs to is supplied per execution, because the object alone cannot be
 * attributed to a cell after the fact.
 */
public final class RecordingDisplaySink implements DisplaySink, DisplayRegistry {

    private DisplaySink delegate;

    // Declared as IdentityHashMap rather than Map on purpose: two cells with
    // equal content are still two cells, and a value-based lookup would merge
    // their output.
    private final IdentityHashMap<CellData, List<Object>> displayedByCell = new IdentityHashMap<>();
    private CellData activeCell;

    public RecordingDisplaySink() {
        this(null);
    }

    public RecordingDisplaySink(DisplaySink delegate) {
        this.delegate = delegate;
    }

    /**
     * Retargets the sink that actually shows the output.
     *
     * <p>One recorder serves a whole sheet, but the widget an output belongs to
     * differs per execution — every cell has its own output box — so the
     * delegate is set alongside {@link #startCell(CellData)} rather than fixed
     * at construction.
     */
    public void setDelegate(DisplaySink delegate) {
        this.delegate = delegate;
    }

    /**
     * Points the recorder at the cell that is about to execute.
     *
     * <p>Called before each execution, mirroring
     * {@code DisplayExtension.setCurrentCellData}.
     */
    public void startCell(CellData cell) {
        this.activeCell = cell;
    }

    @Override
    public void accept(DisplayResult result) {
        if (null != activeCell) {
            displayedByCell.computeIfAbsent(activeCell, cell -> new ArrayList<>()).add(result.displayed());
        }
        if (null != delegate) {
            delegate.accept(result);
        }
    }

    @Override
    public List<Object> displayedIn(CellData cell) {
        var displayed = displayedByCell.get(cell);
        return null == displayed ? List.of() : List.copyOf(displayed);
    }
}
