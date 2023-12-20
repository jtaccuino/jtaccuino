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
package org.jtaccuino;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.ImportSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.StatementSnippet;
import jdk.jshell.VarSnippet;
import org.jtaccuino.control.CompletionPopup;

public class JavaCellFactory implements CellFactory {

    public static JavaCellFactory INSTANCE = new JavaCellFactory();

    @Override
    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet) {
        var cell = new JavaCell(cellData, parent, sheet, sheet.getNextId());
        return cell;
    }

    public static class JavaCell extends Sheet.Cell {

        private final int cellNumber;
        private final VBox parentBox;

        public JavaCell(CellData cellData, VBox parent, Sheet sheet, int cellNumber) {
            super(cellData, sheet);
            this.cellNumber = cellNumber;
            this.parentBox = parent;
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            return new JavaCellSkin(this);
        }

        @Override
        public void requestFocus() {
            ((JavaCellSkin) getSkin()).requestFocus();
        }

        @Override
        public void execute() {
            ((JavaCellSkin) getSkin()).execute();
        }

    }

    public static class JavaCellSkin extends AbstractCellSkin<JavaCell> {

        private static final PseudoClass HIGHLIGHT = PseudoClass.getPseudoClass("highlight");

        private final JavaCell control;

        private CompletionPopup completionPopup = new CompletionPopup();
        private final VBox inputBox;
        private final SheetSkin.ResizingTextArea input;
        private final VBox outputBox;
        private final BorderPane output;
        private final Label execResult;
        private final Region success;
        private final Region failure;

        private JavaCellSkin(JavaCell javaCell) {
            super(javaCell);
            this.control = javaCell;
            input = new SheetSkin.ResizingTextArea();
            input.setPrefRowCount(1);
            input.setPromptText("Type code here");
            input.setId("input_" + this.control.cellNumber);
            input.getStyleClass().add("code-editor");
            input.focusWithinProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    input.getPseudoClassStates().add(HIGHLIGHT);
                } else {
                    input.getPseudoClassStates().remove(HIGHLIGHT);
                }

            });
            if (null != javaCell.getCellData().getSource()) {
                input.setText(javaCell.getCellData().getSource());
            }
            javaCell.getCellData().sourceProperty().bind(input.textProperty());

            input.prefWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
            output = new BorderPane();
            output.setVisible(false);
            outputBox = new VBox();
            outputBox.setPadding(new Insets(5));
            output.setCenter(outputBox);
            var toolbar = createToolbar();
            toolbar.visibleProperty().bind(Bindings.or(input.focusedProperty(), toolbar.focusWithinProperty()));
            execResult = new Label();
            execResult.setVisible(false);
            success = new Region();
            success.getStyleClass().addAll("toolbar-button-graphics", "code-success");
            failure = new Region();
            failure.getStyleClass().addAll("toolbar-button-graphics", "code-failure");

            var inputControl = new AnchorPane(input, toolbar, execResult);
            AnchorPane.setLeftAnchor(input, 0d);
            AnchorPane.setTopAnchor(input, 0d);
            AnchorPane.setRightAnchor(toolbar, 15d);
            AnchorPane.setTopAnchor(toolbar, 0d);
            AnchorPane.setRightAnchor(execResult, 15d);
            AnchorPane.setBottomAnchor(execResult, 2d);

            inputBox = new VBox(inputControl, output);
            inputBox.setPadding(new Insets(5));
            input.addEventFilter(KeyEvent.KEY_PRESSED, t -> {
                if (KeyCode.ENTER == t.getCode() && t.isShiftDown()) {
                    handleExecution();
                    t.consume();
                } else if (KeyCode.TAB == t.getCode()) {
                    var oldText = input.getText();
                    var oldCaretPosition = input.caretPositionProperty().get();
                    Rectangle2D characterBounds = ((TextAreaSkin) input.getSkin()).getCharacterBounds(oldCaretPosition);
                    handleTabCompletion(input.getText(), oldCaretPosition, characterBounds, (completionUpdate) -> {
                        if (null != completionUpdate) {
                            input.setText(completionUpdate.newContent);
                            input.positionCaret(completionUpdate.newCaret);
                        } else {
                            input.setText(oldText);
                            input.positionCaret(oldCaretPosition);
                        }
                    });
                    t.consume();
                } else {
                    input.setPrefRowCount((int) input.getText().chars().filter(c -> c == '\n').count() + 1);
                    Platform.runLater(() -> this.control.getSheet().ensureCellVisible(control));
                    handleSyntaxHighlighting(input.getText());
                }
            });
            input.caretPositionProperty().addListener((observable) -> {
                if (completionPopup.isShowing()) {
                    filterCompletion(input.getText(), input.caretPositionProperty().get());
                }
            });

            output.setCenter(outputBox);
        }

        void requestFocus() {
            input.requestFocus();
            this.control.getSheet().ensureCellVisible(getNode());
        }

        @Override
        protected void execute() {
            handleExecution();
        }

        private void handleExecution() {
            this.control.getSheet()
                    .getReactiveJShell().evalAsync(
                            () -> {
                                ShellUtil.INSTANCE.setActiveOutput(outputBox);
                                ShellUtil.INSTANCE.setCurrentCellData(control.getCellData());
                                control.getCellData().getOutputData().clear();
                                Platform.runLater(outputBox.getChildren()::clear);
                            },
                            input.getText(),
                            evalResult -> {
// Move this to debug ouput mode...
//                                events.forEach(event -> {
//                                    var l = new Label(this.format(event.snippet(), event.value(), event.status()));
//                                    l.setTooltip(new Tooltip(event.snippet().source()));
//                                    Platform.runLater(() -> outputBox.getChildren().add(l));
//                                });
                                Platform.runLater(() -> {
                                    if (!output.isVisible()) {
                                        output.setVisible(true);
                                    }
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
                                                control.getCellData().getOutputData().add(
                                                        new CellData.OutputData(org.jtaccuino.CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("text/plain", resultData)));
                                            });
                                        });
                                    } else {
                                        evalResult.snippetEvents().stream()
                                                .filter(event -> null != event.exception())
                                                .map(event -> event.exception())
                                                .forEach(exception -> {
                                                    var text = exception.getCause().getClass().getName() + ": " + exception.getCause().getMessage();
                                                    text += "\n" + Arrays.stream(exception.getCause().getStackTrace())
                                                            .limit(exception.getCause().getStackTrace().length - 2)
                                                            .map(ste -> "\t" + ste.toString())
                                                            .collect(Collectors.joining("\n"));
                                                    var l = new Label(text);
                                                    l.getStyleClass().add("jshell_eval_exception");
                                                    Platform.runLater(() -> outputBox.getChildren().add(l));
                                                });
                                        evalResult.snippetEvents().stream()
                                                .filter(event -> Snippet.Kind.ERRONEOUS == event.snippet().kind() || Snippet.Status.REJECTED == event.status() || Snippet.Status.RECOVERABLE_NOT_DEFINED == event.status())
                                                .forEach(event
                                                        -> this.control.getSheet().getReactiveJShell().diagnose(event.snippet())
                                                        .forEachOrdered(diag -> {
                                                            var message = diag.getMessage(Locale.getDefault());
                                                            message += "\n" + event.snippet().source();
                                                            if (diag.getEndPosition() > 0) {
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
                                                        }));
                                        Platform.runLater(() -> {
                                            execResult.setGraphic(failure);
                                            execResult.setVisible(true);
                                        });
                                    }
                                });
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

        private void handleTabCompletion(String text, int caretPos, Rectangle2D caretCoordiantes, Consumer<CompletionUpdate> consumer) {
            this.control.getSheet().getReactiveJShell().completionAsync(text, caretPos, result -> {
                var distinctCompletionSuggestions = result.suggestions().stream().map(SourceCodeAnalysis.Suggestion::continuation).distinct().toList();
                // only one completion - just do it
                if (distinctCompletionSuggestions.size() == 1) {
                    String withCompletion = text.substring(0, result.anchor()) + distinctCompletionSuggestions.getFirst();
                    int newCaretPos = withCompletion.length();
                    Platform.runLater(() -> consumer.accept(new CompletionUpdate(withCompletion + text.substring(caretPos), newCaretPos)));
                } else {
                    String completableCommonPrefix = UiUtils.longestCommonPrefix(distinctCompletionSuggestions);
                    if (!completableCommonPrefix.isEmpty()
                            && !text.substring(result.anchor(), caretPos).equals(completableCommonPrefix)) {
                        String withCompletion = text.substring(0, result.anchor()) + completableCommonPrefix;
                        int newCaretPos = withCompletion.length();
                        Platform.runLater(() -> consumer.accept(new CompletionUpdate(withCompletion + text.substring(caretPos), newCaretPos)));
                    } else {
                        completionPopup.getSuggestions().setAll(distinctCompletionSuggestions);
                        completionPopup.setOnCompletion((event) -> {
                            String withCompletion = text.substring(0, result.anchor()) + event.getSuggestion();
                            int newCaretPos = withCompletion.length();
                            Platform.runLater(() -> consumer.accept(new CompletionUpdate(withCompletion + text.substring(caretPos), newCaretPos)));
                        });
                        Platform.runLater(() -> completionPopup.show(this.control.getScene().focusOwnerProperty().get(), caretCoordiantes));
                    }
                }
            });
        }

        private void handleSyntaxHighlighting(String text) {
            //List<SourceCodeAnalysis.Highlight> highlights = reactiveJShell.sourceCodeAnalysis().highlights(text);
            //highlights.forEach(System.out::println);
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

        static record CompletionUpdate(String newContent, int newCaret) {

        }

    }
}
