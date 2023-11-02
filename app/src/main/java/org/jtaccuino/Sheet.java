package org.jtaccuino;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import org.jtaccuino.jshell.ReactiveJShell;

public class Sheet extends Control {

    private SimpleObjectProperty<ReactiveJShell> reactiveJShellProperty = new SimpleObjectProperty<>();
    private final UUID uuid;
    private int counter = 0;

    private final ObservableList<CellData> cells = FXCollections.observableArrayList();

    public static Sheet of(Notebook notebook) {
        var s = new Sheet();
        notebook.cells().stream().map(c -> CellData.of(c.cell_type(), c.source())).forEach(s::addCell);
        return s;
    }

    public static Sheet of() {
        var s = new Sheet();
        s.addCell(new CellData("java", null));
        return s;
    }

    private Sheet() {
        uuid = UUID.randomUUID();
        reactiveJShellProperty.set(ReactiveJShell.create());
        initShell();
    }

    public void addCell(CellData cell) {
        cells.add(cell);
    }

    public ObservableList<CellData> getCells() {
        return cells;
    }
    
    public void execute() {
        ((SheetSkin) getSkin()).execute();
    }
    
    public void resetAndExecute() {
        getReactiveJShell().shudown();
        reactiveJShellProperty.set(ReactiveJShell.create());
        initShell();
        execute();
    }

    public void moveFocusToNextCell(Cell currentCell) {
        ((SheetSkin) getSkin()).moveFocusToNextCell(currentCell);
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

    public Notebook toNotebook() {
        var nbCells = cells.stream().map(c -> new Notebook.Cell(c.getType(), Map.of(), c.getSource(), List.of())).toList();
        return new Notebook(Map.of(), 4, 4, nbCells);
    }

    private void initShell() {
        getReactiveJShell().eval("""
            import org.jtaccuino.ShellUtil;
            """);
        getReactiveJShell().eval("""
            public void display(javafx.scene.Node node) {
                System.out.println("Should now display " + node + " on " + jsci_uuid);
                ShellUtil.INSTANCE.display(node, jsci_uuid);
            }""");

        getReactiveJShell().eval("""
            public void addDependency(String mavenCoordinate) {
                ShellUtil.INSTANCE.resolve(mavenCoordinate, jsci_uuid);
            }""");
        getReactiveJShell().eval("""
            import java.util.UUID;
            """);
        getReactiveJShell().eval("var jsci_uuid = UUID.fromString(\"" + uuid + "\");");
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getNextId() {
        return ++counter;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SheetSkin(this);
    }

    void close() {
        getReactiveJShell().shudown();
    }

    public static class CellData {

        private final SimpleStringProperty type = new SimpleStringProperty();
        private final SimpleStringProperty source = new SimpleStringProperty();

        public static CellData of(String type, String source) {
            return new CellData(type, source);
        }

        public static CellData empty() {
            return new CellData(null, null);
        }

        public static CellData empty(String type) {
            return new CellData(type, null);
        }

        private CellData(String type, String source) {
            this.type.set(type);
            this.source.set(source);
        }

        public String getType() {
            return type.get();
        }

        public String getSource() {
            return source.get();
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public SimpleStringProperty sourceProperty() {
            return source;
        }

    }

    public static abstract class Cell extends Control {

        private ObjectProperty<CellData> cellDataProperty = new SimpleObjectProperty<>();

        public Cell(CellData cellData) {
            cellDataProperty.set(cellData);
        }

        public CellData getCellData() {
            return cellDataProperty.get();
        }

        public ReadOnlyObjectProperty<CellData> cellDataProperty() {
            return cellDataProperty;
        }

        public abstract void requestFocus();
        
        public abstract void execute();
    }

}
