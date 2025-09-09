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
package org.jtaccuino.app.studio.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An Utility class.
 */
public final class Util {

    private Util() {
    }

    /**
     * Extract the file name from a filesystem path or a URL.
     *
     * @param filePath a filesystem path or a URL e.g.
     * {@code /Users/ikost/Documents/xchart.ipynb}.
     * @return the filename + extension (e.g. {@code xchart.ipynb})
     */
    public static String getFileNamePartOf(String filePath) {
        int slashIndex = filePath.lastIndexOf('/');
        if (slashIndex == -1) {
            return filePath;
        }
        return filePath.substring(slashIndex + 1);
    }

    /**
     * Check if the URL is valid (protocol, structure)
     *
     * @param url the remote file's URL
     * @return {@code true} if this is a valid URL
     */
    public static boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            // URI must have scheme and host for remote files
            return !(uri.getScheme() == null || uri.getHost() == null);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
