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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class RenderHelper {

    // Todo: Replace with preferenee / options
    public final static double CELL_MAX_HEIGHT = 200;
    public final static double COLUMN_PREF_WIDTH = 300;

    public static List<TableColumn<Object, String>> tableColumnsFromRecordComponents(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(rc -> {
                    var tc = new TableColumn<Object, String>(rc.getName());
                    tc.setCellValueFactory(o -> {
                        var valueProp = new SimpleStringProperty();
                        if (null != o) {
                            try {
                                var value = rc.getAccessor().invoke(o.getValue());
                                valueProp.set(String.valueOf(value));
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            } catch (InvocationTargetException ite) {
                                ite.getTargetException().printStackTrace();
                            }
                        }
                        return valueProp;
                    });
                    return tc;
                }).toList();
    }

    public static List<TableColumn<Map.Entry<Object, Object>, String>> tableColumnsFromRecordComponentsForMapValues(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(rc -> {
                    var tc = new TableColumn<Map.Entry<Object, Object>, String>(rc.getName());
                    tc.setCellValueFactory(o -> {
                        var valueProp = new SimpleStringProperty();
                        if (null != o) {
                            try {
                                var value = rc.getAccessor().invoke(o.getValue().getValue());
                                valueProp.set(String.valueOf(value));
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            } catch (InvocationTargetException ite) {
                                ite.getTargetException().printStackTrace();
                            }
                        }
                        return valueProp;
                    });
                    return tc;
                }).toList();
    }

    public static <T> Node convertArrayToListView(T[] a) {
        var observableList = FXCollections.observableList(Arrays.stream(a).map(String::valueOf).toList());
        var lv = new ListView<String>(observableList);
        lv.setMaxHeight(CELL_MAX_HEIGHT);
        return lv;
    }

    public static TableView<Object> recordsToTable(Object[] objects) {
        return recordsToTable(objects[0].getClass(), (tv) -> tv.getItems().addAll(objects));
    }

    public static TableView<Object> recordsToTable(Collection<?> collection) {
        return recordsToTable(collection.iterator().next().getClass(), (tv) -> tv.getItems().addAll(collection));
    }

    private  static TableView<Object> recordsToTable(Class<?> recordClass, Consumer<TableView<Object>> fillTable) {
        var tv = new TableView<Object>();
        tv.getColumns().addAll(RenderHelper.tableColumnsFromRecordComponents(recordClass));
        fillTable.accept(tv);
        tv.getColumns().forEach(c -> c.setPrefWidth(RenderHelper.COLUMN_PREF_WIDTH));
        tv.setMaxHeight(RenderHelper.CELL_MAX_HEIGHT);
        return tv;
    }
}
