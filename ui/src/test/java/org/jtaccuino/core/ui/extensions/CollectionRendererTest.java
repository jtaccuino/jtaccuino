/*
 * Copyright 2025 JTaccuino Contributors
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

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CollectionRendererTest {

    public CollectionRendererTest() {
    }

    @BeforeAll
    static void initFxRuntime() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            latch.countDown();
        });
        latch.await();
    }

    @AfterAll
    static void shutdownFxRuntime() throws InterruptedException {
        Platform.exit();
    }

    @Test
    public void testRenderingOfListOfRecords() {
        var r = new TestRecord("Hallo", LocalDate.of(1969, Month.SEPTEMBER, 1));
        var list = List.of(r);
        DisplayExtension ext = new DisplayExtension.Factory().createExtension(null);
        var result = ext.convertToNode(list, null);
        assertNotNull(result);
        assertEquals(TableView.class, result.getClass());
    }

    @Test
    public void testRenderingOfListOfMapEntries() {
        var map = Map.of("Hi", 5.0d, "ho", 10.0d);
        var list = map.entrySet().stream().toList();
        DisplayExtension ext = new DisplayExtension.Factory().createExtension(null);
        var result = ext.convertToNode(list, null);
        assertNotNull(result);
        assertEquals(TableView.class, result.getClass());
    }

    @Test
    public void testRenderingOfSetOfRecords() {
        var r = new TestRecord("Hallo", LocalDate.of(1969, Month.SEPTEMBER, 1));
        var set = Set.of(r);
        DisplayExtension ext = new DisplayExtension.Factory().createExtension(null);
        var result = ext.convertToNode(set, null);
        assertNotNull(result);
        assertEquals(TableView.class, result.getClass());
    }

    @Test
    public void testRenderingOfSetOfMapEntries() {
        var map = Map.of("Hi", 5.0d, "ho", 10.0d);
        var set = map.entrySet();
        DisplayExtension ext = new DisplayExtension.Factory().createExtension(null);
        var result = ext.convertToNode(set, null);
        assertNotNull(result);
        assertEquals(TableView.class, result.getClass());
    }

    record TestRecord(String name, LocalDate birthday) {}
}
