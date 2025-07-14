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
package org.jtaccuino.core.ui.completion;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

class CompletionPopupSkin implements Skin<CompletionPopup> {

    private final CompletionPopup control;
    private final ListView<CompletionItem> completionList;
    final int LIST_CELL_HEIGHT = 24;

    private final CompletionSelectionModel selectionModel;
    private final double completionItemCharWidth;
    private final double completionItemCharHeight;

    public CompletionPopupSkin(CompletionPopup control) {
        this.control = control;
        completionList = new ListView<>(control.getSuggestions());
        selectionModel = new CompletionSelectionModel(control.getSuggestions());
        completionList.setSelectionModel(selectionModel);

        Text tempText = new Text("Q");
        tempText.setFont(Font.font("Monaspace Argon", 11));
        Bounds charBounds = tempText.getLayoutBounds();
        completionItemCharWidth = charBounds.getWidth();
        completionItemCharHeight = charBounds.getHeight();

        completionList.prefHeightProperty().bind(
                Bindings.min(control.visibleCompletionsProperty(), Bindings.size(completionList.getItems()))
                        .multiply(Math.ceil(completionItemCharHeight*(1+0.25+0.25)+3)).add(2));
        completionList.maxHeightProperty().bind(
                Bindings.min(control.visibleCompletionsProperty(), Bindings.size(completionList.getItems()))
                        .multiply(Math.ceil(completionItemCharHeight*(1+0.25+0.25)+3)).add(2));
        completionList.setCellFactory(new CompletionItemRenderer(control.getSuggestions()));

        completionList.prefWidthProperty().bind(control.prefWidthProperty());
        completionList.maxWidthProperty().bind(control.maxWidthProperty());
        completionList.minWidthProperty().bind(control.minWidthProperty());

        completionList.setOnMouseClicked(me -> {
            if (me.getButton() == MouseButton.PRIMARY) {
                var item = completionList.getSelectionModel().getSelectedItem();
                onSuggestionChoosen(item.completion(), item.anchor());
                control.hide();
            }
        });

        completionList.setOnKeyPressed(ke -> {
            switch (ke.getCode()) {
                case TAB -> {
                    if (completionList.getItems().size() == 1) {
                        var item = completionList.getItems().getFirst();
                        onSuggestionChoosen(item.completion(), item.anchor());
                        control.hide();
                    } else {
                        var prefix = CompletionItem.longestCommonPrefix(completionList.getItems());
                        if (!prefix.isEmpty()) {
                            onSuggestionChoosen(prefix, completionList.getItems().getFirst().anchor());
                            control.hide();
                        }
                    }
                    ke.consume();
                }
                case ENTER -> {
                    var item = completionList.getSelectionModel().getSelectedItem();
                    onSuggestionChoosen(item.completion(), item.anchor());
                    control.hide();
                    ke.consume();
                }
                case ESCAPE -> {
                    if (control.isHideOnEscape()) {
                        control.hide();
                    }
                    ke.consume();
                }
                default -> {
                }
            }
        });

        completionList.getItems().addListener((ListChangeListener.Change<? extends CompletionItem> change) -> {
            change.next();
            if (0 == change.getTo()) {
                control.hide();
            } else {
                completionList.getSelectionModel().selectFirst();
                var needsScrollBar = completionList.getItems().size() > getSkinnable().getVisibleCompletions();
                double width = Math.min(calculateListViewPreferredWidth(needsScrollBar), 400);
                getSkinnable().setMinWidth(width);
                getSkinnable().setPrefWidth(width);
                getSkinnable().setMaxWidth(width);
            }
        });

        completionList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            int oldSelectedIndex = completionList.getSelectionModel().getSelectedIndex(); // Capture current selection
            int newCalculatedIndex = oldSelectedIndex; // To store the index after our selection logic

            if (event.getCode() == KeyCode.UP) {
                completionList.getSelectionModel().selectPrevious();
                newCalculatedIndex = completionList.getSelectionModel().getSelectedIndex();
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                completionList.getSelectionModel().selectNext();
                newCalculatedIndex = completionList.getSelectionModel().getSelectedIndex();
                event.consume();
            } else if (event.getCode() == KeyCode.HOME) {
                completionList.getSelectionModel().selectFirst();
                newCalculatedIndex = completionList.getSelectionModel().getSelectedIndex();
                event.consume();
            } else if (event.getCode() == KeyCode.END) {
                completionList.getSelectionModel().selectLast();
                newCalculatedIndex = completionList.getSelectionModel().getSelectedIndex();
                event.consume();
            }

            if (newCalculatedIndex != -1) {
                completionList.getFocusModel().focus(newCalculatedIndex);
            } else {
                completionList.getFocusModel().focus(-1);
            }

            if (newCalculatedIndex != oldSelectedIndex) {
                final int finalNewCalculatedIndex = newCalculatedIndex; // Needs to be final for lambda

                Platform.runLater(() -> {
                    completionList.getFocusModel().focus(finalNewCalculatedIndex);

                    completionList.scrollTo(finalNewCalculatedIndex - control.getVisibleCompletions() + 1);
                });
            }
        });

        control.setOnShown((t) -> {
            Platform.runLater(() -> completionList.requestFocus());
        });
    }

    private double calculateListViewPreferredWidth(boolean needsScrollBar) {
        if (control.getSuggestions().isEmpty()) {
            return Region.USE_PREF_SIZE;
        }

        double maxWidth = control.getSuggestions()
                .stream().mapToInt(c -> c.completion().length()).max().orElse(0) * completionItemCharWidth;

        double cellHorizontalPadding = 10;

        Insets listViewPadding = completionList.getPadding();
        double totalListViewPadding = listViewPadding.getLeft() + listViewPadding.getRight();

        double verticalScrollbarAllowance = 15;

        return maxWidth + cellHorizontalPadding + totalListViewPadding + (needsScrollBar ? verticalScrollbarAllowance : 0);
    }

    private void onSuggestionChoosen(String suggestion, int anchor) {
        if (suggestion != null) {
            Event.fireEvent(control, new CompletionPopup.CompletionEvent(suggestion, anchor));
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
