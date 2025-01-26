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
package org.jtaccuino.app.studio.actions;

import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.SheetAction;

public final class ExecuteNotebookAction extends SheetAction {

    public static final ExecuteNotebookAction INSTANCE = new ExecuteNotebookAction();

    private ExecuteNotebookAction() {
        super("run/execute",
            "Execute",
            "Meta+R");
    }

    @Override
    protected void handle(Sheet sheet) {
        sheet.execute();
    }
}
