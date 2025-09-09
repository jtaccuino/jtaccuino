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
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jtaccuino.app.common.internal.IpynbFormatOperations;
import org.jtaccuino.core.ui.api.CellData;
import org.jtaccuino.core.ui.api.Notebook;

public class NotebookImpl implements Notebook {

    private static class NullStorageImpl implements NullStorage {

        @Override
        public FileStorage toFileStorage(File file) {
            return new FileStorageImpl(file);
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public Optional<Path> getLocalFolder() {
            return Optional.empty();
        }

        @Override
        public Optional<URI> getURI() {
            return Optional.empty();
        }
    }

    private static class FileStorageImpl implements FileStorage {

        private final File file;

        private FileStorageImpl(File file) {
            this.file = file;
        }

        @Override
        public File getFile() {
            return this.file;
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public Optional<Path> getLocalFolder() {
            return Optional.of(this.file.getParentFile().toPath());
        }

        @Override
        public Optional<URI> getURI() {
            return Optional.of(this.file.toURI());
        }
    }

    private static class URIStorageImpl implements URIStorage {

        private final URI uri;

        private URIStorageImpl(URI uri) {
            this.uri = uri;
        }

        @Override
        public FileStorage toFileStorage(File file) {
            return new FileStorageImpl(file);
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public Optional<Path> getLocalFolder() {
            return Optional.empty();
        }

        @Override
        public Optional<URI> getURI() {
            return Optional.of(this.uri);
        }
    }

    private final StringProperty displayNameProperty = new SimpleStringProperty();
    private final StringProperty locationProperty = new SimpleStringProperty();
    private final ObservableList<CellData> cells = FXCollections.observableArrayList();

    private Storage storage;

    NotebookImpl(IpynbFormatOperations ipynb, String displayName, URI uri) {
        this.displayNameProperty.set(displayName);
        this.cells.addAll(null != ipynb ? ipynb.toCellDataList()
                : Arrays.asList(new CellData[]{CellData.of(CellData.Type.CODE, null, UUID.randomUUID())}));
        if (null == uri) {
            this.storage = new NullStorageImpl();
            this.locationProperty.set("No Location. Not persistent yet!");
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            this.storage = new FileStorageImpl(Path.of(uri).toFile());
            this.locationProperty.set(this.storage.getURI().get().toString());
        } else if ("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme())
                || "jar".equalsIgnoreCase(uri.getScheme())) {
            this.storage = new URIStorageImpl(uri);
            this.locationProperty.set(this.storage.getURI().get().toString());
        } else {
            throw new IllegalArgumentException("Unsupported URIs scheme " + uri.getScheme());
        }
    }

    @Override
    public Storage getStorage() {
        return this.storage;
    }

    @Override
    public ObservableList<CellData> getCells() {
        return this.cells;
    }

    @Override
    public ReadOnlyStringProperty displayNameProperty() {
        return displayNameProperty;
    }

    @Override
    public String getDisplayName() {
        return displayNameProperty.get();
    }

    @Override
    public ReadOnlyStringProperty locationProperty() {
        return locationProperty;
    }

    @Override
    public String getLocation() {
        return locationProperty.get();
    }

    @Override
    public void save() {
        this.storage.getURI().ifPresent(uri
                -> NotebookPersistence.INSTANCE.toFile(Path.of(uri).toFile(), cells)
        );
    }

    @Override
    public void saveAs(File selectedFile) {
        NotebookPersistence.INSTANCE.toFile(selectedFile, cells);
        this.storage = new FileStorageImpl(selectedFile);
        this.displayNameProperty.set(selectedFile.getName());
        this.locationProperty.set(this.storage.getURI().get().toString());
    }

    @Override
    public void export(ExportMode exportMode, File selectedFile) {
        switch (exportMode) {
            case NO_OUTPUTS ->
                NotebookPersistence.INSTANCE.toFile(selectedFile, cells, false);
            case MARKDOWN ->
                throw new IllegalArgumentException("Markdown export not yet supported");
        }
    }
}
