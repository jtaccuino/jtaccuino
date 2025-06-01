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
package org.jtaccuino.jshell.extensions.use;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class UseExtension implements JShellExtension {

    public static interface OnDemandExtension {

        public Class<? extends JShellExtension> getExtensionClass();
    }

    private final ReactiveJShell rjs;

    private UseExtension(ReactiveJShell jshell) {
        this.rjs = jshell;
    }

    @Override
    public Optional<String> shellVariableName() {
        return Optional.of("useExtension");
    }

    private String toSnakeCaseEnumName(String extensionShortName) {
        if (extensionShortName == null) {
            return null;
        }
        if (extensionShortName.isEmpty()) {
            return "";
        }

        StringBuilder snakeCaseBuilder = new StringBuilder();
        int n = extensionShortName.length();

        for (int i = 0; i < n; i++) {
            char currentChar = extensionShortName.charAt(i);

            // If it's an uppercase character
            if (Character.isUpperCase(currentChar)) {
                // Check if it's NOT the first character
                // AND if the previous character was lowercase (e.g., "myVariable" -> "my_variable")
                // OR if it's part of a sequence of uppercase characters where
                //    the next character is lowercase (e.g., "HTTPRequest" -> "http_request")
                if (i > 0
                        && (Character.isLowerCase(extensionShortName.charAt(i - 1))
                        || (i + 1 < n && Character.isLowerCase(extensionShortName.charAt(i + 1))))) {
                    snakeCaseBuilder.append('_');
                }
                snakeCaseBuilder.append(currentChar);
            } else {
                // If it's a lowercase character, just append it
                snakeCaseBuilder.append(currentChar);
            }
        }

        return snakeCaseBuilder.toString().toUpperCase(Locale.ENGLISH);
    }

    @Override
    public Optional<String> initCodeSnippet() {
        String enums = JShellExtension.Mode.ON_DEMAND.getExtensionClasses()
                .stream()
                .peek(System.out::println)
                .map(e -> toSnakeCaseEnumName(e.getSimpleName()) + "(" + e.getName() + ".class)")
                .collect(Collectors.joining(",\n"));

        return Optional.of("enum JTExtension implements org.jtaccuino.jshell.extensions.use.UseExtension.OnDemandExtension {\n" + enums + ";\n"
                + """
                              final private Class<? extends org.jtaccuino.jshell.extensions.JShellExtension> extensionClass;

                              public Class<? extends org.jtaccuino.jshell.extensions.JShellExtension> getExtensionClass() {
                                return extensionClass;
                              }

                              JTExtension(Class<? extends org.jtaccuino.jshell.extensions.JShellExtension> exClass) {
                                this.extensionClass = exClass;
                              }
                           }
                           public void use(String extensionShortName) {
                             useExtension.use(extensionShortName);
                           }
                           public void use(org.jtaccuino.jshell.extensions.use.UseExtension.OnDemandExtension extensionIdentifier) {
                             useExtension.use(extensionIdentifier);
                           }
                           """);
    }

    public void use(String extensionShortName) {
        JShellExtension.Mode.ON_DEMAND.getFactoryForExtension(extensionShortName)
                .ifPresent(rjs::activateExtension);
    }

    public void use(OnDemandExtension extension) {
        JShellExtension.Mode.ON_DEMAND.getFactoryForExtension(extension.getExtensionClass())
                .ifPresent(rjs::activateExtension);
    }

    @Descriptor(mode = Mode.SYSTEM, type = UseExtension.class)
    public static class FactoryImpl implements Factory {

        @Override
        public UseExtension createExtension(ReactiveJShell jshell) {
            return new UseExtension(jshell);
        }
    }
}
