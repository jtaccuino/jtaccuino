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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompletionItemComparatorTest {

    @Test
    public void sortsAlphabeticallyWithinMatchingGroup() {
        var items = new ArrayList<>(List.of(
                item("banana", true),
                item("Apple", true),
                item("cherry", true)));
        items.sort(CompletionItemComparator.BY_PRIORITY);
        assertEquals(List.of("Apple", "banana", "cherry"), names(items));
    }

    @Test
    public void keepsMatchesBeforeNonMatches() {
        var items = new ArrayList<>(List.of(
                item("zebra", false),
                item("apple", true),
                item("yak", false),
                item("banana", true)));
        items.sort(CompletionItemComparator.BY_PRIORITY);
        assertEquals(List.of("apple", "banana", "yak", "zebra"), names(items));
    }

    @Test
    public void sortsAlphabeticallyWithinNonMatchingGroup() {
        var items = new ArrayList<>(List.of(
                item("delta", false),
                item("bravo", false),
                item("charlie", false)));
        items.sort(CompletionItemComparator.BY_PRIORITY);
        assertEquals(List.of("bravo", "charlie", "delta"), names(items));
    }

    @Test
    public void caseInsensitiveWithCaseSensitiveTieBreak() {
        var items = new ArrayList<>(List.of(
                item("apple", true),
                item("Apple", true),
                item("APPLE", true)));
        items.sort(CompletionItemComparator.BY_PRIORITY);
        assertEquals(List.of("APPLE", "Apple", "apple"), names(items));
    }

    @Test
    public void fallsBackToCompletionForKeywords() {
        var items = new ArrayList<>(List.of(
                keyword("while "),
                keyword("assert "),
                keyword("break ")));
        items.sort(CompletionItemComparator.BY_PRIORITY);
        assertEquals(List.of("assert ", "break ", "while "), names(items));
    }

    @Test
    public void alphabeticalModeSortsByTextOnly() {
        var items = new ArrayList<>(List.of(
                item("zebra", true),
                item("apple", false),
                item("banana", true)));
        items.sort(CompletionItemComparator.ALPHABETICAL);
        assertEquals(List.of("apple", "banana", "zebra"), names(items));
    }

    private static CompletionItem item(String name, boolean matchesType) {
        return new CompletionItem(name, matchesType, 0, null, false, false, name, "", "", "", () -> "");
    }

    private static CompletionItem keyword(String keyword) {
        return new CompletionItem(keyword, false, 0, null, true, false, "", "", "", "", () -> "");
    }

    private static List<String> names(List<CompletionItem> items) {
        return items.stream()
                .map(item -> item.displayName().isBlank() ? item.completion() : item.displayName())
                .toList();
    }
}
