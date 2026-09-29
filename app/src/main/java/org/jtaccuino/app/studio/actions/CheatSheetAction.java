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
package org.jtaccuino.app.studio.actions;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import org.jtaccuino.core.ui.api.AbstractAction;

/**
 * Opens the bundled cheat sheet PDF with the platform's default viewer.
 */
public final class CheatSheetAction extends AbstractAction {

    private static final String RESOURCE = "/cheat-sheet.pdf";

    public static final CheatSheetAction INSTANCE = new CheatSheetAction();

    private CheatSheetAction() {
        super("help/cheat-sheet", "Cheat Sheet", "");
    }

    @Override
    public void handle(ActionEvent t) {
        try {
            var pdf = extractCheatSheet();
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdf.toFile());
            } else {
                showInfo("The cheat sheet was written to:\n" + pdf);
            }
        } catch (IOException ex) {
            showInfo("The cheat sheet could not be opened:\n" + ex.getMessage());
        }
    }

    /**
     * The PDF is bundled as a resource, so it has to be unpacked to a real file
     * before the desktop can open it.
     */
    static Path extractCheatSheet() throws IOException {
        try (InputStream in = CheatSheetAction.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("Resource " + RESOURCE + " is not bundled");
            }
            var file = Files.createTempFile("jtaccuino-cheat-sheet-", ".pdf");
            file.toFile().deleteOnExit();
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        }
    }

    private static void showInfo(String message) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cheat Sheet");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
