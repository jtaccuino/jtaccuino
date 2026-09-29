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
package org.jtaccuino.notebook;

import java.nio.file.Path;

/**
 * How a notebook should be executed.
 *
 * @param continueOnError when true a failing cell does not stop the run. The
 * default, false, stops at the first failure, because later cells almost always
 * depend on earlier ones and a cascade of confusing secondary errors is worse
 * than stopping where it actually broke
 * @param cwd working directory for the shell, or {@code null} to leave it unset
 * @param displaySink where {@code display} results go, or {@code null} for a
 * headless run that records output and shows nothing. The recorded output is
 * unaffected either way; the sink only decides whether anything is also drawn
 */
public record ExecutionOptions(boolean continueOnError, Path cwd, DisplaySink displaySink) {

    public static ExecutionOptions defaults() {
        return new ExecutionOptions(false, null, null);
    }

    public ExecutionOptions withContinueOnError(boolean continueOnError) {
        return new ExecutionOptions(continueOnError, cwd, displaySink);
    }

    public ExecutionOptions withCwd(Path cwd) {
        return new ExecutionOptions(continueOnError, cwd, displaySink);
    }

    public ExecutionOptions withDisplaySink(DisplaySink displaySink) {
        return new ExecutionOptions(continueOnError, cwd, displaySink);
    }
}
