/*
 * Copyright 2024 JTaccuino Contributors
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
package org.jtaccuino;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CellData {

    public static enum Type {
        CODE, MARKDOWN;

        public static Type of(String cell_type) {
            return switch (cell_type) {
                case "markdown" ->
                    MARKDOWN;
                case "code" ->
                    CODE;
                default ->
                    CODE;
            };
        }
    }

    private final SimpleObjectProperty<Type> type = new SimpleObjectProperty<>();
    private final SimpleStringProperty source = new SimpleStringProperty();
    private final UUID id;
    private final ObservableList<OutputData> outputData = FXCollections.observableArrayList();

    public static CellData of(Type type, String source, UUID uuid) {
        return new CellData(type, source, uuid, Collections.emptyList());
    }

    public static CellData of(Type type, String source, UUID uuid, List<OutputData> outputData) {
        return new CellData(type, source, uuid, outputData);
    }

    public static CellData of(Type type, String source) {
        return new CellData(type, source, UUID.randomUUID(), Collections.emptyList());
    }

    public static CellData empty() {
        return new CellData(null, null, UUID.randomUUID(), Collections.emptyList());
    }

    public static CellData empty(Type type) {
        return new CellData(type, null, UUID.randomUUID(), Collections.emptyList());
    }

    private CellData(Type type, String source, UUID uuid, List<OutputData> outputData) {
        this.type.set(type);
        this.source.set(source);
        this.id = uuid;
        this.outputData.setAll(outputData);
    }

    public Type getType() {
        return type.get();
    }

    public String getSource() {
        return source.get();
    }

    public SimpleObjectProperty<Type> typeProperty() {
        return type;
    }

    public SimpleStringProperty sourceProperty() {
        return source;
    }

    public boolean isEmpty() {
        return (null == source.get()) || source.get().isEmpty();
    }

    public UUID getId() {
        return id;
    }

    public ObservableList<OutputData> getOutputData() {
        return outputData;
    }

    public static record OutputData(OutputType type, Map<String, String> mimeBundle) {

        public static enum MimeType {
            TEXT_PLAIN("text/plain"),
            IMAGE_PNG("image/png");

            private final String mimeType;

            public String toMimeType() {
                return mimeType;
            }

            MimeType(String mimeType) {
                this.mimeType = mimeType;
            }

            public static MimeType of(String mimeType) {
                return switch (mimeType) {
                    case "text/plain" ->
                        TEXT_PLAIN;
                    case "image/png" ->
                        IMAGE_PNG;
                    default ->
                        TEXT_PLAIN;
                };
            }
        }

        public static enum OutputType {
            EXECUTION_DATA("execute_result"),
            DISPLAY_DATA("display_data");

            private final String outputType;

            public String toOutputType() {
                return outputType;
            }

            OutputType(String outputType) {
                this.outputType = outputType;
            }

            public static OutputType of(String outputType) {
                return switch (outputType) {
                    case "display_data" ->
                        DISPLAY_DATA;
                    case "execution_data" ->
                        EXECUTION_DATA;
                    default ->
                        DISPLAY_DATA;
                };
            }
        }

        public static OutputData of(OutputType outputType, Map<String, String> mimeBundle) {
            return new OutputData(outputType, mimeBundle);
        }

    }

}
