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
package org.jtaccuino.core.ui.completion;

import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
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
                setText(null != item ? item.completion() : "");
                getStyleClass().removeAll("type-matches", "separator-cell");
                if (empty) {
                    setGraphic(null);
                    setFocusTraversable(false);
                } else if (CompletionItem.NIL.equals(item)) {
                    setText(null);
                    setGraphic(separator);
                    getStyleClass().add("separator-cell");
                    setFocusTraversable(false);
                } else {
                    setGraphic(null);
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
}
