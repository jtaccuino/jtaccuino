/*
 * Copyright 2025-2026 JTaccuino Contributors
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

import java.util.Optional;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.util.Subscription;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;

/**
 * A markdown cell that can show either its source or its rendered form.
 *
 * <p>The rendered form is an incubator {@link RichTextArea} configured with
 * {@code useContentHeight}, so it reports a preferred height derived from its
 * own content. Nothing here measures control internals: replacing the model
 * invalidates the layout and the owning cell sizes itself again.
 */
public final class MarkdownControl extends InputControl {

    private RichTextArea mdRenderArea;
    private final SimpleBooleanProperty mdRenderAreaFocusedProperty = new SimpleBooleanProperty();

    private Subscription renderedViewSubscription = null;

    public MarkdownControl(int cellNumber) {
        super(cellNumber, Type.MARKDOWN);
    }

    public ReadOnlyBooleanProperty mdRenderAreaFocused() {
        return mdRenderAreaFocusedProperty;
    }

    public Optional<RichTextArea> getMdRenderArea() {
        return Optional.ofNullable(mdRenderArea);
    }

    @Override
    public void requestFocus() {
        if (mdRenderArea != null) {
            mdRenderArea.requestFocus();
        } else {
            getInput().requestFocus();
        }
    }

    /**
     * The view state is tracked by the presence of the render area: the source
     * editor is only removed from the children, so its visible flag would still
     * report true.
     */
    public boolean isRendered() {
        return mdRenderArea != null;
    }

    public void switchToRenderedView(SimpleViewOnlyStyledModel model) {
        if (mdRenderArea != null) {
            updateRenderedView(model);
            return;
        }

        var renderArea = new RichTextArea(model);
        renderArea.getStyleClass().add("markdown-render");
        renderArea.setEditable(false);
        renderArea.setWrapText(true);
        // Stretch to the control width, exactly like the source editor does,
        // so the rendered cell spans the full notebook width instead of
        // collapsing to the width of its longest line.
        AnchorPane.setLeftAnchor(renderArea, 0d);
        AnchorPane.setRightAnchor(renderArea, 0d);
        // Let the control size itself from its content instead of anyone
        // measuring it from the outside.
        renderArea.setUseContentHeight(true);
        renderArea.setPrefHeight(Region.USE_COMPUTED_SIZE);

        mdRenderArea = renderArea;

        getInput().setVisible(false);
        getInput().setManaged(false);
        getChildren().remove(getInput());
        getChildren().add(renderArea);

        mdRenderAreaFocusedProperty.bind(renderArea.focusedProperty());

        // Shift-click is the established way back into the source.
        var shiftClick = new javafx.event.EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 1 && event.isShiftDown()) {
                    switchToSourceView();
                    event.consume();
                }
            }
        };
        renderArea.addEventFilter(MouseEvent.MOUSE_CLICKED, shiftClick);

        renderedViewSubscription = () -> renderArea.removeEventFilter(MouseEvent.MOUSE_CLICKED, shiftClick);
    }

    public void updateRenderedView(SimpleViewOnlyStyledModel model) {
        if (mdRenderArea != null) {
            mdRenderArea.setModel(model);
            // Replacing the model does not invalidate the layout by itself, and
            // the preferred height is derived from the content during layout.
            mdRenderArea.requestLayout();
        }
    }

    public void switchToSourceView() {
        var renderArea = mdRenderArea;
        if (renderArea == null) {
            return;
        }
        if (renderedViewSubscription != null) {
            renderedViewSubscription.unsubscribe();
            renderedViewSubscription = null;
        }
        mdRenderAreaFocusedProperty.unbind();
        mdRenderAreaFocusedProperty.set(false);

        getChildren().remove(renderArea);
        mdRenderArea = null;
        getInput().setManaged(true);
        getChildren().add(getInput());
        // Makes the visible toggle fire the editor focus listener.
        getInput().setVisible(true);
    }
}
