/*
 * Copyright 2025-2026 JTaccuino Contributors
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
package org.jtaccuino.core.ui.completion;

import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

class CompletionItemRenderer implements Callback<ListView<CompletionItem>, ListCell<CompletionItem>> {

    private final Separator separator = new Separator(Orientation.HORIZONTAL);
    private final ObservableList<CompletionItem> items;

    public CompletionItemRenderer(ObservableList<CompletionItem> items) {
        this.items = items;
    }

    @Override
    public ListCell<CompletionItem> call(ListView<CompletionItem> p) {
        return new ListCell<CompletionItem>() {
            @Override
            protected void updateItem(CompletionItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                getStyleClass().removeAll("type-matches", "separator-cell");
                if (empty) {
                    setGraphic(null);
                    setFocusTraversable(false);
                } else if (CompletionItem.NIL.equals(item)) {
                    setGraphic(separator);
                    getStyleClass().add("separator-cell");
                    setFocusTraversable(false);
                } else {
                    setGraphic(createContent(item));
                    setFocusTraversable(true);
                    if (item.matchesType()) {
                        getStyleClass().add("type-matches");
                    }
                    int nilIndex = items.indexOf(CompletionItem.NIL);
                    int itemIndex = items.indexOf(item);
                    boolean isEven = (itemIndex > nilIndex ? itemIndex - 1 : itemIndex) % 2 == 0;
                    pseudoClassStateChanged(PseudoClass.getPseudoClass("even"), isEven);
                    pseudoClassStateChanged(PseudoClass.getPseudoClass("odd"), !isEven);
                }
            }
        };
    }

    private static Node createContent(CompletionItem item) {
        var name = new Label(item.displayName());
        name.getStyleClass().add("completion-name");
        var type = new Label(item.typeInfo());
        type.getStyleClass().add("completion-type");
        var content = new BorderPane();
        var icon = createIcon(item);
        content.setLeft(icon);
        BorderPane.setMargin(icon, new Insets(0, 6, 0, 0));
        BorderPane.setAlignment(name, Pos.CENTER_LEFT);
        content.setCenter(name);
        content.setRight(type);
        return content;
    }

    private static ElementKindIcon createIcon(CompletionItem item) {
        var icon = new ElementKindIcon(item.elementKind(), item.keyword(), item.staticMember());
        icon.getStyleClass().add(styleClass(item));
        return icon;
    }

    private static String styleClass(CompletionItem item) {
        if (item.keyword()) {
            return "completion-icon-keyword";
        }
        return switch (item.elementKind()) {
            case CLASS -> "completion-icon-class";
            case INTERFACE -> "completion-icon-interface";
            case ENUM, ENUM_CONSTANT -> "completion-icon-enum";
            case RECORD -> "completion-icon-record";
            case ANNOTATION_TYPE -> "completion-icon-annotation";
            case METHOD, CONSTRUCTOR -> "completion-icon-method";
            case FIELD -> "completion-icon-field";
            case PACKAGE, MODULE -> "completion-icon-package";
            case PARAMETER, LOCAL_VARIABLE, RESOURCE_VARIABLE, EXCEPTION_PARAMETER, TYPE_PARAMETER -> "completion-icon-variable";
            case null -> "completion-icon-keyword";
            default -> "completion-icon-keyword";
        };
    }
}
