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
package org.jtaccuino.extension.dflib;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class DfLibExtension implements JShellExtension {

    private static final List<String> DEPS = List.of(
            "org.dflib:dflib:2.0.0-M4",
            "org.dflib:dflib-csv:2.0.0-M4"
    );

    private static final List<String> IMPORTS = List.of(
            "org.dflib.csv.Csv",
            "org.dflib.csv.CsvLoader"
    );

    private DfLibExtension() {
        // prevent instantiation
    }

    @Override
    public Optional<String> initCodeSnippet() {
        var dependencies = DEPS.stream().map(dep -> "addDependency(\"" + dep + "\");").collect(Collectors.joining("\n"));
        var imports = IMPORTS.stream().map(imp -> "import " + imp + ";").collect(Collectors.joining("\n"));
        return Optional.of(dependencies + "\n" + imports);
    }

    @Descriptor(mode = Mode.ON_DEMAND, type = DfLibExtension.class)
    public static class FactoryImpl implements Factory {

        @Override
        public DfLibExtension createExtension(ReactiveJShell jshell) {
            return new DfLibExtension();
        }
    }
}
