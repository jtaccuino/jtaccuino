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

import javafx.scene.text.FontPosture;
import org.jtaccuino.core.ui.completion.CompletionItem;
import org.jtaccuino.core.ui.completion.CompletionPopup;
import org.jtaccuino.core.ui.api.CellData;
import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.Selection;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextDecoration;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
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
import org.jtaccuino.core.ui.documentation.DocumentationItem;
import org.jtaccuino.core.ui.documentation.DocumentationPopup;
import org.jtaccuino.core.ui.extensions.DisplayExtension;
import org.jtaccuino.core.ui.extensions.PrintExtension;
import org.jtaccuino.jshell.JavaParserSnippetAnalyzer;
import org.jtaccuino.jshell.SyntaxHighlight;

public class JavaCellFactory implements CellFactory {

    public static JavaCellFactory INSTANCE = new JavaCellFactory();

    @Override
    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet) {
        var cell = new JavaCell(cellData, parent, sheet, sheet.getNextId());
        return cell;
    }

    public static class JavaCell extends Sheet.Cell {

        private static final StyleablePropertyFactory<JavaCell> FACTORY = new StyleablePropertyFactory<>(JavaCell.getClassCssMetaData());

        @SuppressWarnings("unchecked")
        public ObservableValue<Font> baseEditorFontProperty() {
            return (ObservableValue<Font>) baseEditorFont;
        }

        public final Font getBaseEditorFont() {
            return baseEditorFont.getValue();
        }

        public final void setBaseEditorFont(Font baseEditorFont) {
            this.baseEditorFont.setValue(baseEditorFont);
        }

        @SuppressWarnings("this-escape")
        private final StyleableProperty<Font> baseEditorFont
                = FACTORY.createStyleableFontProperty(this, "baseEditorFont", "-base-editor-font", f -> f.baseEditorFont);

        private List<String> snippetIds = Collections.emptyList();

        @SuppressWarnings("this-escape")
        public JavaCell(CellData cellData, VBox parent, Sheet sheet, int cellNumber) {
            super(cellData, sheet, cellNumber);
            getStyleClass().add("java-cell");
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            return new JavaCellSkin(this);
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
            return FACTORY.getCssMetaData();
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
            super.markAsOutdated(isOutdated);
        }

        @Override
        public void markAsSelected(boolean isSelected) {
            ((JavaCellSkin) getSkin()).markAsSelected(isSelected);
            super.markAsSelected(isSelected);
        }

        List<String> getSnippetIds() {
            return Collections.unmodifiableList(snippetIds);
        }
    }

    public static class JavaCellSkin extends AbstractCellSkin<JavaCell> {

        static String colorKeyword = "#034BD3";
        static String colorMethod = "#27647C";
        static String colorDeclaration = "#8716B0";
        static String colorComment = "#8C8C8C";
        static String colorString = "#4B9245";

        static String defaultFont = "Monaspace Argon";

        static TextDecoration presetDecoration = TextDecoration.builder().presets()
                .fontFamily(defaultFont)
                .fontWeight(FontWeight.NORMAL)
                .fontSize(13).build();

        static TextDecoration declarationDecoration = TextDecoration.builder().presets()
                .fontFamily(defaultFont)
                .fontWeight(FontWeight.NORMAL)
                .fontSize(13)
                .foreground(colorDeclaration)
                .build();

        static TextDecoration keywordDecoration = TextDecoration.builder().presets()
                .fontFamily(defaultFont)
                .fontWeight(FontWeight.NORMAL)
                .fontSize(13)
                .foreground(colorKeyword)
                .build();

        static TextDecoration commentDecoration = TextDecoration.builder().presets()
                .fontFamily(defaultFont)
                .fontSize(13)
                .foreground(colorComment)
                .build();

        static TextDecoration methodDeclarationDecoration = TextDecoration.builder().presets()
                .fontFamily(defaultFont)
                .fontSize(13)
                .foreground(colorMethod)
                .build();

        static TextDecoration methodInvocationDeclarationDecoration = TextDecoration.builder().presets()
                .fontFamily(defaultFont)
                .fontPosture(FontPosture.ITALIC)
                .fontSize(13)
                .build();

        static TextDecoration stringDecoration = TextDecoration.builder().presets()
                .fontFamily(defaultFont)
                .fontSize(13)
                .foreground(colorString)
                .build();

        private static final ParagraphDecoration parPreset
                = ParagraphDecoration.builder().presets()
                        .graphicType(ParagraphDecoration.GraphicType.NUMBERED_LIST)
                        .indentationLevel(1)
                        .topInset(3)
                        .build();

        private final JavaCell control;

        private final CompletionPopup completionPopup = new CompletionPopup();
        private final DocumentationPopup documentationPopup = new DocumentationPopup();
        private final VBox inputBox;
        private final RichTextArea input;
        private final VBox outputBox;
        private final Label streamResult;
//        private final BorderPane output;
        private final Label execResult;
        private final Region success;
        private final Region failure;
        private final ProgressIndicator running;

        private JavaCellSkin(JavaCell javaCell) {
            super(javaCell);
            this.control = javaCell;
            this.control.baseEditorFontProperty().addListener(new ChangeListener<Font>() {
                @Override
                public void changed(ObservableValue<? extends Font> observable, Font oldValue, Font newValue) {
                    presetDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize()).build();
                    declarationDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .underline(Boolean.FALSE).fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize()).build();
                    keywordDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize()).build();
                    commentDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize())
                            .foreground(colorComment)
                            .build();
                    methodDeclarationDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize())
                            .foreground(colorMethod)
                            .build();
                    methodInvocationDeclarationDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize())
                            .fontPosture(FontPosture.ITALIC)
                            .build();
                    stringDecoration = TextDecoration.builder().presets().fontFamily(newValue.getFamily())
                            .fontWeight(FontWeight.NORMAL).fontSize(newValue.getSize())
                            .foreground(colorString)
                            .build();

                    handleSyntaxHighlighting(input.getDocument().getText());
                }
            });
            var inputControl = new JavaControl(control.cellNumber);
            input = inputControl.getInput();
            caretRowColumnProperty.bind(input.caretRowColumnProperty());
            input.documentProperty().subscribe((t, u) -> {
                if (!t.getText().equals(u.getText())) {
                    handleSyntaxHighlighting(input.getDocument().getText());
                }
            });

            inputControl.codeEditorFocussed().addListener((observable, oldValue, newValue) -> getSkinnable().markAsSelected(newValue));
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
            streamResult = new Label();
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

            running = new ProgressIndicator();
            running.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            running.getStyleClass().addAll("toolbar-button-graphics", "code-running");
//            running.setPrefSize(20, 20);

            AnchorPane.setRightAnchor(toolbar, 5d);
            AnchorPane.setTopAnchor(toolbar, 0d);
            AnchorPane.setRightAnchor(execResult, 5d);
            AnchorPane.setBottomAnchor(execResult, 2d);

            streamResult.getStyleClass().add("jshell-stream-result");
            execResult.getStyleClass().add("jshell-exec-result");
            inputBox = new VBox(inputControl); //, outputBox);

            inputBox.getStyleClass().add("java-cell-meta");
            streamResult.textProperty().addListener((ov, t, t1) -> {
                Platform.runLater(() -> {
                    if (t1.isEmpty()) {
                        inputBox.getChildren().remove(streamResult);
                    } else {
                        if (!inputBox.getChildren().contains(streamResult)) {
                            inputBox.getChildren().add(streamResult);
                        }
                    }
                });
            });
            outputBox.getChildren()
                    .addListener((ListChangeListener.Change<? extends Node> c) -> {
                        if (c.getList().isEmpty()) {
                            inputBox.getChildren().remove(outputBox);
                        } else {
                            if (!inputBox.getChildren().contains(outputBox)) {
                                inputBox.getChildren().add(outputBox);
                            }
                        }
                    });

            var outputNodes = javaCell.getCellData().getOutputData().stream().map(outputData
                    -> switch (outputData) {
                case CellData.MimeTypeBasedOutputData od ->
                    od.mimeBundle().entrySet().stream().map(me
                    -> switch (me.getKey()) {
                        case "image/png" ->
                            new ImageView(new Image(new ByteArrayInputStream(Base64.getDecoder().decode(me.getValue()))));
                        case "text/plain" ->
                            new Label(me.getValue());
                        default ->
                            new Label(me.getValue());
                    }
                    ).toList();
                case CellData.StreamBasedOutputData od ->
                    List.of(new Label(od.data()));
            }).toList();

            outputNodes.forEach(nodes -> nodes.forEach(n -> outputBox.getChildren().add(n)));

            // works around not customizable input map from RTA (e.g. shift-enter for cell execution)
            input.onKeyPressedProperty()
                    .addListener((ov, t, t1) -> {
                        if (t1 != getKeyHandler()) {
                            input.setOnKeyPressed(getKeyHandler());
                            delegateKeyEvents(t1);
                        }
                    });

            input.addEventFilter(KeyEvent.KEY_PRESSED, t
                    -> {
                if (KeyCode.TAB == t.getCode()) {
                    var oldCaretPosition = input.caretPositionProperty().get();
                    if (t.isShiftDown()) {
                        handleTabDocumentation(input.getDocument().getText(), oldCaretPosition, input.getCaretOrigin().add(0, 13));
                    } else {
                        handleTabCompletion(input.getDocument().getText(), oldCaretPosition,
                                input.getCaretOrigin().add(0, 13),
                                (completionUpdate) -> {
                                    if (null != completionUpdate) {
                                        input.getActionFactory().
                                                insertText(completionUpdate.completionToInsert).
                                                execute(new ActionEvent());
                                    }
                                });
                    }
                    t.consume();
                } else if (KeyCode.BACK_SPACE == t.getCode()) {
                    var column = (int) input.getCaretRowColumn().getX();
                    var row = (int) input.getCaretRowColumn().getY();
                    if (0 == column && 0 == row) {
                        // just consume the event to inhibt deletion of paragraph decoration
                        t.consume();
                    } else if (0 == column) {
                        // automatically remove paragraph decoration in case at beginning of line
                        input.getActionFactory().removeExtremesAndDecorate(
                                new Selection(input.getCaretPosition() - 1, input.getCaretPosition()),
                                ParagraphDecoration.builder().build()).execute(new ActionEvent());
                    }
                } else if (KeyCode.ENTER == t.getCode() && !t.isShiftDown()) {
                    var column = (int) input.getCaretRowColumn().getX();
                    if (0 == column) {
                        input.getActionFactory().insertText("\n").execute(new ActionEvent());
                        t.consume();
                    }
                } else {
                    Platform.runLater(() -> this.control.getSheet().ensureCellVisible(control));
                }
            });

            input.documentProperty().addListener((observable, oldValue, newValue) -> {
                if (completionPopup.isShowing()) {
                    Platform.runLater(() -> filterCompletion(input.getDocument().getText(), input.getDocument().getCaretPosition()));
                }
            });
            input.caretOriginProperty().addListener((observable, oldValue, newValue) -> {
                if (completionPopup.isShowing()) {
                    Platform.runLater(() -> completionPopup.updateLocation(input.getCaretOrigin().add(0, 13)));
                }
            });
        }

        void requestFocus() {
            this.control.getSheet().ensureCellVisible(control);
            this.control.markAsSelected(true);
            input.requestFocus();
        }

        @Override
        protected void execute() {
            handleExecution();
        }

        private void handleExecution() {
            var shell = this.control.getSheet().getReactiveJShell();
            var displayManager = shell.getExtension(DisplayExtension.class);
            var printManager = shell.getExtension(PrintExtension.class);
            shell.evalAsync(() -> {
                Platform.runLater(() -> {
                    displayManager.setActiveOutput(outputBox);
                    displayManager.setCurrentCellData(control.getCellData());
                    printManager.setActiveStreamResult(streamResult);
                    printManager.setCurrentCellData(control.getCellData());
                    control.getCellData().getOutputData().clear();
                    streamResult.setText("");
                    outputBox.getChildren().clear();
                    execResult.setGraphic(running);
                    execResult.setVisible(true);
                });
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
                                    evalResult.lastValueAsString().ifPresent(s -> {
                                        var resultData = evalResult.typeOfLastValue().get() + ": " +
                                                s.replace("\\n", "\n").replace("\\\"","\"");
                                        var result = new Label(resultData);
                                        result.getStyleClass().add("jshell_eval_result");
                                        outputBox.getChildren().add(result);
                                        control.getCellData().getOutputData().add(
                                                CellData.OutputData.of(org.jtaccuino.core.ui.api.CellData.OutputData.OutputType.DISPLAY_DATA,
                                                        Map.of("text/plain", resultData)));
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
                                                        var message = new StringBuilder()
                                                                .append(diag.getMessage(Locale.getDefault()))
                                                                .append('\n')
                                                                .append(event.snippet().source());
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
                                                        var l = new Label(message.toString());
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

        @SuppressWarnings("UnusedMethod") // TODO: Remove if really unused
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
                var distinctCompletionSuggestions = result.suggestions().stream().map(s -> CompletionItem.from(s, result.anchor())).distinct().toList();
                Platform.runLater(() -> completionPopup.setSuggestions(distinctCompletionSuggestions));
            });
        }

        private CompletionUpdate convert(int startOfcompletionText, int caretPosition, String fullCompletionText) {
            int offset = caretPosition - startOfcompletionText; // length to strip from full completion text
            String remainingCompletion = fullCompletionText.substring(offset);
            return new CompletionUpdate(fullCompletionText, remainingCompletion);
        }

        private void handleTabDocumentation(String text, int caretPos, Point2D caretOrigin) {
            this.control.getSheet().getReactiveJShell().documentationAsync(text, caretPos, result -> {
                Platform.runLater(() -> {
                    if (result.isEmpty()) {
                        documentationPopup.hide();
                    } else {
                        documentationPopup.setDocumentations(result.stream().map(d -> DocumentationItem.from(d.signature())).toList());
                        documentationPopup.show(this.control.getScene().focusOwnerProperty().get(), caretOrigin);
                    }
                });
            });
        }

        private void handleTabCompletion(String text, int caretPos, Point2D caretOrigin, Consumer<CompletionUpdate> consumer) {
            this.control.getSheet().getReactiveJShell().completionAsync(text, caretPos, result -> {
                var distinctCompletionSuggestions = result.suggestions().stream().map(s -> CompletionItem.from(s, result.anchor())).distinct().toList();
                // no completions
                if (distinctCompletionSuggestions.isEmpty()) {
                    Platform.runLater(() -> completionPopup.hide());
                    return;
                }
                // only one completion - just do it
                if (distinctCompletionSuggestions.size() == 1) {
                    Platform.runLater(() -> consumer.accept(convert(result.anchor(), caretPos, distinctCompletionSuggestions.getFirst().completion())));
                } else {
                    String completableCommonPrefix = CompletionItem.longestCommonPrefix(distinctCompletionSuggestions);
                    if (!completableCommonPrefix.isEmpty()
                            && !text.substring(result.anchor(), caretPos).equals(completableCommonPrefix)) {
                        Platform.runLater(() -> consumer.accept(convert(result.anchor(), caretPos, completableCommonPrefix)));
                    } else {
                        completionPopup.setOnCompletion(event -> Platform.runLater(() -> consumer.accept(convert(event.getAnchor(), input.getCaretPosition(), event.getSuggestion()))));
                        Platform.runLater(() -> {
                            completionPopup.setSuggestions(distinctCompletionSuggestions);
                            completionPopup.show(this.control.getScene().focusOwnerProperty().get(), caretOrigin);
                        });
                    }
                }
            });
        }

        private void handleSyntaxHighlighting(String text) {
            // commented out code below use JShell to provide code highlight hots. JShell can detect only keywords and declrations
            // this.control.getSheet().getReactiveJShell().highlightingAsync(text, highlights -> {
            JavaParserSnippetAnalyzer.highlightingAsync(text, highlights -> {
//                highlights.forEach(System.out::println);
                Platform.runLater(()
                        -> input.getActionFactory().selectAndDecorate(
                                new Selection(0, input.getTextLength()),
                                presetDecoration).execute(new ActionEvent()));

                List<Runnable> individualHighlightActions = new ArrayList<>();

                for (SyntaxHighlight h : highlights) {
                    var attrs = h.attributes();

                    if (attrs.contains(SyntaxHighlight.SyntaxHighlightAttribute.KEYWORD)) {
                        individualHighlightActions.add(()
                                -> input.getActionFactory().selectAndDecorate(
                                new Selection(h.start(), h.end()),
                                keywordDecoration).execute(new ActionEvent()));
                    } else if (attrs.contains(SyntaxHighlight.SyntaxHighlightAttribute.DECLARATION)) {
                        individualHighlightActions.add(()
                                -> input.getActionFactory().selectAndDecorate(
                                new Selection(h.start(), h.end()),
                                declarationDecoration).execute(new ActionEvent()));
                    } else if (attrs.contains(SyntaxHighlight.SyntaxHighlightAttribute.STRING)) {
                        individualHighlightActions.add(()
                                -> input.getActionFactory().selectAndDecorate(
                                new Selection(h.start(), h.end()),
                                stringDecoration).execute(new ActionEvent()));
                    } else if (attrs.contains(SyntaxHighlight.SyntaxHighlightAttribute.COMMENT)) {
                        individualHighlightActions.add(()
                                -> input.getActionFactory().selectAndDecorate(
                                new Selection(h.start(), h.end()),
                                commentDecoration).execute(new ActionEvent()));
                    } else if (attrs.contains(SyntaxHighlight.SyntaxHighlightAttribute.METHOD_DECLARATION)) {
                        individualHighlightActions.add(()
                                -> input.getActionFactory().selectAndDecorate(
                                new Selection(h.start(), h.end()),
                                methodDeclarationDecoration).execute(new ActionEvent()));
                    } else if (attrs.contains(SyntaxHighlight.SyntaxHighlightAttribute.METHOD_INVOCATION)) {
                        individualHighlightActions.add(()
                                -> input.getActionFactory().selectAndDecorate(
                                new Selection(h.start(), h.end()),
                                methodInvocationDeclarationDecoration).execute(new ActionEvent()));
                    } else if (attrs.contains(SyntaxHighlight.SyntaxHighlightAttribute.VARIABLE_REFERENCE)) {
                        individualHighlightActions.add(()
                                -> input.getActionFactory().selectAndDecorate(
                                new Selection(h.start(), h.end()),
                                declarationDecoration).execute(new ActionEvent()));
                    }
//                    Platform.runLater(() -> System.out.println(input.getDocument().getDecorations()));
                }

                Platform.runLater(() -> {
                    for (var action : individualHighlightActions) {
                        action.run();
                    }
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
