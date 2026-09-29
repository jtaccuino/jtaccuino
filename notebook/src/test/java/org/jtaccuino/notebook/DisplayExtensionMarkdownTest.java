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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Covers the markdown branch of {@code display(...)}: when an object cannot be
 * turned into a node, the output is recorded as markdown text rather than
 * rasterised.
 *
 * <p>The point is not that a message is produced. It is that a message stays
 * text. Storing it as a PNG would leave the notebook holding an unsearchable
 * image where a sentence was meant to be, and a headless export would have no
 * way to recover the words.
 */
class DisplayExtensionMarkdownTest {

    @BeforeAll
    static void startToolkit() {
        var latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyRunning) {
            latch.countDown();
        }
        await(latch);
        Platform.setImplicitExit(false);
    }

    @Test
    void anObjectWithNoRendererIsRecordedAsMarkdown() throws Exception {
        var extension = newExtension();
        var cell = CellData.of(CellData.Type.CODE, "display(unsupported)", UUID.randomUUID());
        extension.setCurrentCellData(cell);

        var result = new AtomicReference<DisplayResult>();
        extension.setDisplaySink(r -> result.set(r));

        display(extension, new Unrenderable());
        awaitFxThread();

        var markdown = assertInstanceOf(DisplayResult.Markdown.class, result.get());
        assertTrue(markdown.markdown().contains("Unrenderable"),
                "the message should name the type it could not convert: " + markdown.markdown());
    }

    @Test
    void theRecordedOutputIsTextAndNotAPng() throws Exception {
        var extension = newExtension();
        var cell = CellData.of(CellData.Type.CODE, "display(unsupported)", UUID.randomUUID());
        extension.setCurrentCellData(cell);
        extension.setDisplaySink(r -> {
        });

        display(extension, new Unrenderable());
        awaitFxThread();

        var outputs = cell.getOutputData().stream()
                .map(CellData.MimeTypeBasedOutputData.class::cast)
                .toList();
        assertEquals(1, outputs.size(), "expected one output, got " + cell.getOutputData());

        var bundle = outputs.getFirst().mimeBundle();
        assertEquals(Map.of("text/markdown", outputs.getFirst().mimeBundle().get("text/markdown")), bundle,
                "the bundle should hold exactly one text/markdown entry");
        assertTrue(bundle.containsKey("text/markdown"), "expected text/markdown, got " + bundle.keySet());
        assertTrue(bundle.keySet().stream().noneMatch(k -> k.startsWith("image/")),
                "a text result must not also be rasterised: " + bundle.keySet());
    }

    @Test
    void theSinkSeesTheSameMarkdownThatIsPersisted() throws Exception {
        var extension = newExtension();
        var cell = CellData.of(CellData.Type.CODE, "display(unsupported)", UUID.randomUUID());
        extension.setCurrentCellData(cell);

        var seen = new AtomicReference<DisplayResult>();
        extension.setDisplaySink(seen::set);

        display(extension, new Unrenderable());
        awaitFxThread();

        var persisted = cell.getOutputData().stream()
                .map(CellData.MimeTypeBasedOutputData.class::cast)
                .map(CellData.MimeTypeBasedOutputData::mimeBundle)
                .map(b -> b.get("text/markdown"))
                .findFirst()
                .orElseThrow();

        assertEquals(persisted, ((DisplayResult.Markdown) seen.get()).markdown(),
                "what the sink showed and what the notebook stored must be the same text");
    }

    @Test
    void theDisplayedObjectIsCarriedAlongForExport() throws Exception {
        var extension = newExtension();
        var cell = CellData.of(CellData.Type.CODE, "display(x)", UUID.randomUUID());
        extension.setCurrentCellData(cell);

        var result = new AtomicReference<DisplayResult>();
        extension.setDisplaySink(r -> result.set(r));

        var displayed = new Unrenderable();
        display(extension, displayed);
        awaitFxThread();

        // the exporter needs the original object to derive a richer
        // representation later, so it must survive resolution
        assertEquals(displayed, result.get().displayed());
    }

    @Test
    void convertToNodeStillReportsTheFailureToCallersThatAskForANode() {
        var extension = newExtension();
        // unlike display(), which falls back to markdown, an explicit request
        // for a node has nothing to return and must say so
        assertThrowsNoSuchElement(() -> extension.convertToNode(new Unrenderable(), null));
    }

    private static void assertThrowsNoSuchElement(Runnable action) {
        try {
            action.run();
        } catch (NoSuchElementException expected) {
            return;
        }
        throw new AssertionError("expected a NoSuchElementException");
    }

    private static void display(DisplayExtension extension, Object value) {
        // Called straight from the test thread on purpose: display() resolves
        // the object on the calling thread and only then defers to the JavaFX
        // thread, which is what the JShell worker thread does. Wrapping this in
        // runLater would queue a second deferred task behind the await below.
        extension.display(value, null);
    }

    private static DisplayExtension newExtension() {
        return new DisplayExtension.Factory().createExtension(null);
    }

    private static void awaitFxThread() throws Exception {
        var latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        await(latch);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(60, TimeUnit.SECONDS), "FX task did not complete");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    /** Nothing on the classpath renders this to a node. */
    private static final class Unrenderable {

        @Override
        public String toString() {
            return "unrenderable";
        }
    }
}
