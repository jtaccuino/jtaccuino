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

import javafx.event.ActionEvent;
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.core.ui.api.AbstractAction;
import org.jtaccuino.core.ui.api.SheetManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.jtaccuino.app.studio.WindowManager;
import org.jtaccuino.app.studio.util.Util;

public final class OpenRemoteAction extends AbstractAction {

    public static final OpenRemoteAction INSTANCE = new OpenRemoteAction();

    private OpenRemoteAction() {
        super("file/open-remote",
                "Open Remote",
                "Meta+Shift+O");
    }

    @Override
    public void handle(ActionEvent t) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(WindowManager.getDefault().getMainWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Open Remote Notebook from URL");
        dialog.setHeaderText("Enter the URL of the remote notebook");
        dialog.setContentText("URL:");
        dialog.showAndWait().ifPresent(this::openRemoteFile);
    }

    private void openRemoteFile(String url) {
        if (Util.isValidUrl(url) && url.toLowerCase(Locale.ROOT).endsWith(".ipynb")) {
            try {
                SheetManager.getDefault().open(NotebookPersistence.INSTANCE.of(new URI(url)));
            } catch (URISyntaxException ex) {
                Logger.getLogger(OpenRemoteAction.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // Show an error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid File");
            alert.setHeaderText("Not a valid .ipynb URL");
            alert.setContentText("Please enter a valid URL ending in .ipynb");
            alert.showAndWait();
        }
    }
}
