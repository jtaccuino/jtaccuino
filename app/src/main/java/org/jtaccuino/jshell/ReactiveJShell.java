package org.jtaccuino.jshell;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;

public class ReactiveJShell {

    private ExecutorService worker = Executors.newSingleThreadExecutor();

    private JShell jshell = jdk.jshell.JShell.builder().executionEngine("local").build();

    private ReactiveJShell() {
    }

    public static ReactiveJShell create() {
        return new ReactiveJShell();
    }

    public JShell getWrappedShell() {
        return jshell;
    }

    public List<SnippetEvent> eval(String string) {
        System.out.println("Evaluating Fragment:\n" + string);
        final List<SnippetEvent> snippetEvents = new ArrayList<>();
        String remaining = string;
        SourceCodeAnalysis.CompletionInfo completionInfo;
        do {
            completionInfo = jshell.sourceCodeAnalysis().analyzeCompletion(remaining);
            if (completionInfo.completeness().isComplete()) {
                System.out.println("--> Found snippet:\n" + completionInfo.source());
                List<SnippetEvent> newSnippetEvents = jshell.eval(completionInfo.source());
                newSnippetEvents.stream()
                        .filter(event -> null == event.causeSnippet())
                        .peek(System.out::println)
                        .forEach(evt -> {
                            snippetEvents.add(evt);
                        });
            }
            remaining = completionInfo.remaining().replaceFirst("\\s*", "");
        } while (!remaining.isEmpty() && SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE != completionInfo.completeness());
        if (SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE == completionInfo.completeness()) {
            System.out.println(completionInfo);
        } else {
            System.out.println("Snippets:");
            jshell.snippets().forEach(snip -> System.out.println(snip + " " + snip.hashCode()));
            System.out.println("End-Snippets\n");
        }
        return snippetEvents;
    }

    public void evalAsync(String string, Consumer<List<SnippetEvent>> consumer) {
        var future = new JShellCompletableFuture<List<SnippetEvent>>(worker);
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
            return new JShellCompletableFuture<U>(executor);
        }

        public Executor defaultExecutor() {
            return executor;
        }

    }

    public static record CompletionSuggestion(List<SourceCodeAnalysis.Suggestion> suggestions, int anchor) {

    }
}
