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
package org.jtaccuino.core.ui.extensions;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.jtaccuino.notebook.DisplayResult;
import org.jtaccuino.notebook.DisplaySink;
import org.jtaccuino.notebook.PrintSink;

/**
 * Sinks that attach notebook output to the widgets of the cell that is
 * currently executing.
 *
 * <p>The extensions themselves live in the notebook module, so that a headless
 * export shares the accumulation and the persistence. All that is left for the
 * IDE is showing the result, which is what these two do.
 */
public final class GuiSinks {

    private GuiSinks() {
    }

    /** Shows {@code println} output in the cell's label. */
    public static PrintSink forLabel(Label label) {
        return label::setText;
    }

    /**
     * Adds displayed output to the cell's output box.
     *
     * <p>Called on the JavaFX application thread, synchronously: the extension
     * snapshots a graphical result as soon as this returns, so the node has to
     * be part of the scene graph by then. A markdown result is not snapshotted
     * at all, so it only has to end up in the box.
     */
    public static DisplaySink forOutputBox(VBox outputBox) {
        return result -> {
            switch (result) {
                case DisplayResult.Graphical graphical -> outputBox.getChildren().add(graphical.node());
                case DisplayResult.Markdown markdown ->
                        outputBox.getChildren().add(new MarkdownOutputNode(markdown.markdown()));
            }
        };
    }
}
