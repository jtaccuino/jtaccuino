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
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;

public interface Notebook {

    public ObservableList<CellData> getCells();

    public ReadOnlyStringProperty displayNameProperty();

    public String getDisplayName();

    public File getFile();

    public void setFile(File file);

    public URI getURI();

    public void setURI(URI uri);

    public void rename(String newName);

    public void save();

    public void saveToFile(File selectedFile);

    public void exportToFile(File selectedFile);
}
