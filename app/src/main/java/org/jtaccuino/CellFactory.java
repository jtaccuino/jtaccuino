package org.jtaccuino;

import javafx.scene.layout.VBox;

public interface CellFactory {

    public Sheet.Cell createCell(Sheet.CellData cell, VBox parent, Sheet sheet);

}
