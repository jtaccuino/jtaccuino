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
package org.jtaccuino.core.ui;

import java.io.File;
import org.jtaccuino.format.Notebook;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import org.jtaccuino.format.Notebook.CodeCell;
import org.jtaccuino.format.Notebook.MarkdownCell;
import org.jtaccuino.format.NotebookCompat;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.ReactiveJShellProvider;

public class Sheet extends Control {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("SheetWorker").factory());

    private final SimpleObjectProperty<ReactiveJShell> reactiveJShellProperty = new SimpleObjectProperty<>();
    private final UUID uuid;
    private int counter = 0;
    private File notebookFile = null;

    private final ObservableList<CellData> cells = FXCollections.observableArrayList();

    public static Sheet of(Notebook notebook) {
        return Sheet.of(notebook, null);
    }

    public static Sheet of(Notebook notebook, File file) {
        var s = new Sheet(file);
        notebook.cells().stream()
                .map(Sheet::convertFromNotebookCell)
                .forEach(s::addCell);
        return s;
    }

    private static final CellData convertFromNotebookCell(Notebook.Cell cell) {
        return switch (cell) {
            case CodeCell c ->
                CellData.of(
                CellData.Type.of(c.cell_type()),
                c.source(),
                c.id() == null ? UUID.randomUUID() : UUID.fromString(c.id()),
                c.outputs() != null ? c.outputs().stream().map(o
                -> CellData.OutputData.of(
                CellData.OutputData.OutputType.of(o.output_type()),
                o.data())
                ).toList() : new ArrayList<>()
                );
            case MarkdownCell m ->
                CellData.of(
                CellData.Type.of(m.cell_type()),
                m.source(),
                m.id() == null ? UUID.randomUUID() : UUID.fromString(m.id())
                );
        };
    }

    public static Sheet of(NotebookCompat notebook) {
        return Sheet.of(notebook, null);
    }

    public static Sheet of(NotebookCompat notebook, File file) {
        var s = new Sheet(file);
        notebook.cells().stream()
                .map(Sheet::convertFromNotebookCompatCell)
                .forEach(s::addCell);
        return s;
    }

    private static final CellData convertFromNotebookCompatCell(NotebookCompat.Cell cell) {
        return switch (cell) {
            case NotebookCompat.CodeCell c ->
                CellData.of(
                CellData.Type.of(c.cell_type()),
                Arrays.stream(c.source()).collect(Collectors.joining()),
                c.id() == null ? UUID.randomUUID() : UUID.fromString(c.id()),
                c.outputs() != null ? c.outputs().stream().map(o
                -> CellData.OutputData.of(
                CellData.OutputData.OutputType.of(o.output_type()),
                o.data())
                ).toList() : new ArrayList<>()
                );
            case NotebookCompat.MarkdownCell m ->
                CellData.of(
                CellData.Type.of(m.cell_type()),
                Arrays.stream(m.source()).collect(Collectors.joining()),
                m.id() == null ? UUID.randomUUID() : UUID.fromString(m.id())
                );
        };
    }

    public static Sheet of() {
        var s = new Sheet(null);
        s.addCell(CellData.of(CellData.Type.CODE, null, UUID.randomUUID()));
        return s;
    }

    private Sheet(File file) {
        notebookFile = file;
        uuid = UUID.randomUUID();
        reactiveJShellProperty.set(ReactiveJShellProvider.createReactiveShell(uuid, null !=notebookFile ? notebookFile.getParentFile().toPath() : null));
    }

    public void addCell(CellData cell) {
        cells.add(cell);
    }

    public ObservableList<CellData> getCells() {
        return cells;
    }

    public void execute() {
        worker.execute(() -> ((SheetSkin) getSkin()).execute());
    }

    public void resetAndExecute() {
        worker.execute(() -> {
            getReactiveJShell().shutdown();
            reactiveJShellProperty.set(ReactiveJShellProvider.createReactiveShell(uuid, null !=notebookFile ? notebookFile.getParentFile().toPath() : null));
            execute();
        });
    }

    public void moveFocusToNextCell(Cell currentCell) {
        currentCell.markAsSelected(false);
        ((SheetSkin) getSkin()).moveFocusToNextCell(currentCell);
    }

    public void moveFocusToFirstCell() {
        ((SheetSkin) getSkin()).moveFocusToFirstCell();
    }

    public void moveFocusToLastCell() {
        ((SheetSkin) getSkin()).moveFocusToLastCell();
    }

    public void moveFocusToCell(Cell cell) {
        cell.requestFocus();
    }

    public ReactiveJShell getReactiveJShell() {
        return reactiveJShellProperty.get();
    }

    public ReadOnlyObjectProperty<ReactiveJShell> reactiveJShellProperty() {
        return reactiveJShellProperty;
    }

    public void ensureCellVisible(Node node) {
        ((SheetSkin) getSkin()).scrollTo(node);
    }

    public void insertCellAfter(Sheet.Cell currentCell) {
        ((SheetSkin) getSkin()).insertCellAfter(currentCell);
    }

    public void insertCellBefore(Sheet.Cell currentCell) {
        ((SheetSkin) getSkin()).insertCellBefore(currentCell);
    }

    public void removeCell(Sheet.Cell currentCell) {
        ((SheetSkin) getSkin()).removeCell(currentCell);
    }

    public void moveCellUp(Sheet.Cell currentCell) {
        ((SheetSkin) getSkin()).moveCellUp(currentCell);
    }

    public void moveCellDown(Sheet.Cell currentCell) {
        ((SheetSkin) getSkin()).moveCellDown(currentCell);
    }

    public void replaceCell(Sheet.Cell currentCell, Sheet.Cell newCell) {
        ((SheetSkin) getSkin()).replaceCell(currentCell, newCell);
    }

    public Cell createCell(CellData.Type type, CellData cellData) {
        return ((SheetSkin) getSkin()).createCell(type, cellData);
    }

    public Notebook toNotebook() {
        var nbCells = cells.stream()
                .filter(c -> !c.isEmpty())
                .map(this::convertToNotebookCell)
                .toList();
        return new Notebook(
                Map.of("kernel_info",
                        Map.of("name", "JTaccuino", "version", "0.1"),
                        "language_info",
                        Map.of("name", "Java", "version", System.getProperty("java.specification.version"))), 4, 5, nbCells);
    }

    private Notebook.Cell convertToNotebookCell(CellData cellData) {
        return switch (cellData.getType()) {
            case CODE ->
                new Notebook.CodeCell(
                cellData.getId().toString(),
                cellData.getType().name().toLowerCase(),
                Map.of(),
                cellData.getSource(),
                cellData.getOutputData().stream().map(od -> new Notebook.Output(od.type().toOutputType(), od.mimeBundle(), Map.of())).toList(),
                0);
            case MARKDOWN ->
                new Notebook.MarkdownCell(
                cellData.getId().toString(),
                cellData.getType().name().toLowerCase(),
                Map.of(),
                cellData.getSource());
        };
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getNextId() {
        return ++counter;
    }

    public <T> void executeAsync(Supplier<T> callable, Consumer<T> consumer) {
        var future = new SheetCompletableFuture<T>(worker);
        future.completeAsync(callable).thenAccept(consumer);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SheetSkin(this);
    }

    public void close() {
        worker.shutdown();
        getReactiveJShell().shutdown();
    }

    public void markCellsAsOutdated(Predicate<Cell> isCellOutdated) {
        ((SheetSkin) getSkin()).markCellsAsOutdated(isCellOutdated);
    }

    public static abstract class Cell extends Control {

        private final ObjectProperty<CellData> cellDataProperty = new SimpleObjectProperty<>();
        private final Sheet sheet;

        public Cell(CellData cellData, Sheet sheet) {
            this.sheet = sheet;
            cellDataProperty.set(cellData);
        }

        public CellData getCellData() {
            return cellDataProperty.get();
        }

        public ReadOnlyObjectProperty<CellData> cellDataProperty() {
            return cellDataProperty;
        }

        public Sheet getSheet() {
            return this.sheet;
        }

        public abstract void execute();

        public void markAsOutdated(boolean isOutdated) {
        }

        public void markAsSelected(boolean isSelected) {
        }
    }

    private static class SheetCompletableFuture<T> extends CompletableFuture<T> {

        Executor executor;

        public SheetCompletableFuture(Executor executor) {
            this.executor = executor;
        }

        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return new SheetCompletableFuture<>(executor);
        }

        @Override
        public Executor defaultExecutor() {
            return executor;
        }
    }
}
