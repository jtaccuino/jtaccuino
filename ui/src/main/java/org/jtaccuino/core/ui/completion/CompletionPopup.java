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
package org.jtaccuino.core.ui.completion;

import java.util.List;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.stage.Window;

public class CompletionPopup extends PopupControl {

    private final ObservableList<CompletionItem> completionSuggestions = FXCollections.observableArrayList();
    private IntegerProperty visibleCompletions = new SimpleIntegerProperty(this, "visibleCompletions", 10);

    public static class CompletionEvent extends Event {

        @java.io.Serial
        static final long serialVersionUID = -1L;

        public static final EventType<CompletionEvent> COMPLETION
                = new EventType<>("Completion" + UUID.randomUUID().toString()); //$NON-NLS-1$

        private final String suggestion;
        private final int anchor;

        public CompletionEvent(String suggestion, int anchor) {
            super(COMPLETION);
            this.suggestion = suggestion;
            this.anchor = anchor;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public int getAnchor() {
            return anchor;
        }
    }

    @SuppressWarnings("this-escape")
    public CompletionPopup() {
        setAutoFix(true);
        setAutoHide(true);
        setHideOnEscape(true);
    }

    public ObservableList<CompletionItem> getSuggestions() {
        return completionSuggestions;
    }

    public void setSuggestions(List<CompletionItem> items) {
        var matchList = items.stream().filter(CompletionItem::matchesType).toList();
        var noMatchlist = items.stream().filter(CompletionItem::notMatchesType).toList();
        boolean requiresSep = !matchList.isEmpty() && !noMatchlist.isEmpty();
        var newList = combine(
                FXCollections.<CompletionItem>observableArrayList(matchList),
                requiresSep ? FXCollections.<CompletionItem>observableArrayList(CompletionItem.NIL) : FXCollections.<CompletionItem>emptyObservableList(),
                FXCollections.<CompletionItem>observableArrayList(noMatchlist));
        Platform.runLater(() -> completionSuggestions.setAll(newList));
    }

    @SafeVarargs
    private ObservableList<CompletionItem> combine(ObservableList<CompletionItem> ... lists) {
        @SuppressWarnings("varargs")
        var combinedList = FXCollections.concat(lists);
        return combinedList;
    }

    public void updateLocation(Point2D caretOrigin) {
        var owner = getOwnerNode();
        var parent = owner.getScene().getWindow();
        var localToScene = owner.localToScene(caretOrigin);
        setX(parent.getX() + localToScene.getX() + owner.getScene().getX());
        setY(parent.getY() + localToScene.getY() + owner.getScene().getY());
    }

    public void show(Node node, Point2D caretOrigin) {
        var parent = node.getScene().getWindow();
        show(node, caretOrigin, parent);
    }

    private void show(Node node, Point2D caretOrigin, Window parent) {
        var localToScene = node.localToScene(caretOrigin);
        this.show(
                node,
                parent.getX() + localToScene.getX() + node.getScene().getX(),
                parent.getY() + localToScene.getY() + node.getScene().getY());
        this.getOwnerNode();
    }

    public final void setVisibleCompletions(int value) {
        visibleCompletions.set(value);
    }

    public final int getVisibleCompletions() {
        return visibleCompletions.get();
    }

    public final IntegerProperty visibleCompletionsProperty() {
        return visibleCompletions;
    }

    public final ObjectProperty<EventHandler<CompletionEvent>> onCompletionProperty() {
        return onCompletion;
    }

    public final void setOnCompletion(EventHandler<CompletionEvent> value) {
        onCompletionProperty().set(value);
    }

    public final EventHandler<CompletionEvent> getOnCompletion() {
        return onCompletionProperty().get();
    }
    private ObjectProperty<EventHandler<CompletionEvent>> onCompletion = new ObjectPropertyBase<EventHandler<CompletionEvent>>() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        protected void invalidated() {
            setEventHandler(CompletionEvent.COMPLETION, (EventHandler<CompletionEvent>) (Object) get());
        }

        @Override
        public Object getBean() {
            return CompletionPopup.this;
        }

        @Override
        public String getName() {
            return "onSuggestion"; //$NON-NLS-1$
        }
    };

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CompletionPopupSkin(this);
    }
}
