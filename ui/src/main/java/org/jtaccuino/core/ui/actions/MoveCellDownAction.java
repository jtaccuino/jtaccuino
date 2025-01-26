/*
 * Copyright 2025 JTaccuino Contributors
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
package org.jtaccuino.core.ui.actions;

import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.CellAction;

public class MoveCellDownAction extends CellAction {

    public static final MoveCellDownAction INSTANCE = new MoveCellDownAction();

    private MoveCellDownAction() {
        super("source/move-cell-down", "Move Cell Up", "Ctrl+Shift+Down");
    }

    @Override
    protected void handle(Sheet.Cell cell) {
        cell.getSheet().moveCellDown(cell);
    }
}
