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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import org.jtaccuino.core.ui.api.CellData;
import org.jtaccuino.core.ui.api.Notebook;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.ReactiveJShellProvider;

public class Sheet extends Control {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("SheetWorker").factory());

    private final SimpleObjectProperty<ReactiveJShell> reactiveJShellProperty = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Cell> activeCellProperty = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Integer> activeCellNumberProperty = new SimpleObjectProperty<>();
    private final UUID uuid;
    private int counter = 0;
    private final Notebook notebook;

    public static Sheet of(Notebook notebook) {
        return new Sheet(notebook);
    }

    private Sheet(Notebook notebook) {
        this.uuid = UUID.randomUUID();
        this.notebook = notebook;
        reactiveJShellProperty.set(ReactiveJShellProvider.createReactiveShell(uuid, null != this.notebook.getFile() ? this.notebook.getFile().getParentFile().toPath() : null));
        activeCellProperty.subscribe(c -> {
            if (c != null) {
                activeCellNumberProperty.set(c.cellNumber);
            }
        });
    }

    public Notebook getNotebook() {
        return this.notebook;
    }

    public void addCell(CellData cell) {
        getCells().add(cell);
    }

    public ObservableList<CellData> getCells() {
        return notebook.getCells();
    }

    public void execute() {
        worker.execute(() -> ((SheetSkin) getSkin()).execute());
    }

    public void resetAndExecute() {
        worker.execute(() -> {
            getReactiveJShell().shutdown();
            reactiveJShellProperty.set(ReactiveJShellProvider.createReactiveShell(uuid, null != notebook ? notebook.getFile().getParentFile().toPath() : null));
            execute();
        });
    }

    public ReadOnlyObjectProperty<Cell> activeCellProperty() {
        return activeCellProperty;
    }

    public Cell getActiveCell() {
        return activeCellProperty.get();
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
        activeCellProperty.set(cell);
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
        private final SimpleBooleanProperty isSelectedProperty = new SimpleBooleanProperty();
        private final SimpleBooleanProperty isOutdatedProperty = new SimpleBooleanProperty();
        private final Sheet sheet;
        protected final int cellNumber;

        public Cell(CellData cellData, Sheet sheet, int cellNumber) {
            this.sheet = sheet;
            this.cellNumber = cellNumber;
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

        public ReadOnlyBooleanProperty isOutdatedProperty() {
            return isOutdatedProperty;
        }

        public void markAsOutdated(boolean isOutdated) {
            isOutdatedProperty.set(isOutdated);
        }

        public ReadOnlyBooleanProperty isSelectedProperty() {
            return isSelectedProperty;
        }

        public void markAsSelected(boolean isSelected) {
            isSelectedProperty.set(isSelected);
            sheet.activeCellProperty.set(this);
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
