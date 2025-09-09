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
package org.jtaccuino.core.ui.api;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;

public interface Notebook {

    public static enum ExportMode {
        NO_OUTPUTS,
        MARKDOWN
    }

    public static sealed interface Storage permits FileStorage, NullStorage, URIStorage {

        public boolean isLocal();

        public Optional<Path> getLocalFolder();

        public Optional<URI> getURI();
    }

    public static non-sealed interface NullStorage extends Storage {

        public FileStorage toFileStorage(File file);
    }

    public static non-sealed interface URIStorage extends Storage {

        public FileStorage toFileStorage(File file);
    }

    public static non-sealed interface FileStorage extends Storage {

        public File getFile();
    }

    public ObservableList<CellData> getCells();

    public ReadOnlyStringProperty displayNameProperty();

    public String getDisplayName();

    public ReadOnlyStringProperty locationProperty();

    public String getLocation();

    public void save();

    public void saveAs(File selectedFile);

    public void export(ExportMode mode, File file);

    public Storage getStorage();
}
