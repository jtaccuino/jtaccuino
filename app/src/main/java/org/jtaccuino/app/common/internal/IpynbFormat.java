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

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jtaccuino.core.ui.api.CellData;

public record IpynbFormat(Map<String, Object> metadata, int nbformat, int nbformat_minor, List<Cell> cells) implements IpynbFormatOperations {

    public static record CodeCell(String id, String cell_type, Map<String, Object> metadata, String source, List<Output> outputs, int execution_count) implements Cell {

        @Override
        public CellData toCellData() {
            return CellData.of(
                    CellData.Type.of(cell_type()),
                    source(),
                    Optional.ofNullable(id()).map(UUID::fromString).orElseGet(UUID::randomUUID),
                    Objects.requireNonNullElse(outputs(), List.<Output>of()).stream()
                            .map(Output::toOutputData)
                            .toList()
            );
        }
    }

    public static record MarkdownCell(String id, String cell_type, Map<String, Object> metadata, String source) implements Cell {

        @Override
        public CellData toCellData() {
            return CellData.of(
                    CellData.Type.of(cell_type()),
                    source(),
                    Optional.ofNullable(id()).map(UUID::fromString).orElseGet(UUID::randomUUID)
            );
        }
    }

    public static record ExecuteResultOutput(String output_type, Map<String, String> data, Map<String, Object> metadata, Integer execution_count) implements Output {

        @SuppressWarnings("unchecked")
        private static ExecuteResultOutput from(JsonObject jsonObject) {
            Integer executionCount = Optional.ofNullable(jsonObject.getJsonNumber("execution_count"))
                    .map(JsonNumber::intValueExact)
                    .orElse(null);

            return new ExecuteResultOutput(
                    jsonObject.getString("output_type"),
                    extractMimeBundle(jsonObject.getJsonObject("data")),
                    (Map<String, Object>) extractJsonData(jsonObject.getJsonObject("metadata")),
                    executionCount);
        }

        @Override
        public CellData.OutputData toOutputData() {
            return CellData.OutputData.of(
                    CellData.OutputData.OutputType.of(output_type()),
                    data());
        }
    }

    public static record DisplayDataOutput(String output_type, Map<String, String> data, Map<String, Object> metadata) implements Output {

        @SuppressWarnings("unchecked")
        private static DisplayDataOutput from(JsonObject jsonObject) {
            return new DisplayDataOutput(
                    jsonObject.getString("output_type"),
                    extractMimeBundle(jsonObject.getJsonObject("data")),
                    (Map<String, Object>) extractJsonData(jsonObject.getJsonObject("metadata")));
        }

        @Override
        public CellData.OutputData toOutputData() {
            return CellData.OutputData.of(
                    CellData.OutputData.OutputType.of(output_type()),
                    data());
        }
    }

    public static record StreamOutput(String output_type, String name, String text) implements Output {

        private static StreamOutput from(JsonObject jsonObject) {
            return new StreamOutput(
                    jsonObject.getString("output_type"),
                    jsonObject.getString("name"),
                    multilineOrArrayToString(jsonObject.get("text")));
        }

        @Override
        public CellData.OutputData toOutputData() {
            return CellData.OutputData.of(
                    CellData.OutputData.OutputType.of(output_type()),
                    text);
        }
    }

    public static record ErrorOutput(String output_type, String ename, String evalue, List<String> traceback) implements Output {

        private static ErrorOutput from(JsonObject jsonObject) {
            return new ErrorOutput(
                    jsonObject.getString("output_type"),
                    jsonObject.getString("ename"),
                    jsonObject.getString("evalue"),
                    jsonObject.getJsonArray("traceback").stream()
                            .map(JsonString.class::cast).map(JsonString::getString).toList());
        }

        @Override
        public CellData.OutputData toOutputData() {
            return CellData.OutputData.of(
                    CellData.OutputData.OutputType.of(output_type()),
                    Map.of());
        }
    }

    public static sealed interface Output permits ExecuteResultOutput, DisplayDataOutput, StreamOutput, ErrorOutput {

        String output_type();

        CellData.OutputData toOutputData();

        static Output from(CellData.OutputData outputData) {
            return switch (outputData) {
                case CellData.MimeTypeBasedOutputData od when CellData.OutputData.OutputType.DISPLAY_DATA == od.type() ->
                    new DisplayDataOutput(outputData.type().toOutputType(), od.mimeBundle(), Map.of());
                case CellData.MimeTypeBasedOutputData od when CellData.OutputData.OutputType.EXECUTION_DATA == od.type() ->
                    new ExecuteResultOutput(outputData.type().toOutputType(), od.mimeBundle(), Map.of(), null);
                default -> null;
            };
        }
    }

    @JsonbTypeDeserializer(CellDeserializer.class)
    public static sealed interface Cell permits CodeCell, MarkdownCell {

        Map<String, Object> metadata();

        String cell_type();

        String id();

        String source();

        CellData toCellData();
    }

    public static class CellDeserializer implements JsonbDeserializer<Cell> {

        @SuppressWarnings("unchecked")
        @Override
        public Cell deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            var o = parser.getObject();
            var type = o.getString("cell_type");
            var id = o.containsKey("id")
                    // Mandatory since nbformat v4.5. Did not exist prior to that
                    ? o.getString("id")
                    : UUID.randomUUID().toString();
            var source = multilineOrArrayToString(o.get("source"));
            var metadata = (Map<String, Object>) extractJsonData(o.getJsonObject("metadata"));
            return switch (type) {
                case "code" -> {
                    var outputs = o.getJsonArray("outputs").stream()
                            .map(JsonValue::asJsonObject)
                            .map(this::deserializeOutput)
                            .toList();
                    yield new CodeCell(id, type, metadata, source, outputs, 0);
                }
                case "markdown" ->
                    new MarkdownCell(id, type, metadata, source);
                default ->
                    throw new IllegalStateException("Unsupported cell type found: " + type);
            };
        }

        private Output deserializeOutput(JsonObject jo) {
            String outputType = jo.getString("output_type");
            return switch (outputType) {
                case "execute_result" ->
                    ExecuteResultOutput.from(jo);
                case "display_data" ->
                    DisplayDataOutput.from(jo);
                case "stream" ->
                    StreamOutput.from(jo);
                case "error" ->
                    ErrorOutput.from(jo);
                default ->
                    throw new IllegalStateException("Unsupported output type found: " + outputType);
            };
        }
    }

    @Override
    public List<CellData> toCellDataList() {
        return cells().stream()
                .map(Cell::toCellData)
                .toList();
    }

    private static String multilineOrArrayToString(JsonValue jsonValue) {
        return switch (jsonValue) {
            case JsonString js ->
                js.getString();
            case JsonArray ja ->
                ja.stream().map(IpynbFormat::multilineOrArrayToString).collect(Collectors.joining());
            default ->
                throw new IllegalStateException("Unsupported conversion to String from " + jsonValue);
        };
    }

    private static Object extractJsonData(JsonValue jsonValue) {
        return switch (jsonValue) {
            case JsonString js ->
                js.getString();
            case JsonArray ja ->
                ja.stream().map(IpynbFormat::extractJsonData).toList();
            case JsonObject jo ->
                jo.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> extractJsonData(e.getValue())));
            case JsonNumber jn ->
                jn.numberValue();
            default ->
                throw new IllegalStateException("Unsupported value extraction from " + jsonValue);
        };
    }

    private static Map<String,String> extractMimeBundle(JsonObject jsonObject) {
        return jsonObject.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> multilineOrArrayToString(e.getValue())));
    }
}
