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
package org.jtaccuino.core.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the toolkit lifetime that the other JavaFX tests rely on.
 *
 * <p>Hiding the last {@link Stage} used to end the runtime, which made every
 * subsequent {@link Platform#runLater(Runnable)} disappear without a trace and
 * fail the next test with a bare "FX task did not complete" timeout. Both steps
 * live in one test method because the order is the whole point of the check.
 */
class FxTestRuntimeTest {

    @BeforeAll
    static void startToolkit() {
        FxTestRuntime.start();
    }

    @Test
    void keepsTheToolkitAliveAfterTheLastStageWasHidden() throws InterruptedException {
        assertTrue(runOnFxThread(() -> {
            var stage = new Stage();
            stage.setScene(new Scene(new StackPane(new Button("probe")), 120, 80));
            stage.show();
            stage.hide();
        }), "showing and hiding a stage should complete");

        assertTrue(runOnFxThread(() -> {
        }), "the FX thread must still accept work after the last stage was hidden");
    }

    private static boolean runOnFxThread(Runnable task) throws InterruptedException {
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                task.run();
            } finally {
                latch.countDown();
            }
        });
        return latch.await(10, TimeUnit.SECONDS);
    }
}
