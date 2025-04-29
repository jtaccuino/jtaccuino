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

import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.jtaccuino.core.ui.extensions.NodeRenderer;
import org.jtaccuino.core.ui.extensions.NodeRenderer.Descriptor;

@Descriptor(type = Object.class)
public class ObjectRenderer implements NodeRenderer<Object> {

    @Override
    public Optional<Node> render(Object object) {
        var label = new Label("No automatic conversion for type " + object.getClass() + " to javafx.scene.Node found!");
        return Optional.of(label);
    }
}
