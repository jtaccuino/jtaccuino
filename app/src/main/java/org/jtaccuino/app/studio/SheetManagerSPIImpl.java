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
package org.jtaccuino.app.studio;

import java.nio.file.Path;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.app.persistence.FilePersistence;
import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.SheetManager;
import org.jtaccuino.core.ui.spi.SheetManagerSPI;

public class SheetManagerSPIImpl extends SheetManagerSPI{

    private ObservableList<SheetManager.RecentFile> recentFiles =
            FXCollections.observableArrayList(
                    FilePersistence.getDefault().getRecentFiles().stream().map(rf -> from(rf.path())).toList());
    private ReadOnlyListWrapper<SheetManager.RecentFile> unmodifiableRecentFiles = new ReadOnlyListWrapper<>(recentFiles);

    private static SheetManager.RecentFile from(Path path) {
        return new SheetManager.RecentFile(path.toFile().getName(), path);
    }

    @Override
    public Sheet of() {
        return Sheet.of(NotebookPersistence.INSTANCE.of());
    }

    @Override
    public void addToRecentNotebooks(Path filePath) {
        var recentFileCandidate = from(filePath);
        recentFiles.remove(recentFileCandidate);
        recentFiles.add(0, recentFileCandidate);
        while (Integer.getInteger("org.jtaccuino.recentFiles.max", 5) < recentFiles.size()) {
            recentFiles.removeLast();
        }
    }

    @Override
    public void removeFromRecentNotebooks(Path filePath) {
        var recentFileCandidate = from(filePath);
        recentFiles.remove(recentFileCandidate);
    }

    @Override
    public SimpleListProperty<SheetManager.RecentFile> getRecentFiles() {
        return unmodifiableRecentFiles;
    }
}
