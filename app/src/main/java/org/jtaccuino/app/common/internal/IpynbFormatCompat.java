/*
 * Copyright 2024-2025 JTaccuino Contributors
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
package org.jtaccuino.app.common.internal;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jtaccuino.core.ui.api.CellData;

public record IpynbFormatCompat(Map<String, Object> metadata, int nbformat, int nbformat_minor, List<Cell> cells) implements IpynbFormatOperations {

    public static record CodeCell(String id, String cell_type, Map<String, Object> metadata, String[] source, List<Output> outputs, int execution_count) implements Cell {
    }

    public static record MarkdownCell(String id, String cell_type, Map<String, Object> metadata, String[] source) implements Cell {
    }

    public static record Output(String output_type, Map<String, String> data, Map<String, Object> metadata) {
    }

    @JsonbTypeDeserializer(CellDeserializer.class)
    public static sealed interface Cell permits CodeCell, MarkdownCell {

        public Map<String, Object> metadata();

        public String cell_type();

        public String id();

        public String[] source();
    }

    public static class CellDeserializer implements JsonbDeserializer<Cell> {

        @Override
        public Cell deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            var o = parser.getObject();
            var type = o.getString("cell_type");
            return switch (type) {
                case "code" ->
                    ctx.deserialize(CodeCell.class, parser);
                case "markdown" ->
                    ctx.deserialize(MarkdownCell.class, parser);
                default ->
                    throw new IllegalStateException("Unsupported cell type found: " + type);
            };
        }
    }

    public List<CellData> toCellDataList() {
        return cells().stream()
                .map(IpynbFormatCompat::convertFromNotebookCell)
                .toList();
    }

    private static final CellData convertFromNotebookCell(Cell cell) {
        return switch (cell) {
            case CodeCell c ->
                CellData.of(
                CellData.Type.of(c.cell_type()),
                Arrays.stream(c.source()).collect(Collectors.joining()),
                c.id() == null ? UUID.randomUUID() : UUID.fromString(c.id()),
                c.outputs() != null ? c.outputs().stream().map(o
                -> CellData.OutputData.of(
                CellData.OutputData.OutputType.of(o.output_type()),
                o.data())
                ).toList() : new ArrayList<>()
                );
            case MarkdownCell m ->
                CellData.of(
                CellData.Type.of(m.cell_type()),
                Arrays.stream(m.source()).collect(Collectors.joining()),
                m.id() == null ? UUID.randomUUID() : UUID.fromString(m.id())
                );
        };
    }
}
