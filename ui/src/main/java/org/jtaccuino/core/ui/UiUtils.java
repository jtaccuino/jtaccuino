/*
 * Copyright 2024 JTaccuino Contributors
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

import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

public class UiUtils {

    public static Button createSVGToolbarButton(String styleClass, String tooltip, String toolbarStyle) {
        var button = new Button();
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add(toolbarStyle);
        var region = new Region();
        region.getStyleClass().addAll("toolbar-button-graphics", styleClass);
        button.setGraphic(region);
        return button;
    }

    public static ToggleButton createSVGToggleToolbarButton(String styleClass, String tooltip, String toolbarStyle) {
        var button = new ToggleButton();
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add(toolbarStyle);
        var region = new Region();
        region.getStyleClass().addAll("toolbar-button-graphics", styleClass);
        button.setGraphic(region);
        return button;
    }

    public static String longestCommonPrefix(List<String> strings) {
        return switch (strings) {
            case null ->
                "";
            case List<String> l when l.isEmpty() ->
                "";
            default -> {
                String firstString = strings.getFirst();
                int minLen = firstString.length();

                for (int i = 0; i < minLen; i++) {
                    char currentChar = firstString.charAt(i);
                    for (String str : strings) {
                        if (str.length() < i + 1 || str.charAt(i) != currentChar) {
                            yield firstString.substring(0, i);
                        }
                    }
                }
                yield firstString;
            }
        };
    }
}
