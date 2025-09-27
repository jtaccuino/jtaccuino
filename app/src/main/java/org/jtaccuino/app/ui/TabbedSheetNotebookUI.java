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
package org.jtaccuino.app.ui;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.app.persistence.FilePersistence;
import org.jtaccuino.app.persistence.PersistenceManager;
import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.SheetManager;

public class TabbedSheetNotebookUI {

    private TabPane sheetPane;
    private Node node;

    public TabbedSheetNotebookUI() {
        node = createNode();
    }

    public Node getNode() {
        return node;
    }

    private Node createNode() {
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

        initTabs();

        return sheetPane;
    }

    private void initTabs() {
        var openFiles = FilePersistence.getDefault().getOpenFiles();
        if (openFiles.isEmpty()) {
            sheetPane.getTabs().add(new TabSheet(SheetManager.getDefault().of(NotebookPersistence.INSTANCE.of())));
        } else {
            openFiles.forEach(file -> {
                SheetManager.getDefault().open(NotebookPersistence.INSTANCE.of(file.uri()));
            });
        }
    }

    void shutdown() {
        var files = FilePersistence.getDefault();
        files.reset();
        sheetPane.getTabs().stream()
                .filter(t -> t instanceof TabSheet)
                .map(t -> (TabSheet) t)
                .forEach(t -> {
                    t.sheet.getNotebook().getStorage().getURI().ifPresent(uri
                            -> files.add(new FilePersistence.OpenFile(uri))
                    );
                    t.close(false);
                });
        SheetManager.getDefault().getRecentFiles().forEach(rf -> {
            files.add(new FilePersistence.RecentFile(rf.uri()));
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
            sheet.getNotebook().getStorage().getURI().ifPresentOrElse(uri
                    -> this.setTooltip(new Tooltip(uri.toString())),
                    () -> this.setTooltip(new Tooltip("No Location")));
        }

        void close(boolean makeNotebookRecent) {
            SheetManager.getDefault().close(this.sheet, makeNotebookRecent);
        }
    }
}
