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
package org.jtaccuino.core.ui.spi;

import java.nio.file.Path;
import javafx.beans.property.SimpleListProperty;
import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.SheetManager.RecentFile;

public abstract class SheetManagerSPI {

    public abstract Sheet of();

    public abstract void addToRecentNotebooks(Path filePath);

    public abstract void removeFromRecentNotebooks(Path filePath);

    public abstract SimpleListProperty<RecentFile> getRecentFiles();
}
