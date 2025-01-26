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

import java.util.Map;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Skin;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import static org.jtaccuino.core.ui.UiUtils.createSVGToggleToolbarButton;
import static org.jtaccuino.core.ui.UiUtils.createSVGToolbarButton;
import org.jtaccuino.core.ui.actions.ChangeCellToJavaAction;
import org.jtaccuino.core.ui.actions.ChangeCellToMarkdownAction;
import org.jtaccuino.core.ui.actions.DeleteCellAction;
import org.jtaccuino.core.ui.actions.ExecuteCellAction;
import org.jtaccuino.core.ui.actions.InsertCellAboveAction;
import org.jtaccuino.core.ui.actions.InsertCellBelowAction;
import org.jtaccuino.core.ui.actions.MoveCellDownAction;
import org.jtaccuino.core.ui.actions.MoveCellUpAction;
import org.jtaccuino.core.ui.api.CellData;

public interface CellFactory {

    public Sheet.Cell createCell(CellData cellData, VBox parent, Sheet sheet);

    public abstract static class AbstractCellSkin<T extends Sheet.Cell> implements Skin<T> {

        protected static final PseudoClass OUTDATED = PseudoClass.getPseudoClass("outdated");
        protected static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

        private static final Map<KeyCombination, EventHandler<ActionEvent>> DEFAULT_INPUT_MAP = Map.ofEntries(
                Map.entry(ExecuteCellAction.INSTANCE.getAccelerator(), ExecuteCellAction.INSTANCE),
                // not added here, needs entry into menu bar to work
                // if added here actions are called twice
//                Map.entry(MoveCellUpAction.INSTANCE.getAccelerator(), MoveCellUpAction.INSTANCE),
//                Map.entry(MoveCellDownAction.INSTANCE.getAccelerator(), MoveCellDownAction.INSTANCE),
//                Map.entry(InsertCellAboveAction.INSTANCE.getAccelerator(), InsertCellAboveAction.INSTANCE),
//                Map.entry(InsertCellBelowAction.INSTANCE.getAccelerator(), InsertCellBelowAction.INSTANCE),
                Map.entry(DeleteCellAction.INSTANCE.getAccelerator(), DeleteCellAction.INSTANCE)
//                Map.entry(ChangeCellToMarkdownAction.INSTANCE.getAccelerator(), ChangeCellToMarkdownAction.INSTANCE),
//                Map.entry(ChangeCellToJavaAction.INSTANCE.getAccelerator(), ChangeCellToJavaAction.INSTANCE)
        );

        private final KeyHandler keyHandler = new KeyHandler();

        private final Sheet sheet;

        protected AbstractCellSkin(T sheetCell) {
            this.sheet = sheetCell.getSheet();
        }

        protected HBox createToolbar() {
            var executeCell = createSVGToolbarButton("execute-cell", "toolbar-button", ExecuteCellAction.INSTANCE);
            var moveCellUp = createSVGToolbarButton("move-cell-up", "toolbar-button", MoveCellUpAction.INSTANCE);
            var moveCellDown = createSVGToolbarButton("move-cell-down", "toolbar-button", MoveCellDownAction.INSTANCE);
            var insertCellBefore = createSVGToolbarButton("insert-cell-before", "toolbar-button", InsertCellAboveAction.INSTANCE);
            var insertCellAfter = createSVGToolbarButton("insert-cell-after", "toolbar-button", InsertCellBelowAction.INSTANCE);
            var deleteCell = createSVGToolbarButton("delete-cell", "toolbar-button", DeleteCellAction.INSTANCE);
            var mdType = createSVGToggleToolbarButton("md-cell-type", "toolbar-button", ChangeCellToMarkdownAction.INSTANCE);
            var javaType = createSVGToggleToolbarButton("java-cell-type", "toolbar-button", ChangeCellToJavaAction.INSTANCE);
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

        protected void delegateKeyEvents(EventHandler<? super KeyEvent> handler) {
            keyHandler.setDelegatingKeyHandler(handler);
        }

        protected EventHandler<KeyEvent> getKeyHandler() {
            return keyHandler;
        }

        protected abstract void execute();

        private static class KeyHandler implements EventHandler<KeyEvent> {

            private EventHandler<? super KeyEvent> delegatingHandler;

            private KeyHandler() {
            }

            @Override
            public void handle(KeyEvent e) {
                // Find an applicable action and execute it if found
                for (KeyCombination kc : DEFAULT_INPUT_MAP.keySet()) {
                    if (kc.match(e)) {
                        ActionEvent ae = new ActionEvent(e.getSource(), e.getTarget());
                        DEFAULT_INPUT_MAP.get(kc).handle(ae);
                        e.consume();
                        return;
                    }
                }
                if (null != delegatingHandler) {
                    delegatingHandler.handle(e);
                }
            }

            void setDelegatingKeyHandler(EventHandler<? super KeyEvent> handler) {
                this.delegatingHandler = handler;
            }
        }
    }
}
