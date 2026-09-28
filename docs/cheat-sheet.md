# JTaccuino Studio Cheat Sheet

JTaccuino Studio is a JavaFX notebook for Java developers. **Java cells** run in
JShell, **Markdown cells** document the notebook, and everything is stored in the
Jupyter `.ipynb` format. `Cmd` is the Command key on macOS; use `Ctrl` on
Windows and Linux.

## Getting started

- Run from source: `./gradlew app:run`
- New notebook `Cmd/Ctrl+N`
- Open a notebook `Cmd/Ctrl+O`
- Open from a URL `Cmd/Ctrl+Shift+O`
- Save `Cmd/Ctrl+S`, Save As `Cmd/Ctrl+Shift+S`
- Export the notebook via **File > Export**

## Cells

A notebook is an ordered list of cells. Each cell is either a **Java** cell
(executed by JShell) or a **Markdown** cell (rendered documentation). A rendered
Markdown cell returns to its source on a shift-click.

- Execute the focused cell `Shift+Enter`
- Execute the whole notebook `Cmd/Ctrl+R`
- Reset and execute from a clean JShell `Alt+Cmd/Ctrl+R`
- Insert cell below `Cmd/Ctrl+Enter`, above `Shift+Cmd/Ctrl+Enter`
- Move cell up `Ctrl+Shift+Up`, down `Ctrl+Shift+Down`
- Change cell type: Java `Cmd/Ctrl+J`, Markdown `Cmd/Ctrl+M`
- Delete cell `Ctrl+Backspace`
- Presentation mode `Cmd+Ctrl+Alt+P`

<!-- col -->

## Keyboard shortcuts

- New notebook — `Cmd/Ctrl+N`
- Open notebook — `Cmd/Ctrl+O`
- Open remote — `Cmd/Ctrl+Shift+O`
- Save — `Cmd/Ctrl+S`
- Save as — `Cmd/Ctrl+Shift+S`
- Execute notebook — `Cmd/Ctrl+R`
- Reset and execute — `Alt+Cmd/Ctrl+R`
- Execute cell — `Shift+Enter`
- Insert cell below — `Cmd/Ctrl+Enter`
- Insert cell above — `Shift+Cmd/Ctrl+Enter`
- Move cell up — `Ctrl+Shift+Up`
- Move cell down — `Ctrl+Shift+Down`
- Cell to Java — `Cmd/Ctrl+J`
- Cell to Markdown — `Cmd/Ctrl+M`
- Delete cell — `Ctrl+Backspace`
- Presentation mode — `Cmd+Ctrl+Alt+P`

## Java cells

Java cells are evaluated by JShell, so the value of the last expression is shown
below the cell and no `main` method or class wrapper is required.

```java
var greeting = "Hello, JTaccuino";
greeting.toUpperCase()
```

- Local type inference with `var` is available.
- Exceptions are shown as cell error output.
- Code is submitted incrementally, so later cells see earlier declarations.
- `Reset and execute` discards the JShell state and re-runs everything.

<!-- col -->

## Completion and Javadoc

Java cells get editing assistance backed by JShell:

- **Code completion** — press `Tab` at the caret. A single candidate is inserted directly; with several the popup opens and `Tab` extends to the longest common prefix.
- In the popup, `Up`/`Down`/`Home`/`End` move, `Enter` inserts the highlighted candidate, `Esc` closes, and a click inserts too.
- **Argument completion** — inside a method call the candidates include the method's parameters, so an argument name can be accepted instead of typed.
- **Javadoc popup** — the documentation of the focused candidate appears automatically after a short delay. Press `Shift+Tab` to show the Javadoc for the element at the caret directly.

```java
// type "Sys" then Tab completes to "System"
Sys
```

## Markdown cells

Markdown cells accept CommonMark plus GFM extensions:

- Headings `#` through `######`
- **bold**, *italic* and ~~strikethrough~~
- Inline code with backticks, fenced code blocks with triple backticks
- Bullet lists, ordered lists and nested lists
- Links `[text](https://example.org)`
- Tables with a header separator row
- Emoji shortcodes such as `:smile:`
- Two trailing spaces produce a hard line break

For example `## Heading`, `**bold**`, `*italic*`, `~~gone~~` and
`[text](https://example.org)` are all recognised.

<!-- page -->

## Extensions

Extensions add a library and its imports to the JShell session. They are
activated on demand with `use(...)`, for example in a Java cell:

```java
use(JTExtension.DF_LIB_EXTENSION);
```

### DfLib - data frames

```java
use(JTExtension.DF_LIB_EXTENSION);

var df = DataFrame.of(
    new String[] {"name", "value"},
    new Object[] {"a", 1, "b", 2});
println(df);
```

Imports `DataFrame`, `Csv`, `CsvLoader` and `TabularPrinter`, and adds a
`println(DataFrame)` helper. A `Csv.load(path)` convenience is available.

### LangChain4j - LLM and RAG with Ollama

```java
use(JTExtension.LANG_CHAIN4J_EXTENSION);

var model = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("llama3")
    .build();

model.chat("Explain a Java record in one sentence.");
```

Imports the chat and embedding models, document loaders and splitters, in-memory
embedding stores, retrieval augmentors and `AiServices`. Requires a running
Ollama instance.

<!-- col -->

### DeepNetts - neural networks

```java
use(JTExtension.DEEP_NETTS_EXTENSION);

TabularDataSet data = DataSets.readCsv(csvFile);
var scaler = new MaxScaler();
scaler.apply(data);
```

Imports `DataSets`, `TabularDataSet`, `MaxScaler`, `ClassifierEvaluator`,
`ConfusionMatrix`, `FeedForwardNetwork`, activation and loss types and the
`BackpropagationTrainer`.

### FX Charts - plotting JavaFX charts

```java
use(JTExtension.FX_CHARTS_EXTENSION);

plotFx(x -> Math.sin(x), -6, 6, 0.1, "x", "sin(x)", "Sine");
scatterFx(new int[] {1, 2, 3}, new double[] {1, 4, 9}, "x", "y", "Squares");
```

Provides `lineFx`, `scatterFx`, `scatterPlot` and `plotFx` returning JavaFX
charts that render inline in the notebook.

### File - file helpers

```java
use(JTExtension.FILE_EXTENSION);

load("https://example.org/data.csv");
```

Adds the static helpers `FileUtilities.isZipFile`, `deleteDirectoryRecursively`
and `load` (which downloads a URL or resolves a path to a local `Path`).

<!-- col -->

### Ad-hoc dependencies

Add any Maven coordinate to the running JShell:

```java
addDependency("com.google.guava:guava:33.0.0-jre");
```

## Output

- JavaFX nodes returned by a cell render inline, including charts.
- Data frames, tables and collections of records render as a table.
- Long output is scrollable inside the cell.

## Presentation mode

`Cmd+Ctrl+Alt+P` hides the editing chrome and shows the notebook cells only,
suitable for projecting. Press it again to return to the normal view.

## Files and persistence

- Notebooks are plain Jupyter `.ipynb` files and can be opened by other
  Jupyter front ends.
- **File > Recent Files** reopens recently used notebooks.
- **File > Export** writes the notebook out for sharing.

## Links

- Project and issues: https://github.com/jtaccuino/jtaccuino
