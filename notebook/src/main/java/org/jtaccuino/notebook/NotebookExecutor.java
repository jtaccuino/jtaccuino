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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import jdk.jshell.DeclarationSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.ReactiveJShellProvider;

/**
 * Executes a notebook headlessly, cell by cell, recording the output on the
 * cells themselves.
 *
 * <p>This is deliberately a synchronous loop over {@link ReactiveJShell#eval}
 * rather than a reuse of the IDE's {@code Sheet}. The sheet drives execution
 * asynchronously with no completion signal, and a successful cell moves focus to
 * a freshly appended cell, which would pollute the notebook being exported.
 *
 * <p>{@code println} and {@code display} persist their output on the JavaFX
 * application thread, so each cell is drained before the next one starts. That
 * ordering is what keeps a cell's output attached to that cell: the print
 * extension reads the active cell when its deferred work runs, not when it is
 * queued.
 *
 * <p><strong>Threading.</strong> The loop must run off the JavaFX application
 * thread. {@code display} defers its work with {@code Platform.runLater}, so
 * blocking the FX thread here to drain a cell would deadlock.
 */
public final class NotebookExecutor {

    private static final Logger LOG = Logger.getLogger(NotebookExecutor.class.getName());

    private static final long DRAIN_TIMEOUT_SECONDS = 120;

    private NotebookExecutor() {
    }

    public static ExecutionResult execute(Notebook notebook) {
        return execute(notebook, ExecutionOptions.defaults());
    }

    public static ExecutionResult execute(Notebook notebook, ExecutionOptions options) {
        Objects.requireNonNull(notebook, "notebook");
        Objects.requireNonNull(options, "options");
        var registry = new RecordingDisplaySink(options.displaySink());
        var failures = new ArrayList<CellData>();
        var shell = ReactiveJShellProvider.createReactiveShell(UUID.randomUUID(), options.cwd());
        try {
            var displayExtension = shell.getExtension(DisplayExtension.class);
            var printExtension = shell.getExtension(PrintExtension.class);
            for (var cell : List.copyOf(notebook.getCells())) {
                if (cell.getType() != CellData.Type.CODE || cell.isEmpty()) {
                    continue;
                }
                if (executeCell(shell, displayExtension, printExtension, registry, cell)) {
                    failures.add(cell);
                    if (!options.continueOnError()) {
                        break;
                    }
                }
            }
        } finally {
            shell.shutdown();
        }
        return new ExecutionResult(notebook, failures, registry);
    }

    /**
     * Runs one code cell.
     *
     * @return {@code true} when the cell failed
     */
    private static boolean executeCell(ReactiveJShell shell, DisplayExtension displayExtension,
            PrintExtension printExtension, RecordingDisplaySink registry, CellData cell) {
        registry.startCell(cell);
        displayExtension.setDisplaySink(registry);
        displayExtension.setCurrentCellData(cell);
        // a headless run has nowhere to draw, but the text and the displayed
        // objects are still recorded by the extensions themselves
        printExtension.setPrintSink(null);
        printExtension.setCurrentCellData(cell);
        cell.getOutputData().clear();

        var result = shell.eval(cell.getSource());

        // compute the outcome text on this thread, then append it on the FX
        // thread so it lands after the display and print output queued during
        // evaluation. The relative order is what pairs displayed objects with
        // their stored representations at export time.
        var outcomes = outcomeTexts(shell, result);
        onFxThread(() -> outcomes.forEach(text -> CellOutputRecorder.record(cell, text)));

        if (result.status().isSuccess()) {
            shell.markUserCodeExecuted();
            return false;
        }
        return true;
    }

    private static List<String> outcomeTexts(ReactiveJShell shell, ReactiveJShell.EvaluationResult result) {
        var outcomes = new ArrayList<String>();
        if (result.status().isSuccess()) {
            result.lastValueAsString().ifPresent(value -> outcomes.add(
                    CellOutputRecorder.resultText(result.typeOfLastValue().orElse(""), value)));
            return outcomes;
        }
        result.snippetEventsCurrent().stream()
                .filter(event -> null != event.exception())
                .map(SnippetEvent::exception)
                .map(CellOutputRecorder::exceptionText)
                .forEach(outcomes::add);
        result.snippetEventsCurrent().stream()
                .filter(event -> Snippet.Kind.ERRONEOUS == event.snippet().kind()
                || Snippet.Status.REJECTED == event.status()
                || Snippet.Status.RECOVERABLE_NOT_DEFINED == event.status()
                || Snippet.Status.RECOVERABLE_DEFINED == event.status())
                .forEach(event -> {
                    shell.diagnose(event.snippet())
                            .forEachOrdered(diag -> outcomes.add(
                                    CellOutputRecorder.diagnosticText(event.snippet().source(), diag)));
                    if ((event.status() == Snippet.Status.RECOVERABLE_NOT_DEFINED
                            || event.status() == Snippet.Status.RECOVERABLE_DEFINED)
                            && event.snippet() instanceof DeclarationSnippet declaration) {
                        var unresolveds = shell.unresolveds(declaration)
                                .map(s -> "    " + s + "\n")
                                .reduce("", String::concat);
                        outcomes.add(CellOutputRecorder.unresolvedText(unresolveds));
                    }
                });
        return outcomes;
    }

    private static void onFxThread(Runnable action) {
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "the JavaFX thread did not drain within " + DRAIN_TIMEOUT_SECONDS + "s");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOG.log(Level.SEVERE, "interrupted while waiting for the JavaFX thread", interrupted);
            throw new IllegalStateException("interrupted while executing a notebook", interrupted);
        }
    }
}
