package org.jtaccuino;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.ImportSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.StatementSnippet;
import jdk.jshell.VarSnippet;
import org.jtaccuino.Sheet.Cell;
import static org.jtaccuino.UiUtils.createSVGToolbarButton;
import org.jtaccuino.control.CompletionPopup;
import org.jtaccuino.jshell.ReactiveJShell;

public class SheetSkin implements Skin<Sheet> {

    private final Sheet sheet;
    private final ScrollPane pane;
    private final ReactiveJShell reactiveJShell;
    int inputCounter = 0;
    private CompletionPopup completionPopup = new CompletionPopup();

    @SuppressWarnings("this-escape")
    public SheetSkin(Sheet sheet) {
        this.sheet = sheet;
        reactiveJShell = sheet.getReactiveJShell();
        VBox box = new VBox();
        pane = new ScrollPane(box);
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ShellUtil.INSTANCE.register(box, sheet.getReactiveJShell().getWrappedShell(), sheet.getUuid());
        ObservableList<Sheet.Cell> cells = sheet.getCells();
        sheet.getCells().stream()
                .map(cell -> createInput(box, sheet.getUuid(), cell))
                .forEach(input -> box.getChildren().add(input));
    }

    private HBox createToolbar(VBox outputBox, UUID uuid, TextArea input, BorderPane output, VBox vBox) {
        var executeCell = createSVGToolbarButton("execute-cell", "Execute Cell", "toolbar-button");
        executeCell.setOnAction((event) -> {
            handleExecution(outputBox, uuid, input, output, vBox);
        });
        var moveCellUp = createSVGToolbarButton("move-cell-up", "Move Cell Up", "toolbar-button");
        var moveCellDown = createSVGToolbarButton("move-cell-down", "Move Cell Down", "toolbar-button");
        var insertCellBefore = createSVGToolbarButton("insert-cell-before", "Insert Cell Before", "toolbar-button");
        var insertCellAfter = createSVGToolbarButton("insert-cell-after", "Insert Cell After", "toolbar-button");
        var deleteCell = createSVGToolbarButton("delete-cell", "Delete Cell", "toolbar-button");
        var hbox = new HBox(executeCell, moveCellUp, moveCellDown, insertCellBefore, insertCellAfter, deleteCell);
        HBox.setHgrow(hbox, Priority.NEVER);
        hbox.maxWidthProperty().bind(hbox.prefWidthProperty());
        hbox.getStyleClass().add("cell-toolbar");
        return hbox;
    }

    private VBox createInput(VBox vBox, UUID uuid, Cell cell) {
        TextArea input = new ResizingTextArea();
        input.setPrefRowCount(1);
        input.setPromptText("Type code here");
        input.setId("input_" + inputCounter++);
        if (null != cell.getSource()) {
            input.setText(cell.getSource());
        }
        cell.sourceProperty().bind(input.textProperty());

        input.prefWidthProperty().bind(Bindings.subtract(vBox.widthProperty(), 20));
        var output = new BorderPane();
        output.setVisible(false);
        var outputBox = new VBox();
        outputBox.setPadding(new Insets(5));
        output.setCenter(outputBox);
        var toolbar = createToolbar(outputBox, uuid, input, output, vBox);
        toolbar.visibleProperty().bind(Bindings.or(input.focusedProperty(), toolbar.focusWithinProperty()));
        var inputControl = new AnchorPane(input, toolbar);
        AnchorPane.setLeftAnchor(input, 0d);
        AnchorPane.setTopAnchor(input, 0d);
        AnchorPane.setRightAnchor(toolbar, 15d);
        AnchorPane.setTopAnchor(toolbar, 0d);
        var inputBox = new VBox(inputControl, output);
        inputBox.setPadding(new Insets(5));
        input.addEventFilter(KeyEvent.KEY_PRESSED, t -> {
            if (KeyCode.ENTER == t.getCode() && t.isShiftDown()) {
                handleExecution(outputBox, uuid, input, output, vBox);
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
                Platform.runLater(() -> scrollTo(pane, input));
                handleSyntaxHighlighting(input.getText());
            }
        });
        input.caretPositionProperty().addListener((observable) -> {
            if (completionPopup.isShowing()) {
                filterCompletion(input.getText(), input.caretPositionProperty().get());
            }
        });

        output.setCenter(outputBox);
        return inputBox;
    }

    private void handleExecution(VBox outputBox, UUID uuid, TextArea input, BorderPane output, final VBox vbox) {
        System.out.println("Executing");
        Platform.runLater(outputBox.getChildren()::clear);
        ShellUtil.INSTANCE.setActiveOutput(outputBox);
        reactiveJShell.evalAsync(input.getText(), events -> {
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
                    int nextInput = Integer.parseInt(input.getId().substring(6)) + 1;
                    Node node = vbox.lookup("#input_" + nextInput);
                    if (null == node) {
                        var c = Cell.empty();
                        sheet.cells.add(c);
                        vbox.getChildren().add(createInput(vbox, uuid, c));
                        var nextNode = vbox.lookup("#input_" + nextInput);
                        nextNode.requestFocus();
                        Platform.runLater(() -> scrollTo(pane, nextNode));
                    } else {
                        node.requestFocus();
                        Platform.runLater(() -> scrollTo(pane, node));
                    }
                } else {
                    events.stream()
                            .filter(event -> Snippet.Kind.ERRONEOUS == event.snippet().kind())
                            .forEach(event
                                    -> reactiveJShell.diagnose(event.snippet())
                                    .forEachOrdered(diag -> {
                                        var l = new Label(diag.getMessage(Locale.getDefault()));
                                        l.setTooltip(new Tooltip(diag.getCode()));
                                        Platform.runLater(() -> outputBox.getChildren().add(l));
                                    }));
                }
            });
        });
    }

    public String format(Snippet snippet, String value, Snippet.Status status) {
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
        reactiveJShell.completionAsync(text, caretPos, result -> {
            System.out.println("Found " + result.suggestions().size() + " possible completions");
            var distinctCompletionSuggestions = result.suggestions().stream().map(SourceCodeAnalysis.Suggestion::continuation).distinct().toList();
            distinctCompletionSuggestions.stream().forEach(System.out::println);
            Platform.runLater(() -> completionPopup.getSuggestions().setAll(distinctCompletionSuggestions));
        });
    }

    private void handleTabCompletion(String text, int caretPos, Rectangle2D caretCoordiantes, Consumer<CompletionUpdate> consumer) {
        System.out.println("Completing: " + text + " length " + text.length() + " caret: " + caretPos + " after " + text.substring(0, caretPos));
        reactiveJShell.completionAsync(text, caretPos, result -> {
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
                    Platform.runLater(() -> completionPopup.show(sheet.getScene().focusOwnerProperty().get(), caretCoordiantes));
                }
            }
        });
    }

    private void handleSyntaxHighlighting(String text) {
        //List<SourceCodeAnalysis.Highlight> highlights = reactiveJShell.sourceCodeAnalysis().highlights(text);
        //highlights.forEach(System.out::println);
    }

    private void scrollTo(ScrollPane scrollPane, Node node) {
        final Node content = scrollPane.getContent();
        Bounds localBounds = node.getBoundsInLocal();
        Point2D position = new Point2D(localBounds.getMinX(), localBounds.getMinY());

        // transform to content coordinates
        while (node != content) {
            position = node.localToParent(position);
            node = node.getParent();
        }

        final Bounds viewportBounds = scrollPane.getViewportBounds();
        final Bounds contentBounds = content.getBoundsInLocal();

        scrollPane.setHvalue(position.getX() / (contentBounds.getWidth() - viewportBounds.getWidth()));
        scrollPane.setVvalue(position.getY() / (contentBounds.getHeight() - viewportBounds.getHeight()));
    }

    record CompletionUpdate(String newContent, int newCaret) {

    }

    @Override
    public Sheet getSkinnable() {
        return sheet;
    }

    @Override
    public Node getNode() {
        return pane;
    }

    @Override
    public void dispose() {
    }

    public class ResizingTextArea extends TextArea {

        public ResizingTextArea() {
            this(null);
        }

        @SuppressWarnings("this-escape")
        public ResizingTextArea(String text) {
            super(text);
            setWrapText(true);
            setPrefRowCount(1);
            setMaxWidth(Double.MAX_VALUE);
            textProperty().addListener((observable) -> this.adjustHeightToText());
        }

        protected void adjustHeightToText() {
            Text text = new Text(getText());
            text.setFont(getFont());
            text.setWrappingWidth(getWidth());
            double height = text.getLayoutBounds().getHeight();
            double newPrefHeight = Math.max(height * 1.09 + 2 * getFont().getSize(), getMinHeight());
            setPrefHeight(newPrefHeight);
            requestLayout();
        }
    }

}
