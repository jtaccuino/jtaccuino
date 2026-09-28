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
package org.jtaccuino.cheatsheet;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders the JTaccuino cheat sheet markdown to PDF.
 *
 * <p>This is a build time tool. It runs from the {@code :cheatsheet:pdf} task
 * and writes the PDF that is bundled into the application and attached to the
 * release.
 */
public final class CheatSheetGenerator {

    private static final Pattern PAGE = Pattern.compile("<!--\\s*page\\s*-->");
    private static final Pattern COLUMN = Pattern.compile("<!--\\s*col\\s*-->");
    private static final int COLUMNS = 3;

    private CheatSheetGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: CheatSheetGenerator <markdown> <css> <output.pdf>");
            System.exit(2);
        }
        var markdown = Path.of(args[0]);
        var css = Path.of(args[1]);
        var output = Path.of(args[2]);

        var options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create()));

        var document = Parser.builder(options).build()
                .parse(Files.readString(markdown, StandardCharsets.UTF_8));
        var cssText = Files.readString(css, StandardCharsets.UTF_8);
        // exportToPdf's third argument is a base URL, not CSS, so the stylesheet
        // has to be embedded into the HTML first.
        var html = PdfConverterExtension.embedCss(
                layout(HtmlRenderer.builder(options).build().render(document)), cssText);

        var parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(output)) {
            PdfConverterExtension.exportToPdf(out, html,
                    parent == null ? "" : parent.toUri().toString(), options);
        }
    }

    /**
     * Wraps the rendered markdown so that the leading title becomes a banner
     * and the content is laid out in a fixed number of columns per page.
     *
     * <p>openhtmltopdf's CSS multi-column support is not reliable: a column
     * block that spans a page break draws overlapping content. The layout is
     * therefore a plain table, one row per page and one cell per column, split
     * at {@code <!-- col -->} and {@code <!-- page -->} markers in the source
     * document.
     */
    private static String layout(String body) {
        var banner = "";
        var content = body;
        var close = "</h1>";
        var index = body.indexOf(close);
        if (index >= 0) {
            banner = body.substring(0, index + close.length());
            content = body.substring(index + close.length()).stripLeading();
            if (content.startsWith("<p>")) {
                var end = content.indexOf("</p>") + "</p>".length();
                banner += content.substring(0, end);
                content = content.substring(end);
            }
        }

        var html = new StringBuilder();
        html.append("<div class=\"banner\">").append(banner).append("</div>");
        var pages = split(content, PAGE);
        for (var pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            var columns = split(pages.get(pageIndex), COLUMN);
            while (columns.size() < COLUMNS) {
                columns.add("");
            }
            html.append("<div class=\"page");
            if (pageIndex == 0) {
                html.append(" first");
            }
            html.append("\"><table class=\"layout\"><tr>");
            for (var column : columns) {
                html.append("<td class=\"col\">").append(column).append("</td>");
            }
            html.append("</tr></table></div>");
        }
        return html.toString();
    }

    private static List<String> split(String text, Pattern pattern) {
        var parts = new ArrayList<String>();
        var matcher = pattern.matcher(text);
        var start = 0;
        while (matcher.find()) {
            parts.add(text.substring(start, matcher.start()));
            start = matcher.end();
        }
        parts.add(text.substring(start));
        return parts;
    }
}
