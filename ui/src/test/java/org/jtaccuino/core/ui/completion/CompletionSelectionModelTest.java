/*
 * Copyright 2026 JTaccuino Contributors
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

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CompletionSelectionModelTest {

    @Test
    public void keepsSelectionInBoundsWhenListShrinks() {
        var items = FXCollections.observableArrayList(items(10));
        var model = new CompletionSelectionModel(items);
        model.select(8);
        assertEquals(8, model.getSelectedIndex());

        assertDoesNotThrow(() -> items.setAll(items(2)));
        assertEquals(1, model.getSelectedIndex());
    }

    @Test
    public void keepsSelectionInBoundsWhenListShrinksToSingleItem() {
        var items = FXCollections.observableArrayList(items(10));
        var model = new CompletionSelectionModel(items);
        model.select(9);

        assertDoesNotThrow(() -> items.setAll(items(1)));
        assertEquals(0, model.getSelectedIndex());
    }

    @Test
    public void clearsSelectionWhenListBecomesEmpty() {
        var items = FXCollections.observableArrayList(items(3));
        var model = new CompletionSelectionModel(items);
        model.select(2);

        assertDoesNotThrow(items::clear);
        assertEquals(-1, model.getSelectedIndex());
    }

    private static List<CompletionItem> items(int count) {
        var result = new ArrayList<CompletionItem>();
        for (int i = 0; i < count; i++) {
            result.add(item("item" + i));
        }
        return result;
    }

    private static CompletionItem item(String name) {
        return new CompletionItem(name, true, 0, null, false, false, name, "", "", "", () -> "");
    }
}
