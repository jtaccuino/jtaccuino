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
package org.jtaccuino.jshell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private final ExecutorService worker = Executors
            .newSingleThreadExecutor(Thread.ofVirtual().name("ReactiveJShellWorker").factory());

    private final JShell jshell = JShell.builder()
            .compilerOptions("--enable-preview", "-source", System.getProperty("java.specification.version"),
                    "--add-modules", "jdk.incubator.vector", "-Xlint:-incubating")
            .executionEngine("local")
            .build();

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
            var mayBeLastEvent = Optional.ofNullable(snippetEventsCurrentSnippets.isEmpty() ? null : snippetEventsCurrentSnippets.getLast());
            return mayBeLastEvent.map(lastEvent
                    -> switch (lastEvent.snippet()) {
                case VarSnippet v when v.name().startsWith("$") -> {
                    var varName = v.name();
                    var varType = sourceCodeAnalysis().analyzeType(varName,
                            varName.length());
                    var varValue = lastEvent.value();
                    yield new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets,
                    ResultStatus.SUCCESS, Optional.of(varValue), Optional.of(varType));
                }
                case ExpressionSnippet e -> {
                    var varName = e.name();
                    var varType = sourceCodeAnalysis().analyzeType(varName,
                            varName.length());
                    var varValue = lastEvent.value();
                    yield new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets,
                    ResultStatus.SUCCESS, Optional.of(varValue), Optional.of(varType));
                }
                default ->
                    null;
            }).orElseGet(() -> new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets,
                    ResultStatus.SUCCESS, Optional.empty(), Optional.empty()));
        }
        return new EvaluationResult(snippetEventsCurrentSnippets, snippetEventsInfluencedSnippets, ResultStatus.FAILURE, Optional.empty(), Optional.empty());
    }

    public void evalAsync(Runnable preAction, String codeSnippet, Consumer<EvaluationResult> consumer) {
        CompletableFuture.runAsync(preAction, worker)
                .thenRun(() -> consumer.accept(eval(codeSnippet)))
                .exceptionally(this::logThrowable);
    }

    private Void logThrowable(Throwable t) {
        Logger.getLogger(ReactiveJShell.class.getName()).log(Level.SEVERE, null, t);
        return null;
    }

    public void completionAsync(String text, int caretPosition,
            Consumer<CompletionSuggestion> consumer) {
        CompletableFuture.supplyAsync(
                () -> {
                    int[] anchor = new int[1];
                    var completionSuggestions = jshell.sourceCodeAnalysis().completionSuggestions(text, caretPosition, anchor);
                    return new CompletionSuggestion(completionSuggestions, anchor[0]);
                },
                worker)
                .thenAccept(consumer)
                .exceptionally(this::logThrowable);
    }

    public void documentationAsync(String text, int caretPosition, Consumer<List<Documentation>> consumer) {
        CompletableFuture.supplyAsync(()
                -> jshell.sourceCodeAnalysis().documentation(text, caretPosition, true)
                        .stream()
                        .map(d -> new Documentation(d.signature(), d.javadoc()))
                        .toList(),
                 worker)
                .thenAccept(consumer)
                .exceptionally(this::logThrowable);
    }

    public Stream<Diag> diagnose(Snippet snippet) {
        return jshell.diagnostics(snippet);
    }

    public Stream<String> unresolveds(DeclarationSnippet snippet) {
        return jshell.unresolvedDependencies(snippet);
    }

    public void highlightingAsync(String text, Consumer<List<SyntaxHighlight>> consumer) {
        CompletableFuture.supplyAsync(() -> sourceCodeAnalysis().highlights(text).stream().map(SyntaxHighlight::fromJShellHighlight).toList(), worker)
                .thenAccept(consumer)
                .exceptionally(this::logThrowable);
    }

    private SourceCodeAnalysis sourceCodeAnalysis() {
        return jshell.sourceCodeAnalysis();
    }

    public void activateExtension(JShellExtension.Factory factory) {
        JShellExtension extension = factory.createExtension(this);
        ExtensionManager.register(extension, uuid);
        extension.shellVariableName().ifPresent(shellVariablename -> {
            String extensionVarInit = "var " + shellVariablename + " = org.jtaccuino.jshell.extensions.ExtensionManager.lookup(" + extension.getClass().getName() + ".class, _$jsci$uuid)";
            this.eval(extensionVarInit);
            this.getWrappedShell().onSnippetEvent((t) -> {
                if (t.snippet().source().equals(extensionVarInit)) {
                    System.out.println("Init extensionVar extension.shellVariableName() status changed from " + t.previousStatus() + " to : " + t.status());
                    System.out.println("Caused by: " + t.causeSnippet() == null ? "Empty" : t.causeSnippet().source());
                }
            });
        });
        extension.initCodeSnippet().ifPresent(initCodeSnippet -> {
            ReactiveJShell.EvaluationResult evalResult = this.eval(initCodeSnippet);
            if (evalResult.status().isSuccess()) {
                System.out.println("Extension " + extension + " init code registered successfully");
            } else {
                System.out.println("Extension " + extension + " failed to load init code!");
                System.out.println(evalResult.snippetEventsCurrent());
            }
        });
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

    public static record Documentation(String signature, String javadoc) {
    }
}
