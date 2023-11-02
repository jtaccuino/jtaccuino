package org.jtaccuino;

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
                            System.out.println("Current: " + firstString.substring(0, i));
                            yield firstString.substring(0, i);
                        }
                    }
                    System.out.println("Current: " + firstString.substring(0, i));
                }
                System.out.println("Current: " + firstString);
                yield firstString;
            }
        };
    }

}
