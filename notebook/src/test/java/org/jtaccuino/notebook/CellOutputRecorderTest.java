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
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pins the text the notebook records for evaluated cells. This is the single
 * source of that text for both the IDE and a headless export, so a change here
 * changes what every already-executed notebook looks like once it is exported
 * again.
 */
class CellOutputRecorderTest {

    @Test
    void resultTextPrefixesTheType() {
        assertEquals("int: 42", CellOutputRecorder.resultText("int", "42"));
    }

    @Test
    void resultTextUnescapesNewlinesAndQuotes() {
        assertEquals("java.lang.String: a\nb\"c\"",
                CellOutputRecorder.resultText("java.lang.String", "a\\nb\\\"c\\\""));
    }

    @Test
    void exceptionTextUnwrapsTheCause() {
        var wrapped = new RuntimeException("wrapper", new IllegalStateException("boom"));
        var text = CellOutputRecorder.exceptionText(wrapped);
        assertTrue(text.startsWith("java.lang.IllegalStateException: boom"), text);
    }

    @Test
    void exceptionTextFallsBackToTheExceptionItself() {
        var text = CellOutputRecorder.exceptionText(new IllegalStateException("boom"));
        assertTrue(text.startsWith("java.lang.IllegalStateException: boom"), text);
    }

    @Test
    void exceptionTextAppendsStackFrames() {
        var text = CellOutputRecorder.exceptionText(new IllegalStateException("boom"));
        // frames follow the message, each on its own line, tab indented
        assertTrue(text.contains("\n\t" + CellOutputRecorderTest.class.getName()), text);
    }

    @Test
    void unresolvedTextListsTheDeclarations() {
        assertEquals("Declaration not useable until\n    int x\n    int y\nare defined",
                CellOutputRecorder.unresolvedText("    int x\n    int y\n"));
    }

    @Test
    void recordAppendsAPlainTextDisplayEntry() {
        var cell = CellData.of(CellData.Type.CODE, "1 + 1", UUID.randomUUID());
        CellOutputRecorder.record(cell, "int: 2");

        assertEquals(1, cell.getOutputData().size());
        var output = cell.getOutputData().getFirst();
        assertEquals(CellData.OutputData.OutputType.DISPLAY_DATA, output.type());
        var mimeBased = assertInstanceOf(CellData.MimeTypeBasedOutputData.class, output);
        assertEquals(Map.of("text/plain", "int: 2"), mimeBased.mimeBundle());
    }

    @Test
    void recordAppendsInOrder() {
        var cell = CellData.of(CellData.Type.CODE, "boom", UUID.randomUUID());
        CellOutputRecorder.record(cell, "first");
        CellOutputRecorder.record(cell, "second");

        assertEquals(2, cell.getOutputData().size());
    }
}
