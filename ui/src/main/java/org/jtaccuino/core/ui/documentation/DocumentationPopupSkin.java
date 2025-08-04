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
package org.jtaccuino.core.ui.documentation;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class DocumentationPopupSkin implements Skin<DocumentationPopup> {

    private final DocumentationPopup control;
    private final ListView<DocumentationItem> listView;

    private final double documentationItemCharWidth;
    private final double documentationItemCharHeight;
    private final DocumentationRenderer documentationRenderer;

    @SuppressWarnings("this-escape")
    public DocumentationPopupSkin(DocumentationPopup control) {
        this.control = control;
        this.listView = new ListView<>(this.control.getDocumentations());
        this.documentationRenderer = new DocumentationRenderer();
        this.listView.setCellFactory(this.documentationRenderer);

        Text tempText = new Text("Q");
        tempText.setFont(Font.font("Monaspace Argon", 11));
        Bounds charBounds = tempText.getLayoutBounds();
        documentationItemCharWidth = charBounds.getWidth();
        documentationItemCharHeight = charBounds.getHeight();

        control.minWidthProperty().bind(listView.widthProperty());
        control.prefWidthProperty().bind(listView.widthProperty());
        control.maxWidthProperty().bind(listView.widthProperty());

        control.minHeightProperty().bind(listView.heightProperty());
        control.prefHeightProperty().bind(listView.heightProperty());
        control.maxHeightProperty().bind(listView.heightProperty());

        listView.minHeightProperty().bind(
                Bindings.min(control.visibleDocumentationsProperty(), Bindings.size(listView.getItems()))
                        .multiply(Math.ceil(documentationItemCharHeight * (1 + 0.25 + 0.25) * 1.2)).add(2));
        listView.prefHeightProperty().bind(
                Bindings.min(control.visibleDocumentationsProperty(), Bindings.size(listView.getItems()))
                        .multiply(Math.ceil(documentationItemCharHeight * (1 + 0.25 + 0.25) * 1.2)).add(2));
        listView.maxHeightProperty().bind(
                Bindings.min(control.visibleDocumentationsProperty(), Bindings.size(listView.getItems()))
                        .multiply(Math.ceil(documentationItemCharHeight * (1 + 0.25 + 0.25) * 1.2)).add(2));

        double width = Math.min(calculateListViewPreferredWidth(true), 800);

        listView.prefWidthProperty().set(width);
        listView.maxWidthProperty().set(width);
        listView.minWidthProperty().set(width);

        listView.getItems().addListener((ListChangeListener.Change<? extends DocumentationItem> change) -> {
            change.next();
            if (0 == change.getTo()) {
                control.hide();
            } else {
                listView.getSelectionModel().selectFirst();
                calculateTotalChars();
                updatePopupSize();
            }
        });

        listView.setOnKeyPressed(ke -> {
            switch (ke.getCode()) {
                case ESCAPE -> {
                    if (control.isHideOnEscape()) {
                        control.hide();
                    }
                    ke.consume();
                }
                default -> {}
            }
        });
        Platform.runLater(() -> {
            updatePopupSize();
            listView.requestFocus();
        });
    }

    private int calculateTotalChars() {
        int longestSignature = control.getDocumentations()
                .stream().mapToInt(di -> di.itemString().length()).max().orElse(0);
        int longestReturnType = control.getDocumentations()
                .stream().mapToInt(di -> di.returnType().length()).max().orElse(0);
        documentationRenderer.setLineLength(longestReturnType + longestSignature);
        return longestReturnType + longestSignature;
    }

    private void updatePopupSize() {
        double completionListWidth = Math.min(calculateListViewPreferredWidth(true), 800);
        listView.setMinWidth(completionListWidth);
        listView.setPrefWidth(completionListWidth);
        listView.setMaxWidth(completionListWidth);
    }

    private double calculateListViewPreferredWidth(boolean needsScrollBar) {
        if (control.getDocumentations().isEmpty()) {
            return Region.USE_PREF_SIZE;
        }

        double maxWidth = (calculateTotalChars() +1) * documentationItemCharWidth * 1.05;

        double cellHorizontalPadding = 10;

        Insets listViewPadding = this.listView.getPadding();
        double totalListViewPadding = listViewPadding.getLeft() + listViewPadding.getRight();

        double verticalScrollbarAllowance = 20;

        return maxWidth + cellHorizontalPadding + totalListViewPadding + (needsScrollBar ? verticalScrollbarAllowance : 5);
    }

    @Override
    public DocumentationPopup getSkinnable() {
        return this.control;
    }

    @Override
    public Node getNode() {
        return listView;
    }

    @Override
    public void dispose() {
    }
}
