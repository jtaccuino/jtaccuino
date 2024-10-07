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

import javafx.css.PseudoClass;
import javafx.scene.control.Skin;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import static org.jtaccuino.core.ui.CellData.Type.CODE;
import static org.jtaccuino.core.ui.CellData.Type.MARKDOWN;
import static org.jtaccuino.core.ui.UiUtils.createSVGToggleToolbarButton;
import static org.jtaccuino.core.ui.UiUtils.createSVGToolbarButton;

public interface CellFactory {

    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet);

    public abstract static class AbstractCellSkin<T extends Sheet.Cell> implements Skin<T> {
        protected static final PseudoClass OUTDATED = PseudoClass.getPseudoClass("outdated");
        protected static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

        private final Sheet sheet;

        protected AbstractCellSkin(T sheetCell) {
            this.sheet = sheetCell.getSheet();
        }

        protected HBox createToolbar() {
            var executeCell = createSVGToolbarButton("execute-cell", "Execute Cell", "toolbar-button");
            executeCell.setOnAction((event) -> {
                execute();
            });
            var moveCellUp = createSVGToolbarButton("move-cell-up", "Move Cell Up", "toolbar-button");
            moveCellUp.setOnAction((event) -> {
                this.sheet.moveCellUp(getSkinnable());
            });
            var moveCellDown = createSVGToolbarButton("move-cell-down", "Move Cell Down", "toolbar-button");
            moveCellDown.setOnAction((event) -> {
                this.sheet.moveCellDown(getSkinnable());
            });

            var insertCellBefore = createSVGToolbarButton("insert-cell-before", "Insert Cell Before", "toolbar-button");
            insertCellBefore.setOnAction((event) -> {
                this.sheet.insertCellBefore(getSkinnable());
            });
            var insertCellAfter = createSVGToolbarButton("insert-cell-after", "Insert Cell After", "toolbar-button");
            insertCellAfter.setOnAction((event) -> {
                this.sheet.insertCellAfter(getSkinnable());
            });
            var deleteCell = createSVGToolbarButton("delete-cell", "Delete Cell", "toolbar-button");
            deleteCell.setOnAction((event) -> {
                this.sheet.removeCell(getSkinnable());
            });

            var mdType = createSVGToggleToolbarButton("md-cell-type", "Use Markdown for Cell", "toolbar-button");
            mdType.setOnAction((event) -> {
                this.sheet.replaceCell(getSkinnable(), sheet.createCell(CellData.Type.MARKDOWN, getSkinnable().getCellData()));
            });
            var javaType = createSVGToggleToolbarButton("java-cell-type", "Use Java Code for Cell", "toolbar-button");
            javaType.setOnAction((event) -> {
                this.sheet.replaceCell(getSkinnable(), sheet.createCell(CellData.Type.CODE, getSkinnable().getCellData()));
            });
            final ToggleGroup toggleGroup = new ToggleGroup();

            mdType.setToggleGroup(toggleGroup);
            javaType.setToggleGroup(toggleGroup);
            switch (getSkinnable().getCellData().getType()) {
                case CODE ->
                    toggleGroup.selectToggle(javaType);
                case MARKDOWN ->
                    toggleGroup.selectToggle(mdType);
            }

            var hbox = new HBox(executeCell, moveCellUp, moveCellDown, insertCellBefore, insertCellAfter, deleteCell, mdType, javaType);
            HBox.setHgrow(hbox, Priority.NEVER);
            hbox.maxWidthProperty().bind(hbox.prefWidthProperty());
            hbox.getStyleClass().add("cell-toolbar");
            return hbox;
        }

        protected abstract void execute();
    }
}
