/*
 * Copyright 2026 JTaccuino Contributors
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
package org.jtaccuino.notebook;

import java.util.Locale;

/**
 * Which image representation an export should prefer.
 *
 * <p>This only constrains images. A displayed collection still exports as a
 * markdown table even with {@link #PNG}: the flag is about vector versus raster,
 * not about suppressing the better text form.
 */
public enum ImageFormat {

    /** Vector when one is available, otherwise the stored raster. */
    BEST,
    /** The stored raster, ignoring any vector representation. */
    PNG,
    /** Vector, falling back to the raster with a warning when none is available. */
    SVG;

    public static ImageFormat of(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "best" ->
                BEST;
            case "png" ->
                PNG;
            case "svg" ->
                SVG;
            default ->
                throw new IllegalArgumentException("unknown image format: " + value);
        };
    }
}
