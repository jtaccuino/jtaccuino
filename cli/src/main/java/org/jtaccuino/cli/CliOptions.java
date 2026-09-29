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
package org.jtaccuino.cli;

import java.nio.file.Path;
import org.jtaccuino.notebook.ImageFormat;

/**
 * The parsed command line.
 *
 * @param notebook the notebook to execute, never {@code null}
 * @param output the markdown file to write
 * @param ipynb when non-{@code null}, the executed notebook is also written
 * back as an ipynb
 * @param continueOnError keep executing after a failing cell instead of
 * stopping at the first
 * @param extractImages write sidecar image files instead of inline base64
 * @param imageFormat which image representation to prefer
 * @param cwd working directory for the shell; defaults to the notebook's folder
 * @param help the caller asked for usage rather than an export
 */
record CliOptions(Path notebook, Path output, Path ipynb, boolean continueOnError,
        boolean extractImages, ImageFormat imageFormat, Path cwd, boolean help) {

    static CliOptions parse(String[] args) throws CliUsageException {
        Path notebook = null;
        Path output = null;
        Path ipynb = null;
        Path cwd = null;
        var continueOnError = false;
        var extractImages = false;
        var help = false;
        var imageFormat = ImageFormat.BEST;

        for (var index = 0; index < args.length; index++) {
            var argument = args[index];
            switch (argument) {
                case "-h", "--help" ->
                    help = true;
                case "-o", "--output" ->
                    output = Path.of(valueOf(args, ++index, argument));
                case "--ipynb" ->
                    ipynb = Path.of(valueOf(args, ++index, argument));
                case "--cwd" ->
                    cwd = Path.of(valueOf(args, ++index, argument));
                case "--continue-on-error" ->
                    continueOnError = true;
                case "--extract-images" ->
                    extractImages = true;
                case "--image-format" -> {
                    var value = valueOf(args, ++index, argument);
                    try {
                        imageFormat = ImageFormat.of(value);
                    } catch (IllegalArgumentException unknown) {
                        throw new CliUsageException(unknown.getMessage());
                    }
                }
                default -> {
                    if (argument.startsWith("-")) {
                        throw new CliUsageException("unknown option: " + argument);
                    }
                    if (null != notebook) {
                        throw new CliUsageException("only one notebook can be exported, got '"
                                + notebook + "' and '" + argument + "'");
                    }
                    notebook = Path.of(argument);
                }
            }
        }

        if (help) {
            return new CliOptions(null, null, null, false, false, ImageFormat.BEST, null, true);
        }
        if (null == notebook) {
            throw new CliUsageException("no notebook given");
        }
        var absoluteNotebook = notebook.toAbsolutePath();
        var parent = absoluteNotebook.getParent();
        return new CliOptions(
                absoluteNotebook,
                null != output ? output : defaultOutput(absoluteNotebook),
                ipynb,
                continueOnError,
                extractImages,
                imageFormat,
                null != cwd ? cwd : parent,
                false);
    }

    private static String valueOf(String[] args, int index, String option) throws CliUsageException {
        if (index >= args.length) {
            throw new CliUsageException("option " + option + " needs a value");
        }
        return args[index];
    }

    private static Path defaultOutput(Path notebook) {
        var name = notebook.getFileName().toString();
        var dot = name.lastIndexOf('.');
        var stem = dot < 0 ? name : name.substring(0, dot);
        return notebook.resolveSibling(stem + ".md");
    }

    static final class CliUsageException extends Exception {

        private static final long serialVersionUID = 1L;

        CliUsageException(String message) {
            super(message);
        }
    }
}
