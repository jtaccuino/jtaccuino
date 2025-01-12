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

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.jtaccuino.app.studio.actions.ExecuteNotebookAction;
import org.jtaccuino.app.studio.actions.NewAction;
import org.jtaccuino.app.studio.actions.OpenAction;
import org.jtaccuino.app.studio.actions.ResetAndExecuteNotebookAction;
import org.jtaccuino.app.studio.actions.SaveAction;
import org.jtaccuino.core.ui.api.Action;

public class MenuBar {

    private MenuBar() {
    }

    public static javafx.scene.control.MenuBar createMainMenuBar() {
        var menuBar = new javafx.scene.control.MenuBar();
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Mac")) {
            menuBar.useSystemMenuBarProperty().set(true);
        }
        var newMenu = createMenuItem(NewAction.INSTANCE);
        var openMenu = createMenuItem(OpenAction.INSTANCE);
        var saveMenu = createMenuItem(SaveAction.INSTANCE);

        var fileMenu = new Menu("File");
        fileMenu.getItems().addAll(
                newMenu,
                new SeparatorMenuItem(),
                openMenu,
                new SeparatorMenuItem(),
                saveMenu);

        var executeMenu = createMenuItem(ExecuteNotebookAction.INSTANCE);
        var resetAndExecuteMenu = createMenuItem(ResetAndExecuteNotebookAction.INSTANCE);
        var runMenu = new Menu("Run");
        runMenu.getItems().addAll(
                executeMenu,
                resetAndExecuteMenu,
                new SeparatorMenuItem()
        );
        menuBar.getMenus().addAll(fileMenu, runMenu);
        return menuBar;
    }

    private static MenuItem createMenuItem(Action action) {
        var item = new MenuItem(action.getDisplayString());
        item.setAccelerator(action.getAccelerator());
        item.setOnAction(action);
        return item;
    }
}
