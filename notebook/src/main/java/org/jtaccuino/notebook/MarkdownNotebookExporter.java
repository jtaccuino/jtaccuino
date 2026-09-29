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
package org.jtaccuino.notebook;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MarkdownNotebookExporter {

    private static final String CODE_FENCE = "```";

    private static final String MIME_TYPE_MARKDOWN = "text/markdown";

    private static final String MIME_TYPE_SVG = "image/svg+xml";

    private static final String MIME_TYPE_PNG = "image/png";

    private static final String MIME_TYPE_HTML = "text/html";

    private static final String MIME_TYPE_PLAIN = "text/plain";

    private MarkdownNotebookExporter() {
    }

    public static void export(List<CellData> cells, File target) throws IOException {
        export(cells, target, null, ExportOptions.defaults());
    }

    /**
     * Exports, preferring a markdown rendering of each displayed object over the
     * picture stored for it.
     *
     * @param displayRegistry the objects displayed during execution, or
     * {@code null} to export only what the notebook holds
     */
    public static void export(List<CellData> cells, File target, DisplayRegistry displayRegistry) throws IOException {
        export(cells, target, displayRegistry, ExportOptions.defaults());
    }

    /**
     * Exports with explicit control over image handling.
     *
     * @return what was written and any non-fatal warnings
     */
    public static ExportResult export(List<CellData> cells, File target, DisplayRegistry displayRegistry,
            ExportOptions options) throws IOException {
        var sidecar = options.extractImages() ? SidecarWriter.forTarget(target) : null;
        var result = render(cells, displayRegistry, options, sidecar);
        Files.writeString(target.toPath(), result.markdown(), StandardCharsets.UTF_8);
        return result;
    }

    static String toMarkdown(List<CellData> cells) {
        return toMarkdown(cells, null);
    }

    static String toMarkdown(List<CellData> cells, DisplayRegistry displayRegistry) {
        try {
            return render(cells, displayRegistry, ExportOptions.defaults(), null).markdown();
        } catch (IOException impossible) {
            // an in-memory render never writes an image file, so it cannot fail
            // this way; the signature is shared with the file export
            throw new UncheckedIOException(impossible);
        }
    }

    public static ExportResult render(List<CellData> cells, DisplayRegistry displayRegistry, ExportOptions options)
            throws IOException {
        return render(cells, displayRegistry, options, null);
    }

    private static ExportResult render(List<CellData> cells, DisplayRegistry displayRegistry, ExportOptions options,
            SidecarWriter sidecar) throws IOException {
        return new Renderer(displayRegistry, options, sidecar).render(cells);
    }

    /**
     * Renders cells to markdown, choosing one representation per output.
     *
     * <p>An instance rather than static methods because image handling carries
     * state: the image counter within a cell, and the warnings the caller is
     * told about.
     */
    private static final class Renderer {

        private final DisplayRegistry displayRegistry;
        private final ExportOptions options;
        private final SidecarWriter sidecar;
        private final List<String> warnings = new ArrayList<>();
        private int imageIndex;

        private Renderer(DisplayRegistry displayRegistry, ExportOptions options, SidecarWriter sidecar) {
            this.displayRegistry = displayRegistry;
            this.options = options;
            this.sidecar = sidecar;
        }

        ExportResult render(List<CellData> cells) throws IOException {
            var markdown = new StringBuilder();
            for (var cell : cells) {
                if (cell.isEmpty()) {
                    continue;
                }
                imageIndex = 0;
                appendCell(markdown, cell);
            }
            return new ExportResult(markdown.toString().strip(), warnings);
        }

        private void appendCell(StringBuilder markdown, CellData cell) throws IOException {
            switch (cell.getType()) {
                case MARKDOWN ->
                    appendMarkdownCell(markdown, cell);
                case CODE -> {
                    appendCodeCell(markdown, cell);
                    appendOutputs(markdown, cell);
                }
            }
        }

        private void appendMarkdownCell(StringBuilder markdown, CellData cell) {
            markdown.append('\n').append('\n').append(cell.getSource());
        }

        private void appendCodeCell(StringBuilder markdown, CellData cell) {
            markdown.append('\n').append('\n').append(fencedCodeBlock(cell.getSource(), "java"));
        }

        private void appendOutputs(StringBuilder markdown, CellData cell) throws IOException {
            var displayedObjects = null == displayRegistry ? List.of() : displayRegistry.displayedIn(cell);
            var displayIndex = new int[]{0};
            for (var outputData : cell.getOutputData()) {
                switch (outputData) {
                    case CellData.MimeTypeBasedOutputData od -> {
                        var displayed = displayIndex[0] < displayedObjects.size()
                                ? displayedObjects.get(displayIndex[0])
                                : null;
                        displayIndex[0]++;
                        appendBestRepresentation(markdown, cell, od.mimeBundle(), displayed);
                    }
                    case CellData.StreamBasedOutputData od ->
                        markdown.append('\n').append('\n').append(fencedCodeBlock(od.data()));
                }
            }
        }

        /**
         * One output yields one representation, not one per mime type.
         *
         * <p>A single bundle often carries several forms of the same thing — a
         * PNG plus a text fallback, for instance. Emitting them all would put
         * the same output in the document twice.
         *
         * <p>When the live object is known, a renderer that can express it
         * better than the stored representation wins: markdown first, because
         * text stays searchable and reflows, then SVG, because it scales. Only
         * then is the stored representation used.
         *
         * <p>Bundles that contain none of the known types are still emitted in
         * full, so an exotic mime type is not silently dropped.
         */
        private void appendBestRepresentation(StringBuilder markdown, CellData cell, Map<String, String> mimeBundle,
                Object displayed) throws IOException {
            var markdownRepresentation = MarkdownRepresentations.of(displayed);
            if (markdownRepresentation.isPresent()) {
                markdown.append('\n').append('\n').append(markdownRepresentation.get());
                return;
            }
            if (mimeBundle.containsKey(MIME_TYPE_MARKDOWN)) {
                // emitted raw: this is already markdown, and fencing it would
                // show the reader the syntax instead of the formatted result
                markdown.append('\n').append('\n').append(mimeBundle.get(MIME_TYPE_MARKDOWN));
                return;
            }
            if (options.imageFormat() != ImageFormat.PNG) {
                var svg = SvgRepresentations.of(displayed);
                if (svg.isPresent()) {
                    appendSvg(markdown, cell, svg.get());
                    return;
                }
            }
            var storedImage = bestStoredImage(mimeBundle);
            if (storedImage.isPresent()) {
                if (options.imageFormat() == ImageFormat.SVG && !MIME_TYPE_SVG.equals(storedImage.get().getKey())) {
                    warnings.add("cell " + cell.getId()
                            + ": no SVG representation available, falling back to the stored image");
                }
                appendStoredImage(markdown, cell, storedImage.get());
                return;
            }
            if (options.imageFormat() == ImageFormat.SVG && hasAnyImage(mimeBundle)) {
                // a stored image of some other type is about to be emitted as-is;
                // a text-only output is not an image, so it is not warned about
                warnings.add("cell " + cell.getId()
                        + ": no SVG representation available, falling back to the stored image");
            }
            appendTextFallback(markdown, cell, mimeBundle);
        }

        private static boolean hasAnyImage(Map<String, String> mimeBundle) {
            return mimeBundle.keySet().stream().anyMatch(mimeType -> mimeType.startsWith("image/"));
        }

        private Optional<Map.Entry<String, String>> bestStoredImage(Map<String, String> mimeBundle) {
            var preference = options.imageFormat() == ImageFormat.PNG
                    ? List.of(MIME_TYPE_PNG)
                    : List.of(MIME_TYPE_SVG, MIME_TYPE_PNG);
            return preference.stream()
                    .filter(mimeBundle::containsKey)
                    .findFirst()
                    .map(mimeType -> Map.entry(mimeType, mimeBundle.get(mimeType)));
        }

        private void appendSvg(StringBuilder markdown, CellData cell, String svg) throws IOException {
            appendImage(markdown, cell, MIME_TYPE_SVG, svg.getBytes(StandardCharsets.UTF_8));
        }

        private void appendStoredImage(StringBuilder markdown, CellData cell, Map.Entry<String, String> entry)
                throws IOException {
            appendImage(markdown, cell, entry.getKey(), Base64.getDecoder().decode(entry.getValue()));
        }

        private void appendImage(StringBuilder markdown, CellData cell, String mimeType, byte[] bytes)
                throws IOException {
            markdown.append('\n').append('\n').append("![").append(cell.getId()).append("](");
            if (null == sidecar) {
                markdown.append("data:").append(mimeType).append(";base64,")
                        .append(Base64.getEncoder().encodeToString(bytes));
            } else {
                markdown.append(sidecar.write(cell, imageIndex++, mimeType, bytes));
            }
            markdown.append(')');
        }

        private void appendTextFallback(StringBuilder markdown, CellData cell, Map<String, String> mimeBundle) {
            if (mimeBundle.containsKey(MIME_TYPE_HTML)) {
                appendMimeBundleEntry(markdown, cell, Map.entry(MIME_TYPE_HTML, mimeBundle.get(MIME_TYPE_HTML)));
                return;
            }
            if (mimeBundle.containsKey(MIME_TYPE_PLAIN)) {
                appendMimeBundleEntry(markdown, cell, Map.entry(MIME_TYPE_PLAIN, mimeBundle.get(MIME_TYPE_PLAIN)));
                return;
            }
            mimeBundle.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(entry -> appendMimeBundleEntry(markdown, cell, entry));
        }

        private static void appendMimeBundleEntry(StringBuilder markdown, CellData cell, Map.Entry<String, String> entry) {
            var mimeType = entry.getKey();
            if (mimeType.startsWith("image/")) {
                markdown.append('\n').append('\n').append("![")
                        .append(cell.getId())
                        .append("](data:").append(mimeType)
                        .append(";base64,").append(entry.getValue())
                        .append(')');
            } else if (MIME_TYPE_MARKDOWN.equals(mimeType)) {
                // emitted raw: this is already markdown, and fencing it would show
                // the reader the syntax instead of the formatted result
                markdown.append('\n').append('\n').append(entry.getValue());
            } else {
                markdown.append('\n').append('\n').append(fencedCodeBlock(entry.getValue(), languageOf(mimeType)));
            }
        }

        private static String languageOf(String mimeType) {
            return switch (mimeType) {
                case "text/html" ->
                    "html";
                default ->
                    null;
            };
        }

        private static String fencedCodeBlock(String content, String language) {
            return CODE_FENCE + (null != language ? language : "")
                    + '\n'
                    + content
                    + '\n'
                    + CODE_FENCE;
        }

        private static String fencedCodeBlock(String content) {
            return fencedCodeBlock(content, null);
        }
    }

    /**
     * Writes extracted images next to the markdown and hands back the relative
     * path to reference them by.
     *
     * <p>The directory is named after the markdown file, mirroring the
     * {@code _files} convention a Jupyter export uses, so two exports into the
     * same directory do not collide with each other's images.
     */
    private static final class SidecarWriter {

        private final Path directory;
        private final String referencePrefix;

        private SidecarWriter(Path directory, String referencePrefix) {
            this.directory = directory;
            this.referencePrefix = referencePrefix;
        }

        static SidecarWriter forTarget(File target) {
            var name = target.getName();
            var dot = name.lastIndexOf('.');
            var stem = dot < 0 ? name : name.substring(0, dot);
            var directoryName = stem + "_files";
            return new SidecarWriter(target.toPath().toAbsolutePath().resolveSibling(directoryName), directoryName);
        }

        String write(CellData cell, int index, String mimeType, byte[] bytes) throws IOException {
            Files.createDirectories(directory);
            var fileName = cell.getId() + "-" + index + extensionOf(mimeType);
            Files.write(directory.resolve(fileName), bytes);
            return referencePrefix + "/" + fileName;
        }

        private static String extensionOf(String mimeType) {
            return switch (mimeType) {
                case MIME_TYPE_PNG ->
                    ".png";
                case MIME_TYPE_SVG ->
                    ".svg";
                default ->
                    ".bin";
            };
        }
    }
}
