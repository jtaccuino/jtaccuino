/*
 * Copyright 2024 JTaccuino Contributors
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

import org.jtaccuino.format.Notebook;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jtaccuino.core.ui.Sheet;
import static org.jtaccuino.core.ui.UiUtils.createSVGToolbarButton;
import org.jtaccuino.format.NotebookCompat;

public class Studio extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static final String TITLE = "JTaccuino Studio - A scientific notebook powered by Java";

    private Scene scene;
    private BorderPane bp;
    private TabPane tabPane;

    @Override
    public void start(Stage stage) throws Exception {
        bp = new BorderPane();
        tabPane = new TabPane();
        tabPane.addEventFilter(KeyEvent.KEY_PRESSED, t -> {
                if (KeyCode.S == t.getCode() && t.isShortcutDown() && !t.isShiftDown()) {
                    if (tabPane.getSelectionModel().getSelectedItem() instanceof TabSheet ts) {
                        ts.save();
                    }
                    t.consume();
                }
            });

        tabPane.getTabs().add(new TabSheet(Sheet.of(), "Scratch", null));
        bp.setCenter(tabPane);
        bp.setTop(createMainToolBar(stage));
        scene = new Scene(bp);
        scene.getStylesheets().add(this.getClass().getResource("/jtaccuino.css").toExternalForm());
        stage.setScene(scene);
        stage.setHeight(Screen.getPrimary().getBounds().getHeight() * 0.75);
        stage.setWidth(Screen.getPrimary().getBounds().getWidth() * 0.6);
        stage.setTitle(TITLE);
        stage.getIcons().add(new Image("notebook-svgrepo-com_256.png"));
        stage.setOnCloseRequest((event) -> {
            tabPane.getTabs().stream()
                    .filter(t-> t instanceof TabSheet)
                    .forEach(t-> ((TabSheet)t).close());
        });
        stage.show();
    }

    private void activateNotebook(NotebookCompat notebook, String displayName, File file) {
        tabPane.getTabs().add(new TabSheet(
                switch(notebook) {
                    case null -> Sheet.of();
                    default -> Sheet.of(notebook, file);
                }, displayName, file)
        );
        tabPane.getSelectionModel().selectLast();
    }

    private void activateNotebook(Notebook notebook, String displayName, File file) {
        tabPane.getTabs().add(new TabSheet(
                switch(notebook) {
                    case null -> Sheet.of();
                    default -> Sheet.of(notebook, file);
                }, displayName, file)
        );
        tabPane.getSelectionModel().selectLast();
    }

    private HBox createMainToolBar(Stage stage) {
        var empty = createSVGToolbarButton("empty-notebook", "Empty Notebook", "main-toolbar-button");
        empty.setOnAction((event) -> {
            activateNotebook((Notebook)null, "Empty", null);
        });

        var load = createSVGToolbarButton("load-notebook", "Load Notebook", "main-toolbar-button");
        load.setOnAction((event) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Notebook");
            fileChooser.getExtensionFilters().addAll(
                    new ExtensionFilter("Notebook Files", "*.ipynb"),
                    new ExtensionFilter("All Files", "*.*"));
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                System.out.println(selectedFile);
                try {
                    var jsonb = JsonbBuilder.create();
                    var notebook = jsonb.fromJson(new FileReader(selectedFile), Notebook.class);
                    jsonb.close();
                    activateNotebook(notebook, selectedFile.getName(), selectedFile);
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    try {
                        Jsonb jsonb = JsonbBuilder.create();
                        var notebook = jsonb.fromJson(new FileReader(selectedFile), NotebookCompat.class);
                        jsonb.close();
                        activateNotebook(notebook, selectedFile.getName(), selectedFile);
                    } catch (Exception ee) {
                    // try compat mode
                        ee.printStackTrace();
                    }
                }
            }
        });
        var save = createSVGToolbarButton("save-notebook", "Save Notebook", "main-toolbar-button");
        save.setOnAction((event) -> {
            if (tabPane.getSelectionModel().getSelectedItem() instanceof TabSheet t) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Notebook");
                fileChooser.getExtensionFilters().addAll(
                        new ExtensionFilter("Notebook Files", "*.ipynb"),
                        new ExtensionFilter("All Files", "*.*"));
                File selectedFile = fileChooser.showSaveDialog(stage);
                if (null != selectedFile) {
                    t.saveToFile(selectedFile);
                }
            }
        });

        var execute = createSVGToolbarButton("execute-notebook", "Execute Notebook", "main-toolbar-button");
        execute.setOnAction((event) -> {
            if (tabPane.getSelectionModel().getSelectedItem() instanceof TabSheet t) {
                t.execute();
            }
        });
        var resetAndExecute = createSVGToolbarButton("reset-execute-notebook", "Reset Shell And Execute Notebook", "main-toolbar-button");
        resetAndExecute.setOnAction((event) -> {
            if (tabPane.getSelectionModel().getSelectedItem() instanceof TabSheet t) {
                t.resetAndExecute();
            }
        });
        var toolbar = new HBox(empty, load, save, execute, resetAndExecute);
        HBox.setHgrow(toolbar, Priority.NEVER);
        toolbar.maxWidthProperty().bind(toolbar.prefWidthProperty());
        toolbar.getStyleClass().add("main-toolbar");
        return toolbar;
    }

    static class TabSheet extends Tab {

        private final Sheet sheet;
        private File file;

        TabSheet(Sheet sheet, String displayName, File file) {
            super(displayName, sheet);
            this.sheet = sheet;
            setFile(file);
            this.onCloseRequestProperty().set((t) -> {
                sheet.close();
            });
        }

        void close() {
            sheet.close();
        }

        void execute() {
            sheet.execute();
        }

        void resetAndExecute() {
            sheet.resetAndExecute();
        }

        void save() {
            saveToFile(file);
        }

        void saveToFile(File selectedFile) {
            var config = new JsonbConfig();
            config.setProperty(JsonbConfig.FORMATTING, true);
            Jsonb jsonb = JsonbBuilder.create(config);
            try {
                jsonb.toJson(sheet.toNotebook(), new FileWriter(selectedFile));
                jsonb.close();
                setFile(selectedFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void setFile(File file) {
            this.file = file;
            if (null != file) {
                setTooltip(new Tooltip(file.getPath()));
                setText(file.getName());
            } else {
                setTooltip(null);
            }
        }
    }
}
