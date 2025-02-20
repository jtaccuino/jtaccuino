/*
 * Copyright 2024-2025 JTaccuino Contributors
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
package org.jtaccuino.jshell.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.jtaccuino.jshell.ReactiveJShell;

public interface JShellExtension {

    public static enum Mode {
        /**
         * Automatically instantiated and initialized at startup of shell
         */
        SYSTEM,
        /**
         * Activated on event occuring in the system / shell,
         * e.g. classpath change by addDependency - currently unused
         */
        ON_EVENT,
        /**
         * Activated on explicit request issued by shell interaction,
         * possible future syntax e.g.
         * {@snippet id='on_demand_example' :
         *   use("MyExtensionIdentifier")
         * }
         * currently unused
         */
        ON_DEMAND;

        public List<JShellExtension.Factory> getExtensionFactories() {
            var factories = ServiceLoader.load(JShellExtension.Factory.class);
            return factories
                    .stream()
                    .filter(p -> this == p.type().getAnnotation(JShellExtension.Descriptor.class).mode())
                    .map(p -> p.get()).toList();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Descriptor {

        Mode mode() default Mode.ON_DEMAND;
        Class<? extends JShellExtension> type();
    }

    public static interface Factory {
        public JShellExtension createExtension(ReactiveJShell jshell);
    }

    public default Optional<String> shellVariableName() {
        return Optional.empty();
    }

    public default Optional<String> initCodeSnippet() {
        return Optional.empty();
    }
}
