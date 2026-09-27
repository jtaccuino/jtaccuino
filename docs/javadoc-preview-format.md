# Javadoc Preview Rendering Spec

The javadoc popup displays the documentation of the currently focused
completion item. The popup content is produced by the following pipeline:

    JShell javadoc (HTML)  --[converter]-->  Markdown  --[flexmark AST]-->  RichTextArea model

This document defines the markdown that the HTML converter must produce and
therefore the look and feel of the rendered popup.

## Layout

The popup shows, top to bottom:

1. **Enclosing type** - the qualified name of the declaring type, e.g. `java.lang.Object`
2. **Signature** - the member signature, e.g. `public boolean equals(Object obj)`
3. **Description** - the javadoc description, one paragraph per HTML `<p>`
4. **Sections** - the javadoc tags, each as a bold label followed by bullet items

## Example target output

Given the javadoc of `Object.equals`:

```html
Indicates whether some other object is "equal to" this one.
<p>
The <code>equals</code> method implements an equivalence relation.
<p>
<b>API Note:</b> It is generally necessary to override the <code>hashCode</code> method.
<dl>
<dt>Parameters:</dt>
<dd><code>obj</code> - the reference object with which to compare.</dd>
<dt>Returns:</dt>
<dd><code>true</code> if this object is the same as the <code>obj</code> argument.</dd>
<dt>Throws:</dt>
<dd><code>NullPointerException</code> - if the specified argument is null.</dd>
</dl>
```

The converter must produce the following markdown:

```markdown
java.lang.Object

**public boolean equals(Object obj)**

Indicates whether some other object is "equal to" this one.

The `equals` method implements an equivalence relation.

**API Note:** It is generally necessary to override the `hashCode` method.

**Parameters:**

- `obj` - the reference object with which to compare.

**Returns:**

- `true` if this object is the same as the `obj` argument.

**Throws:**

- `NullPointerException` - if the specified argument is null.
```

## Markdown conventions

| Markdown construct          | Rendered as                                             |
|-----------------------------|---------------------------------------------------------|
| `java.lang.Object`          | enclosing type, monospace, small, grey                   |
| `**public boolean equals(Object obj)**` | signature, bold, slightly larger           |
| paragraph text              | description paragraph, body font with line spacing       |
| `` `code` ``                | monospace font, light background chip                    |
| `**Label:**` alone          | section label, bold + accent color                       |
| `**Bold**`                  | bold text                                               |
| `*Italic*`                  | italic text                                             |
| `- item`                    | bullet list item, indented with hanging wrap             |
| blank line                  | vertical space between blocks                           |

## HTML to markdown mapping

| HTML                                | Markdown                                    |
|-------------------------------------|---------------------------------------------|
| `<p>...</p>` (description)          | paragraph, blank line after                 |
| `<code>...</code>`                  | `` `code` ``                                |
| `<pre>...</pre>`                    | fenced code block (```)                     |
| `<b>`, `<strong>`                   | `**...**`                                   |
| `<i>`, `<em>`                       | `*...*`                                     |
| `<h1>`..`<h6>`                      | `#`..`######` heading                       |
| `<ul><li>...</li></ul>`             | `- item` lines                              |
| `<dl><dt>Label:</dt><dd>...</dd>`   | `**Label:**` line followed by `- item` lines |
| `{@code ...}`                       | `` `...` ``                                 |
| `{@link ...}`, `{@value ...}`       | plain text (no hyperlink)                   |
| `@param`, `@return`, `@throws`, `@see` (plain text) | same as `<dl>` section handling  |
| `&lt;`, `&gt;`, `&amp;`, `&nbsp;`   | decoded character                           |
| `@index ...`, `@hidden`, unknown tags | dropped                                    |

## Spacing rules

- One blank line separates: header, description paragraphs, and each section.
- Bullet items are single lines without blank lines between them.
- No blank line between a section label and its first item.
- A blank line separates the last bullet of a section from the next section.
