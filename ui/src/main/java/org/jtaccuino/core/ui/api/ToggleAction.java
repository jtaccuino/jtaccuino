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
package org.jtaccuino.core.ui.api;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyCombination;

public final class ToggleAction implements Action {

    private Action activeAction = null;
    private final Action actionA;
    private final Action actionB;

    public ToggleAction(Action actionA, Action actionB) {
        this.actionA = actionA;
        this.actionB = actionB;
        this.activeAction = actionA;
    }

    @Override
    public final void handle(ActionEvent event) {
        this.activeAction.handle(event);
        this.activeAction = (activeAction == actionA) ? actionB : actionA;
    }

    @Override
    public KeyCombination getAccelerator() {
        return this.activeAction.getAccelerator();
    }

    @Override
    public String getDisplayString() {
        return this.activeAction.getDisplayString();
    }

    @Override
    public String getName() {
        return this.activeAction.getName();
    }

    @Override
    public ReadOnlyBooleanProperty enabledProperty() {
        return this.activeAction.enabledProperty();
    }
}
