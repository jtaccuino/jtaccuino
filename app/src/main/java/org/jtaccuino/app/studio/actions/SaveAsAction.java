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

import javafx.stage.FileChooser;
import org.jtaccuino.app.studio.WindowManager;
import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.SheetAction;
import org.jtaccuino.core.ui.api.StatusDisplayer;

import java.io.File;

public final class SaveAsAction extends SheetAction {

    public static final SaveAsAction INSTANCE = new SaveAsAction();
    private final FileChooser fileChooser = new FileChooser();

    private SaveAsAction() {
        super("file/save_as",
                "Save As",
                "Shift+Meta+S");
        fileChooser.setTitle("Save Notebook File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Notebook Files", "*.ipynb"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
    }

    @Override
    protected void handle(Sheet sheet) {
        File selectedFile = fileChooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (null != selectedFile) {
            sheet.getNotebook().rename(selectedFile.getName());
            sheet.getNotebook().setFile(selectedFile);
            sheet.getNotebook().save();
            StatusDisplayer.display("Saved notebook " + sheet.getNotebook().getDisplayName() + ".");
        }
    }
}
