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
package org.jtaccuino.core.ui.documentation;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

class DocumentationRenderer implements Callback<ListView<DocumentationItem>, ListCell<DocumentationItem>> {

    private int lineLength;

    @Override
    public ListCell<DocumentationItem> call(ListView<DocumentationItem> p) {
        return new ListCell<DocumentationItem>() {
            @Override
            protected void updateItem(DocumentationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setFocusTraversable(false);
                    setText(null);
                } else {
                    setGraphic(null);
                    setFocusTraversable(true);
                    var padding = lineLength-item.itemString().length()-item.returnType().length() + 1;
                    setText(item.itemString() + " ".repeat(padding) + item.returnType());
                }
            }
        };
    }

    void setLineLength(int lineLength) {
        this.lineLength = lineLength;
    }
}
