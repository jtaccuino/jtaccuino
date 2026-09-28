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

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;

/**
 * Starts the JavaFX toolkit once per test JVM.
 *
 * <p>The toolkit must not be shut down between test classes: the font factory
 * is torn down by {@link Platform#exit()}, after which {@code Font.font(...)}
 * fails with a {@link NullPointerException}. Gradle terminates the worker
 * process at the end of the run, so there is nothing to clean up here.
 */
public final class FxTestRuntime {

    private FxTestRuntime() {
    }

    public static void start() {
        try {
            var latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException alreadyStarted) {
            // The toolkit is already running for this test JVM.
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
