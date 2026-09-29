# JTaccuino

JTaccuino is a JavaFX based notebook application for Java developers.

It is built for usages in education, interactive experimentation with algorithms and possible more advanced use cases.

Java code execution is provided by JShell, the awesome Java REPL.

The notebook model, execution and export live in a UI-free `notebook` module, so
the same engine that powers the desktop application can run headlessly: a
Jupyter `.ipynb` can be executed and exported to markdown from a command line
tool or embedded in another program. See
[Headless export](#headless-export) and
[Using the notebook engine](#using-the-notebook-engine).

## Building and running JTaccuino from source
Clone the repository and run on Linux and Mac
`./gradlew app:run`
or on Windows
`./gradlew.bat app:run`

To build the headless exporter:

`./gradlew :cli:installDist`

which produces `jtaccuino-cli` under
`cli/build/install/jtaccuino-cli/bin`.

## Modules

```
shell     JShell wrapper, extension SPI, dependency resolution
  ^
notebook  CellData/Notebook model, .ipynb read/write, markdown export,
  |       execution API, display registry (shell + javafx.base/graphics)
  +-- ui  JavaFX controls, cell skins and output renderers
  +-- app Studio, the desktop application
  +-- cli headless command line exporter
```

`notebook` deliberately keeps `javafx.controls` and `javafx.swing` off its
runtime classpath, so a consumer gets notebook support without a desktop UI
stack.

## Headless export

`jtaccuino-cli [options] <notebook.ipynb>` executes a notebook and writes it
out as markdown, without starting the desktop application. Images are
rasterised with headless JavaFX (the Glass platform and the software Prism
pipeline), so no window system, display or GPU is needed.

Once the artifact is published, jbang runs it without a checkout:

```bash
jbang org.jtaccuino:jtaccuino-cli:VERSION book.ipynb -o book.md
```

The published CLI is a single self-contained jar: it bundles the notebook
engine, the renderers and the JavaFX runtime for the platform it was published
for, which is what lets jbang start it without a JavaFX setup.

Add `--enable-preview` for notebooks that use preview features, and
`-R--enable-native-access=ALL-UNNAMED` to silence the JavaFX native-access
warning:

```bash
jbang run --enable-preview -R--enable-native-access=ALL-UNNAMED \
    org.jtaccuino:jtaccuino-cli:VERSION book.ipynb
```

JavaFX loaded from the classpath logs an "Unsupported JavaFX configuration"
warning; it is harmless for a headless export.

```
-o, --output <file>        markdown output (default <notebook>.md)
    --ipynb <file>         also write the executed notebook back
    --continue-on-error    keep going after a failing cell
    --extract-images       write sidecar images instead of inline base64
    --image-format FMT     best | png | svg (default best)
    --cwd <dir>            working directory (default the notebook's folder)
-h, --help                 show usage
```

Exit codes: `0` success, `1` usage or load error, `2` a cell failed, `3` the
export could not be written.

The markdown export picks the best representation per output: a displayed
collection or array becomes a markdown table, `println` output stays verbatim
text, and a chart or node becomes an image, preferring SVG over PNG when a
renderer offers it. `--image-format svg` falls back to the stored raster and
warns when no vector representation is available.

## Using the notebook engine

The `notebook` module exposes a small API for scripts and services:

```java
var notebook = Notebooks.read(Path.of("book.ipynb"));
var result = Notebooks.execute(notebook);
Files.writeString(Path.of("book.md"), result.toMarkdown());
result.failedCells();
```

`ExecutionOptions` controls `continueOnError`, the working directory and an
optional `DisplaySink`; `result.displayRegistry()` carries the live displayed
objects, so the exporter can render them as text rather than as the picture
stored for them.

## Publishing

The consumable artifacts are `jtaccuino-notebook` (which depends on
`jtaccuino-shell`) and `jtaccuino-cli`. Publish them to Maven Local with:

```
./gradlew publishToMavenLocal
```

To publish to GitHub Packages, supply the group GitHub requires and a token:

```
./gradlew publish \
    -Pgroup=com.github.<owner>.<repo> \
    -Pgpr.user=<user> -Pgpr.key=<token>
```

Signing is performed only when `-PsigningKey`/`-PsigningPassword` (or the
`SIGNING_KEY`/`SIGNING_PASSWORD` environment variables) are present, so an
ordinary build never needs a key.

Both artifacts are platform specific: the notebook POM pins the OpenJFX
classifier it was built with, and the CLI jar bundles JavaFX for its platform.
Publish from the platform you target, or publish per platform.

## Licenses and used 3rd party software / components

### Libraries

#### Flexmark
License: BSD-2-Clause license

flexmark-java is a Java implementation of CommonMark (spec 0.28) parser using the blocks first, inlines after Markdown parsing architecture.

#### Gluon Emoji
License: GPL-3.0 license

Emoji support for JavaFX, used to render emoji shortcodes in markdown.

#### GemsFX
License: Apache-2.0 license

GemsFX is a collection of custom controls and utilities for JavaFX.

#### OpenJFX
License: GPL-2.0 with Classpath Exception

OpenJFX is an open source, next generation client application platform for desktop, mobile and embedded systems based on JavaSE. It is a collaborative effort by many individuals and companies with the goal of producing a modern, efficient, and fully featured toolkit for developing rich client applications. This is the open source project where we develop JavaFX.

#### Yasson
License: Eclipse Public License - v 2.0

Yasson is a Java framework which provides a standard binding layer between Java classes and JSON documents. This is similar to what JAXB is doing in the XML world. Yasson is an official reference implementation of JSON Binding.

#### Jakarta JSONB
License: Eclipse Public License - v 2.0

Jakarta JSON Binding

#### Maven Resolver
License: Apache-2.0 license

Apache Maven Artifact Resolver is a library for working with artifact repositories and dependency resolution.

### Resources

#### Fonts
The application uses [Monaspace](https://github.com/githubnext/monaspace) fonts provided by GitHub Next especially
- Monaspace Argon
- Monaspace Radon

both licensed under [SIL OPEN FONT LICENSE Version 1.1](http://scripts.sil.org/OFL)

#### Icons
Icons used are downloaded from [SVGRepo](https://www.svgrepo.com/) and converted to SVG paths useable in JavaFX by the [SVG Path Extractor at JFXCentral](https://www.jfx-central.com/utilities/pathextractor).

Icons used are from the following collections
- Meteor Line Interface Icons
  - License: [MIT License](https://www.svgrepo.com/page/licensing/#MIT)
  - Author: ShopWare
- Zest Interface Icons
  - License: [MIT License](https://www.svgrepo.com/page/licensing/#MIT)
  - Author: zest
-  Flat Icon Design Dark Vectors
  - License: [PD License](https://www.svgrepo.com/page/licensing/#PD)
  - Author: flat-icon-design
