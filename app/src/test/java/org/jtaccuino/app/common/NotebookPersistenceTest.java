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
import jakarta.json.bind.JsonbConfig;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jtaccuino.app.common.internal.IpynbFormat;
import org.jtaccuino.app.common.internal.IpynbFormatCompat;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NotebookPersistenceTest {

    @TempDir
    private File tempDir;

    @ParameterizedTest
    @MethodSource("exampleNotebookFiles")
    void canNotebookPersistenceReadWriteFile(File notebookFile) {
        // first read the notebook
        NotebookImpl nb = NotebookPersistence.INSTANCE.of(notebookFile);
        System.out.println("Loaded notebook: " + nb);

        // then write the notebook
        nb.saveToFile(new File(tempDir, "writtenNotebook.ipynb"));
    }

    @SuppressWarnings("try")
    @ParameterizedTest
    @MethodSource("exampleNotebookFiles")
    void canIpynbFormatReadFile(File notebookFile) throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            var ipynb = jsonb.fromJson(new FileReader(notebookFile, Charset.forName("UTF-8")), IpynbFormat.class);
            System.out.println("ipynb: " + ipynb);
            ipynb.toCellDataList().forEach(System.out::println);
        }
    }

    @SuppressWarnings({"raw", "try", "unchecked"})
    @ParameterizedTest
    @MethodSource("exampleNotebookFiles")
    void canIpynbFormatCompatReadFile(File notebookFile) throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            Map<String, Object> modifiableJson = jsonb.fromJson(Files.readString(notebookFile.toPath()), Map.class);
            List<Map<String, Object>> cells = (List<Map<String, Object>>) modifiableJson.get("cells");

            // ensure source is String[] for IpynbFormatCompat
            cells.forEach(cell -> cell.computeIfPresent(
                    "source",
                    (k, v) -> v instanceof String mlString ? mlString.split("\n") : v));

            // serialize modifiableJson for conversion to IpynbFormatCompat
            String modifiedJson = jsonb.toJson(modifiableJson);

            var ipynb = jsonb.fromJson(modifiedJson, IpynbFormatCompat.class);
            ipynb.toCellDataList().forEach(System.out::println);
        }
    }

    private static List<Arguments> exampleNotebookFiles() throws IOException {
        try (Stream<Path> s = Files.walk(Paths.get("..", "examples"))) {
            return s.map(Path::toFile)
                    .filter(File::isFile)
                    .filter(f -> f.getName().endsWith(".ipynb"))
                    .sorted()
                    .map(Arguments::of)
                    .toList();
        }
    }
}
