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

import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

class CompletionSelectionModel extends MultipleSelectionModel<CompletionItem> {

    private final ObservableList<CompletionItem> items;
    private final ObservableList<Integer> selectedIndicesList = FXCollections.observableArrayList();
    private final ObservableList<CompletionItem> selectedItemsList = FXCollections.observableArrayList();

    public CompletionSelectionModel(ObservableList<CompletionItem> items) {
        this.items = items;
        setSelectedIndex(-1);
        setSelectedItem(null);
        items.addListener((ListChangeListener<CompletionItem>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) {
                    int currentSelected = getSelectedIndex();
                    if (currentSelected != -1 && (currentSelected >= items.size() || items.get(currentSelected) == null)) {
                        int newSelection = findNextSelectableIndex(currentSelected, false); // Try forward from current index
                        if (newSelection == -1) {
                            newSelection = findPrevSelectableIndex(currentSelected - 1, false);
                        }
                        if (newSelection != -1) {
                            select(newSelection);
                        } else {
                            clearSelection();
                        }
                    }
                }
            }
        });
    }

    @Override
    public ObservableList<Integer> getSelectedIndices() {
        return selectedIndicesList;
    }

    @Override
    public ObservableList<CompletionItem> getSelectedItems() {
        return selectedItemsList;
    }

    @Override
    public void select(int index) {
        clearAndSelect(index);
    }

    @Override
    public void select(CompletionItem obj) {
        int index = items.indexOf(obj);
        if (index != -1 && obj != null) {
            select(index);
        } else {
            clearSelection();
        }
    }

    @Override
    public void selectNext() {
        int currentIndex = getSelectedIndex();
        int size = items.size();
        if (size == 0) {
            select(-1); // Clear selection if list is empty
            return;
        }

        int newIndex;
        if (currentIndex == -1) {
            // If nothing is currently selected, try to select the first selectable item
            newIndex = findNextSelectableIndex(0, false);
        } else {
            // Try to find the next item without wrapping
            newIndex = findNextSelectableIndex(currentIndex + 1, false);

            // If no next item found and we were at the last item, then wrap around
            if (newIndex == -1 && currentIndex == size - 1) {
                newIndex = findNextSelectableIndex(0, false); // Search from beginning (wrap-around)
            }
        }
        select(newIndex);
    }

    @Override
    public void selectPrevious() {
        int currentIndex = getSelectedIndex();
        int size = items.size();
        if (size == 0) {
            select(-1); // Clear selection if list is empty
            return;
        }

        int newIndex;
        if (currentIndex == -1) {
            // If nothing is currently selected, try to select the last selectable item (for previous action)
            newIndex = findPrevSelectableIndex(size - 1, false);
        } else {
            // Try to find the previous item without wrapping
            newIndex = findPrevSelectableIndex(currentIndex - 1, false);

            // If no previous item found and we were at the first item, then wrap around
            if (newIndex == -1 && currentIndex == 0) {
                newIndex = findPrevSelectableIndex(size - 1, false); // Search from end (wrap-around)
            }
        }
        select(newIndex);
    }

    private int findNextSelectableIndex(int startIndex, boolean wrapAround) {
        int size = items.size();
        if (size == 0) return -1;

        for (int i = startIndex; i < size; i++) {
            if (!CompletionItem.NIL.equals(items.get(i))) {
                return i;
            }
        }
        if (wrapAround) {
            for (int i = 0; i < startIndex; i++) {
                if (!CompletionItem.NIL.equals(items.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findPrevSelectableIndex(int startIndex, boolean wrapAround) {
        int size = items.size();
        if (size == 0) return -1;

        for (int i = startIndex; i >= 0; i--) {
            if (!CompletionItem.NIL.equals(items.get(i))) {
                return i;
            }
        }
        if (wrapAround) {
            for (int i = size - 1; i > startIndex; i--) {
                if (!CompletionItem.NIL.equals(items.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void selectFirst() {
        select(findNextSelectableIndex(0, false));
    }

    @Override
    public void selectLast() {
        select(findPrevSelectableIndex(items.size() - 1, false));
    }

    @Override
    public void clearSelection(int index) {
        if (getSelectedIndex() == index) {
            clearSelection();
        }
    }

    @Override
    public void clearSelection() {
        if (getSelectedIndex() != -1) {
            // Clear the contents of the internal lists
            selectedIndicesList.clear();
            selectedItemsList.clear();
            // Notify properties that selection has changed
            setSelectedIndex(-1);
            setSelectedItem(null);
        }
    }

    @Override
    public boolean isEmpty() {
        return getSelectedIndex() == -1;
    }

    @Override
    public boolean isSelected(int index) {
        return getSelectedIndex() == index;
    }

    @Override
    public void clearAndSelect(int index) {
        if (index < 0 || index >= items.size()) {
            clearSelection();
            return;
        }
        var candidateItem = items.get(index);
        if (CompletionItem.NIL.equals(candidateItem)) {
            int redirectIndex = findNextSelectableIndex(index + 1, false);
            if (redirectIndex == -1) {
                redirectIndex = findPrevSelectableIndex(index - 1, false);
            }
            if (redirectIndex != -1) {
                clearAndSelect(redirectIndex);
                return;
            } else {
                clearSelection();
            }
        }
        int currentSelectedIndex = getSelectedIndex();
        var currentSelectedItem = getSelectedItem();
        if (currentSelectedIndex == index && Objects.equals(currentSelectedItem, candidateItem)) {
            return;
        }
        selectedIndicesList.clear();
        selectedItemsList.clear();
        setSelectedIndex(index);
        setSelectedItem(candidateItem);
        selectedIndicesList.add(index);
        selectedItemsList.add(candidateItem);
    }

    @Override
    public void selectAll() {
        clearSelection();
    }

    @Override
    public void selectIndices(int index, int... indices) {
        select(index);
    }

    @Override
    public void selectRange(int from, int to) {
        clearSelection();
    }
}
