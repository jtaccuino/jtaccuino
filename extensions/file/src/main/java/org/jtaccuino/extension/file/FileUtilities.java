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
package org.jtaccuino.extension.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtilities {

    private static byte[] ZIP_MAGIC_NUMBER = {0x50, 0x4B, 0x03, 0x04};

    public static boolean isZipFile(Path path) {
        if (!Files.exists(path) || !Files.isReadable(path) || path.toFile().length() < ZIP_MAGIC_NUMBER.length) {
            return false;
        }
        try (InputStream is = Files.newInputStream(path)) {
            byte[] fileHeader = new byte[ZIP_MAGIC_NUMBER.length];
            if (is.read(fileHeader) != ZIP_MAGIC_NUMBER.length) {
                return false;
            }
            return java.util.Arrays.equals(fileHeader, ZIP_MAGIC_NUMBER);
        } catch (IOException e) {
            Logger.getLogger(FileUtilities.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
    }

    public static void deleteDirectoryRecursively(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ioex) {
                            Logger.getLogger(FileUtilities.class.getName()).log(Level.SEVERE, null, ioex);
                        }
                    });
        } catch (IOException e) {
            Logger.getLogger(FileUtilities.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static Optional<Path> load(String uriString) {
        Path downloadedFile = null;

        try {
            var uri = URI.create(uriString);
            downloadedFile = Files.createTempFile("download-", ".tmp");
            Files.delete(downloadedFile);

            try (InputStream inputStream = uri.toURL().openStream()) {
                Files.copy(inputStream, downloadedFile);
            }

            if (isZipFile(downloadedFile)) {
                Path tempDir = Files.createTempDirectory("unpacked-");
                try (ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(downloadedFile))) {
                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {
                        Path entryPath = tempDir.resolve(entry.getName()).normalize();
                        if (!entryPath.startsWith(tempDir)) {
                            // Security check to prevent Zip Slip vulnerability
                            throw new IOException("Zip entry is outside of the target directory: " + entry.getName());
                        }
                        if (entry.isDirectory()) {
                            Files.createDirectories(entryPath);
                        } else {
                            Files.createDirectories(entryPath.getParent());
                            Files.copy(zipStream, entryPath);
                        }
                    }
                }
                Files.delete(downloadedFile);
                return Optional.of(tempDir);
            } else {
                return Optional.of(downloadedFile);
            }

        } catch (IOException e) {
            Logger.getLogger(FileUtilities.class.getName()).log(Level.SEVERE, "Failed to download or process file from URL: " + uriString, e);
            if (downloadedFile != null) {
                try {
                    Files.deleteIfExists(downloadedFile);
                } catch (IOException cleanupException) {
                    Logger.getLogger(FileUtilities.class.getName()).log(Level.SEVERE, "Failed to clean up downloaded file: " + cleanupException.getMessage(), e);
                }
            }
            return Optional.empty();
        }
    }
}
