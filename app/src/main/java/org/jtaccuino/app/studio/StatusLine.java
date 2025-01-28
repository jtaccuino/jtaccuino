/*
 * Copyright 2025 JTaccuino Contributors
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
package org.jtaccuino.app.studio;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.jtaccuino.core.ui.Sheet;
import org.jtaccuino.core.ui.api.SheetManager;

public class StatusLine {

    private static final StatusLine INSTANCE = new StatusLine();

    private Node node = null;
    private final Label statusLabel = new Label();

    private StatusLine() {
        // empty private instance generation
    }

    /**
     * Get the default instance of the status line
     *
     * @return instance of status line
     */
    public static StatusLine getDefault() {
        return INSTANCE;
    }

    Label getStatusLabel() {
        return statusLabel;
    }

    public Node getNode() {
        if (null == node) {
            node = initNode();
        }
        return node;
    }

    private Node initNode() {
        var statusLine = new HBox();
        statusLine.setSpacing(5);
        statusLine.setStyle("-fx-padding:2 10 2 10");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusLine.getChildren().addAll(
                getStatusLabel(),
                spacer,
                new Separator(Orientation.VERTICAL),
                rowColNode(),
                new Separator(Orientation.VERTICAL),
                cellTracker()
        );
        return statusLine;
    }

    private Node cellTracker() {
        var cellLabel = new Label();
        final ChangeListener<Sheet.Cell> l = (o,t,u) -> {
            if (null != u) {
                var numberOfCells = u.getSheet().getCells().size();
                var activeCell = u.getSheet().getCellIndex(u);
                cellLabel.setText("Cell " + activeCell + " of " + numberOfCells);
            } else {
                cellLabel.setText("No active cell");
            }
        };

        SheetManager.getDefault().activeSheet().subscribe((t, u) -> {
            if (null != t) {
                t.activeCellProperty().removeListener(l);
            }
            if (null != u) {
                u.activeCellProperty().addListener(l);
                l.changed(u.activeCellProperty(), null, u.getActiveCell());
            }
        });

        return cellLabel;
    }

    private Node rowColNode() {
        var rowColLabel = new Label();
        final ChangeListener<Point2D> colRowListener = (o,t,u) -> {
            if (null != u) {
                var col = (int) u.getX();
                var row = (int) u.getY();
                rowColLabel.setText(row + " : " + col);
            } else {
                rowColLabel.setText("No active cell");
            }
        };

        final ChangeListener<Sheet.Cell> l = (o,t,u) -> {
            if (null != t) {
                t.caretRowColumnProperty().removeListener(colRowListener);
            }
            if (null != u) {
                u.caretRowColumnProperty().addListener(colRowListener);
                colRowListener.changed(u.caretRowColumnProperty(), null, u.caretRowColumnProperty().get());
            }
        };

        SheetManager.getDefault().activeSheet().subscribe((t, u) -> {
            if (null != t) {
                t.activeCellProperty().removeListener(l);
            }
            if (null != u) {
                u.activeCellProperty().addListener(l);
                l.changed(u.activeCellProperty(), null, u.getActiveCell());
            }
        });

        return rowColLabel;
    }
}
