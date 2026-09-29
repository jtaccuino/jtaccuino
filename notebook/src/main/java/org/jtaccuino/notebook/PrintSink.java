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

/**
 * Receives the text accumulated by {@code println} and {@code print} for the
 * cell that is currently executing.
 *
 * <p>The extension owns accumulation and persistence; a sink only decides
 * where the text becomes visible. That split is what lets the IDE show a live
 * label while a headless export persists the same text and shows nothing.
 *
 * <p>Every call carries the whole accumulated text, not just an increment, so a
 * sink never has to reconstruct state.
 */
@FunctionalInterface
public interface PrintSink {

    /**
     * Shows everything printed by the current cell so far.
     *
     * @param accumulatedText the full text since the cell started executing,
     * already terminated with {@code \n} where a newline was printed
     */
    void accept(String accumulatedText);
}
