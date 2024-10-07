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

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Skin;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.util.Callback;
import org.jtaccuino.core.ui.UiUtils;

public class CompletionPopupSkin implements Skin<CompletionPopup> {

    private final CompletionPopup control;
    private final ListView<String> completionList;
    final int LIST_CELL_HEIGHT = 24;

    public CompletionPopupSkin(CompletionPopup control) {
        this(control, TextFieldListCell.forListView());
    }

    public CompletionPopupSkin(CompletionPopup control, Callback<ListView<String>, ListCell<String>> cellFactory) {
        this.control = control;
        completionList = new ListView<>(control.getSuggestions());

        completionList.prefHeightProperty().bind(
                Bindings.min(control.visibleCompletionsProperty(), Bindings.size(completionList.getItems()))
                        .multiply(LIST_CELL_HEIGHT).add(2));
        completionList.setCellFactory(cellFactory);

        completionList.prefWidthProperty().bind(control.prefWidthProperty());
        completionList.maxWidthProperty().bind(control.maxWidthProperty());
        completionList.minWidthProperty().bind(control.minWidthProperty());

        completionList.setOnMouseClicked(me -> {
            if (me.getButton() == MouseButton.PRIMARY) {
                onSuggestionChoosen(completionList.getSelectionModel().getSelectedItem());
                control.hide();
            }
        });

        completionList.setOnKeyPressed(ke -> {
            switch (ke.getCode()) {
                case TAB -> {
                    if (completionList.getItems().size() == 1) {
                        onSuggestionChoosen(completionList.getItems().getFirst());
                        control.hide();
                    } else {
                        var prefix = UiUtils.longestCommonPrefix(completionList.getItems());
                        if (!prefix.isEmpty()) {
                            onSuggestionChoosen(prefix);
                            control.hide();
                        }
                    }
                }
                case ENTER -> {
                    onSuggestionChoosen(completionList.getSelectionModel().getSelectedItem());
                    control.hide();
                }
                case ESCAPE -> {
                    if (control.isHideOnEscape()) {
                        control.hide();
                    }
                }
                default -> {
                }
            }
        });

        completionList.getItems().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                change.next();
                if (0 == change.getTo()) {
                    control.hide();
                } else {
                    completionList.getSelectionModel().selectFirst();
                }
            }
        });
    }

    private void onSuggestionChoosen(String suggestion) {
        if (suggestion != null) {
            Event.fireEvent(control, new CompletionPopup.CompletionEvent(suggestion));
        }
    }

    @Override
    public Node getNode() {
        return completionList;
    }

    @Override
    public CompletionPopup getSkinnable() {
        return control;
    }

    @Override
    public void dispose() {
    }
}
