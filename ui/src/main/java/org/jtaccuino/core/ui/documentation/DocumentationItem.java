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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record DocumentationItem(String fullType, String methodName, String returnType, List<Argument> arguments, List<String> exceptions) {

    private static final DocumentationItem EMPTY = new DocumentationItem("", "", "", List.of(), List.of());

    public static DocumentationItem from(String docString) {

        if (docString.isEmpty()) {
            return EMPTY;
        }

        int returnTypeBegin = 0;
        int returnTypeEnd = docString.indexOf(" ");
        int argsStart = docString.indexOf("(");
        int argsEnd = docString.indexOf(")");
        var fullName = returnTypeEnd + 1 < argsStart ? docString.substring(returnTypeEnd + 1, argsStart) : docString.substring(0, argsStart);
        int typeEnd = fullName.lastIndexOf(".");
        var fullType = typeEnd != -1 ? fullName.substring(0, typeEnd) : "";
        var returnType = returnTypeEnd != -1 && returnTypeEnd + 1 < argsStart ? docString.substring(returnTypeBegin, returnTypeEnd) : "";
        var methodName = fullName.substring(typeEnd + 1);
        var args = docString.contains("()") ? List.<Argument>of()
                : ((argsEnd - 1 - (argsStart + 1)) == 0) ? List.<Argument>of() : Arrays.stream(docString.substring(argsStart + 1, argsEnd).split(", "))
                        .map(arg -> arg.split(" "))
                        .map(p -> new Argument(p[0], p.length > 1 ? p[1] : ""))
                        .toList();
        var exceptionStart = docString.lastIndexOf(" throws ");
        var exceptions = !docString.contains(" throws ") ? List.<String>of() : Arrays.stream(docString.substring(exceptionStart + 8, docString.length()).split(", "))
                .toList();
        return new DocumentationItem(fullType, methodName, returnType, args, exceptions);
    }

    public String itemString() {
        return methodName() + "("
                + arguments().stream().map(a -> a.type() + (a.name().isEmpty() ? "" : " " + a.name())).collect(Collectors.joining(", "))
                + ")"
                + (exceptions.isEmpty() ? "" : " throws " + exceptions().stream().collect(Collectors.joining(", ")));
    }

    public record Argument(String type, String name) {
    }
}
