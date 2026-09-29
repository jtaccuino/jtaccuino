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
package org.jtaccuino.core.ui.renderer;

import java.util.Optional;
import org.jtaccuino.notebook.SvgRepresentable;
import org.jtaccuino.notebook.SvgRepresentable.Descriptor;

/**
 * A provider that exists only for tests, because nothing in production offers
 * SVG yet. It lets the export precedence be exercised without inventing a real
 * charting integration just to have a subject.
 */
@Descriptor(type = TestSvgSubject.class)
public class TestSvgRepresentable implements SvgRepresentable<TestSvgSubject> {

    @Override
    public Optional<String> toSvg(TestSvgSubject object) {
        return Optional.of("<svg width=\"" + object.size() + "\"></svg>");
    }
}
