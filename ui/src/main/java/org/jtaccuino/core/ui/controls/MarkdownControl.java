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
import javafx.scene.input.MouseEvent;
import javafx.util.Subscription;

public final class MarkdownControl extends InputControl {

    private Group group;

    private final double padding = 0;
    private final double MIN_WIDTH = 100;
    private double inputPadding = 0;

    private RichTextArea mdRenderArea;
    private final SimpleBooleanProperty mdRenderAreaFocusedProperty = new SimpleBooleanProperty();

    private Subscription heightSubscription = null;
    private Subscription widthSubscription = null;

    public MarkdownControl(int cellNumber) {
        super(cellNumber, Type.MARKDOWN);
    }

    public ReadOnlyBooleanProperty mdRenderAreaFocused() {
        return mdRenderAreaFocusedProperty;
    }

    public Optional<RichTextArea> getMdRenderArea() {
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
        double textAreaHeight = mdRenderArea.getFullHeight() + inputPadding;
        mdRenderArea.setMinHeight(textAreaHeight);
        mdRenderArea.setPrefHeight(textAreaHeight);
        mdRenderArea.setMaxHeight(textAreaHeight);
        mdRenderArea.requestLayout();

        double newHeight = textAreaHeight + 2;
        setMinHeight(newHeight);
        setPrefHeight(newHeight);
        setMaxHeight(newHeight);
        requestLayout();
    }

    public void updateRenderedView(Document doc) {
        if (null != mdRenderArea) {
            mdRenderArea.getActionFactory().open(doc).execute(new ActionEvent());
        }
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
            if (1 == t.getClickCount() && t.isShiftDown()) {
                getChildren().remove(mdRenderArea);
                // getChildren().add(0, getInput());
                heightSubscription.unsubscribe();
                widthSubscription.unsubscribe();
                subscribeToInput();
                getInput().setManaged(true);
                getChildren().add(getInput());
                mdRenderArea = null;
                group = null;
                t.consume();
            }
        });
        unsubscribeFromInput();
        heightSubscription = mdRenderArea.fullHeightProperty().subscribe((h) -> recalculatePreviewRTA(getWidth()));
        widthSubscription = widthProperty().subscribe((w) -> recalculatePreviewRTA(w.doubleValue()));

        //getChildren().remove(getInput());
        getInput().setManaged(false);
        getChildren().remove(getInput());
        getChildren().add(mdRenderArea);
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
