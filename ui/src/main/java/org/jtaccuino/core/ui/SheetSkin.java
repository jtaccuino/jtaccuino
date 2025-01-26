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

import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import org.jtaccuino.core.ui.api.CellData;

public class SheetSkin implements Skin<Sheet> {

    private final Sheet sheet;
    private final ScrollPane pane;
    private final VBox cellBox;

    private ObservableList<Sheet.Cell> cells = FXCollections.observableArrayList();

    private JavaCellFactory javaCellFactory = new JavaCellFactory();
    private MarkdownCellFactory markdownCellFactory = new MarkdownCellFactory();

    @SuppressWarnings("this-escape")
    public SheetSkin(Sheet sheet) {
        this.sheet = sheet;
        cellBox = new VBox();
        cellBox.getStyleClass().add("cell-box");
        pane = new ScrollPane(cellBox);
        pane.setFitToWidth(true);
//        pane.setFitToHeight(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Bindings.bindContent(cellBox.getChildren(), cells);
        cells.addAll(sheet.getCells().stream()
                .map(cellData -> {
                    return switch (cellData.getType()) {
                        case CODE ->
                            javaCellFactory.createCell(cellData, cellBox, sheet);
                        case MARKDOWN ->
                            markdownCellFactory.createCell(cellData, cellBox, sheet);
                    };
                }).toList());
        Platform.runLater(this::moveFocusToFirstCell);
    }

    void execute() {
        cells.stream().forEach(cell -> cell.execute());
    }

    public void moveFocusToNextCell(Sheet.Cell currentCell) {
        int indexOfNextCell = cells.indexOf(currentCell) + 1;
        if (indexOfNextCell == cells.size()) {
            var newCell = insertCellAfter(currentCell);
            newCell.sceneProperty().addListener((observable, oldValue, newValue) -> {
                if (null != newValue) {
                    Platform.runLater(() -> {
                        try {
                            newCell.requestFocus();
                        } catch(Throwable t) {
                            System.out.println("Ex: " + t.getMessage());
                            t.printStackTrace();
                        }
                    });
                }
            });
        }

        Platform.runLater(() -> cells.get(indexOfNextCell).requestFocus());
    }

    public void moveFocusToFirstCell() {
        Platform.runLater(() -> cells.getFirst().requestFocus());
    }

    public void moveFocusToLastCell() {
        Platform.runLater(() -> cells.getLast().requestFocus());
    }

    public Sheet.Cell insertCellAfter(Sheet.Cell currentCell) {
        int indexOfCurrentCell = cells.indexOf(currentCell);
        org.jtaccuino.core.ui.api.CellData newCellData = CellData.empty(currentCell.getCellData().getType());
        sheet.getCells().add(indexOfCurrentCell + 1, newCellData);
        var newCell = switch (currentCell.getCellData().getType()) {
            case CODE ->
                javaCellFactory.createCell(newCellData, cellBox, sheet);
            case MARKDOWN ->
                markdownCellFactory.createCell(newCellData, cellBox, sheet);
        };

        cells.add(indexOfCurrentCell + 1, newCell);
        return newCell;
    }

    public void insertCellBefore(Sheet.Cell currentCell) {
        int indexOfCurrentCell = cells.indexOf(currentCell);
        org.jtaccuino.core.ui.api.CellData newCellData = CellData.empty(currentCell.getCellData().getType());
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

    void replaceCell(Sheet.Cell currentCell, Sheet.Cell newCell) {
        int indexOfCurrentCell = cells.indexOf(currentCell);
        cells.remove(indexOfCurrentCell);
        cells.add(indexOfCurrentCell, newCell);
        Platform.runLater(() -> newCell.requestFocus());
    }

    Sheet.Cell createCell(CellData.Type type, CellData cellData) {
        return switch (type) {
            case CODE -> {
                cellData.typeProperty().set(CellData.Type.CODE);
                yield javaCellFactory.createCell(cellData, cellBox, sheet);
            }
            case MARKDOWN -> {
                cellData.typeProperty().set(CellData.Type.MARKDOWN);
                yield markdownCellFactory.createCell(cellData, cellBox, sheet);
            }
        };
    }

    void scrollTo(Node node) {
        final Bounds viewPortBounds = pane.getViewportBounds();
        final double heightOfContent = cellBox.getBoundsInLocal().getHeight();
        final double heightOfViewport = viewPortBounds.getHeight();

        final double oversizedHeight = heightOfContent - heightOfViewport;

        final Bounds boundsInParent = node.getBoundsInParent();
        final double maxYValue = boundsInParent.getMaxY();
        final double maxYValueWithPadding = maxYValue + 5;
        final double vValue = pane.getVvalue();

        final double currentVisibleMinValue = oversizedHeight * vValue;
        final double currentVisibleMaxValue = currentVisibleMinValue + heightOfViewport;

        // node fits in viewport
        if (boundsInParent.getHeight() < heightOfViewport) {
            // bottom of component currently not visible
            if (maxYValueWithPadding > currentVisibleMaxValue) {
                // calculate newVValue
                double newVValue = (maxYValueWithPadding - heightOfViewport) / oversizedHeight;
                pane.setVvalue(newVValue);
            }
        }
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

    void markCellsAsOutdated(Predicate<Sheet.Cell> isCellOutdated) {
        cells.stream().filter(isCellOutdated).forEach(c -> c.markAsOutdated(true));
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
            text.setBoundsType(TextBoundsType.VISUAL);
            double height = text.getLayoutBounds().getHeight();
            double newPrefHeight = Math.max(
                    Math.ceil(height / getFont().getSize()) * 1.3 * getFont().getSize() + getFont().getSize(),
                    getMinHeight()
            );
            setPrefHeight(newPrefHeight);
            requestLayout();
        }
    }
}
