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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

/**
 * Covers the recorder that lets an export reach the objects the IDE displayed.
 *
 * <p>Two behaviours matter and are easy to get wrong: the delegate is
 * retargeted per execution, because every cell has its own output box, and
 * objects are attributed to the cell that was executing rather than to the
 * most recent cell overall.
 */
class RecordingDisplaySinkTest {

    @Test
    void remembersWhatWasDisplayedForTheCellThatWasRunning() {
        var recorder = new RecordingDisplaySink();
        var cell = cell();
        recorder.startCell(cell);

        var displayed = List.of("a", "b");
        recorder.accept(new DisplayResult.Markdown("shown", displayed));

        assertEquals(List.of(displayed), recorder.displayedIn(cell));
    }

    @Test
    void forwardsToTheDelegateThatIsCurrentlyInstalled() {
        var shown = new ArrayList<DisplayResult>();
        var recorder = new RecordingDisplaySink();
        recorder.setDelegate(shown::add);
        recorder.startCell(cell());

        var result = new DisplayResult.Markdown("shown", "x");
        recorder.accept(result);

        assertEquals(List.of(result), shown);
    }

    @Test
    void retargetingTheDelegateKeepsAccumulating() {
        // one recorder serves the whole sheet, but the widget differs per cell,
        // so the delegate is swapped and the history has to survive that
        var firstBox = new ArrayList<DisplayResult>();
        var secondBox = new ArrayList<DisplayResult>();
        var recorder = new RecordingDisplaySink();
        var first = cell();
        var second = cell();

        recorder.setDelegate(firstBox::add);
        recorder.startCell(first);
        recorder.accept(new DisplayResult.Markdown("one", "one"));

        recorder.setDelegate(secondBox::add);
        recorder.startCell(second);
        recorder.accept(new DisplayResult.Markdown("two", "two"));

        assertEquals(1, firstBox.size(), "the first cell's output went to the wrong box");
        assertEquals(1, secondBox.size(), "the second cell's output went to the wrong box");
        assertEquals(List.of("one"), recorder.displayedIn(first));
        assertEquals(List.of("two"), recorder.displayedIn(second));
    }

    @Test
    void aCellThatWasNeverExecutedHasNothing() {
        var recorder = new RecordingDisplaySink();
        recorder.startCell(cell());
        recorder.accept(new DisplayResult.Markdown("x", "x"));

        assertTrue(recorder.displayedIn(cell()).isEmpty());
    }

    @Test
    void twoCellsWithTheSameContentAreStillTwoCells() {
        // CellData does not override equals, and the registry is keyed by
        // identity, so a value-based lookup would merge these two
        var recorder = new RecordingDisplaySink();
        var first = CellData.of(CellData.Type.CODE, "same", UUID.randomUUID());
        var second = CellData.of(CellData.Type.CODE, "same", UUID.randomUUID());

        recorder.startCell(first);
        recorder.accept(new DisplayResult.Markdown("x", "first-object"));
        recorder.startCell(second);
        recorder.accept(new DisplayResult.Markdown("x", "second-object"));

        assertEquals(List.of("first-object"), recorder.displayedIn(first));
        assertEquals(List.of("second-object"), recorder.displayedIn(second));
    }

    @Test
    void recordingWorksWithNoDelegateAtAll() {
        // a headless consumer has nothing to show, but the registry must still
        // be populated for the export
        var recorder = new RecordingDisplaySink();
        var cell = cell();
        recorder.startCell(cell);

        recorder.accept(new DisplayResult.Graphical(new Region(), "object"));

        assertEquals(List.of("object"), recorder.displayedIn(cell));
    }

    private static CellData cell() {
        return CellData.of(CellData.Type.CODE, "display(x);", UUID.randomUUID());
    }
}
