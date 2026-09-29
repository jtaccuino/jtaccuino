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

/**
 * How a markdown export should write its images.
 *
 * @param imageFormat which image representation to prefer; see
 * {@link ImageFormat}
 * @param extractImages when true, images are written next to the markdown and
 * referenced by path instead of being embedded as base64 data URIs. Only
 * meaningful for a file export, where there is a directory to write beside
 */
public record ExportOptions(ImageFormat imageFormat, boolean extractImages) {

    public static ExportOptions defaults() {
        return new ExportOptions(ImageFormat.BEST, false);
    }

    public ExportOptions withImageFormat(ImageFormat imageFormat) {
        return new ExportOptions(imageFormat, extractImages);
    }

    public ExportOptions withExtractImages(boolean extractImages) {
        return new ExportOptions(imageFormat, extractImages);
    }
}
