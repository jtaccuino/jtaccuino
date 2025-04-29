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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.jtaccuino.core.ui.extensions.NodeRenderer;
import org.jtaccuino.core.ui.extensions.NodeRenderer.Descriptor;

@Descriptor(type = Collection.class)
public class CollectionRenderer implements NodeRenderer<Collection<?>> {

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Node> render(Collection<?> collection) {
        if (!collection.isEmpty() && collection.iterator().next() instanceof Map.Entry<?, ?> me) {
            var entrySet = (Collection<Map.Entry<Object, Object>>) collection;
            var tv = new TableView<Map.Entry<Object, Object>>();
            var tcKey = new TableColumn<Map.Entry<Object, Object>, String>("Key");
            tcKey.setCellValueFactory(o -> {
                var valueProp = new SimpleStringProperty();
                if (null != o) {
                    valueProp.set(String.valueOf(o.getValue().getKey()));
                }
                return valueProp;
            });
            tv.getColumns().add(tcKey);
            if (me.getValue().getClass().isRecord()) {
                tv.getColumns().addAll(RenderHelper.tableColumnsFromRecordComponentsForMapValues(me.getValue().getClass()));
            } else {
                var tcValue = new TableColumn<Map.Entry<Object, Object>, String>("Value");
                tcValue.setCellValueFactory(o -> {
                    var valueProp = new SimpleStringProperty();
                    if (null != o) {
                        valueProp.set(String.valueOf(o.getValue().getValue()));
                    }
                    return valueProp;
                });
                tv.getColumns().add(tcValue);
            }
            tv.getItems().addAll(entrySet);
            tv.getColumns().forEach(c -> c.setPrefWidth(RenderHelper.COLUMN_PREF_WIDTH));
            tv.setMaxHeight(RenderHelper.CELL_MAX_HEIGHT);
            return Optional.of(tv);
        } else if(!collection.isEmpty() && collection.iterator().next().getClass().isRecord()) {
            TableView<Object> tv = RenderHelper.recordsToTable(collection);
            return Optional.of(tv);
        } else {
            var observableList = FXCollections.observableList(collection.stream().map(String::valueOf).toList());
            var lv = new ListView<String>(observableList);
            lv.setMaxHeight(RenderHelper.CELL_MAX_HEIGHT);
            return Optional.of(lv);
        }
    }
}
