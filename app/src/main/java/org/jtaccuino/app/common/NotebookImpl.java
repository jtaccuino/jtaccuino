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
package org.jtaccuino.app.common;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jtaccuino.app.common.internal.IpynbFormatOperations;
import org.jtaccuino.core.ui.api.CellData;
import org.jtaccuino.core.ui.api.Notebook;

public class NotebookImpl implements Notebook{

    private final String displayName;
    private File file;
    private final ObservableList<CellData> cells = FXCollections.observableArrayList();

    NotebookImpl(IpynbFormatOperations ipynb, String displayName, File file) {
        this.displayName = displayName;
        this.file = file;
        this.cells.addAll(null != ipynb ? ipynb.toCellDataList()
                : Arrays.asList(new CellData[]{CellData.of(CellData.Type.CODE, null, UUID.randomUUID())}));
    }

    public ObservableList<CellData> getCells() {
        return this.cells;
    }

    public String getDisplayName() {
        return displayName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void save() {
        saveToFile(file);
    }

    public void saveToFile(File selectedFile) {
        NotebookPersistence.INSTANCE.toFile(selectedFile, cells);
    }
}
