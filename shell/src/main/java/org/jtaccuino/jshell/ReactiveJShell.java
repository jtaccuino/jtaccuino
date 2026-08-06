/*
 * Copyright 2024-2026 JTaccuino Contributors
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
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

    private final ExecutorService worker = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            Thread.ofVirtual().name("ReactiveJShellWorker").factory(),
            new ThreadPoolExecutor.DiscardPolicy());

    private final JShell jshell = JShell.builder()
            .compilerOptions("--enable-preview", "-source", System.getProperty("java.specification.version"),
                    "--add-modules", "jdk.incubator.vector", "-Xlint:-incubating")
            .executionEngine("local")
            .build();

    private final UUID uuid;

    private final AtomicBoolean userCodeExecuted = new AtomicBoolean(false);

    private ReactiveJShell(UUID uuid) {
        this.uuid = uuid;
    }

    public void markUserCodeExecuted() {
        userCodeExecuted.set(true);
    }

    public boolean hasExecutedUserCode() {
        return userCodeExecuted.get();
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
                    int[] anchorHolder = new int[1];
                    var completionItems = jshell.sourceCodeAnalysis().completionSuggestions(text, caretPosition,
                            (state, elementSuggestions) -> {
                                if (!elementSuggestions.isEmpty()) {
                                    anchorHolder[0] = elementSuggestions.getFirst().anchor();
                                }
                                return elementSuggestions.stream()
                                        .filter(suggestion -> !"module ".equals(suggestion.keyword()))
                                        .map(CompletionItem::from)
                                        .sorted(Comparator.comparingInt(item -> item.matchesType() ? 0 : 1))
                                        .toList();
                            });
                    return new CompletionSuggestion(completionItems, anchorHolder[0]);
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

    public void highlightingAsync(String text, Consumer<List<SourceCodeAnalysis.Highlight>> consumer) {
        CompletableFuture.supplyAsync(() -> sourceCodeAnalysis().highlights(text), worker)
                .thenAccept(consumer)
                .exceptionally(this::logThrowable);
    }

    public void parseErrorsAsync(String text, Consumer<List<ErrorRange>> consumer) {
        CompletableFuture.supplyAsync(() -> parseErrors(text), worker)
                .thenAccept(consumer)
                .exceptionally(this::logThrowable);
    }

    private List<ErrorRange> parseErrors(String text) {
        var errors = new ArrayList<ErrorRange>();
        if (text.isBlank()) {
            return errors;
        }
        var analysis = jshell.sourceCodeAnalysis();
        var declaredIdentifiers = analysis.highlights(text).stream()
                .filter(highlight -> highlight.attributes().contains(SourceCodeAnalysis.Attribute.DECLARATION))
                .map(highlight -> text.substring(highlight.start(), highlight.end()))
                .collect(Collectors.toSet());
        var remaining = text;
        var base = 0;
        while (!remaining.isBlank()) {
            var completionInfo = analysis.analyzeCompletion(remaining);
            if (SourceCodeAnalysis.Completeness.EMPTY == completionInfo.completeness()
                    || SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE == completionInfo.completeness()) {
                break;
            }
            var source = completionInfo.source();
            if (null == source || source.isBlank()) {
                break;
            }
            var snippetOffset = text.indexOf(source, base);
            if (snippetOffset < 0) {
                snippetOffset = base;
            }
            for (var snippet : analysis.sourceToSnippets(source)) {
                for (var diag : jshell.diagnostics(snippet).toList()) {
                    if (diag.isError() && !isSuppressed(diag, source, declaredIdentifiers)) {
                        var start = diag.getStartPosition();
                        var end = diag.getEndPosition();
                        if (Diag.NOPOS != start && Diag.NOPOS != end) {
                            errors.add(new ErrorRange(snippetOffset + (int) start, snippetOffset + (int) end,
                                    diag.getMessage(null)));
                        }
                    }
                }
            }
            var rest = completionInfo.remaining();
            if (rest.equals(remaining)) {
                break;
            }
            base = text.length() - rest.length();
            remaining = rest;
        }
        return errors;
    }

    private static boolean isSuppressed(Diag diag, String snippetSource, Set<String> declaredIdentifiers) {
        var code = diag.getCode();
        if (!"compiler.err.cant.resolve.location".equals(code)
                && !"compiler.err.cant.resolve.location.args".equals(code)) {
            return false;
        }
        var identifier = leadingIdentifier(snippetSource, diag.getStartPosition());
        return null != identifier && declaredIdentifiers.contains(identifier);
    }

    private static String leadingIdentifier(String source, long position) {
        if (position < 0 || position >= source.length()) {
            return null;
        }
        var index = (int) position;
        if (!Character.isJavaIdentifierStart(source.codePointAt(index))) {
            return null;
        }
        var builder = new StringBuilder();
        while (index < source.length()) {
            var codePoint = source.codePointAt(index);
            if (!Character.isJavaIdentifierPart(codePoint)) {
                break;
            }
            builder.appendCodePoint(codePoint);
            index += Character.charCount(codePoint);
        }
        return builder.toString();
    }

    public static record ErrorRange(int start, int end, String message) {
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

    public static record CompletionSuggestion(List<ReactiveJShell.CompletionItem> suggestions, int anchor) {
    }

    public static record CompletionItem(String completion, boolean matchesType, int anchor,
            ElementKind elementKind, boolean keyword, boolean staticMember, String displayName, String typeInfo) {

        static CompletionItem from(SourceCodeAnalysis.ElementSuggestion elementSuggestion) {
            var keyword = elementSuggestion.keyword();
            if (keyword != null) {
                return new CompletionItem(keyword, elementSuggestion.matchesType(), elementSuggestion.anchor(), null, true, false, "", "");
            }
            var element = elementSuggestion.element();
            if (element != null) {
                var kind = element.getKind();
                var name = element.getSimpleName().toString();
                var staticMember = element.getModifiers().contains(Modifier.STATIC);
                var completion = switch (kind) {
                    case METHOD, CONSTRUCTOR -> name + "(";
                    default -> name;
                };
                return switch (kind) {
                    case METHOD -> {
                        var executable = (ExecutableType) element.asType();
                        yield new CompletionItem(completion, elementSuggestion.matchesType(), elementSuggestion.anchor(), kind, false,
                                staticMember, name + "(" + simpleTypeNames(executable.getParameterTypes()) + ")",
                                simpleTypeName(executable.getReturnType()));
                    }
                    case CONSTRUCTOR -> {
                        var executable = (ExecutableType) element.asType();
                        yield new CompletionItem(completion, elementSuggestion.matchesType(), elementSuggestion.anchor(), kind, false,
                                staticMember, name + "(" + simpleTypeNames(executable.getParameterTypes()) + ")", "");
                    }
                    case FIELD, ENUM_CONSTANT, PARAMETER, LOCAL_VARIABLE, RESOURCE_VARIABLE,
                            EXCEPTION_PARAMETER, TYPE_PARAMETER, BINDING_VARIABLE ->
                        new CompletionItem(completion, elementSuggestion.matchesType(), elementSuggestion.anchor(), kind, false,
                                staticMember, name, simpleTypeName(element.asType()));
                    default ->
                        new CompletionItem(completion, elementSuggestion.matchesType(), elementSuggestion.anchor(), kind, false,
                                staticMember, name, "");
                };
            }
            return new CompletionItem("", elementSuggestion.matchesType(), elementSuggestion.anchor(), null, true, false, "", "");
        }

        private static String simpleTypeNames(List<? extends TypeMirror> types) {
            return String.join(", ", types.stream().map(CompletionItem::simpleTypeName).toList());
        }

        private static String simpleTypeName(TypeMirror type) {
            return switch (type.getKind()) {
                case DECLARED -> {
                    var declared = (DeclaredType) type;
                    var base = declared.asElement().getSimpleName().toString();
                    var typeArguments = declared.getTypeArguments();
                    if (typeArguments.isEmpty()) {
                        yield base;
                    }
                    yield base + "<" + String.join(", ",
                            typeArguments.stream().map(CompletionItem::simpleTypeName).toList()) + ">";
                }
                case ARRAY ->
                    simpleTypeName(((ArrayType) type).getComponentType()) + "[]";
                case WILDCARD -> {
                    var wildcard = (WildcardType) type;
                    if (wildcard.getExtendsBound() != null) {
                        yield "? extends " + simpleTypeName(wildcard.getExtendsBound());
                    }
                    if (wildcard.getSuperBound() != null) {
                        yield "? super " + simpleTypeName(wildcard.getSuperBound());
                    }
                    yield "?";
                }
                case TYPEVAR ->
                    ((TypeVariable) type).asElement().getSimpleName().toString();
                case VOID ->
                    "void";
                default ->
                    type.toString();
            };
        }
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
