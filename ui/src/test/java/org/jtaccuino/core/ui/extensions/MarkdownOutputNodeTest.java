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
package org.jtaccuino.core.ui.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import jfx.incubator.scene.control.richtext.RichTextArea;
import org.jtaccuino.core.ui.markdown.MarkdownStyle;
import org.jtaccuino.core.ui.FxTestRuntime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks that markdown output picks up the same CSS fonts as a rendered
 * markdown cell.
 *
 * <p>This is the part of {@link MarkdownOutputNode} that is easy to get wrong:
 * the fonts are styleable properties, and CSS is only applied once the node is
 * in a styled scene. Reading them earlier returns the JavaFX defaults, which
 * looks plausible and is silently wrong, so the assertion is that the resolved
 * value differs from the default.
 */
class MarkdownOutputNodeTest {

    @BeforeAll
    static void startToolkit() {
        FxTestRuntime.start();
    }

    @Test
    void theNodeCarriesTheMarkdownCellStyleClass() {
        // this is what makes the .markdown-cell CSS rule apply to output
        var node = new MarkdownOutputNode("hello");
        assertTrue(node.getStyleClass().contains("markdown-cell"), node.getStyleClass().toString());
        assertTrue(node.getStyleClass().contains("markdown-output"), node.getStyleClass().toString());
    }

    @Test
    void fontsComeFromCssAndNotFromTheJavaFxDefaults() throws Exception {
        var style = styleOf("# heading\n\nsome **text**");

        var expected = Font.font("Arial", 12);
        assertEquals(expected.getFamily(), style.base().getFamily(),
                "base font family should come from CSS");
        assertEquals(expected.getSize(), style.base().getSize(), 0.001,
                "base font size should come from CSS");
        // Deliberately no comparison against Font.getDefault(): on Windows the
        // default size is 12, exactly the CSS value, so "differs from default"
        // would fail even though the stylesheet took effect. The family above
        // and the heading and monospace sizes below are the platform
        // independent proof that the CSS was applied.
        assertEquals(6, style.headings().size());
        assertEquals(24.0, style.heading(1).getSize(), 0.001, "heading one should be 24px");
        assertEquals(13.0, style.monospace().getSize(), 0.001, "monospace should be 13px");
    }

    @Test
    void theMarkdownIsRenderedIntoAReadOnlyArea() throws Exception {
        var root = new StackPane();
        var node = new MarkdownOutputNode("hello **world**");
        root.getChildren().add(node);
        styledScene(root);

        var text = new AtomicReference<String>();
        onFxThread(() -> {
            root.applyCss();
            root.layout();
            var area = (RichTextArea) node.lookup(".markdown-render");
            assertNotNull(area, "no rendered markdown area was created");
            assertFalse(area.isEditable(), "output markdown must not be editable");
            text.set(area.getModel().getPlainText(0));
        });

        assertTrue(text.get().contains("hello"), "rendered text was: " + text.get());
        assertTrue(text.get().contains("world"), "rendered text was: " + text.get());
    }

    @Test
    void settingNewMarkdownRerendersIntoTheSameArea() throws Exception {
        var root = new StackPane();
        var node = new MarkdownOutputNode("before");
        root.getChildren().add(node);
        styledScene(root);

        var first = new AtomicReference<String>();
        onFxThread(() -> {
            root.applyCss();
            root.layout();
            first.set(((RichTextArea) node.lookup(".markdown-render")).getModel().getPlainText(0));
        });
        assertTrue(first.get().contains("before"), first.get());

        node.setMarkdown("after");
        var second = new AtomicReference<String>();
        onFxThread(() -> {
            root.applyCss();
            root.layout();
            second.set(((RichTextArea) node.lookup(".markdown-render")).getModel().getPlainText(0));
        });

        assertTrue(second.get().contains("after"), "new markdown was not rendered: " + second.get());
        assertFalse(second.get().contains("before"), "the old text is still there: " + second.get());
    }

    private static MarkdownStyle styleOf(String markdown) throws Exception {
        var root = new StackPane();
        var node = new MarkdownOutputNode(markdown);
        root.getChildren().add(node);
        styledScene(root);
        onFxThread(() -> {
            root.applyCss();
            node.applyCss();
        });
        return node.resolvedStyle();
    }

    private static Scene styledScene(StackPane root) {
        var scene = new Scene(root, 400, 300);
        var css = Path.of("src/test/resources/org/jtaccuino/core/ui/extensions/markdown-output-test.css");
        scene.getStylesheets().add(css.toUri().toString());
        return scene;
    }

    private static void onFxThread(Runnable action) throws Exception {
        var latch = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(60, TimeUnit.SECONDS), "FX task did not complete");
        if (failure.get() != null) {
            throw new AssertionError("failed on the FX thread", failure.get());
        }
    }
}
