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

import java.io.IOException;
import java.io.PrintStream;
import org.jtaccuino.notebook.ExecutionOptions;
import org.jtaccuino.notebook.ExecutionResult;
import org.jtaccuino.notebook.ExportOptions;
import org.jtaccuino.notebook.MarkdownNotebookExporter;
import org.jtaccuino.notebook.Notebook;
import org.jtaccuino.notebook.NotebookPersistence;
import org.jtaccuino.notebook.Notebooks;

/**
 * Executes a notebook headlessly and exports it to markdown.
 *
 * <p>Exit codes: {@code 0} success, {@code 1} usage or load error, {@code 2} a
 * cell failed, {@code 3} the export could not be written.
 */
public final class Main {

    static final String USAGE = """
        Usage: jtaccuino-cli [options] <notebook.ipynb>

          -o, --output <file>      markdown output            (default: <notebook>.md)
              --ipynb <file>       also write the executed notebook back as .ipynb
              --continue-on-error  keep going after a failing cell (default: stop at first)
              --extract-images     sidecar images instead of inline base64
              --image-format FMT   best | png | svg            (default: best)
              --cwd <dir>          working directory           (default: notebook's parent)
          -h, --help               show this help""";

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (CliOptions.CliUsageException usage) {
            err.println(usage.getMessage());
            err.println(USAGE);
            return 1;
        }
        if (options.help()) {
            out.println(USAGE);
            return 0;
        }

        JavaFxRuntime.start();

        var notebook = readNotebook(options, err);
        if (null == notebook) {
            return 1;
        }

        var executionOptions = ExecutionOptions.defaults()
                .withContinueOnError(options.continueOnError())
                .withCwd(options.cwd())
                .withDisplaySink(new HeadlessDisplaySink());
        var result = Notebooks.execute(notebook, executionOptions);

        if (!writeOutputs(options, notebook, result, err)) {
            return 3;
        }
        return result.success() ? 0 : 2;
    }

    private static Notebook readNotebook(CliOptions options, PrintStream err) {
        try {
            return Notebooks.read(options.notebook());
        } catch (IOException loadFailure) {
            err.println("Could not read " + options.notebook() + ": " + loadFailure.getMessage());
            return null;
        }
    }

    private static boolean writeOutputs(CliOptions options, Notebook notebook,
            ExecutionResult result, PrintStream err) {
        try {
            var exportOptions = new ExportOptions(options.imageFormat(), options.extractImages());
            var exported = MarkdownNotebookExporter.export(notebook.getCells(), options.output().toFile(),
                    result.displayRegistry(), exportOptions);
            exported.warnings().forEach(warning -> err.println("warning: " + warning));
            if (null != options.ipynb()) {
                NotebookPersistence.INSTANCE.toFile(options.ipynb().toFile(), notebook.getCells(), true);
            }
            return true;
        } catch (IOException | RuntimeException exportFailure) {
            err.println("Export failed: " + exportFailure.getMessage());
            return false;
        }
    }
}
