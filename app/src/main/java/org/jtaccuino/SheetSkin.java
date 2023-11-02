package org.jtaccuino;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class SheetSkin implements Skin<Sheet> {

    private final Sheet sheet;
    private final ScrollPane pane;
    private final VBox cellBox;

    private ObservableList<Sheet.Cell> cells = FXCollections.observableArrayList();

    private JavaCellFactory javaCellFactory = new JavaCellFactory();

    @SuppressWarnings("this-escape")
    public SheetSkin(Sheet sheet) {
        this.sheet = sheet;
        cellBox = new VBox();
        pane = new ScrollPane(cellBox);
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ShellUtil.INSTANCE.register(cellBox, sheet.getReactiveJShell().getWrappedShell(), sheet.getUuid());
        
        sheet.reactiveJShellProperty().addListener((observable, oldValue, newValue) -> {
            ShellUtil.INSTANCE.register(cellBox, newValue.getWrappedShell(), sheet.getUuid());
        });
        Bindings.bindContent(cellBox.getChildren(), cells);
        cells.addAll(sheet.getCells().stream()
                .map(cellData -> javaCellFactory.createCell(cellData, cellBox, sheet)).toList());
    }
    
    void execute() {
        cells.stream().forEach(cell -> cell.execute());
    }

    public void moveFocusToNextCell(Sheet.Cell currentCell) {
        int indexOfNextCell = cells.indexOf(currentCell) + 1;
        if (indexOfNextCell == cells.size()) {
            // Select Factory based on type...
            insertCellAfter(currentCell);
        }
        Platform.runLater(() -> cells.get(indexOfNextCell).requestFocus());
    }

    public void insertCellAfter(Sheet.Cell currentCell) {
        int indexOfCurrentCell = cells.indexOf(currentCell);
        var newCellData = Sheet.CellData.empty(currentCell.getCellData().getType());
        sheet.getCells().add(indexOfCurrentCell + 1, newCellData);
        var newCell = javaCellFactory.createCell(newCellData, cellBox, sheet);

        cells.add(indexOfCurrentCell + 1, newCell);
    }

    public void insertCellBefore(Sheet.Cell currentCell) {
        int indexOfCurrentCell = cells.indexOf(currentCell);
        var newCellData = Sheet.CellData.empty(currentCell.getCellData().getType());
        sheet.getCells().add(indexOfCurrentCell, newCellData);
        var newCell = javaCellFactory.createCell(newCellData, cellBox, sheet);
        cells.add(indexOfCurrentCell, newCell);
    }

    public void removeCell(Sheet.Cell currentCell) {
        sheet.getCells().remove(currentCell.getCellData());
        cells.remove(currentCell);
    }

    public void moveCellUp(Sheet.Cell currentCell) {
        int indexOfCurrentCell = cells.indexOf(currentCell);
        if (0 < indexOfCurrentCell) {
            cells.remove(currentCell);
            sheet.getCells().remove(currentCell.getCellData());
            cells.add(indexOfCurrentCell - 1, currentCell);
            sheet.getCells().add(indexOfCurrentCell - 1, currentCell.getCellData());
            Platform.runLater(() -> currentCell.requestFocus());
        }
    }

    public void moveCellDown(Sheet.Cell currentCell) {
        int indexOfCurrentCell = cells.indexOf(currentCell);
        if (cells.size() - 1 >= indexOfCurrentCell) {
            cells.remove(currentCell);
            sheet.getCells().remove(currentCell.getCellData());
            cells.add(indexOfCurrentCell + 1, currentCell);
            sheet.getCells().add(indexOfCurrentCell + 1, currentCell.getCellData());
            Platform.runLater(() -> currentCell.requestFocus());
        }
    }

    void scrollTo(Node node) {
        final Node content = pane.getContent();
        Bounds localBounds = node.getBoundsInLocal();
        Point2D position = new Point2D(localBounds.getMinX(), localBounds.getMinY());

        // transform to content coordinates
        while (node != content) {
            position = node.localToParent(position);
            node = node.getParent();
        }

        final Bounds viewportBounds = pane.getViewportBounds();
        final Bounds contentBounds = content.getBoundsInLocal();

        pane.setHvalue(position.getX() / (contentBounds.getWidth() - viewportBounds.getWidth()));
        pane.setVvalue(position.getY() / (contentBounds.getHeight() - viewportBounds.getHeight()));
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

    public static class ResizingTextArea extends TextArea {

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
