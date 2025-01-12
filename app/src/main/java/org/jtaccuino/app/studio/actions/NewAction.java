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
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.app.studio.SheetManager;
import org.jtaccuino.core.ui.api.AbstractAction;

public final class NewAction extends AbstractAction {

    public static final NewAction INSTANCE = new NewAction();

    private NewAction() {
        super("file/new",
            "New",
            "Meta+N");
    }

    @Override
    public void handle(ActionEvent t) {
        SheetManager.getDefault().open(NotebookPersistence.INSTANCE.of());
    }
}
