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
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import org.jtaccuino.app.common.internal.IpynbFormat;
import org.jtaccuino.app.common.internal.IpynbFormatCompat;
import org.jtaccuino.core.ui.api.CellData;
import static org.jtaccuino.core.ui.api.CellData.Type.CODE;
import static org.jtaccuino.core.ui.api.CellData.Type.MARKDOWN;

public class NotebookPersistence {

    public static NotebookPersistence INSTANCE = NotebookPersistence.singleton();

    private static NotebookPersistence singleton() {
        return new NotebookPersistence();
    }

    private NotebookPersistence() {
    }

    public NotebookImpl of() {
        return new NotebookImpl(null, "Scratch", null);
    }

    public NotebookImpl of(File file) {
        try {
            var jsonb = JsonbBuilder.create();
            var ipynb = jsonb.fromJson(new FileReader(file), IpynbFormat.class);
            jsonb.close();
            return new NotebookImpl(ipynb, file.getName(), file);
        } catch (final Exception ex) {
            ex.printStackTrace();
            try {
                Jsonb jsonb = JsonbBuilder.create();
                var ipynb = jsonb.fromJson(new FileReader(file), IpynbFormatCompat.class);
                jsonb.close();
                return new NotebookImpl(ipynb, file.getName(), file);
            } catch (Exception ee) {
                // try compat mode
                ee.printStackTrace();
            }
        }
        return null;
    }

    public void toFile(File selectedFile, List<CellData> cells) {
        var config = new JsonbConfig();
        config.setProperty(JsonbConfig.FORMATTING, true);
        Jsonb jsonb = JsonbBuilder.create(config);
        try {
            jsonb.toJson(toIpynbFormat(cells), new FileWriter(selectedFile));
            jsonb.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private IpynbFormat toIpynbFormat(List<CellData> cells) {
        java.util.List<org.jtaccuino.app.common.internal.IpynbFormat.Cell> nbCells = cells.stream().filter(c -> !c.isEmpty()).map(this::convertToNotebookCell).toList();
        return new IpynbFormat(Map.of("kernel_info", Map.of("name", "JTaccuino", "version", "0.1"), "language_info", Map.of("name", "Java", "version", System.getProperty("java.specification.version"))), 4, 5, nbCells);
    }

    private IpynbFormat.Cell convertToNotebookCell(CellData cellData) {
        return switch (cellData.getType()) {
            case CODE ->
                new IpynbFormat.CodeCell(
                cellData.getId().toString(),
                cellData.getType().name().toLowerCase(),
                Map.of(),
                cellData.getSource(),
                cellData.getOutputData().stream().map(od -> new IpynbFormat.Output(od.type().toOutputType(), od.mimeBundle(), Map.of())).toList(),
                0);
            case MARKDOWN ->
                new IpynbFormat.MarkdownCell(
                cellData.getId().toString(),
                cellData.getType().name().toLowerCase(),
                Map.of(),
                cellData.getSource());
        };
    }
}
