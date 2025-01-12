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
package org.jtaccuino.app.studio;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.jtaccuino.app.studio.actions.ExecuteNotebookAction;
import org.jtaccuino.app.studio.actions.NewAction;
import org.jtaccuino.app.studio.actions.OpenAction;
import org.jtaccuino.app.studio.actions.ResetAndExecuteNotebookAction;
import org.jtaccuino.app.studio.actions.SaveAction;
import static org.jtaccuino.core.ui.UiUtils.createSVGToolbarButton;

public class ToolBar {

    private ToolBar() {
    }

    public static HBox createMainToolBar() {
        var empty = createSVGToolbarButton("empty-notebook", "Empty Notebook", "main-toolbar-button", NewAction.INSTANCE);
        var load = createSVGToolbarButton("load-notebook", "Load Notebook", "main-toolbar-button", OpenAction.INSTANCE);
        var save = createSVGToolbarButton("save-notebook", "Save Notebook", "main-toolbar-button", SaveAction.INSTANCE);
        var execute = createSVGToolbarButton("execute-notebook", "Execute Notebook", "main-toolbar-button", ExecuteNotebookAction.INSTANCE);
        var resetAndExecute = createSVGToolbarButton("reset-execute-notebook", "Reset Shell And Execute Notebook", "main-toolbar-button", ResetAndExecuteNotebookAction.INSTANCE);

        var toolbar = new HBox(empty, load, save, execute, resetAndExecute);
        HBox.setHgrow(toolbar, Priority.NEVER);
        toolbar.maxWidthProperty().bind(toolbar.prefWidthProperty());
        toolbar.getStyleClass().add("main-toolbar");
        return toolbar;
    }
}
