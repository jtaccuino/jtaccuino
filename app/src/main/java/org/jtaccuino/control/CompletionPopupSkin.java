package org.jtaccuino.control;

import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Skin;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.util.Callback;
import org.jtaccuino.UiUtils;

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
                        .multiply(LIST_CELL_HEIGHT).add(18));
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
