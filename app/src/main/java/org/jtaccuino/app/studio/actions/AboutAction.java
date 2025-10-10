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

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.jtaccuino.app.Studio;
import org.jtaccuino.core.ui.api.AbstractAction;

public final class AboutAction extends AbstractAction {

    public static final AboutAction INSTANCE = new AboutAction();

    private AboutAction() {
        super("help/about",
                "About JTaccuino",
                "");
    }

    private Label createVersionLabel(String versionName, String version) {
        var label = new Label(versionName + ":\t" + version);
        label.getStyleClass().add("about-version");
        return label;
    }

    @Override
    public void handle(ActionEvent t) {
        var aboutDialog = new Dialog<>();
        aboutDialog.setTitle("About JTaccuino");
        aboutDialog.initModality(Modality.APPLICATION_MODAL);
        aboutDialog.getDialogPane().getStyleClass().add("aboutDialog");

        // Load the icon
        var appIcon = new Image(getClass().getResourceAsStream("/notebook-svgrepo-com_256.png"));
        ImageView imageView = new ImageView(appIcon);
        imageView.setFitHeight(64);
        imageView.setFitWidth(64);

        // Create labels for the information
        var titleLabel = new Label("JTaccuino - The Java™ Notebook");
        titleLabel.getStyleClass().add("about-title");

        VBox infoBox = new VBox(5);
        infoBox.setPadding(new Insets(10, 0, 0, 0));
        infoBox.getChildren().addAll(
                titleLabel,
                createVersionLabel("Version", "\t" + Studio.getVersion()),
                createVersionLabel("Java Version", "\t" +System.getProperty("java.specification.version")),
                createVersionLabel("JavaFX Version", "\t" + System.getProperty("javafx.runtime.version")),
                createVersionLabel("Java Runtime Version", System.getProperty("java.runtime.version"))
        );

        HBox contentBox = new HBox(15);
        contentBox.getChildren().addAll(imageView, infoBox);
        contentBox.setPadding(new Insets(10));

        // Add the content to the dialog pane
        aboutDialog.getDialogPane().setContent(contentBox);

        // Add a close button
        aboutDialog.getDialogPane().getButtonTypes().add(
                new javafx.scene.control.ButtonType("Close", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        aboutDialog.getDialogPane().getScene().getStylesheets().add(this.getClass().getResource("/jtaccuino.css").toExternalForm());

        aboutDialog.showAndWait();
    }
}
