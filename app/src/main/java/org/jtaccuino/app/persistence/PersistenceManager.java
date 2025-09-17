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
package org.jtaccuino.app.persistence;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtaccuino.app.common.NotebookPersistence;

public class PersistenceManager {

    private static Path getConfigBaseDirectory() {
        return Path.of(System.getProperty("user.home"), ".config", "jtaccuino");
    }

    public static void writePersistenceFile(String filename, Object object) {
        var config = new JsonbConfig();
        config.setProperty(JsonbConfig.FORMATTING, true);
        Jsonb jsonb = JsonbBuilder.create(config);
        try {
            var persistencFile = getConfigBaseDirectory().resolve(filename).toFile();
            persistencFile.getParentFile().mkdirs();
            jsonb.toJson(object, new FileWriter(persistencFile, StandardCharsets.UTF_8));
            jsonb.close();
        } catch (Exception ex) {
            Logger.getLogger(NotebookPersistence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @SuppressWarnings("try")
    public static <T> Optional<T> readPersistenceFile(String filename, Class<T> persistenceClass) {
        Optional<T> maybeT = null;
        try (var jsonb = JsonbBuilder.create()){
            var persistencFile = getConfigBaseDirectory().resolve(filename).toFile();
            var persistence = jsonb.fromJson(new FileReader(persistencFile, StandardCharsets.UTF_8), persistenceClass);
            maybeT = Optional.of(persistence);
        } catch (final Exception ex) {
            Logger.getLogger(NotebookPersistence.class.getName()).log(Level.SEVERE, null, ex);
            maybeT = Optional.empty();
        }
        return maybeT;
    }

    private PersistenceManager() {
    }
}
