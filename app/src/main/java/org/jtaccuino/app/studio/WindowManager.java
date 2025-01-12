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

import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Central hub for interacting with the core UI Elements of JTaccuino
 * In a first prototype everthing is simply implemented here
 */
public class WindowManager {

    private static final WindowManager INSTANCE = new WindowManager();

    private WindowManager() {
        // empty private instance generation
    }

    /**
     * Get the default instance of the window manager
     * @return instance of window manager
     */
    public static WindowManager getDefault() {
        return INSTANCE;
    }

    /**
     * Returns the main window of JTaccuino.
     * This shall be used only in cases, where access o the windows hierarchy is necessary.
     * @return the JavaFX window reference
     */
    public Window getMainWindow() {
        // For now assume the first window is the correct one.
        return Stage.getWindows().getFirst();
    }
}
