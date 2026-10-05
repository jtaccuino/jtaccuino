/*
 * Copyright 2025-2026 JTaccuino Contributors
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

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilePersistence {

    private static final FilePersistence INSTANCE
            = PersistenceManager.readPersistenceFile("files", FilePersistence.class)
                    // ensure only files with uri present are managed
                    .map(fp -> {
                        while (fp.openFiles.remove(null)) {
                        }
                        while (fp.recentFiles.remove(null)) {
                        }
                        return fp;
                    })
                    .orElse(new FilePersistence());

    @JsonbTypeDeserializer(OpenFileDeserializer.class)
    public static record OpenFile(URI uri) {
    }

    @JsonbTypeDeserializer(RecentFileDeserializer.class)
    public static record RecentFile(URI uri) {
    }

    public static class OpenFileDeserializer implements JsonbDeserializer<OpenFile> {

        @SuppressWarnings("unchecked")
        @Override
        public OpenFile deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            var o = parser.getObject();
            if (o.containsKey("uri")) {
                return new OpenFile(URI.create(o.getString("uri")));
            } else if (o.containsKey("path")) {
                return new OpenFile(new File(o.getString("path")).toURI());
            } else {
                Logger.getLogger(FilePersistence.class.getName()).log(Level.SEVERE, "Unable to deserialze an OpenFile instance from " + o);
                return null;
            }
        }
    }

    public static class RecentFileDeserializer implements JsonbDeserializer<RecentFile> {

        @SuppressWarnings("unchecked")
        @Override
        public RecentFile deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            var o = parser.getObject();
            if (o.containsKey("uri")) {
                return new RecentFile(URI.create(o.getString("uri")));
            } else if (o.containsKey("path")) {
                return new RecentFile(new File(o.getString("path")).toURI());
            } else {
                Logger.getLogger(FilePersistence.class.getName()).log(Level.SEVERE, "Unable to deserialze a RecentFile instance from " + o);
                return null;
            }
        }
    }

    public static FilePersistence getDefault() {
        return INSTANCE;
    }

    private List<OpenFile> openFiles = new ArrayList<>();
    private List<RecentFile> recentFiles = new ArrayList<>();

    public FilePersistence() {
    }

    public List<OpenFile> getOpenFiles() {
        return Collections.unmodifiableList(openFiles);
    }

    public void setOpenFiles(List<OpenFile> openFiles) {
        this.openFiles = openFiles;
    }

    public List<RecentFile> getRecentFiles() {
        return Collections.unmodifiableList(recentFiles);
    }

    public void setRecentFiles(List<RecentFile> recentFiles) {
        this.recentFiles = recentFiles;
    }

    public void add(OpenFile openFile) {
        openFiles.add(openFile);
    }

    public void remove(OpenFile openFile) {
        openFiles.remove(openFile);
    }

    public void add(RecentFile recentFile) {
        recentFiles.add(recentFile);
    }

    public void remove(RecentFile recentFile) {
        recentFiles.remove(recentFile);
    }

    public void reset() {
        openFiles.clear();
        recentFiles.clear();
    }
}
