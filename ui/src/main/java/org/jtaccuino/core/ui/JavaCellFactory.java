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
package org.jtaccuino.core.ui;

import org.jtaccuino.core.ui.api.CellData;
import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.Selection;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextDecoration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import jdk.jshell.DeclarationSnippet;
import jdk.jshell.EvalException;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.ImportSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.StatementSnippet;
import jdk.jshell.VarSnippet;
import org.jtaccuino.core.ui.controls.JavaControl;
import org.jtaccuino.core.ui.extensions.DisplayExtension;

public class JavaCellFactory implements CellFactory {

    public static JavaCellFactory INSTANCE = new JavaCellFactory();

    @Override
    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet) {
        var cell = new JavaCell(cellData, parent, sheet, sheet.getNextId());
        return cell;
    }

    public static class JavaCell extends Sheet.Cell {

        private final int cellNumber;

        private List<String> snippetIds = Collections.emptyList();

        public JavaCell(CellData cellData, VBox parent, Sheet sheet, int cellNumber) {
            super(cellData, sheet);
            this.cellNumber = cellNumber;
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            return new JavaCellSkin(this);
        }

        @Override
        public void requestFocus() {
            if (null == getSkin()) {
                skinProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        ((JavaCellSkin) getSkin()).requestFocus();
                    }
                });
            } else {
                ((JavaCellSkin) getSkin()).requestFocus();
            }
        }

        @Override
        public void execute() {
            ((JavaCellSkin) getSkin()).execute();
        }

        @Override
        public void markAsOutdated(boolean isOutdated) {
            ((JavaCellSkin) getSkin()).markAsOutdated(isOutdated);
        }

        @Override
        public void markAsSelected(boolean isSelected) {
            ((JavaCellSkin) getSkin()).markAsSelected(isSelected);
        }

        List<String> getSnippetIds() {
            return Collections.unmodifiableList(snippetIds);
        }
    }

    public static class JavaCellSkin extends AbstractCellSkin<JavaCell> {

        private static final PseudoClass HIGHLIGHT = PseudoClass.getPseudoClass("highlight");

        private static final KeyCodeCombination SHIFT_ENTER_KEY_COMBINATION
                = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);

        static final TextDecoration presetDecoration = TextDecoration.builder().presets().fontFamily("Monaspace Argon")
                .fontWeight(FontWeight.NORMAL).fontSize(13).build();

        private static final ParagraphDecoration parPreset
                = ParagraphDecoration.builder().presets()
                        .graphicType(ParagraphDecoration.GraphicType.NUMBERED_LIST)
                        .indentationLevel(1)
                        .topInset(3)
                        .build();

        private final JavaCell control;

        private final CompletionPopup completionPopup = new CompletionPopup();
        private final VBox inputBox;
        private final RichTextArea input;
        private final VBox outputBox;
//        private final BorderPane output;
        private final Label execResult;
        private final Region success;
        private final Region failure;

        private JavaCellSkin(JavaCell javaCell) {
            super(javaCell);
            this.control = javaCell;
            var inputControl = new JavaControl(control.cellNumber);
            input = inputControl.getInput();

            input.documentProperty().subscribe((t, u) -> {
                if (!t.getText().equals(u.getText())) {
                    handleSyntaxHighlighting(input.getDocument().getText());
                }
            });

            inputControl.codeEditorFocussed().addListener((observable, oldValue, newValue) -> markAsSelected(newValue));
            input.textLengthProperty().subscribe(nv -> {
                if (nv.doubleValue() == 0) {
                    input.getActionFactory().decorate(presetDecoration).execute(new ActionEvent());
                }
            });
            String source = javaCell.getCellData().getSource();
            if (null != source) {
                inputControl.openDocument(
                        new Document(
                                source,
                                List.of(new DecorationModel(0, source.length(), presetDecoration, parPreset)),
                                source.length() - 1
                        ));
            } else {
                inputControl.openDocument(
                        new Document("",
                                List.of(new DecorationModel(0, 0, presetDecoration, parPreset)),
                                0
                        ));
            }
            input.documentProperty().subscribe(doc -> javaCell.getCellData().sourceProperty().set(doc.getText()));

//            output = new BorderPane();
//            output.setVisible(false);
            outputBox = new VBox();
            outputBox.getStyleClass().add("java-cell-output");
//            output.setCenter(outputBox);
            var toolbar = createToolbar();
            toolbar.visibleProperty().bind(Bindings.or(input.focusedProperty(), toolbar.focusWithinProperty()));
            execResult = new Label();
            execResult.setVisible(false);
            inputControl.getChildren().addAll(toolbar, execResult);
            success = new Region();
            success.getStyleClass().addAll("toolbar-button-graphics", "code-success");
            failure = new Region();
            failure.getStyleClass().addAll("toolbar-button-graphics", "code-failure");

            AnchorPane.setRightAnchor(toolbar, 5d);
            AnchorPane.setTopAnchor(toolbar, 0d);
            AnchorPane.setRightAnchor(execResult, 5d);
            AnchorPane.setBottomAnchor(execResult, 2d);

            inputBox = new VBox(inputControl); //, outputBox);

            inputBox.getStyleClass().add("java-cell-meta");
            outputBox.getChildren()
                    .addListener((ListChangeListener.Change<? extends Node> c) -> {
                        if (c.getList().isEmpty()) {
                            inputBox.getChildren().remove(outputBox);
                        } else {
                            inputBox.getChildren().add(outputBox);
                        }
                    });
            input.addEventFilter(KeyEvent.KEY_PRESSED, e
                    -> {
                if (SHIFT_ENTER_KEY_COMBINATION.match(e)) {
                    handleExecution();
                    e.consume();
                }
            });
//            input.addEventFilter(KeyEvent.KEY_PRESSED, e
//                    -> {
//                if (KeyCode.BACK_SPACE != e.getCode()) {
//                    handleSyntaxHighlighting(input.getDocument().getText());
//                }
//            });
            input.addEventFilter(KeyEvent.KEY_PRESSED, t
                    -> {
                if (KeyCode.TAB == t.getCode()) {
                    var oldCaretPosition = input.caretPositionProperty().get();
                    handleTabCompletion(input.getDocument().getText(), oldCaretPosition,
                            input.getCaretOrigin(),
                            (completionUpdate) -> {
                                if (null != completionUpdate) {
                                    System.out.println("Text before: " + input.getDocument().getText());
                                    System.out.println("Completion: " + completionUpdate.completion);
                                    System.out.println("Completion to insert: " + completionUpdate.completionToInsert);
                                    System.out.println("Decorations before: " + input.getDocument().getDecorations());
                                    input.getActionFactory().
                                            insertText(completionUpdate.completionToInsert).
                                            execute(new ActionEvent());
                                    Platform.runLater(() -> {
                                        System.out.println("Text after: " + input.getDocument().getText());
                                        System.out.println("Decorations after: " + input.getDocument().getDecorations());
                                    });
                                }
                            });
                    t.consume();
                } else {
//                    input.setPrefRowCount((int) input.getText().chars().filter(c -> c == '\n').count() + 1);
                    Platform.runLater(() -> this.control.getSheet().ensureCellVisible(control));
                }
            }
            );
            input.caretPositionProperty()
                    .addListener((observable) -> {
                        if (completionPopup.isShowing()) {
                            filterCompletion(input.getDocument().getText(), input.caretPositionProperty().get());
                        }
                    }
                    );
        }

        void requestFocus() {
            this.control.getSheet().ensureCellVisible(getNode());
            input.requestFocus();
        }

        @Override
        protected void execute() {
            handleExecution();
        }

        private void handleExecution() {
            var shell = this.control.getSheet().getReactiveJShell();
            var displayManager = shell.getExtension(DisplayExtension.class);
            shell.evalAsync(() -> {
                displayManager.setActiveOutput(outputBox);
                displayManager.setCurrentCellData(control.getCellData());
                control.getCellData().getOutputData().clear();
                Platform.runLater(outputBox.getChildren()::clear);
            },
                    input.getDocument().getText(),
                    evalResult -> {
                        this.control.snippetIds = evalResult.snippetEventsCurrent().stream().map(sne -> sne.snippet().id()).toList();
                        var probablyOutdatedIds = evalResult.snippetEventsOutdated().stream().map(sne -> sne.snippet().id())
                                .filter(id -> !this.control.snippetIds.contains(id)).toList();
                        Platform.runLater(() -> {
                            this.control.markAsOutdated(false);
                            this.control.getSheet().markCellsAsOutdated(c -> {
                                if (c instanceof JavaCell jc) {
                                    return probablyOutdatedIds.stream().anyMatch(id -> jc.getSnippetIds().contains(id));
                                } else {
                                    return false;
                                }
                            });
                            if (evalResult.status().isSuccess()) {
                                Platform.runLater(() -> {
                                    execResult.setGraphic(success);
                                    execResult.setVisible(true);
                                    this.control.getSheet().moveFocusToNextCell(control);
                                    evalResult.lastValueAsString().ifPresent(action -> {
                                        var resultData = evalResult.typeOfLastValue().get() + ": " + evalResult.lastValueAsString().get();
                                        var result = new Label(resultData);
                                        result.getStyleClass().add("jshell_eval_result");
                                        outputBox.getChildren().add(result);
                                        control.getCellData().getOutputData().add(new CellData.OutputData(org.jtaccuino.core.ui.api.CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("text/plain", resultData)));
                                    });
                                });
                            } else {
                                evalResult.snippetEventsCurrent().stream()
                                        .filter(event -> null != event.exception())
                                        .map(event -> event.exception())
                                        .forEach(exception -> {
                                            var realEx = (null != exception.getCause()) ? exception.getCause() : exception;
                                            var text = switch (realEx) {
                                                case EvalException e ->
                                                    e.getExceptionClassName();
                                                default ->
                                                    realEx.getClass().getName();
                                            } + ": " + realEx.getMessage();
                                            var t = realEx;
                                            do {
                                                text += "\n" + Arrays.stream(realEx.getStackTrace())
                                                        .limit(realEx.getStackTrace().length > 2 ? realEx.getStackTrace().length - 2 : realEx.getStackTrace().length)
                                                        .map(ste -> "\t" + ste.toString())
                                                        .collect(Collectors.joining("\n"));
                                                t = t.getCause();
                                            } while (t != null);
                                            var l = new Label(text);
                                            l.getStyleClass().add("jshell_eval_exception");
                                            l.setWrapText(true);
                                            Platform.runLater(() -> outputBox.getChildren().add(l));
                                        });
                                evalResult.snippetEventsCurrent().stream()
                                        .filter(event -> Snippet.Kind.ERRONEOUS == event.snippet().kind()
                                        || Snippet.Status.REJECTED == event.status()
                                        || Snippet.Status.RECOVERABLE_NOT_DEFINED == event.status()
                                        || Snippet.Status.RECOVERABLE_DEFINED == event.status()
                                        )
                                        .forEach(event -> {
                                            this.control.getSheet().getReactiveJShell().diagnose(event.snippet())
                                                    .forEachOrdered(diag -> {
                                                        var message = diag.getMessage(Locale.getDefault());
                                                        message += "\n" + event.snippet().source();
                                                        if (diag.getEndPosition() > 0 && diag.getEndPosition() - diag.getStartPosition() > 1) {
                                                            var pointer = new StringBuffer();
                                                            pointer.repeat(" ", (int) diag.getStartPosition())
                                                                    .append("^")
                                                                    .repeat("-", (int) (diag.getEndPosition() - diag.getStartPosition() - 1))
                                                                    .append("^");
                                                            message += "\n" + pointer;
                                                        }
                                                        var problem = new StringBuffer();
                                                        problem.repeat(" ", (int) diag.getPosition())
                                                                .append("^");
                                                        message += "\n" + problem;
                                                        var l = new Label(message);
                                                        l.getStyleClass().add("jshell_eval_erroneous");
                                                        l.setTooltip(new Tooltip(diag.getCode()));
                                                        Platform.runLater(() -> outputBox.getChildren().add(l));
                                                    });
                                            if ((event.status() == Snippet.Status.RECOVERABLE_NOT_DEFINED
                                                    || event.status() == Snippet.Status.RECOVERABLE_DEFINED)
                                                    && event.snippet() instanceof DeclarationSnippet d) {
                                                var message = "Declaration not useable until\n";
                                                var unresolveds = this.control.getSheet().getReactiveJShell().unresolveds(d)
                                                        .map(s -> "    " + s + "\n")
                                                        .collect(Collectors.joining());
                                                var l = new Label(message + unresolveds + "are defined");
                                                l.getStyleClass().add("jshell_eval_erroneous");
                                                Platform.runLater(() -> outputBox.getChildren().add(l));
                                            }
                                        });
                                Platform.runLater(() -> {
                                    execResult.setGraphic(failure);
                                    execResult.setVisible(true);
                                });
                            }
                        }
                        );
                    });
        }

        private String format(Snippet snippet, String value, Snippet.Status status) {
            return "/" + snippet.kind() + "/ " + switch (snippet) {
                case VarSnippet v ->
                    v.name() + " (" + v.typeName() + ") ==> " + value + " [" + status.name() + "]";
                case ExpressionSnippet e ->
                    e.name() + " (" + e.typeName() + ") ==> " + value + " [" + status.name() + "]";
                case ImportSnippet i ->
                    "Imported " + i.fullname() + (i.isStatic() ? " as static" : "") + " [" + status.name() + "]";
                case StatementSnippet s ->
                    "==> " + value + " [" + status.name() + "]";
                case Snippet s when snippet.subKind() == Snippet.SubKind.UNKNOWN_SUBKIND ->
                    snippet.toString();
                default ->
                    "Implementation missing for " + snippet.subKind();
            };
        }

        private void filterCompletion(String text, int caretPos) {
            this.control.getSheet().getReactiveJShell().completionAsync(text, caretPos, result -> {
                var distinctCompletionSuggestions = result.suggestions().stream().map(SourceCodeAnalysis.Suggestion::continuation).distinct().toList();
                Platform.runLater(() -> completionPopup.getSuggestions().setAll(distinctCompletionSuggestions));
            });
        }

        private CompletionUpdate convert(String fullText, int startOfcompletionText, int caretPosition, String fullCompletionText) {
            int offset = caretPosition - startOfcompletionText; // length to strip from full completion text
            String remainingCompletion = fullCompletionText.substring(offset);
            return new CompletionUpdate(fullCompletionText, remainingCompletion);
        }

        private void handleTabCompletion(String text, int caretPos, Point2D caretOrigin, Consumer<CompletionUpdate> consumer) {
            this.control.getSheet().getReactiveJShell().completionAsync(text, caretPos, result -> {
                var distinctCompletionSuggestions = result.suggestions().stream().map(SourceCodeAnalysis.Suggestion::continuation).distinct().toList();
                // only one completion - just do it
                if (distinctCompletionSuggestions.size() == 1) {
                    Platform.runLater(() -> consumer.accept(convert(text, result.anchor(), caretPos, distinctCompletionSuggestions.getFirst())));
                } else {
                    String completableCommonPrefix = UiUtils.longestCommonPrefix(distinctCompletionSuggestions);
                    if (!completableCommonPrefix.isEmpty()
                            && !text.substring(result.anchor(), caretPos).equals(completableCommonPrefix)) {
                        Platform.runLater(() -> consumer.accept(convert(text, result.anchor(), caretPos, completableCommonPrefix)));
                    } else {
                        completionPopup.getSuggestions().setAll(distinctCompletionSuggestions);
                        completionPopup.setOnCompletion((event) -> {
                            String withCompletion = text.substring(0, result.anchor()) + event.getSuggestion();
                            int newCaretPos = withCompletion.length();
                            Platform.runLater(() -> consumer.accept(convert(text, result.anchor(), caretPos, event.getSuggestion())));
                        });
                        Platform.runLater(() -> completionPopup.show(this.control.getScene().focusOwnerProperty().get(), caretOrigin));
                    }
                }
            });
        }

        static final TextDecoration declarationDecoration = TextDecoration.builder().presets().fontFamily("Monaspace Argon")
                .underline(Boolean.TRUE).fontWeight(FontWeight.NORMAL).fontSize(13).build();
        static final TextDecoration keywordDecoration = TextDecoration.builder().presets().fontFamily("Monaspace Argon")
                .fontWeight(FontWeight.BOLD).fontSize(13).build();

        private void handleSyntaxHighlighting(String text) {
            this.control.getSheet().getReactiveJShell().highlightingAsync(text, highlights -> {
//                highlights.forEach(System.out::println);
                Platform.runLater(()
                        -> input.getActionFactory().selectAndDecorate(
                                new Selection(0, input.getTextLength()),
                                presetDecoration).execute(new ActionEvent()));
                highlights.stream().forEach(h -> {
                    var attrs = h.attributes();
                    if (attrs.contains(SourceCodeAnalysis.Attribute.KEYWORD)) {
                        Platform.runLater(()
                                -> input.getActionFactory().selectAndDecorate(
                                        new Selection(h.start(), h.end()),
                                        keywordDecoration).execute(new ActionEvent()));
                    } else if (attrs.contains(SourceCodeAnalysis.Attribute.DECLARATION)) {
                        Platform.runLater(()
                                -> input.getActionFactory().selectAndDecorate(
                                        new Selection(h.start(), h.end()),
                                        declarationDecoration).execute(new ActionEvent()));
                    }
//                    Platform.runLater(() -> System.out.println(input.getDocument().getDecorations()));
                });
            });
        }

        @Override

        public JavaCell getSkinnable() {
            return this.control;
        }

        @Override
        public Node getNode() {
            return inputBox;
        }

        @Override
        public void dispose() {
        }

        void markAsOutdated(boolean isOutdated) {
            this.getNode().pseudoClassStateChanged(OUTDATED, isOutdated);
        }

        void markAsSelected(boolean isSelected) {
            this.getNode().pseudoClassStateChanged(SELECTED, isSelected);
        }

        static record CompletionUpdate(String completion, String completionToInsert) {
        }
    }
}
