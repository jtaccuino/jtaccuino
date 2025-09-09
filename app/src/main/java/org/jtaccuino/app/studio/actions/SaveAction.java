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
import org.jtaccuino.core.ui.api.StatusDisplayer;

public final class SaveAction extends SheetAction {

    public static final SaveAction INSTANCE = new SaveAction();

    private SaveAction() {
        super("file/save",
                "Save",
                "Meta+S");
    }

    @Override
    protected void handle(Sheet sheet) {
        if (!sheet.getNotebook().getStorage().isLocal()) {
            SaveAsAction.INSTANCE.handle(sheet);
        } else {
            sheet.getNotebook().save();
            StatusDisplayer.display("Saved notebook " + sheet.getNotebook().getDisplayName() + ".");
        }
    }
}
