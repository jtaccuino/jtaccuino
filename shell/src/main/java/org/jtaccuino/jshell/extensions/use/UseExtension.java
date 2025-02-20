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

import java.util.Optional;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class UseExtension implements JShellExtension {

    private final ReactiveJShell rjs;

    private UseExtension(ReactiveJShell jshell) {
        this.rjs = jshell;
    }

    @Override
    public Optional<String> shellVariableName() {
        return Optional.of("useExtension");
    }

    @Override
    public Optional<String> initCodeSnippet() {
        return Optional.of("""
                           public void use(String extensionShortName) {
                             useExtension.use(extensionShortName);
                           }
                           """);
    }

    public void use(String extensionShortName) {
        JShellExtension.Mode.ON_DEMAND.getExtensionFactories()
                .stream()
                .filter(f -> f.getClass().getName().contains(extensionShortName))
                .findFirst()
                .ifPresent(f -> rjs.activateExtension(f));
    }

    @Descriptor(mode = Mode.SYSTEM, type = UseExtension.class)
    public static class FactoryImpl implements Factory {

        @Override
        public UseExtension createExtension(ReactiveJShell jshell) {
            return new UseExtension(jshell);
        }
    }
}
