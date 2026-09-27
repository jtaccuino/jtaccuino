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

import java.util.Comparator;

/**
 * Comparator for completion items, modelled on NetBeans'
 * {@code CompletionItemComparator}. Items are ordered by a sort priority first
 * (type matches before non-matching suggestions) and alphabetically by their
 * sort text within the same priority, so each group is ordered and the groups
 * stay separated. The text comparison is case insensitive with a case sensitive
 * tie break, which keeps the ordering stable and predictable.
 */
final class CompletionItemComparator implements Comparator<CompletionItem> {

    /**
     * Orders by sort priority first, then alphabetically by sort text.
     */
    static final Comparator<CompletionItem> BY_PRIORITY = new CompletionItemComparator(true);

    /**
     * Orders alphabetically by sort text first, then by sort priority.
     */
    static final Comparator<CompletionItem> ALPHABETICAL = new CompletionItemComparator(false);

    private final boolean byPriority;

    private CompletionItemComparator(boolean byPriority) {
        this.byPriority = byPriority;
    }

    @Override
    public int compare(CompletionItem i1, CompletionItem i2) {
        if (i1 == i2) {
            return 0;
        }
        int priorityDiff = Integer.compare(priority(i1), priority(i2));
        int textDiff = compareText(sortText(i1), sortText(i2));
        return byPriority
                ? (priorityDiff != 0 ? priorityDiff : textDiff)
                : (textDiff != 0 ? textDiff : priorityDiff);
    }

    private static int priority(CompletionItem item) {
        return item.matchesType() ? 0 : 1;
    }

    /**
     * The text used to order items alphabetically. Falls back to the completion
     * text for items without a display name (e.g. keywords).
     */
    private static String sortText(CompletionItem item) {
        var text = item.displayName();
        return text == null || text.isBlank() ? item.completion() : text;
    }

    private static int compareText(String text1, String text2) {
        String t1 = text1 == null ? "" : text1;
        String t2 = text2 == null ? "" : text2;
        int caseInsensitive = String.CASE_INSENSITIVE_ORDER.compare(t1, t2);
        return caseInsensitive != 0 ? caseInsensitive : t1.compareTo(t2);
    }
}
