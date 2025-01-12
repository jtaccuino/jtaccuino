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
package org.jtaccuino.app.studio;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.Notebook;

/**
 * Manage sheet instances and meta data. Works as a model for the different ui
 * states, e.g. presentation mode.
 */
public class SheetManager {

    private static final SheetManager INSTANCE = new SheetManager();

    private ObjectProperty<Sheet> activeSheetProperty = new SimpleObjectProperty<>();

    private List<Sheet> sheets = new ArrayList<>();

    private EventHandler<SheetEvent> onOpen = null;

    public static SheetManager getDefault() {
        return INSTANCE;
    }

    private SheetManager() {
    }

    public Sheet of(Notebook notebook) {
        var s = Sheet.of(notebook);
        this.sheets.add(s);
        return s;
    }

    public Sheet of() {
        var s = Sheet.of(NotebookPersistence.INSTANCE.of());
        this.sheets.add(s);
        return s;
    }

    public void close(Sheet sheet) {
        sheet.close();
    }

    public void open(Notebook notebook) {
        var sheet = of(notebook);
        setActiveSheet(sheet);
        if (null != onOpen) {
            onOpen.handle(new SheetEvent(sheet, SheetEvent.SHEET_OPENED));
        }
    }

    public void setOnOpen(EventHandler<SheetEvent> handler) {
        this.onOpen = handler;
    }

    public Sheet getActiveSheet() {
        return activeSheetProperty.get();
    }

    public void setActiveSheet(Sheet sheet) {
        activeSheetProperty.set(sheet);
    }

    public ObjectProperty<Sheet> activeSheet() {
        return activeSheetProperty;
    }
}
