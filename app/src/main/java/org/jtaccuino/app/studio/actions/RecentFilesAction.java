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
package org.jtaccuino.app.studio.actions;

import java.net.URI;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import org.jtaccuino.app.common.NotebookPersistence;
import org.jtaccuino.core.ui.api.AbstractAction;
import org.jtaccuino.core.ui.api.Action;
import org.jtaccuino.core.ui.api.DynamicAction;
import org.jtaccuino.core.ui.api.SheetManager;

public class RecentFilesAction extends AbstractAction implements DynamicAction {

    public static final RecentFilesAction INSTANCE = new RecentFilesAction();

    private static class RecentFileAction extends AbstractAction {

        private final URI uri;

        private RecentFileAction(String displayText, URI uri, boolean mnemonicEnabled) {
            super("file/recent/" + displayText, displayText, mnemonicEnabled ? "Meta+Shift+T" : "");
            this.uri = uri;
        }

        @Override
        public void handle(ActionEvent event) {
            SheetManager.getDefault().open(NotebookPersistence.INSTANCE.of(uri));
        }
    }

    private ObservableList<Action> recentFileActions = FXCollections.observableArrayList();
    private ReadOnlyListWrapper<Action> unmodifiableRecentFileActions = new ReadOnlyListWrapper<>(recentFileActions);

    @SuppressWarnings("this-escape")
    public RecentFilesAction() {
        super("file/recent", "Recent Files", "");
        SheetManager.getDefault().getRecentFiles().addListener(new ListChangeListener<SheetManager.RecentFile>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends SheetManager.RecentFile> c) {
                Platform.runLater(() -> {
                    updateActions(c.getList());
                });
            }

        });

        enabled().bind(SheetManager.getDefault().getRecentFiles().emptyProperty().not());
        // ensure initial state
        updateActions(SheetManager.getDefault().getRecentFiles());
    }

    private void updateActions(List<? extends SheetManager.RecentFile> recentFiles) {
        recentFileActions.clear();
        boolean isFirstAction = true;
        for (var rf : recentFiles) {
            recentFileActions.add(new RecentFileAction(rf.displayName(), rf.uri(), isFirstAction));
            isFirstAction = false;
        }
    }

    @Override
    public void handle(ActionEvent event) {
        // do nothing here
    }

    @Override
    public SimpleListProperty<Action> actions() {
        return unmodifiableRecentFileActions;
    }
}
