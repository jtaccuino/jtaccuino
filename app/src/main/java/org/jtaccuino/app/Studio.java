/*
 * Copyright 2024-2025 JTaccuino Contributors
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
package org.jtaccuino.app;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.app.persistence.FilePersistence;
import org.jtaccuino.app.persistence.PersistenceManager;
import org.jtaccuino.app.studio.MenuBar;
import org.jtaccuino.app.studio.StatusLine;
import org.jtaccuino.core.ui.api.SheetManager;
import org.jtaccuino.app.studio.ToolBar;
import org.jtaccuino.core.ui.Sheet;

public class Studio extends Application {

    private static final String TITLE = "JTaccuino Studio - A scientific notebook powered by Java";
    private TabPane sheetPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent node = createParentNode();
        Scene scene = new Scene(node);
        scene.getStylesheets().add(this.getClass().getResource("/jtaccuino.css").toExternalForm());
        stage.setScene(scene);
        stage.setHeight(Screen.getPrimary().getBounds().getHeight() * 0.75);
        stage.setWidth(Screen.getPrimary().getBounds().getWidth() * 0.6);
        stage.setTitle(TITLE);
        stage.getIcons().add(new Image("notebook-svgrepo-com_256.png"));
        stage.setOnCloseRequest((event) -> {
            shutdown();
        });
        stage.show();
    }

    public Parent createParentNode() {
        var bp = new BorderPane();
        sheetPane = new TabPane();

        SheetManager.getDefault().setOnOpen(evt -> {
            var ts = new TabSheet(evt.getSource());
            sheetPane.getTabs().add(ts);
            sheetPane.getSelectionModel().select(ts);
        });

        sheetPane.getSelectionModel().selectedItemProperty().subscribe((t) -> {
            if (t instanceof TabSheet tabSheet) {
                SheetManager.getDefault().setActiveSheet(tabSheet.sheet);
            }
        });

        bp.setCenter(sheetPane);
        bp.setTop(new VBox(
                MenuBar.createMainMenuBar(),
                ToolBar.createMainToolBar()));
        bp.setBottom(StatusLine.getDefault().getNode());

        initTabs();

        return bp;
    }

    private void initTabs() {
        var openFiles = FilePersistence.getDefault().getOpenFiles();
        if (openFiles.isEmpty()) {
            sheetPane.getTabs().add(new TabSheet(SheetManager.getDefault().of(NotebookPersistence.INSTANCE.of())));
        } else {
            openFiles.forEach(file -> {
                if (file.path().toFile().exists()) {
                    SheetManager.getDefault().open(NotebookPersistence.INSTANCE.of(file.path().toFile()));
                }
            });
        }
    }

    private void shutdown() {
        var files = FilePersistence.getDefault();
        files.reset();
        sheetPane.getTabs().stream()
                .filter(t -> t instanceof TabSheet)
                .map(t -> (TabSheet) t)
                .forEach(t -> {
                    if (null != t.sheet.getNotebook().getFile()) {
                        files.add(new FilePersistence.OpenFile(t.sheet.getNotebook().getFile().toPath()));
                    }
                    t.close(false);
                });
        SheetManager.getDefault().getRecentFiles().forEach(rf -> {
            files.add(new FilePersistence.RecentFile(rf.path()));
        });

        PersistenceManager.writePersistenceFile("files", files);
    }

    static class TabSheet extends Tab {

        private final Sheet sheet;

        TabSheet(Sheet sheet) {
            super(sheet.getNotebook().getDisplayName(), sheet);
            this.textProperty().bind(sheet.getNotebook().displayNameProperty());
            this.sheet = sheet;
            this.onCloseRequestProperty().set((t) -> {
                this.close(true);
            });
        }

        void close(boolean makeNotebookRecent) {
            SheetManager.getDefault().close(this.sheet, makeNotebookRecent);
        }
    }
}
