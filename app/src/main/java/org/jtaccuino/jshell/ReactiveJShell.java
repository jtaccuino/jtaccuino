/*
 * Copyright 2024 JTaccuino Contributors
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
package org.jtaccuino.jshell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jshell.Diag;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.VarSnippet;

public class ReactiveJShell {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("ReactiveJShellWorker").factory());

    private final JShell jshell = jdk.jshell.JShell.builder().executionEngine("local").build();

    private ReactiveJShell() {
    }

    public static ReactiveJShell create() {
        return new ReactiveJShell();
    }

    public JShell getWrappedShell() {
        return jshell;
    }

    public EvaluationResult eval(String string) {
        final List<SnippetEvent> snippetEvents = new ArrayList<>();
        String remaining = string;
        SourceCodeAnalysis.CompletionInfo completionInfo;
        do {
            completionInfo = jshell.sourceCodeAnalysis().analyzeCompletion(remaining);
            if (completionInfo.completeness().isComplete()) {
                List<SnippetEvent> newSnippetEvents = jshell.eval(completionInfo.source());
                newSnippetEvents.stream()
                        .filter(event -> null == event.causeSnippet())
                        .forEach(evt -> {
                            snippetEvents.add(evt);
                        });
            }
            remaining = completionInfo.remaining().replaceFirst("\\s*", "");
        } while (!remaining.isEmpty() && SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE != completionInfo.completeness());
        if (SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE == completionInfo.completeness()) {
            return new EvaluationResult(List.of(), ResultStatus.FAILURE, Optional.empty(), Optional.empty());
        }
        if (snippetEvents.stream().allMatch(event -> Snippet.Kind.ERRONEOUS != event.snippet().kind())) {
            var lastEvent = snippetEvents.getLast();
            if (lastEvent.snippet() instanceof VarSnippet v) {
                var varName = v.name();
                var varType = v.typeName();
                var varValue = lastEvent.value();
                if (varName.startsWith("$")) {
                    return new EvaluationResult(snippetEvents, ResultStatus.SUCCESS, Optional.of(varValue), Optional.of(varType));
                }
            }
            if (lastEvent.snippet() instanceof ExpressionSnippet e) {
                var varName = e.name();
                var varType = e.typeName();
                var varValue = lastEvent.value();
                return new EvaluationResult(snippetEvents, ResultStatus.SUCCESS, Optional.of(varValue), Optional.of(varType));
            }
            return new EvaluationResult(snippetEvents, ResultStatus.SUCCESS, Optional.empty(), Optional.empty());
        }
        return new EvaluationResult(snippetEvents, ResultStatus.FAILURE, Optional.empty(), Optional.empty());
    }

    public void evalAsync(Runnable preAction, String string, Consumer<EvaluationResult> consumer) {
        worker.execute(preAction);
        var future = new JShellCompletableFuture<EvaluationResult>(worker);
        future.completeAsync(() -> eval(string)).thenAccept(consumer);
    }

    public void completionAsync(String text, int caretPosition,
            Consumer<CompletionSuggestion> consumer) {
        var future = new JShellCompletableFuture<CompletionSuggestion>(worker);
        future.completeAsync(() -> {
            int[] anchor = new int[1];
            return new CompletionSuggestion(jshell.sourceCodeAnalysis().completionSuggestions(text, caretPosition, anchor), anchor[0]);
        }).thenAccept(consumer);
    }

    public Stream<Diag> diagnose(Snippet snippet) {
        return jshell.diagnostics(snippet);
    }

    public SourceCodeAnalysis sourceCodeAnalysis() {
        return jshell.sourceCodeAnalysis();
    }

    public void shudown() {
        System.out.println("Shutting-Down Worker Execution Service");
        worker.shutdown();
        System.out.println("Worker Execution Service Shutdown Complete");
        System.out.println("Shutting-Down JShell");
        jshell.stop();
        System.out.println("JShell Shutdown complete");
    }

    private static class JShellCompletableFuture<T> extends CompletableFuture<T> {

        Executor executor;

        public JShellCompletableFuture(Executor executor) {
            this.executor = executor;
        }

        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return new JShellCompletableFuture<>(executor);
        }

        @Override
        public Executor defaultExecutor() {
            return executor;
        }

    }

    public static record CompletionSuggestion(List<SourceCodeAnalysis.Suggestion> suggestions, int anchor) {

    }

    public static record EvaluationResult(List<SnippetEvent> snippetEvents, ResultStatus status, Optional<String> lastValueAsString, Optional<String> typeOfLastValue) {

    }

    public static enum ResultStatus {
        SUCCESS(true), FAILURE(false);

        private final boolean isSuccess;

        public boolean isSuccess() {
            return isSuccess;
        }

        ResultStatus(boolean isSuccess) {
            this.isSuccess = isSuccess;
        }
    }
}
