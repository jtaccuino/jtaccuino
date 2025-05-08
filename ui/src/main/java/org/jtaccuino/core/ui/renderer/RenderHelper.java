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
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class RenderHelper {

    // Todo: Replace with preferenee / options
    public final static double CELL_MAX_HEIGHT = 200;
    public final static double COLUMN_PREF_WIDTH = 300;

    private RenderHelper() {
        // prevent instantiation
    }

    public static List<TableColumn<Object, String>> tableColumnsFromRecordComponents(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(rc -> {
                    var tc = new TableColumn<Object, String>(rc.getName());
                    tc.setCellValueFactory(stringValueOfCellValueFactory(rc, Function.identity()));
                    return tc;
                }).toList();
    }

    public static List<TableColumn<Map.Entry<Object, Object>, String>> tableColumnsFromRecordComponentsForMapValues(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(rc -> {
                    var tc = new TableColumn<Map.Entry<Object, Object>, String>(rc.getName());
                    tc.setCellValueFactory(stringValueOfCellValueFactory(rc, Map.Entry::getValue));
                    return tc;
                }).toList();
    }

    private static <T> Callback<TableColumn.CellDataFeatures<T, String>, ObservableValue<String>> stringValueOfCellValueFactory(
            final RecordComponent rc, Function<T, Object> valueExtractor) {
        return o -> {
            var valueProp = new SimpleStringProperty();
            if (null != o) {
                try {
                    var value = rc.getAccessor().invoke(valueExtractor.apply(o.getValue()));
                    valueProp.set(String.valueOf(value));
                } catch (IllegalAccessException iae) {
                    Logger.getLogger(RenderHelper.class.getName()).log(Level.SEVERE, "Failed to extract value", iae);
                } catch (InvocationTargetException ite) {
                    Logger.getLogger(RenderHelper.class.getName()).log(Level.SEVERE, "Failed to extract value", ite.getTargetException());
                }
            }
            return valueProp;
        };
    }

    public static <T> ListView<T> arrayToListView(T[] a) {
        return listToListViewImpl(FXCollections.observableArrayList(a));
    }

    public static <T, V> ListView<V> arrayToListView(T[] a, Function<T, V> valueConverter) {
        return collectionToListView(Arrays.stream(a).map(valueConverter).toList());
    }

    public static <T> ListView<T> collectionToListView(Collection<T> list) {
        return listToListViewImpl(FXCollections.observableArrayList(list));
    }

    public static <T, V> ListView<V> collectionToListView(Collection<T> list, Function<T, V> valueConverter) {
        return listToListViewImpl(FXCollections.observableArrayList(list.stream().map(valueConverter).toList()));
    }

    private static <T> ListView<T> listToListViewImpl(ObservableList<T> list) {
        var lv = new ListView<T>(list);
        lv.setMaxHeight(CELL_MAX_HEIGHT);
        return lv;
    }

    public static TableView<Object> recordsToTable(Object[] objects) {
        return recordsToTable(objects[0].getClass(), (tv) -> tv.getItems().addAll(objects));
    }

    public static TableView<Object> recordsToTable(Collection<?> collection) {
        return recordsToTable(collection.iterator().next().getClass(), (tv) -> tv.getItems().addAll(collection));
    }

    private static TableView<Object> recordsToTable(Class<?> recordClass, Consumer<TableView<Object>> fillTable) {
        var tv = new TableView<Object>();
        tv.getColumns().addAll(RenderHelper.tableColumnsFromRecordComponents(recordClass));
        fillTable.accept(tv);
        tv.getColumns().forEach(c -> c.setPrefWidth(RenderHelper.COLUMN_PREF_WIDTH));
        tv.setMaxHeight(RenderHelper.CELL_MAX_HEIGHT);
        return tv;
    }
}
