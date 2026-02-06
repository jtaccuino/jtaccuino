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
package org.jtaccuino.core.ui.extensions;

import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.jtaccuino.core.ui.api.CellData;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class PrintExtension implements JShellExtension {

    @SuppressWarnings("UnusedVariable")
    private final ReactiveJShell reactiveJShell;

    @SuppressWarnings("UnusedVariable")
    private CellData activeCellData;
    private Label streamResult;

    @Descriptor(mode = Mode.SYSTEM, type = PrintExtension.class)
    public static class Factory implements JShellExtension.Factory {

        @Override
        public PrintExtension createExtension(ReactiveJShell jshell) {
            return new PrintExtension(jshell);
        }
    }

    private PrintExtension(ReactiveJShell reactiveJShell) {
        this.reactiveJShell = reactiveJShell;
    }

    @Override
    public Optional<String> shellVariableName() {
        return Optional.of("printManager");
    }

    @Override
    public Optional<String> initCodeSnippet() {
        return Optional.of("""
            public void println(String text, Object... args) {
                printManager.println(text, args);
            }
            public void println(Object toOutput, Object... args) {
                printManager.println(String.valueOf(toOutput), args);
            }
            public void print(String text, Object... args) {
                printManager.print(text, args);
            }
            public void print(Object toOutput, Object... args) {
                printManager.print(String.valueOf(toOutput), args);
            }
            """);
    }

    @SuppressWarnings("AnnotateFormatMethod")
    public void println(String text, Object... args) {
        var formatted = text.formatted(args);
        Platform.runLater(() -> {
            var nextText = streamResult.getText() + formatted + "\n";
            streamResult.setText(nextText);
        });
    }

    @SuppressWarnings("AnnotateFormatMethod")
    public void print(String text, Object... args) {
        var formatted = text.formatted(args);
        Platform.runLater(() -> {
            var nextText = streamResult.getText() + formatted;
            streamResult.setText(nextText);
        });
    }

    public void setCurrentCellData(CellData cellData) {
        this.activeCellData = cellData;
    }

    public void setActiveStreamResult(Label streamResult) {
        this.streamResult = streamResult;
    }
}
