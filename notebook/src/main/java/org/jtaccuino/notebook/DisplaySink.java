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

/**
 * Receives what a call to {@code display(...)} resolved to, for the cell that
 * is currently executing.
 *
 * <p>Like {@link PrintSink} this exists to separate "what to show" from "how
 * to show it": the extension converts the object, the sink attaches the result.
 * The IDE sink adds a {@link DisplayResult.Graphical} node to the cell's
 * output box and renders a {@link DisplayResult.Markdown} result with its
 * markdown renderer; a headless sink does nothing at all, since the notebook
 * already holds the result.
 *
 * <p><strong>Threading.</strong> Implementations are always called on the JavaFX
 * application thread.
 *
 * <p><strong>Ordering.</strong> A graphical result is snapshotted immediately
 * after this call returns. A sink that needs the node laid out before it is
 * rasterised (which every implementation does, because layout is synchronous on
 * the node being snapshotted) should therefore attach it synchronously rather
 * than deferring.
 */
@FunctionalInterface
public interface DisplaySink {

    /**
     * Attaches the result somewhere it can be seen, or at least measured.
     *
     * @param result what the displayed object resolved to, never {@code null}
     */
    void accept(DisplayResult result);
}
