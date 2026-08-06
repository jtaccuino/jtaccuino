/*
 * Copyright 2024-2026 JTaccuino Contributors
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

    public CompletionPopupSkin(CompletionPopup control) {
        this.control = control;
        completionList = new ListView<>(control.getSuggestions());
        selectionModel = new CompletionSelectionModel(control.getSuggestions());
        completionList.setSelectionModel(selectionModel);

        completionList.prefHeightProperty().bind(
                Bindings.min(control.visibleCompletionsProperty(), Bindings.size(completionList.getItems()))
                        .multiply(LIST_CELL_HEIGHT).add(2));
        completionList.maxHeightProperty().bind(
                Bindings.min(control.visibleCompletionsProperty(), Bindings.size(completionList.getItems()))
                        .multiply(LIST_CELL_HEIGHT).add(2));
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
                double width = calculateListViewPreferredWidth(needsScrollBar);
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

        Font nameFont = Font.font("Monaspace Argon", 11);
        Font typeFont = Font.font("Monaspace Argon", 10);
        double maxNameWidth = 0;
        double maxTypeWidth = 0;
        for (CompletionItem item : control.getSuggestions()) {
            if (CompletionItem.NIL.equals(item)) {
                continue;
            }
            maxNameWidth = Math.max(maxNameWidth, measureText(item.displayName(), nameFont));
            if (!item.typeInfo().isEmpty()) {
                maxTypeWidth = Math.max(maxTypeWidth, measureText(item.typeInfo(), typeFont));
            }
        }

        double completionIconAllowance = 24;
        double nameTypeGap = 16;
        double cellHorizontalPadding = 10;

        Insets listViewPadding = completionList.getPadding();
        double totalListViewPadding = listViewPadding.getLeft() + listViewPadding.getRight();

        double verticalScrollbarAllowance = needsScrollBar ? 15 : 0;

        double contentWidth = maxNameWidth + (maxTypeWidth > 0 ? nameTypeGap + maxTypeWidth : 0);

        return contentWidth + completionIconAllowance + cellHorizontalPadding
                + totalListViewPadding + verticalScrollbarAllowance;
    }

    private static double measureText(String text, Font font) {
        Text tempText = new Text(text);
        tempText.setFont(font);
        return tempText.getLayoutBounds().getWidth();
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
