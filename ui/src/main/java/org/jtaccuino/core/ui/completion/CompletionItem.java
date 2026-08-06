/*
 * Copyright 2025-2026 JTaccuino Contributors
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

import java.util.List;
import javax.lang.model.element.ElementKind;
import org.jtaccuino.jshell.ReactiveJShell;

public record CompletionItem(String completion, boolean matchesType, int anchor,
        ElementKind elementKind, boolean keyword, boolean staticMember, String displayName, String typeInfo) {

    public static CompletionItem NIL = new CompletionItem("N/A", false, 0, null, false, false, "", "");

    public static String longestCommonPrefix(List<CompletionItem> completionItems) {
        return switch (completionItems) {
            case null ->
                "";
            case List<CompletionItem> l when l.isEmpty() ->
                "";
            default -> {
                String firstString = completionItems.getFirst().completion();
                int minLen = firstString.length();
                for (int i = 0; i < minLen; i++) {
                    char currentChar = firstString.charAt(i);
                    for (CompletionItem item : completionItems) {
                        if (item.completion().length() < i + 1 || item.completion().charAt(i) != currentChar) {
                            yield firstString.substring(0, i);
                        }
                    }
                }
                yield firstString;
            }
        };
    }

    public boolean notMatchesType() {
        return !matchesType();
    }

    public static CompletionItem from(ReactiveJShell.CompletionItem item) {
        return new CompletionItem(item.completion(), item.matchesType(), item.anchor(), item.elementKind(), item.keyword(),
                item.staticMember(), item.displayName(), item.typeInfo());
    }
}
