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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jshell.DeclarationSnippet;
import jdk.jshell.Diag;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.VarSnippet;
import org.jtaccuino.jshell.extensions.ExtensionManager;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class ReactiveJShell {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("ReactiveJShellWorker").factory());

    private final JShell jshell = jdk.jshell.JShell.builder().compilerOptions("--enable-preview", "-source", "23").executionEngine("local").build();
    private final UUID uuid;

    private ReactiveJShell(UUID uuid) {
        this.uuid = uuid;
    }

    public static ReactiveJShell create(UUID uuid) {
        return new ReactiveJShell(uuid);
    }

    public JShell getWrappedShell() {
        return jshell;
    }

    public <T extends JShellExtension> T getExtension(Class<T> extensionClass) {
        return ExtensionManager.lookup(extensionClass, uuid);
    }

    public EvaluationResult eval(String string) {
        final List<SnippetEvent> snippetEventsCurrentSnippets = new ArrayList<>();
        final List<SnippetEvent> snippetEventsInfluencedSnippets = new ArrayList<>();
        String remaining = string;
        SourceCodeAnalysis.CompletionInfo completionInfo;
        do {
            completionInfo = jshell.sourceCodeAnalysis().analyzeCompletion(remaining);
            if (completionInfo.completeness().isComplete()) {
                List<SnippetEvent> newSnippetEvents = jshell.eval(completionInfo.source());
//                newSnippetEvents.forEach(sne -> System.out.println(sne.previousStatus() + "->" + sne.status()
//                        + " of " + sne.snippet().id() + " "
//                        + (sne.causeSnippet() != null ? sne.causeSnippet().id() : "?")
//                        + sne.snippet().source()));
                newSnippetEvents.stream()
                        .filter(event -> null == event.causeSnippet())
                        .forEach(evt -> {
                            snippetEventsCurrentSnippets.add(evt);
                        });
                newSnippetEvents.stream()
                        .filter(event -> null != event.causeSnippet())
                        .forEach(evt -> {
                            snippetEventsInfluencedSnippets.add(evt);
                        });
            }
            remaining = completionInfo.remaining().replaceFirst("\\s*", "");
        } while (!remaining.isEmpty() && SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE != completionInfo.completeness());

        if (SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE == completionInfo.completeness()) {
            return new EvaluationResult(List.of(), List.of(), ResultStatus.FAILURE, Optional.empty(), Optional.empty());
        }

        Optional<SnippetEvent> firstException = snippetEventsCurrentSnippets.stream().filter(event -> null != event.exception()).findFirst();

        if (firstException.isPresent()) {
            return new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets, ResultStatus.FAILURE, Optional.empty(), Optional.empty());
        }

        if (snippetEventsCurrentSnippets.stream().allMatch(event -> Snippet.Kind.ERRONEOUS != event.snippet().kind() && Snippet.Status.VALID == event.status())) {
            var lastEvent = snippetEventsCurrentSnippets.getLast();
            if (lastEvent.snippet() instanceof VarSnippet v) {
                var varName = v.name();
                var varType = sourceCodeAnalysis().analyzeType(varName,
                        varName.length());
                var varValue = lastEvent.value();
                if (varName.startsWith("$")) {
                    return new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets, ResultStatus.SUCCESS, Optional.of(varValue), Optional.of(varType));
                }
            }
            if (lastEvent.snippet() instanceof ExpressionSnippet e) {
                var varName = e.name();
                var varType = sourceCodeAnalysis().analyzeType(varName,
                        varName.length());
                var varValue = lastEvent.value();
                return new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets, ResultStatus.SUCCESS, Optional.of(varValue), Optional.of(varType));
            }
            return new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets, ResultStatus.SUCCESS, Optional.empty(), Optional.empty());
        }
        return new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets,ResultStatus.FAILURE, Optional.empty(), Optional.empty());
    }

    public void evalAsync(Runnable preAction, String codeSnippet, Consumer<EvaluationResult> consumer) {
        worker.execute(preAction);
        var future = new JShellCompletableFuture<EvaluationResult>(worker);
        future.completeAsync(() -> eval(codeSnippet)).thenAccept(consumer);
    }

    public void completionAsync(String text, int caretPosition,
            Consumer<CompletionSuggestion> consumer) {
        var future = new JShellCompletableFuture<CompletionSuggestion>(worker);
        future.completeAsync(() -> {
            int[] anchor = new int[1];
            try {
                var completionSuggestions = jshell.sourceCodeAnalysis().completionSuggestions(text, caretPosition, anchor);
                System.out.println("Completion suggestions: " + completionSuggestions);
                return new CompletionSuggestion(completionSuggestions, anchor[0]);
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            }
        }).thenAccept(consumer);
    }

    public Stream<Diag> diagnose(Snippet snippet) {
        return jshell.diagnostics(snippet);
    }

    public Stream<String> unresolveds(DeclarationSnippet snippet) {
        return jshell.unresolvedDependencies(snippet);
    }

    public void highlightingAsync(String text, Consumer<List<SourceCodeAnalysis.Highlight>> consumer) {
        var future = new JShellCompletableFuture<List<SourceCodeAnalysis.Highlight>>(worker);
        future.completeAsync(() -> {
            return sourceCodeAnalysis().highlights(text);
        }).thenAccept(consumer);
    }

    private SourceCodeAnalysis sourceCodeAnalysis() {
        return jshell.sourceCodeAnalysis();
    }

    public void shutdown() {
        System.out.println("Shutting-Down Worker Execution Service");
        worker.shutdown();
        System.out.println("Worker Execution Service Shutdown Complete");
        System.out.println("Shutting-Down JShell");
        jshell.stop();
        ExtensionManager.cleanup(uuid);
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

    public static record EvaluationResult(List<SnippetEvent> snippetEventsCurrent, List<SnippetEvent> snippetEventsOutdated,
            ResultStatus status, Optional<String> lastValueAsString, Optional<String> typeOfLastValue) {
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
