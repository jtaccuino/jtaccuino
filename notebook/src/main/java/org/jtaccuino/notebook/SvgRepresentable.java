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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

/**
 * Offers a vector rendering of an object that would otherwise be rasterised.
 *
 * <p>JavaFX has no general {@code Node} to SVG conversion — {@code snapshot()}
 * is raster only — so vector output cannot be derived from the stored PNG. It
 * has to be a capability the renderer itself opts into, which is what this
 * interface is for.
 *
 * <p>SVG is export-only. It is never stored in the notebook and the IDE never
 * uses it, because the IDE draws the node: the SVG exists purely so an exported
 * document can carry a scalable image rather than a screenshot.
 *
 * <p>This is the least implemented of the representation SPIs. Nothing on the
 * classpath provides it yet, and a consumer that finds no provider simply keeps
 * using the PNG. It exists now so that a charting integration has somewhere to
 * plug in without another round of plumbing.
 */
public interface SvgRepresentable<T> {

    /**
     * Renders the object as SVG.
     *
     * @param object never {@code null}
     * @return a complete SVG document, or empty if this renderer does not
     * handle this particular value
     */
    Optional<String> toSvg(T object);

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Descriptor {

        Class<?> type();
    }
}
