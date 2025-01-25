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
package org.jtaccuino.core.ui.controls;

import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.model.Document;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

public final class MarkdownControl extends InputControl {

    private final static double RTA_LINE_HEIGHT = 25;
    private ListView<?> paragraphListView;
    private Group group;

    private final double padding = 0;
    private final double MIN_WIDTH = 100;
    private double inputPadding = 0;

    private RichTextArea mdRenderArea;
    private final SimpleBooleanProperty mdRenderAreaFocusedProperty = new SimpleBooleanProperty();

    public MarkdownControl(int cellNumber) {
        super(cellNumber, Type.MARKDOWN);
    }

    public ReadOnlyBooleanProperty mdRenderAreaFocused() {
        return mdRenderAreaFocusedProperty;
    }

    public Optional<RichTextArea>  getMdRenderArea() {
        return Optional.of(mdRenderArea);
    }

    @Override
    public void requestFocus() {
        if (getInput().isVisible()) {
            getInput().requestFocus();
        } else {
            getMdRenderArea().ifPresent(rta -> rta.requestFocus());
        }
    }

    private void recalculatePreviewRTA(double width) {
        mdRenderArea.setPrefWidth(width - 2);
        double textAreaHeight = computePreviewRTAPrefHeight(mdRenderArea.getWidth()) + inputPadding;
        mdRenderArea.setMinHeight(textAreaHeight);
        mdRenderArea.setPrefHeight(textAreaHeight);
        mdRenderArea.setMaxHeight(textAreaHeight);
        mdRenderArea.requestLayout();

        double newHeight = mdRenderArea.getHeight();
        setMinHeight(newHeight);
        setPrefHeight(newHeight);
        setMaxHeight(newHeight);
        requestLayout();
    }

    private double computePreviewRTAPrefHeight(double width) {
        if (group == null) {
            group = (Group) this.lookup(".sheet");
            paragraphListView = (ListView<?>) this.lookup(".paragraph-list-view");
        }
        if (null != group) {
            double averageCellHeight = group.getChildren().stream()
                    .filter(ListCell.class::isInstance)
                    .map(ListCell.class::cast)
                    .filter(cell -> cell.getGraphic() != null)
                    .mapToDouble(n -> n.prefHeight(width))
                    .average()
                    .orElse(RTA_LINE_HEIGHT);
            return Math.max(RTA_LINE_HEIGHT,
                    averageCellHeight * paragraphListView.getItems().size() + 2);
        }
        return RTA_LINE_HEIGHT;
    }

    public void switchToRenderedView(Document doc) {
        mdRenderArea = new RichTextArea();

        // external padding + 2 pixels from border width
        inputPadding = mdRenderArea.getPadding().getTop() + mdRenderArea.getPadding().getBottom() + 2;
        mdRenderArea.setTranslateX(padding);
        mdRenderArea.setTranslateY(padding);

//        rta.prefWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
//        rta.maxWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));
//        rta.contentAreaWidthProperty().bind(Bindings.subtract(this.control.parentBox.widthProperty(), 20));

        mdRenderArea.documentProperty().subscribe(d
                -> Platform.runLater(()
                -> recalculatePreviewRTA(Math.max(MIN_WIDTH, getWidth()))));

        mdRenderArea.setEditable(false);
        mdRenderArea.addEventFilter(MouseEvent.MOUSE_CLICKED, t -> {
            if (1 == t.getClickCount() & t.isShiftDown()) {
                getChildren().remove(mdRenderArea);
                getInput().setVisible(true);
                // getChildren().add(0, getInput());
                double newHeight = getInput().heightProperty().doubleValue() + 2 * padding;
                setMinHeight(newHeight);
                setPrefHeight(newHeight);
                setMaxHeight(newHeight);
                mdRenderArea = null;
                t.consume();
            }
        });
        mdRenderArea.heightProperty().subscribe((h0, h) -> {
            double newHeight = h.doubleValue() + 2 * padding;
            setMinHeight(newHeight);
            setPrefHeight(newHeight);
            setMaxHeight(newHeight);
            requestLayout();
        });

        //getChildren().remove(getInput());

        getChildren().add(mdRenderArea);
        getInput().setVisible(false);
        mdRenderArea.documentProperty().subscribe((ov, nv) -> {
            if (nv != null) {
                recalculatePreviewRTA(Math.max(MIN_WIDTH, getWidth()));
            }
        });
        mdRenderArea.getActionFactory().open(doc).execute(new ActionEvent());
        mdRenderAreaFocusedProperty.unbind();
        mdRenderAreaFocusedProperty.bind(mdRenderArea.focusedProperty());
    }
}
