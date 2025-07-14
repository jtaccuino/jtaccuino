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
package org.jtaccuino.core.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jtaccuino.core.ui.api.Action;

public class UiUtils {

    public static Button createSVGToolbarButton(String styleClass, String toolbarStyle, Action action) {
        var button = new Button();
        button.setTooltip(new Tooltip(action.getDisplayString() + " (" + action.getAccelerator().getDisplayText() + ")"));
        button.getStyleClass().add(toolbarStyle);
        var region = new Region();
        region.getStyleClass().addAll("toolbar-button-graphics", styleClass);
        button.setGraphic(region);
        button.setOnAction(action);
        return button;
    }

    public static Button createSVGToolbarButton(String styleClass, String tooltip, String toolbarStyle, EventHandler<ActionEvent> handler) {
        var button = new Button();
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add(toolbarStyle);
        var region = new Region();
        region.getStyleClass().addAll("toolbar-button-graphics", styleClass);
        button.setGraphic(region);
        button.setOnAction(handler);
        return button;
    }

    public static ToggleButton createSVGToggleToolbarButton(String styleClass, String toolbarStyle, Action action) {
        var button = new ToggleButton();
        button.setTooltip(new Tooltip(action.getDisplayString() + " (" + action.getAccelerator().getDisplayText() + ")"));
        button.getStyleClass().add(toolbarStyle);
        var region = new Region();
        region.getStyleClass().addAll("toolbar-button-graphics", styleClass);
        button.setGraphic(region);
        button.setOnAction(action);
        return button;
    }

    public static ToggleButton createSVGToggleToolbarButton(String styleClass, String tooltip, String toolbarStyle, EventHandler<ActionEvent> handler) {
        var button = new ToggleButton();
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add(toolbarStyle);
        var region = new Region();
        region.getStyleClass().addAll("toolbar-button-graphics", styleClass);
        button.setGraphic(region);
        button.setOnAction(handler);
        return button;
    }
}
