package org.jtaccuino.jshell;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.Statement;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaParserSnippetAnalyzer {

    private final static ExecutorService worker = Executors
            .newSingleThreadExecutor(Thread.ofVirtual().name("JavaParserSnippetAnalyzerWorker").factory());

    public static void highlightingAsync(String text, Consumer<List<SyntaxHighlight>> consumer) {
        CompletableFuture.supplyAsync(() -> analyze(text), worker)
                .thenAccept(consumer)
                .exceptionally(JavaParserSnippetAnalyzer::logThrowable);
    }

    private static Void logThrowable(Throwable t) {
        Logger.getLogger(JavaParserSnippetAnalyzer.class.getName()).log(Level.SEVERE, null, t);
        return null;
    }


    private static List<SyntaxHighlight> analyze(String snippet) {
        Objects.requireNonNull(snippet, "snippet");

        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
                .setStoreTokens(true);
        JavaParser parser = new JavaParser(cfg);

        Set<SyntaxHighlight> out = new LinkedHashSet<>();
        Set<String> declaredVars = new LinkedHashSet<>();

        // Whole-snippet class decls (top-level or via wrapper)
        detectClassDeclsWholeSnippet(snippet, parser, out);

        // Segment-wise analyses
        List<Segment> segments = splitSnippet(snippet);
        for (Segment seg : segments) {
            // JShell-friendly member detection first (also fills declaredVars for fields)
            analyzeSegmentAsMember(seg, parser, out, declaredVars);
            // Then statements (locals, calls, and variable references)
            analyzeSegmentAsStatement(seg, parser, out, declaredVars);
        }

        // Whole-snippet comments & keywords (outside strings/comments)
        List<SyntaxHighlight> comments = findComments(snippet);
        out.addAll(comments);
        out.addAll(findKeywords(snippet));

        out.addAll(findStringsAndTextBlocks(snippet, comments));

        // Return sorted
        List<SyntaxHighlight> res = new ArrayList<>(out);
        return res;
    }

    private static final class Segment {
        final String text;
        final int start; // in original snippet
        final int end;   // in original snippet (inclusive)
        Segment(String text, int start, int end) { this.text = text; this.start = start; this.end = end; }

        @Override
        public String toString() {
            return "Segment{" +
                    "text='" + text + '\'' +
                    ", start=" + start +
                    ", end=" + end +
                    '}';
        }
    }

    /**
     * Split by top-level semicolons and when a top-level block closes ('}'),
     * outside comments/strings/chars. Each segment is non-empty and preserves text.
     */
    private static List<Segment> splitSnippet(String s) {
        List<Segment> segs = new ArrayList<>();
        int n = s.length();
        boolean inLine = false, inBlock = false, inStr = false, inChar = false, esc = false;
        int segStart = 0;
        int depth = 0; // brace depth

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char next = (i + 1 < n ? s.charAt(i + 1) : '\0');
            char prev = (i > 0 ? s.charAt(i - 1) : '\0');

            if (inLine) {
                if (c == '\n') {
                    if (segStart < i) segs.add(new Segment(s.substring(segStart, i), segStart, i - 1));
                    segStart = i + 1;
                    inLine = false;
                }
                continue;
            }
            if (inBlock) {
                if (prev == '*' && c == '/') {
                    int end = i;
                    if (segStart <= end) segs.add(new Segment(s.substring(segStart, end + 1), segStart, end));
                    segStart = i + 1;
                    inBlock = false;
                }
                continue;
            }
            if (inStr) {
                if (!esc && c == '"') inStr = false;
                esc = (!esc && c == '\\');
                continue;
            }
            if (inChar) {
                if (!esc && c == '\'') inChar = false;
                esc = (!esc && c == '\\');
                continue;
            }

            // entering comments / strings
            if (c == '/' && next == '/') { inLine = true; i++; continue; }
            if (c == '/' && next == '*') { inBlock = true; i++; continue; }
            if (c == '"') { inStr = true; esc = false; continue; }
            if (c == '\'') { inChar = true; esc = false; continue; }

            // track top-level braces
            if (c == '{') { depth++; continue; }
            if (c == '}') {
                if (depth > 0) depth--;
                if (depth == 0) {
                    segs.add(new Segment(s.substring(segStart, i + 1), segStart, i));
                    segStart = i + 1;
                }
                continue;
            }

            // top-level semicolon ends a statement segment
            if (c == ';' && depth == 0) {
                segs.add(new Segment(s.substring(segStart, i + 1), segStart, i));
                segStart = i + 1;
            }
        }

        // tail
        if (segStart < n) {
            String tail = s.substring(segStart);
            if (!tail.trim().isEmpty()) segs.add(new Segment(tail, segStart, n - 1));
        }

        return segs.stream()
                .filter(seg -> seg.text != null && !seg.text.trim().isEmpty())
                .collect(Collectors.toList());
    }

    // ======== Statement path: variables & invocations & variable references ========
    private static void analyzeSegmentAsStatement(Segment seg, JavaParser parser, Set<SyntaxHighlight> out, Set<String> declaredVars) {
        ParseResult<Statement> pr = parser.parse(ParseStart.STATEMENT, Providers.provider(seg.text));
        Optional<Statement> stmtOpt = pr.getResult();
        if (!stmtOpt.isPresent()) return;

        Statement stmt = stmtOpt.get();
        List<Integer> lineStarts = computeLineStartOffsets(seg.text);

        // Local variable declarations (collect Finding + declared set)
        stmt.findAll(VariableDeclarationExpr.class).forEach(vdx ->
                vdx.findAll(VariableDeclarator.class).forEach(vd ->
                        vd.getName().getRange().ifPresent(r -> {
                            String name = vd.getNameAsString();
                            int startLocal = rangeStartIndex(r, lineStarts);
                            int endLocal   = startLocal + name.length();

                            out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.DECLARATION)));
                            declaredVars.add(name);
                        })
                )
        );

        // Method invocations (qualified or not)
        stmt.findAll(MethodCallExpr.class).forEach(mc ->
                mc.getName().getRange().ifPresent(r -> {
                    String name = mc.getNameAsString();
                    int startLocal = rangeStartIndex(r, lineStarts);
                    int endLocal   = startLocal + name.length();
                    out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.METHOD_INVOCATION)));
                })
        );

        // NEW: Variable references — any NameExpr that matches a declared variable
        stmt.findAll(NameExpr.class).forEach(ne ->
                ne.getName().getRange().ifPresent(r -> {
                    String name = ne.getNameAsString();
                    if (!declaredVars.contains(name)) return; // only count known declared variables

                    int startLocal = rangeStartIndex(r, lineStarts);
                    int endLocal   = startLocal + name.length();
                    out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.VARIABLE_REFERENCE)));
                })
        );
    }

    // ======== Member path: method/field/class declarations ========
    private static void analyzeSegmentAsMember(Segment seg, JavaParser parser, Set<SyntaxHighlight> out, Set<String> declaredVars) {
        // JShell-friendly: parse a single BodyDeclaration first (no wrappers)
        if (analyzeSegmentAsSingleBodyDeclaration(seg, parser, out, declaredVars)) return;

        // Member in wrapper
        if (tryMemberInWrapper(seg.text, seg, parser, out, declaredVars)) return;

        // If it looks like "method(...) { ... } ;" → retry without trailing semicolons
        String trimmed = seg.text;
        if (LOOKS_LIKE_METHOD_BLOCK_WITH_TRAILING_SEMI.matcher(trimmed).find()) {
            String noTrailingSemis = trimmed.replaceFirst("[\\s;]+$", "");
            if (tryMemberInWrapper(noTrailingSemis, seg, parser, out, declaredVars)) return;
        }

        // Last-resort: lenient method-declaration regex to capture name
        Matcher m = METHOD_DECL_LAX.matcher(seg.text);
        if (m.find()) {
            int nameStart = seg.start + m.start("name");
            int nameEnd   = seg.start + m.end("name");
            out.add(new SyntaxHighlight(nameStart, nameEnd, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.METHOD_DECLARATION)));
        }
    }

    /** Try to parse the segment as a single BodyDeclaration (method/field/class) without wrappers. */
    private static boolean analyzeSegmentAsSingleBodyDeclaration(Segment seg, JavaParser parser, Set<SyntaxHighlight> out, Set<String> declaredVars) {
        ParseResult<BodyDeclaration<?>> pr = parser.parseBodyDeclaration(seg.text);
        Optional<BodyDeclaration<?>> bdOpt = pr.getResult();
        if (!bdOpt.isPresent()) return false;

        BodyDeclaration<?> bd = bdOpt.get();
        List<Integer> lineStarts = computeLineStartOffsets(seg.text);
        boolean found = false;

        // Method declaration
        if (bd.isMethodDeclaration()) {
            MethodDeclaration md = bd.asMethodDeclaration();
            md.getName().getRange().ifPresent(r -> {
                String name = md.getNameAsString();
                int startLocal = rangeStartIndex(r, lineStarts);
                int endLocal   = startLocal + name.length();
                out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.METHOD_DECLARATION)));
            });
            found = true;
        }

        // Field declaration → variables
        if (bd.isFieldDeclaration()) {
            FieldDeclaration fd = bd.asFieldDeclaration();
            for (VariableDeclarator vd : fd.getVariables()) {
                vd.getName().getRange().ifPresent(r -> {
                    String name = vd.getNameAsString();
                    int startLocal = rangeStartIndex(r, lineStarts);
                    int endLocal   = startLocal + name.length();
                    out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.DECLARATION)));
                    declaredVars.add(name); // ⟵ NEW
                });
            }
            found = true;
        }

        // Class or interface declaration (also collect members within)
        if (bd.isClassOrInterfaceDeclaration()) {
            ClassOrInterfaceDeclaration c = bd.asClassOrInterfaceDeclaration();
            c.getName().getRange().ifPresent(r -> {
                String name = c.getNameAsString();
                int startLocal = rangeStartIndex(r, lineStarts);
                int endLocal   = startLocal + name.length();
                out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.CLASS_DECLARATION)));
            });
            // Fields
            c.findAll(VariableDeclarator.class).forEach(vd ->
                    vd.getName().getRange().ifPresent(r -> {
                        String name = vd.getNameAsString();
                        int startLocal = rangeStartIndex(r, lineStarts);
                        int endLocal   = startLocal + name.length();
                        out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.DECLARATION)));
                        declaredVars.add(name);
                    })
            );
            // Methods
            c.findAll(MethodDeclaration.class).forEach(md ->
                    md.getName().getRange().ifPresent(r -> {
                        String name = md.getNameAsString();
                        int startLocal = rangeStartIndex(r, lineStarts);
                        int endLocal   = startLocal + name.length();
                        out.add(new SyntaxHighlight(seg.start + startLocal, seg.start + endLocal, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.METHOD_DECLARATION)));
                    })
            );
            found = true;
        }

        return found;
    }

    private static boolean tryMemberInWrapper(String memberText, Segment seg, JavaParser parser, Set<SyntaxHighlight> out, Set<String> declaredVars) {
        final String PREFIX = "class __Dummy__ { ";
        final String SUFFIX = " }";
        String wrapped = PREFIX + memberText + SUFFIX;

        Optional<CompilationUnit> cuOpt = parseCU(parser, wrapped);
        if (!cuOpt.isPresent()) return false;

        CompilationUnit cu = cuOpt.get();
        List<Integer> lineStarts = computeLineStartOffsets(wrapped);
        int segStartInWrapped = PREFIX.length();
        int segEndInWrapped   = segStartInWrapped + memberText.length() - 1;

        // Method declarations
        cu.findAll(MethodDeclaration.class).forEach(md ->
                md.getName().getRange().ifPresent(r -> {
                    String name = md.getNameAsString();
                    int startWrapped = rangeStartIndex(r, lineStarts);
                    int endWrapped   = startWrapped + name.length() - 1;
                    if (startWrapped >= segStartInWrapped && endWrapped <= segEndInWrapped) {
                        int start = seg.start + (startWrapped - segStartInWrapped);
                        int end   = seg.start + (endWrapped   - segStartInWrapped);
                        out.add(new SyntaxHighlight(start, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.METHOD_DECLARATION)));
                    }
                })
        );

        // Field-style variable declarators
        cu.findAll(VariableDeclarator.class).forEach(vd ->
                vd.getName().getRange().ifPresent(r -> {
                    String name = vd.getNameAsString();
                    int startWrapped = rangeStartIndex(r, lineStarts);
                    int endWrapped   = startWrapped + name.length() - 1;
                    if (startWrapped >= segStartInWrapped && endWrapped <= segEndInWrapped) {
                        int start = seg.start + (startWrapped - segStartInWrapped);
                        int end   = seg.start + (endWrapped   - segStartInWrapped);
                        out.add(new SyntaxHighlight(start, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.DECLARATION)));
                        declaredVars.add(name);
                    }
                })
        );

        // Also capture nested class/interface declarations inside this member segment
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls ->
                cls.getName().getRange().ifPresent(r -> {
                    String name = cls.getNameAsString();
                    int startWrapped = rangeStartIndex(r, lineStarts);
                    int endWrapped   = startWrapped + name.length() - 1;
                    if (startWrapped >= segStartInWrapped && endWrapped <= segEndInWrapped) {
                        int start = seg.start + (startWrapped - segStartInWrapped);
                        int end   = seg.start + (endWrapped   - segStartInWrapped);
                        out.add(new SyntaxHighlight(start, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.CLASS_DECLARATION)));
                    }
                })
        );

        return true;
    }

    // Matches a method declaration with a block followed by trailing semicolons:
    // e.g., "static void m() {};"" or "public int m() { ... } ;"
    private static final Pattern LOOKS_LIKE_METHOD_BLOCK_WITH_TRAILING_SEMI =
            Pattern.compile("\\)\\s*\\{[\\s\\S]*?\\}\\s*;\\s*$");

    // Lenient method-declaration pattern: handles either:
    // - no-body method: "... name(...) ;"
    // - with-body (possibly followed by semicolons): "... name(...) { ... } ;?"
    // Captures the method NAME in group "name".
    private static final Pattern METHOD_DECL_LAX = Pattern.compile(
            "(?is)^(?:\\s*(?:public|protected|private|static|final|abstract|synchronized|native|strictfp|default|sealed|non-sealed|transient|volatile)\\s+)*"
                    + "(?:[\\w$\\[\\].<>?,\\s]+?)\\s+(?<name>[A-Za-z_$][\\w$]*)\\s*\\("
                    + "[^)]*\\)\\s*(?:\\{[\\s\\S]*?\\}\\s*;?\\s*|;\\s*)$"
    );

    // ======== Whole-snippet class decls (optional) ========
    private static void detectClassDeclsWholeSnippet(String snippet, JavaParser parser, Set<SyntaxHighlight> out) {
        // Try RAW first
        parseCU(parser, snippet).ifPresent(cu -> {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls ->
                    cls.getName().getRange().ifPresent(r -> {
                        String name = cls.getNameAsString();
                        int start = positionToIndex(r.begin, snippet);
                        int end   = start + name.length();
                        out.add(new SyntaxHighlight(start, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.CLASS_DECLARATION)));
                    })
            );
        });

        // If none found, try class-wrapped to discover member/local classes typed as members
        if (out.stream().noneMatch(f -> f.attributes().contains(SyntaxHighlight.SyntaxHighlightAttribute.CLASS_DECLARATION))) {
            final String PREFIX = "class __Dummy__ {\n";
            final String SUFFIX = "\n}\n";
            String wrapped = PREFIX + snippet + SUFFIX;

            parseCU(parser, wrapped).ifPresent(cu -> {
                List<Integer> lineStarts = computeLineStartOffsets(wrapped);
                int snippetStart = PREFIX.length();
                int snippetEnd   = snippetStart + snippet.length() - 1;

                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls ->
                        cls.getName().getRange().ifPresent(r -> {
                            String name = cls.getNameAsString();
                            int startWrapped = rangeStartIndex(r, lineStarts);
                            int endWrapped   = startWrapped + name.length();
                            if (startWrapped >= snippetStart && endWrapped <= snippetEnd) {
                                out.add(new SyntaxHighlight(startWrapped - snippetStart, endWrapped - snippetStart, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.CLASS_DECLARATION)));
                            }
                        })
                );
            });
        }
    }

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue",
            "default","do","double","else","enum","extends","final","finally","float","for","goto","if",
            "implements","import","instanceof","int","interface","long","native","new","package","private",
            "protected","public","return","short","static","strictfp","super","switch","synchronized","this",
            "throw","throws","transient","try","void","volatile","while",
            // JPMS:
            "module","open","opens","requires","exports","to","uses","provides","with","transitive",
            // modern:
            "var","yield","record","sealed","permits","non-sealed"
    ));

    private static List<SyntaxHighlight> findComments(String s) {
        List<SyntaxHighlight> out = new ArrayList<>();
        // Block (/* ... */), including javadoc
        Matcher mb = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(s);
        while (mb.find()) {
            int start = mb.start(), end = mb.end();
            out.add(new SyntaxHighlight(start, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.COMMENT)));
        }
        // Line (// ... EOL)
        Matcher ml = Pattern.compile("//.*?(?=\\r?\\n|$)").matcher(s);
        while (ml.find()) {
            int start = ml.start(), end = ml.end();
            if (insideAny(start, end, out)) continue;
            out.add(new SyntaxHighlight(start, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.COMMENT)));
        }
        return out;
    }

    private static boolean insideAny(int start, int end, List<SyntaxHighlight> spans) {
        for (SyntaxHighlight f : spans) {
            if (!f.attributes().contains(SyntaxHighlight.SyntaxHighlightAttribute.COMMENT)) continue;
            if (start >= f.start() && end <= f.end()) return true;
        }
        return false;
    }

    private static List<SyntaxHighlight> findKeywords(String s) {
        boolean[] masked = new boolean[s.length()];
        for (SyntaxHighlight c : findComments(s)) {
            for (int i = c.start(); i <= c.end() && i < s.length(); i++) masked[i] = true;
        }
        maskStringsAndChars(s, masked);

        List<SyntaxHighlight> out = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            while (i < n && (masked[i] || !isWordChar(s.charAt(i)))) i++;
            int start = i;
            while (i < n && !masked[i] && isWordChar(s.charAt(i))) i++;
            int end = i;
            if (start <= end) {
                    String word = s.substring(start, end);
                if (JAVA_KEYWORDS.contains(word)) {
                    out.add(new SyntaxHighlight(start, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.KEYWORD)));
                }
            }
        }
        return out;
    }

    private static void maskStringsAndChars(String s, boolean[] masked) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (masked[i]) continue;
            char c = s.charAt(i);
            if (c == '"') {
                int j = i + 1; boolean esc = false;
                while (j < n) {
                    char cj = s.charAt(j);
                    if (!esc && cj == '"') { j++; break; }
                    esc = (!esc && cj == '\\'); j++;
                }
                for (int k = i; k < j && k < n; k++) masked[k] = true;
                i = j - 1;
            } else if (c == '\'') {
                int j = i + 1; boolean esc = false;
                while (j < n) {
                    char cj = s.charAt(j);
                    if (!esc && cj == '\'') { j++; break; }
                    esc = (!esc && cj == '\\'); j++;
                }
                for (int k = i; k < j && k < n; k++) masked[k] = true;
                i = j - 1;
            }
        }
    }

    private static boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
    }

    private static Optional<CompilationUnit> parseCU(JavaParser parser, String source) {
        return parser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(source)).getResult();
    }

    private static List<Integer> computeLineStartOffsets(String s) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                if (i + 1 < s.length()) starts.add(i + 1);
            }
        }
        return starts;
    }

    private static int posToIndex(Position p, List<Integer> lineStarts) {
        int lineStart = lineStarts.get(Math.max(0, p.line - 1));
        return lineStart + (p.column - 1);
    }

    private static int rangeStartIndex(Range r, List<Integer> lineStarts) {
        return posToIndex(r.begin, lineStarts);
    }

    private static int positionToIndex(Position p, String text) {
        int line = p.line, col = p.column;
        int idx = 0, curLine = 1;
        for (int i = 0; i < text.length() && curLine < line; i++) {
            if (text.charAt(i) == '\n') { curLine++; idx = i + 1; }
        }
        return idx + (col - 1);
    }

    private static List<SyntaxHighlight> findStringsAndTextBlocks(String s, List<SyntaxHighlight> comments) {
        boolean[] masked = new boolean[s.length()];
        // mask comments
        for (SyntaxHighlight c : comments) {
            for (int i = c.start(); i < c.end() && i < s.length(); i++) masked[i] = true;
        }
        List<SyntaxHighlight> out = new ArrayList<>();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (masked[i]) continue;
            char c = s.charAt(i);
            // text block """..."""
            if (c == '"' && i + 2 < n && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"') {
                int j = i + 3;
                while (j + 2 < n) {
                    if (s.charAt(j) == '"' && s.charAt(j + 1) == '"' && s.charAt(j + 2) == '"') { j += 3; break; }
                    j++;
                }
                out.add(new SyntaxHighlight(i, Math.min(j, n), Set.of(SyntaxHighlight.SyntaxHighlightAttribute.STRING)));
                for (int k = i; k < Math.min(j, n); k++) masked[k] = true;
                i = Math.min(j, n) - 1;
                continue;
            }
            // standard string literal — stop at EOL if unterminated; also add ERROR on unterminated span
            if (c == '"') {
                int j = i + 1;
                boolean esc = false;
                boolean closed = false;
                while (j < n) {
                    char cj = s.charAt(j);
                    if (cj == '\n') break; // unterminated: stop BEFORE newline
                    if (!esc && cj == '"') { j++; closed = true; break; }
                    esc = (!esc && cj == '\\'); j++;
                }
                int end = Math.min(j, n);
                if (closed) {
                    out.add(new SyntaxHighlight(i, end, Set.of(SyntaxHighlight.SyntaxHighlightAttribute.STRING)));
                }

                for (int k = i; k < end; k++) masked[k] = true;
                i = end - 1;
            }
        }
        return out;
    }
}