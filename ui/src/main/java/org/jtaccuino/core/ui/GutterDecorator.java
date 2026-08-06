/*
 * Copyright 2026 JTaccuino Contributors
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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.LineNumberDecorator;
import jfx.incubator.scene.control.richtext.SideDecorator;

class GutterDecorator implements SideDecorator {

    enum MarkerKind {
        ERROR(0),
        WARNING(1),
        INFO(2),
        HINT(3),
        QUICK_FIX(4);

        private final int priority;

        MarkerKind(int priority) {
            this.priority = priority;
        }

        int priority() {
            return priority;
        }

        String styleClass() {
            return "gutter-marker-" + name().toLowerCase(Locale.ROOT);
        }
    }

    record GutterMarker(MarkerKind kind, String message, Runnable action) {
    }

    static final int DEFAULT_MAX_ICONS_PER_LINE = 2;
    private static final int ICON_SIZE = 10;
    private static final int ICON_SPACING = 2;
    private static final Comparator<GutterMarker> PRIORITY = Comparator.comparingInt(m -> m.kind().priority());

    private final LineNumberDecorator numbers = new LineNumberDecorator();
    private final ReadOnlyObjectWrapper<Map<Integer, List<GutterMarker>>> markers =
            new ReadOnlyObjectWrapper<>(Map.of());
    private final CodeArea area;
    private int maxIconsPerLine = DEFAULT_MAX_ICONS_PER_LINE;

    GutterDecorator(CodeArea area) {
        this.area = area;
    }

    void setMarkers(Map<Integer, List<GutterMarker>> markers) {
        this.markers.set(Map.copyOf(markers));
    }

    ReadOnlyObjectProperty<Map<Integer, List<GutterMarker>>> markersProperty() {
        return markers.getReadOnlyProperty();
    }

    void setMaxIconsPerLine(int maxIconsPerLine) {
        this.maxIconsPerLine = maxIconsPerLine;
        setMarkers(markers.get());
    }

    @Override
    public double getPrefWidth(double height) {
        var numbersWidth = numbers.getPrefWidth(height);
        var worst = 0;
        for (var lineMarkers : markers.get().values()) {
            var icons = Math.min(lineMarkers.size(), maxIconsPerLine);
            if (lineMarkers.size() > maxIconsPerLine) {
                icons++;
            }
            worst = Math.max(worst, icons);
        }
        if (worst == 0) {
            return numbersWidth;
        }
        var iconsWidth = worst * ICON_SIZE + (worst - 1) * ICON_SPACING;
        return Math.max(numbersWidth, iconsWidth);
    }

    @Override
    public Node getMeasurementNode(int index) {
        return numbers.getMeasurementNode(index);
    }

    @Override
    public Node getNode(int index) {
        var number = new Label(Integer.toString(index + 1));
        number.getStyleClass().add("line-number-decorator");
        number.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        number.setMinHeight(1);
        number.setPrefHeight(1);
        number.setAlignment(Pos.CENTER_RIGHT);
        number.fontProperty().bind(area.fontProperty());

        var icons = new HBox(ICON_SPACING);
        icons.setAlignment(Pos.CENTER_RIGHT);
        icons.getStyleClass().add("gutter-marker-row");

        var cell = new HBox();
        cell.setAlignment(Pos.CENTER_RIGHT);
        cell.getChildren().addAll(number, icons);

        Runnable update = () -> updateNode(index, number, icons);
        markers.addListener((obs, oldValue, newValue) -> update.run());
        update.run();
        return cell;
    }

    private void updateNode(int index, Label number, HBox icons) {
        var lineMarkers = markers.get().getOrDefault(index, List.of()).stream()
                .sorted(PRIORITY)
                .toList();
        number.setVisible(lineMarkers.isEmpty());
        icons.getChildren().clear();
        if (lineMarkers.isEmpty()) {
            return;
        }
        for (var marker : lineMarkers.subList(0, Math.min(lineMarkers.size(), maxIconsPerLine))) {
            icons.getChildren().add(markerIcon(marker));
        }
        if (lineMarkers.size() > maxIconsPerLine) {
            icons.getChildren().add(moreIcon(lineMarkers.subList(maxIconsPerLine, lineMarkers.size())));
        }
    }

    private Node markerIcon(GutterMarker marker) {
        var icon = new Region();
        icon.getStyleClass().addAll("gutter-marker", marker.kind().styleClass());
        icon.setMinSize(ICON_SIZE, ICON_SIZE);
        icon.setPrefSize(ICON_SIZE, ICON_SIZE);
        icon.setMaxSize(ICON_SIZE, ICON_SIZE);
        var tooltip = new Tooltip(marker.message());
        tooltip.setAutoHide(true);
        Tooltip.install(icon, tooltip);
        if (null != marker.action()) {
            icon.setCursor(Cursor.HAND);
            icon.setOnMouseClicked(event -> marker.action().run());
        }
        return icon;
    }

    private Node moreIcon(List<GutterMarker> hidden) {
        var label = new Label("+" + hidden.size());
        label.getStyleClass().add("gutter-marker-more");
        var tooltip = new Tooltip();
        tooltip.setAutoHide(true);
        tooltip.setText(hidden.stream().map(GutterMarker::message).collect(Collectors.joining("\n")));
        Tooltip.install(label, tooltip);
        return label;
    }
}
