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
package org.jtaccuino.core.ui.renderer;

import java.awt.image.BufferedImage;
import java.util.Optional;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import org.jtaccuino.core.ui.extensions.NodeRenderer;
import org.jtaccuino.core.ui.extensions.NodeRenderer.Descriptor;

@Descriptor(type = BufferedImage.class)
public class BufferedImageRenderer implements NodeRenderer<BufferedImage> {

    // Todo: Replace with preferenee / options
    private final static double CELL_MAX_HEIGHT = 200;

    @Override
    public Optional<Node> render(BufferedImage bi) {
        var iv = new ImageView();
        var fxi = SwingFXUtils.toFXImage(bi, null);
        iv.setImage(fxi);
        iv.setPreserveRatio(true);
        iv.setFitHeight(CELL_MAX_HEIGHT);
        return Optional.of(iv);
    }
}
