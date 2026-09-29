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

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jshell.Diag;
import jdk.jshell.EvalException;

/**
 * Turns the outcome of evaluating a cell into the plain text that ends up in the
 * notebook, and appends it to a cell's output.
 *
 * <p>
 * Every piece of evaluation output shown in the UI is produced here, so a
 * headless export of a notebook and the notebook as rendered by the IDE cannot
 * drift apart. The text expressions are deliberately kept free of JavaFX: the
 * caller decides how to present them.
 */
public final class CellOutputRecorder {

    private CellOutputRecorder() {
    }

    /**
     * Formats the value of the last evaluated snippet, e.g.
     * {@code int: 42}.
     */
    public static String resultText(String typeOfLastValue, String lastValueAsString) {
        return typeOfLastValue + ": " + lastValueAsString.replace("\\n", "\n").replace("\\\"", "\"");
    }

    /**
     * Renders an evaluation failure, including the stack trace of the exception
     * and of each of its causes.
     */
    public static String exceptionText(Throwable exception) {
        var realEx = (null != exception.getCause()) ? exception.getCause() : exception;
        var text = switch (realEx) {
            case EvalException e ->
                e.getExceptionClassName();
            default ->
                realEx.getClass().getName();
        } + ": " + realEx.getMessage();
        // Note: the stack trace appended per iteration is deliberately that of
        // the outermost exception, matching the behaviour this code had while
        // it lived in the UI. Changing it would alter existing notebooks.
        var t = realEx;
        do {
            text += "\n" + Arrays.stream(realEx.getStackTrace())
                    .limit(realEx.getStackTrace().length > 2 ? realEx.getStackTrace().length - 2 : realEx.getStackTrace().length)
                    .map(ste -> "\t" + ste.toString())
                    .collect(Collectors.joining("\n"));
            t = t.getCause();
        } while (t != null);
        return text;
    }

    /**
     * Renders a JShell diagnostic: the message, the offending source, and a
     * caret run marking the region the diagnostic refers to.
     */
    public static String diagnosticText(String snippetSource, Diag diag) {
        var message = new StringBuilder()
                .append(diag.getMessage(Locale.getDefault()))
                .append('\n')
                .append(snippetSource);
        if (diag.getEndPosition() > 0 && diag.getEndPosition() - diag.getStartPosition() > 1) {
            message.append('\n')
                    .repeat(' ', (int) diag.getStartPosition())
                    .append("^")
                    .repeat('-', (int) (diag.getEndPosition() - diag.getStartPosition() - 1))
                    .append('^');
        }
        message.append('\n')
                .repeat(' ', (int) diag.getPosition())
                .append('^');
        return message.toString();
    }

    /**
     * Renders the hint listing declarations that must come first.
     *
     * @param unresolveds the unresolved declaration names, one per line
     */
    public static String unresolvedText(String unresolveds) {
        return "Declaration not useable until\n" + unresolveds + "are defined";
    }

    /**
     * Appends evaluation output to a cell as a {@code text/plain} mime entry.
     */
    public static void record(CellData cellData, String text) {
        cellData.getOutputData().add(
                CellData.OutputData.of(
                        CellData.OutputData.OutputType.DISPLAY_DATA,
                        Map.of("text/plain", text)));
    }
}
