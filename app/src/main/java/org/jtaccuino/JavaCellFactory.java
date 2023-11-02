package org.jtaccuino;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.ImportSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.StatementSnippet;
import jdk.jshell.VarSnippet;
import static org.jtaccuino.UiUtils.createSVGToggleToolbarButton;
import static org.jtaccuino.UiUtils.createSVGToolbarButton;
import org.jtaccuino.control.CompletionPopup;

public class JavaCellFactory implements CellFactory {

    public static JavaCellFactory INSTANCE = new JavaCellFactory();

    @Override
    public Sheet.Cell createCell(Sheet.CellData cellData, VBox parent, Sheet sheet) {
        var cell = new JavaCell(cellData, parent, sheet, sheet.getNextId());
        return cell;
    }

    public static class JavaCell extends Sheet.Cell {

        private final int cellNumber;
        private final VBox parentBox;
        private final Sheet sheet;

        public JavaCell(Sheet.CellData cellData, VBox parent, Sheet sheet, int cellNumber) {
            super(cellData);
            this.cellNumber = cellNumber;
            this.parentBox = parent;
            this.sheet = sheet;
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

    public static class JavaCellSkin implements Skin<JavaCell> {

        private final JavaCell control;

        private CompletionPopup completionPopup = new CompletionPopup();
        private final VBox inputBox;
        private final SheetSkin.ResizingTextArea input;
        private final VBox outputBox;
        private final BorderPane output;

        private JavaCellSkin(JavaCell javaCell) {
            this.control = javaCell;
            input = new SheetSkin.ResizingTextArea();
            input.setPrefRowCount(1);
            input.setPromptText("Type code here");
            input.setId("input_" + this.control.cellNumber);
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
            var toolbar = createToolbar(outputBox, this.control.sheet.getUuid(), input, output, this.control.parentBox);
            toolbar.visibleProperty().bind(Bindings.or(input.focusedProperty(), toolbar.focusWithinProperty()));
            var inputControl = new AnchorPane(input, toolbar);
            AnchorPane.setLeftAnchor(input, 0d);
            AnchorPane.setTopAnchor(input, 0d);
            AnchorPane.setRightAnchor(toolbar, 15d);
            AnchorPane.setTopAnchor(toolbar, 0d);
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
                    Platform.runLater(() -> this.control.sheet.ensureCellVisible(control));
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
        }

        private HBox createToolbar(VBox outputBox, UUID uuid, TextArea input, BorderPane output, VBox vBox) {
            var executeCell = createSVGToolbarButton("execute-cell", "Execute Cell", "toolbar-button");
            executeCell.setOnAction((event) -> {
                handleExecution();
            });
            var moveCellUp = createSVGToolbarButton("move-cell-up", "Move Cell Up", "toolbar-button");
            moveCellUp.setOnAction((event) -> {
                this.control.sheet.moveCellUp(control);
            });
            var moveCellDown = createSVGToolbarButton("move-cell-down", "Move Cell Down", "toolbar-button");
            moveCellDown.setOnAction((event) -> {
                this.control.sheet.moveCellDown(control);
            });

            var insertCellBefore = createSVGToolbarButton("insert-cell-before", "Insert Cell Before", "toolbar-button");
            insertCellBefore.setOnAction((event) -> {
                this.control.sheet.insertCellBefore(control);
            });
            var insertCellAfter = createSVGToolbarButton("insert-cell-after", "Insert Cell After", "toolbar-button");
            insertCellAfter.setOnAction((event) -> {
                this.control.sheet.insertCellAfter(control);
            });
            var deleteCell = createSVGToolbarButton("delete-cell", "Delete Cell", "toolbar-button");
            deleteCell.setOnAction((event) -> {
                this.control.sheet.removeCell(control);
            });

            var mdType = createSVGToggleToolbarButton("md-cell-type", "Use Markdown for Cell", "toolbar-button");
            var javaType = createSVGToggleToolbarButton("java-cell-type", "Use Java Code for Cell", "toolbar-button");
            final ToggleGroup toggleGroup = new ToggleGroup();

            mdType.setToggleGroup(toggleGroup);
            javaType.setToggleGroup(toggleGroup);
            if (control.getCellData().getType().equals("java")) {
                toggleGroup.selectToggle(javaType);
            } else if (control.getCellData().getType().equals("markdown")) {
                toggleGroup.selectToggle(mdType);
            }

            var hbox = new HBox(executeCell, moveCellUp, moveCellDown, insertCellBefore, insertCellAfter, deleteCell, mdType, javaType);
            HBox.setHgrow(hbox, Priority.NEVER);
            hbox.maxWidthProperty().bind(hbox.prefWidthProperty());
            hbox.getStyleClass().add("cell-toolbar");
            return hbox;
        }

        void execute() {
            handleExecution();
        }
        
        private void handleExecution() {
            System.out.println("Executing");
            Platform.runLater(outputBox.getChildren()::clear);
            ShellUtil.INSTANCE.setActiveOutput(outputBox);
            this.control.sheet.getReactiveJShell().evalAsync(input.getText(), events -> {
                events.forEach(event -> {
                    var l = new Label(this.format(event.snippet(), event.value(), event.status()));
                    l.setTooltip(new Tooltip(event.snippet().source()));
                    Platform.runLater(() -> outputBox.getChildren().add(l));
                });
                Platform.runLater(() -> {
                    if (!output.isVisible()) {
                        output.setVisible(true);
                    }
                    if (events.stream().allMatch(event -> Snippet.Kind.ERRONEOUS != event.snippet().kind())) {
                        Platform.runLater(() -> {
                            this.control.sheet.moveFocusToNextCell(control);
                        });
                    } else {
                        events.stream()
                                .filter(event -> Snippet.Kind.ERRONEOUS == event.snippet().kind())
                                .forEach(event
                                        -> this.control.sheet.getReactiveJShell().diagnose(event.snippet())
                                        .forEachOrdered(diag -> {
                                            var l = new Label(diag.getMessage(Locale.getDefault()));
                                            l.setTooltip(new Tooltip(diag.getCode()));
                                            Platform.runLater(() -> outputBox.getChildren().add(l));
                                        }));
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
            this.control.sheet.getReactiveJShell().completionAsync(text, caretPos, result -> {
                System.out.println("Found " + result.suggestions().size() + " possible completions");
                var distinctCompletionSuggestions = result.suggestions().stream().map(SourceCodeAnalysis.Suggestion::continuation).distinct().toList();
                distinctCompletionSuggestions.stream().forEach(System.out::println);
                Platform.runLater(() -> completionPopup.getSuggestions().setAll(distinctCompletionSuggestions));
            });
        }

        private void handleTabCompletion(String text, int caretPos, Rectangle2D caretCoordiantes, Consumer<CompletionUpdate> consumer) {
            System.out.println("Completing: " + text + " length " + text.length() + " caret: " + caretPos + " after " + text.substring(0, caretPos));
            this.control.sheet.getReactiveJShell().completionAsync(text, caretPos, result -> {
                System.out.println("Found " + result.suggestions().size() + " possible completions");
                var distinctCompletionSuggestions = result.suggestions().stream().map(SourceCodeAnalysis.Suggestion::continuation).distinct().toList();
                distinctCompletionSuggestions.stream().forEach(System.out::println);
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
