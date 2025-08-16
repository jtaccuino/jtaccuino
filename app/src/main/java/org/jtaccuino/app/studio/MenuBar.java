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
import org.jtaccuino.app.studio.actions.ExportAction;
import org.jtaccuino.app.studio.actions.NewAction;
import org.jtaccuino.app.studio.actions.OpenAction;
import org.jtaccuino.app.studio.actions.ResetAndExecuteNotebookAction;
import org.jtaccuino.app.studio.actions.SaveAction;
import org.jtaccuino.app.studio.actions.SaveAsAction;
import org.jtaccuino.core.ui.actions.ChangeCellToJavaAction;
import org.jtaccuino.core.ui.actions.ChangeCellToMarkdownAction;
import org.jtaccuino.core.ui.actions.DeleteCellAction;
import org.jtaccuino.core.ui.actions.ExecuteCellAction;
import org.jtaccuino.core.ui.actions.InsertCellAboveAction;
import org.jtaccuino.core.ui.actions.InsertCellBelowAction;
import org.jtaccuino.core.ui.actions.MoveCellDownAction;
import org.jtaccuino.core.ui.actions.MoveCellUpAction;
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
        var saveAsMenu = createMenuItem(SaveAsAction.INSTANCE);
        var exportMenu = createMenuItem(ExportAction.INSTANCE);

        var fileMenu = new Menu("File");
        fileMenu.getItems().addAll(
                newMenu,
                new SeparatorMenuItem(),
                openMenu,
                new SeparatorMenuItem(),
                saveMenu, saveAsMenu, exportMenu);

        var executeMenu = createMenuItem(ExecuteNotebookAction.INSTANCE);
        var resetAndExecuteMenu = createMenuItem(ResetAndExecuteNotebookAction.INSTANCE);
// Called from RTA for now, do not register to avoid duplicate triggering
//        var executeCellMenu = createMenuItem(ExecuteCellAction.INSTANCE);
        var runMenu = new Menu("Run");
        runMenu.getItems().addAll(
                executeMenu,
                resetAndExecuteMenu
//                new SeparatorMenuItem(),
//                executeCellMenu
        );
//        var deleteCell = createMenuItem(DeleteCellAction.INSTANCE);
        var moveCellUp = createMenuItem(MoveCellUpAction.INSTANCE);
        var moveCellDown = createMenuItem(MoveCellDownAction.INSTANCE);
        var insertCellAbove = createMenuItem(InsertCellAboveAction.INSTANCE);
        var insertCellBelow = createMenuItem(InsertCellBelowAction.INSTANCE);
        var changeCellToJava = createMenuItem(ChangeCellToJavaAction.INSTANCE);
        var changeCellToMarkdown = createMenuItem(ChangeCellToMarkdownAction.INSTANCE);
        var sourceMenu = new Menu("Source");
        sourceMenu.getItems().addAll(
//                deleteCell,
                moveCellUp,
                moveCellDown,
                insertCellAbove,
                insertCellBelow,
                changeCellToJava,
                changeCellToMarkdown
        );
        menuBar.getMenus().addAll(fileMenu, sourceMenu, runMenu);
        return menuBar;
    }

    private static MenuItem createMenuItem(Action action) {
        var item = new MenuItem(action.getDisplayString());
        item.setAccelerator(action.getAccelerator());
        item.setOnAction(action);
        return item;
    }
}
