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
package org.jtaccuino.cli;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

/**
 * Starts the JavaFX toolkit for a headless run.
 *
 * <p>{@code println} and {@code display} persist their output on the JavaFX
 * application thread and {@code display} snapshots a node, so the toolkit has
 * to exist. It runs with the headless Glass platform and software Prism, set as
 * JVM properties by the launcher, so no window system, display or GPU is
 * involved and no window is ever shown.
 */
final class JavaFxRuntime {

    private static final long STARTUP_TIMEOUT_SECONDS = 60;

    private JavaFxRuntime() {
    }

    static void start() {
        // The installed scripts pass these as JVM arguments. A jar started
        // through jbang or `java -jar` does not, so default them here; an
        // explicit -D still wins. They must be set before the toolkit starts.
        System.setProperty("glass.platform", System.getProperty("glass.platform", "headless"));
        System.setProperty("prism.order", System.getProperty("prism.order", "sw"));

        var latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                latch.countDown();
            });
        } catch (IllegalStateException alreadyRunning) {
            // a test JVM or an embedding host may already have started it
            Platform.setImplicitExit(false);
            return;
        }
        try {
            if (!latch.await(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the JavaFX toolkit did not start within "
                        + STARTUP_TIMEOUT_SECONDS + "s");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while starting the JavaFX toolkit", interrupted);
        }
    }
}
