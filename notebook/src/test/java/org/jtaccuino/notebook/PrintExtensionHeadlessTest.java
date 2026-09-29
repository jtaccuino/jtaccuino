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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.ReactiveJShellProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Proves the point of moving {@code print} and {@code println} into this
 * module: a consumer with no IDE on its classpath still gets them.
 *
 * <p>This test is only meaningful because the {@code notebook} test runtime
 * classpath does not contain {@code ui}. If someone put the extensions back
 * there, or left them registered from a service file that {@code ui} owns, every
 * test below would still pass while the actual claim of the module quietly
 * stopped being true — so the negative case is asserted explicitly at the end.
 */
class PrintExtensionHeadlessTest {

    @BeforeAll
    static void startToolkit() {
        // println defers its work with Platform.runLater, so a toolkit has to
        // exist even though nothing is ever shown.
        var latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyRunning) {
            latch.countDown();
        }
        try {
            assertTrue(latch.await(60, TimeUnit.SECONDS), "toolkit did not start");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void theIdeIsNotOnThisClasspath() {
        // The premise of the whole module. Asserted so that a later change
        // which quietly adds ui to the test dependencies cannot turn this
        // class into a test that no longer tests anything.
        assertThrowsClassNotFound("org.jtaccuino.core.ui.extensions.DisplayExtension");
        assertThrowsClassNotFound("org.jtaccuino.core.ui.JavaCellFactory");
    }

    @Test
    void printlnIsRegisteredAsASystemExtensionWithoutTheIde() {
        var shell = newShell();
        var extension = shell.getExtension(PrintExtension.class);
        assertTrue(extension != null, "PrintExtension was not activated as a SYSTEM extension");
    }

    @Test
    void printlnIsReachableFromJShell() {
        var shell = newShell();
        var result = shell.eval("println(\"hello from jshell\");");
        assertTrue(result.status().isSuccess(), "evaluation failed: " + result.snippetEventsCurrent());
    }

    @Test
    void printlnPersistsToTheCellItWasGiven() throws Exception {
        var shell = newShell();
        var printExtension = shell.getExtension(PrintExtension.class);
        var cell = CellData.of(CellData.Type.CODE, "println(\"recorded\");", UUID.randomUUID());
        printExtension.setCurrentCellData(cell);

        var result = shell.eval("println(\"recorded\");");
        assertTrue(result.status().isSuccess(), result.snippetEventsCurrent().toString());
        awaitFxThread();

        var streams = cell.getOutputData().stream()
                .filter(CellData.StreamBasedOutputData.class::isInstance)
                .map(CellData.StreamBasedOutputData.class::cast)
                .toList();

        assertEquals(1, streams.size(), "expected exactly one stream output, got " + cell.getOutputData());
        assertEquals("recorded\n", streams.getFirst().data());
    }

    @Test
    void consecutivePrintlnsAccumulateIntoOneOutput() throws Exception {
        var shell = newShell();
        var printExtension = shell.getExtension(PrintExtension.class);
        var cell = CellData.of(CellData.Type.CODE, "println(\"a\"); println(\"b\");", UUID.randomUUID());
        printExtension.setCurrentCellData(cell);

        shell.eval("println(\"a\");");
        shell.eval("println(\"b\");");
        awaitFxThread();

        var streams = cell.getOutputData().stream()
                .filter(CellData.StreamBasedOutputData.class::isInstance)
                .map(CellData.StreamBasedOutputData.class::cast)
                .toList();

        // each call replaces the previous output rather than appending a new entry
        assertEquals(1, streams.size(), "expected one accumulated output, got " + cell.getOutputData());
        assertEquals("a\nb\n", streams.getFirst().data());
    }

    @Test
    void printDoesNotAddANewline() throws Exception {
        var shell = newShell();
        var printExtension = shell.getExtension(PrintExtension.class);
        var cell = CellData.of(CellData.Type.CODE, "print(\"x\");", UUID.randomUUID());
        printExtension.setCurrentCellData(cell);

        shell.eval("print(\"x\");");
        awaitFxThread();

        var stream = cell.getOutputData().stream()
                .filter(CellData.StreamBasedOutputData.class::isInstance)
                .map(CellData.StreamBasedOutputData.class::cast)
                .findFirst()
                .orElse(null);

        assertInstanceOf(CellData.StreamBasedOutputData.class, stream);
        assertEquals("x", stream.data());
    }

    @Test
    void aPrintSinkSeesTheSameTextThatIsPersisted() throws Exception {
        var shell = newShell();
        var printExtension = shell.getExtension(PrintExtension.class);
        var cell = CellData.of(CellData.Type.CODE, "println(\"both\");", UUID.randomUUID());
        printExtension.setCurrentCellData(cell);

        var seen = new ArrayList<String>();
        printExtension.setPrintSink(seen::add);

        shell.eval("println(\"both\");");
        awaitFxThread();

        var stream = cell.getOutputData().stream()
                .filter(CellData.StreamBasedOutputData.class::isInstance)
                .map(CellData.StreamBasedOutputData.class::cast)
                .findFirst()
                .orElseThrow();

        // this equality is the guarantee that a headless export and the IDE
        // cannot disagree about what a cell printed
        assertEquals(List.of(stream.data()), seen);
    }

    private static ReactiveJShell newShell() {
        return ReactiveJShellProvider.createReactiveShell(UUID.randomUUID(), null);
    }

    private static void awaitFxThread() throws Exception {
        var latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(60, TimeUnit.SECONDS), "FX task did not complete");
    }

    private static void assertThrowsClassNotFound(String className) {
        try {
            Class.forName(className);
            throw new AssertionError(className + " is on the classpath, so this test proves nothing");
        } catch (ClassNotFoundException expected) {
            // the point of the assertion
        }
    }
}
