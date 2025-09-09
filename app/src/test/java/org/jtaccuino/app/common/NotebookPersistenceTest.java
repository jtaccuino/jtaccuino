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
package org.jtaccuino.app.common;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jtaccuino.app.common.internal.IpynbFormat;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({"try", "unchecked"})
public class NotebookPersistenceTest {

    @TempDir
    private File tempDir;

    @ParameterizedTest
    @MethodSource("exampleNotebookFiles")
    void canNotebookPersistenceReadWriteFile(File notebookFile) {
        // first read the notebook
        NotebookImpl nb = NotebookPersistence.INSTANCE.of(notebookFile.toURI());
        System.out.println("Loaded notebook: " + nb);

        // then write the notebook
        nb.saveAs(new File(tempDir, "writtenNotebook.ipynb"));
    }

    @ParameterizedTest
    @MethodSource("exampleNotebookFiles")
    void canIpynbFormatReadFileAsIs(File notebookFile) throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            var ipynb = jsonb.fromJson(new FileReader(notebookFile, StandardCharsets.UTF_8), IpynbFormat.class);
            System.out.println("ipynb: " + ipynb);
            ipynb.toCellDataList().forEach(System.out::println);
        }
    }

    @ParameterizedTest
    @MethodSource("exampleNotebookFiles")
    void canIpynbFormatReadFileConvertedToCompat(File notebookFile) throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String modifiedJson = modifyJsonObject(
                    Files.readString(notebookFile.toPath()),
                    jsonb,
                    NotebookPersistenceTest::toCompatJson);

            var ipynb = jsonb.fromJson(modifiedJson, IpynbFormat.class);
            System.out.println("ipynb: " + ipynb);
            ipynb.toCellDataList().forEach(System.out::println);
        }
    }

    @Test
    void parseFailureIpynbFormatCellSource() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String modifiedJson = modifyJsonObject(
                    Files.readString(locateAllExampleNotebookFiles().get(0).toPath()),
                    jsonb,
                    // change source of the first cell to type Integer
                    m -> ((List<Map<String, Object>>) m.get("cells")).get(0).put("source", 42));

            JsonbException jsonbEx = assertThrows(JsonbException.class, () -> jsonb.fromJson(modifiedJson, IpynbFormat.class));
            Assertions.assertInstanceOf(IllegalStateException.class, jsonbEx.getCause());
            Assertions.assertEquals("Unsupported conversion to String from 42", jsonbEx.getCause().getMessage());
        }
    }

    @Test
    void parseFailureIpynbFormatCellType() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String modifiedJson = modifyJsonObject(
                    Files.readString(locateAllExampleNotebookFiles().get(0).toPath()),
                    jsonb,
                    // change source of the first cell to type Integer
                    m -> ((List<Map<String, Object>>) m.get("cells")).get(0).put("cell_type", "Willie Wonka"));

            JsonbException jsonbEx = assertThrows(JsonbException.class, () -> jsonb.fromJson(modifiedJson, IpynbFormat.class));
            Assertions.assertInstanceOf(IllegalStateException.class, jsonbEx.getCause());
            Assertions.assertEquals("Unsupported cell type found: Willie Wonka", jsonbEx.getCause().getMessage());
        }
    }

    private static void toCompatJson(Map<String, Object> modifiableJson) {
        List<Map<String, Object>> cells = (List<Map<String, Object>>) modifiableJson.get("cells");

        // ensure source is String[] for IpynbFormatCompat
        cells.forEach(cell -> cell.computeIfPresent(
                "source",
                (k, v) -> v instanceof String mlString ? mlString.split("\n") : v));
    }

    private static String modifyJsonObject(String jsonDocument, Jsonb jsonb, Consumer<Map<String, Object>> modifier) throws JsonbException {
        // parse jsonDocument
        Map<String, Object> modifiableJson = jsonb.fromJson(jsonDocument, Map.class);

        // modify JSON
        modifier.accept(modifiableJson);

        // serialize JSON and return
        return jsonb.toJson(modifiableJson);
    }

    private static List<Arguments> exampleNotebookFiles() throws IOException {
        return locateAllExampleNotebookFiles().stream()
                .map(Arguments::of)
                .toList();
    }

    private static List<File> locateAllExampleNotebookFiles() throws IOException {
        try (Stream<Path> s = Files.walk(Paths.get("..", "examples"))) {
            return s.map(Path::toFile)
                    .filter(File::isFile)
                    .filter(f -> f.getName().endsWith(".ipynb"))
                    .sorted()
                    .toList();
        }
    }
}
