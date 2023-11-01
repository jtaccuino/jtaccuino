package org.jtaccuino;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import org.jtaccuino.jshell.ReactiveJShell;

public class Sheet extends Control {

    private ReactiveJShell reactiveJShell;
    private final UUID uuid;

    final ObservableList<Cell> cells = FXCollections.observableArrayList();

    public static Sheet of(Notebook notebook) {
        var s = new Sheet();
        notebook.cells().stream().map(c -> Cell.of(c.cell_type(), c.source())).forEach(s::addCell);
        return s;
    }

    public static Sheet of() {
        var s = new Sheet();
        s.addCell(new Cell("java", null));
        return s;
    }

    private Sheet() {
        uuid = initShell();
    }

    public void addCell(Cell cell) {
        cells.add(cell);
    }

    public ObservableList<Cell> getCells() {
        return cells;
    }

    public Notebook toNotebook() {
        var nbCells = cells.stream().map(c -> new Notebook.Cell(c.getType(), Map.of(), c.getSource(), List.of())).toList();
        return new Notebook(Map.of(), 4, 4, nbCells);
    }

    private UUID initShell() {
        reactiveJShell = ReactiveJShell.create();
        UUID uuid = UUID.randomUUID();
        reactiveJShell.eval("""
            import org.jtaccuino.ShellUtil;
            """);
        reactiveJShell.eval("""
            public void display(javafx.scene.Node node) {
                System.out.println("Should now display " + node + " on " + jsci_uuid);
                ShellUtil.INSTANCE.display(node, jsci_uuid);
            }""");

        reactiveJShell.eval("""
            public void addDependency(String mavenCoordinate) {
                ShellUtil.INSTANCE.resolve(mavenCoordinate, jsci_uuid);
            }""");
        reactiveJShell.eval("""
            import java.util.UUID;
            """);
        reactiveJShell.eval("var jsci_uuid = UUID.fromString(\"" + uuid + "\");");
        return uuid;
    }

    ReactiveJShell getReactiveJShell() {
        return reactiveJShell;
    }

    UUID getUuid() {
        return uuid;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SheetSkin(this);
    }

    void close() {
        reactiveJShell.shudown();
    }

    public static class Cell {

        private final SimpleStringProperty type = new SimpleStringProperty();
        private final SimpleStringProperty source = new SimpleStringProperty();

        public static Cell of(String type, String source) {
            return new Cell(type, source);
        }

        public static Cell empty() {
            return new Cell(null, null);
        }

        private Cell(String type, String source) {
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

}
