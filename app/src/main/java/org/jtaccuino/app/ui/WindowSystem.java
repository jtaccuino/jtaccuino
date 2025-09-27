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
package org.jtaccuino.app.ui;

import java.net.URL;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jtaccuino.app.studio.StatusLine;
import org.jtaccuino.app.studio.WindowManager;

public class WindowSystem {

    private static WindowSystem INSTANCE = new WindowSystem();

    private static final URL PRESENTATION_MODE_STYLES = WindowSystem.class.getResource("/presentationMode.css");

    private final TabbedSheetNotebookUI tabbedSheetNotebookUI;
    private BorderPane bp;
    private final VBox metaBox;

    private WindowSystem() {
        tabbedSheetNotebookUI = new TabbedSheetNotebookUI();
        bp = new BorderPane();
        bp.setCenter(tabbedSheetNotebookUI.getNode());
        metaBox = new VBox(
                MenuBar.createMainMenuBar(),
                ToolBar.getMainToolBar());
        bp.setTop(metaBox);
        bp.setBottom(StatusLine.getDefault().getNode());
    }

    public static WindowSystem getDefault() {
        return INSTANCE;
    }

    public Parent getNode() {
        return bp;
    }

    public void enableToolBar(boolean enable) {
    }

    public void shutdown() {
        tabbedSheetNotebookUI.shutdown();
    }

    public void togglePresentationMode(boolean enable) {
        if (enable) {
            ((Stage) WindowManager.getDefault().getMainWindow()).setFullScreenExitHint("");
            ((Stage) WindowManager.getDefault().getMainWindow()).setFullScreen(true);
            ((Stage) WindowManager.getDefault().getMainWindow()).getScene().getStylesheets().add(PRESENTATION_MODE_STYLES.toExternalForm());
            metaBox.getChildren().remove(ToolBar.getMainToolBar());
        } else {
            metaBox.getChildren().add(ToolBar.getMainToolBar());
            ((Stage) WindowManager.getDefault().getMainWindow()).setFullScreen(false);
            ((Stage) WindowManager.getDefault().getMainWindow()).setFullScreenExitHint(null);
            ((Stage) WindowManager.getDefault().getMainWindow()).getScene().getStylesheets().remove(PRESENTATION_MODE_STYLES.toExternalForm());
        }
    }
}
