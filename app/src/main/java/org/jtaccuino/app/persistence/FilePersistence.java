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
package org.jtaccuino.app.persistence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilePersistence {

    private static final FilePersistence INSTANCE =
            PersistenceManager.readPersistenceFile("files", FilePersistence.class).orElse(new FilePersistence());

    public static record OpenFile(Path path) {
    }

    public static record RecentFile(Path path) {
    }

    public static FilePersistence getDefault() {
        return INSTANCE;
    }

    private List<OpenFile> openFiles = new ArrayList<>();
    private List<RecentFile> recentFiles = new ArrayList<>();

    public FilePersistence() {
    }

    public List<OpenFile> getOpenFiles() {
        return Collections.unmodifiableList(openFiles);
    }

    public void setOpenFiles(List<OpenFile> openFiles) {
        this.openFiles = openFiles;
    }

    public List<RecentFile> getRecentFiles() {
        return Collections.unmodifiableList(recentFiles);
    }

    public void setRecentFiles(List<RecentFile> recentFiles) {
        this.recentFiles = recentFiles;
    }

    public void add(OpenFile openFile) {
        openFiles.add(openFile);
    }

    public void remove(OpenFile openFile) {
        openFiles.remove(openFile);
    }

    public void add(RecentFile recentFile) {
        recentFiles.add(recentFile);
    }

    public void remove(RecentFile recentFile) {
        recentFiles.remove(recentFile);
    }

    public void reset() {
        openFiles.clear();
        recentFiles.clear();
    }
}
