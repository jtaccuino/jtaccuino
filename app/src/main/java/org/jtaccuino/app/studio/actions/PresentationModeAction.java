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
package org.jtaccuino.app.studio.actions;

import javafx.event.ActionEvent;
import org.jtaccuino.app.ui.WindowSystem;
import org.jtaccuino.core.ui.api.AbstractAction;
import org.jtaccuino.core.ui.api.ToggleAction;

public class PresentationModeAction {

    private static class PresentationModeOn extends AbstractAction {

        private PresentationModeOn() {
            super("file/open",
                "Enable Presentation Mode",
                "Meta+Ctrl+Alt+P");
        }

        @Override
        public void handle(ActionEvent event) {
            WindowSystem.getDefault().togglePresentationMode(true);
        }
    }

    private static class PresentationModeOff extends AbstractAction {

        private PresentationModeOff() {
            super("file/open",
                "Disable Presentation Mode",
                "Meta+Ctrl+Alt+P");
        }

        @Override
        public void handle(ActionEvent event) {
            WindowSystem.getDefault().togglePresentationMode(false);
        }
    }

    public static final ToggleAction INSTANCE = new ToggleAction(new PresentationModeOn(), new PresentationModeOff());
}
