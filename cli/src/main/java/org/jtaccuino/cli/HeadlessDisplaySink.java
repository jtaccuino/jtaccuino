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
package org.jtaccuino.cli;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.jtaccuino.notebook.DisplayResult;
import org.jtaccuino.notebook.DisplaySink;

/**
 * Gives a displayed node a place to be laid out so it can be snapshotted, with
 * nothing ever shown.
 *
 * <p>{@code display} attaches the node through the sink and then immediately
 * rasterises it. A node only lays out once it belongs to a scene, so unlike the
 * IDE's sink, which adds it to the cell's output box, this one adds it to a
 * scene of its own. The scene is never put on a stage; under the headless Glass
 * platform it exists purely to drive layout.
 *
 * <p>A markdown result has no node and is not snapshotted, so there is nothing
 * to do for it: the text is already stored on the cell.
 *
 * <p>Called on the JavaFX application thread, by contract of {@link DisplaySink}.
 */
final class HeadlessDisplaySink implements DisplaySink {

    private static final double SCENE_WIDTH = 1024;
    private static final double SCENE_HEIGHT = 768;

    private final VBox root = new VBox();
    @SuppressWarnings("UnusedVariable") // keeps the scene, and therefore the layout, alive
    private final Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);

    @Override
    public void accept(DisplayResult result) {
        if (result instanceof DisplayResult.Graphical graphical) {
            root.getChildren().setAll(graphical.node());
            root.applyCss();
            root.layout();
        }
    }
}
