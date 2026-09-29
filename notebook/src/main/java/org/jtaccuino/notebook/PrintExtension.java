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
package org.jtaccuino.notebook;

import java.util.Optional;
import javafx.application.Platform;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

/**
 * Implements {@code print} and {@code println}, which are not part of JShell.
 *
 * <p>This lives in the notebook module rather than in the IDE so that a
 * consumer without the IDE on its classpath still gets them, and so that a
 * headless export records exactly the text the IDE shows. Accumulation and
 * persistence live here; presentation is delegated to a {@link PrintSink}.
 */
public class PrintExtension implements JShellExtension {

    @SuppressWarnings("UnusedVariable")
    private final ReactiveJShell reactiveJShell;

    private CellData activeCellData;
    private PrintSink printSink;
    private final StringBuilder accumulatedStream = new StringBuilder();

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
            accumulatedStream.append(formatted).append('\n');
            reRenderStream();
        });
    }

    @SuppressWarnings("AnnotateFormatMethod")
    public void print(String text, Object... args) {
        var formatted = text.formatted(args);
        Platform.runLater(() -> {
            accumulatedStream.append(formatted);
            reRenderStream();
        });
    }

    private void reRenderStream() {
        var text = accumulatedStream.toString();
        if (null != printSink) {
            printSink.accept(text);
        }
        persistStream(text);
    }

    private void persistStream(String text) {
        if (null != activeCellData) {
            activeCellData.getOutputData().removeIf(od -> od instanceof CellData.StreamBasedOutputData);
            activeCellData.getOutputData().add(CellData.OutputData.of(
                    CellData.OutputData.OutputType.EXECUTION_DATA,
                    text));
        }
    }

    public void setCurrentCellData(CellData cellData) {
        this.activeCellData = cellData;
        this.accumulatedStream.setLength(0);
    }

    public void setPrintSink(PrintSink printSink) {
        this.printSink = printSink;
    }
}
