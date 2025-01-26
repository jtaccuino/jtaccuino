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

import java.io.File;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.core.ui.api.SheetManager;
import org.jtaccuino.app.studio.WindowManager;
import org.jtaccuino.core.ui.api.AbstractAction;

public final class OpenAction extends AbstractAction {

    public static final OpenAction INSTANCE = new OpenAction();

    private OpenAction() {
        super("file/open",
            "Open",
            "Meta+O");
    }

    @Override
    public void handle(ActionEvent t) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Notebook File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Notebook Files", "*.ipynb"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
        if (null != selectedFile) {
            SheetManager.getDefault().open(NotebookPersistence.INSTANCE.of(selectedFile));
        }
    }
}
