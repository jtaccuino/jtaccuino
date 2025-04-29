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

import java.util.Arrays;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import org.jtaccuino.core.ui.extensions.NodeRenderer;
import org.jtaccuino.core.ui.extensions.NodeRenderer.Descriptor;

@Descriptor(type = Object[].class)
public class ObjectArrayRenderer implements NodeRenderer<Object[]> {

    @Override
    public Optional<Node> render(Object[] objects) {
        return Optional.of(convertArrayToNode(objects));
    }

    protected Node convertArrayToNode(Object[] objects) {
        if (objects.length > 0 && null != objects[0] && objects[0].getClass().isRecord()) {
            TableView<Object> tv = RenderHelper.recordsToTable(objects);
            return tv;
        }
        var observableList = FXCollections.observableList(Arrays.stream(objects).map(String::valueOf).toList());
        var lv = new ListView<String>(observableList);
        lv.setMaxHeight(RenderHelper.CELL_MAX_HEIGHT);
        return lv;
    }
}
