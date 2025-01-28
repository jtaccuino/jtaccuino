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
import org.jtaccuino.app.studio.MenuBar;
import org.jtaccuino.app.studio.StatusLine;
import org.jtaccuino.core.ui.api.SheetManager;
import org.jtaccuino.app.studio.ToolBar;
import org.jtaccuino.core.ui.Sheet;

public class Studio extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static final String TITLE = "JTaccuino Studio - A scientific notebook powered by Java";

    private Scene scene;
    private BorderPane bp;
    private TabPane sheetPane;

    @Override
    public void start(Stage stage) throws Exception {
        Parent node = createParentNode();
        scene = new Scene(node);
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

        var notebook = NotebookPersistence.INSTANCE.of();

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

        sheetPane.getTabs().add(new TabSheet(SheetManager.getDefault().of(notebook)));

        return bp;
    }

    public void shutdown() {
        sheetPane.getTabs().stream()
                .filter(t -> t instanceof TabSheet)
                .forEach(t -> ((TabSheet) t).close());
    }

    static class TabSheet extends Tab {

        private final Sheet sheet;

        TabSheet(Sheet sheet) {
            super(sheet.getNotebook().getDisplayName(), sheet);
            this.sheet = sheet;
            this.onCloseRequestProperty().set((t) -> {
                this.sheet.close();
            });
        }

        void close() {
            this.sheet.close();
        }
    }
}
