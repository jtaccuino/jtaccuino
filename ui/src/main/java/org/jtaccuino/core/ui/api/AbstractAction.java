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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public abstract class AbstractAction implements Action {

    private final String name;
    private final String displayText;
    private final String keyCode;
    private final SimpleBooleanProperty enabledProperty = new SimpleBooleanProperty(true);

    protected AbstractAction(String name, String displayText, String keyCode) {
        this.name = name;
        this.displayText = displayText;
        this.keyCode = keyCode;
    }

    @Override
    public KeyCombination getAccelerator() {
        return this.keyCode.isEmpty() ? KeyCodeCombination.NO_MATCH : KeyCodeCombination.valueOf(this.keyCode);
    }

    @Override
    public String getDisplayString() {
        return this.displayText;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ReadOnlyBooleanProperty enabledProperty() {
        return enabledProperty;
    }

    protected BooleanProperty enabled() {
        return enabledProperty;
    }

    protected void enable() {
        enabledProperty.set(true);
    }

    protected void disable() {
        enabledProperty.set(false);
    }
}
