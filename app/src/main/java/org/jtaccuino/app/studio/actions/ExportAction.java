/*
 * Copyright 2025-2026 JTaccuino Contributors
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

import java.io.File;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser;
import org.jtaccuino.app.studio.WindowManager;
import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.notebook.Notebook;
import org.jtaccuino.core.ui.api.SheetAction;
import org.jtaccuino.core.ui.api.StatusDisplayer;

public final class ExportAction extends SheetAction {

    public static final ExportAction INSTANCE = new ExportAction();

    private ExportAction() {
        super("file/export",
                "Export",
                "");
    }

    @Override
    protected void handle(Sheet sheet) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Notebook File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Notebook Files", "*.ipynb"),
                new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (null != selectedFile) {
            var exportMode = isMarkdownFile(selectedFile) ? Notebook.ExportMode.MARKDOWN : Notebook.ExportMode.NO_OUTPUTS;
            try {
                sheet.getNotebook().export(exportMode, selectedFile, sheet.getDisplaySink());
            } catch (Exception ex) {
                Logger.getLogger(ExportAction.class.getName()).log(Level.SEVERE, "Export failed", ex);
                StatusDisplayer.display("Export failed: " + ex.getMessage() + ".");
                return;
            }
            StatusDisplayer.display("Exported notebook " + sheet.getNotebook().getDisplayName() + ".");
        }
    }

    private static boolean isMarkdownFile(File file) {
        return Optional.ofNullable(file.getName())
                .map(String::toLowerCase)
                .map(name -> name.endsWith(".md"))
                .orElse(false);
    }
}
