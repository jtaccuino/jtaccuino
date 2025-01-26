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
import org.jtaccuino.core.ui.api.CellData;

public class ChangeCellToMarkdownAction extends CellAction {

    public static final ChangeCellToMarkdownAction INSTANCE = new ChangeCellToMarkdownAction();

    private ChangeCellToMarkdownAction() {
        super("source/change-cell-to-markdown", "Change Cell to Markdown", "Meta+M");
    }

    @Override
    protected void handle(Sheet.Cell cell) {
        var sheet = cell.getSheet();
        sheet.replaceCell(cell, sheet.createCell(CellData.Type.MARKDOWN, cell.getCellData()));
    }
}
