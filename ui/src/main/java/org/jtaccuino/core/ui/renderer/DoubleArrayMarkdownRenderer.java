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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jtaccuino.notebook.MarkdownRepresentable;
import org.jtaccuino.notebook.MarkdownRepresentable.Descriptor;

/**
 * Renders a double[] as a one column markdown table, mirroring
 * {@link DoubleArrayRenderer}, which shows it as a list.
 */
@Descriptor(type = double[].class)
public class DoubleArrayMarkdownRenderer implements MarkdownRepresentable<double[]> {

    @Override
    public Optional<String> toMarkdown(double[] values) {
        if (values.length == 0) {
            return Optional.empty();
        }
        return Optional.of(MarkdownTables.ofValues(List.of(Arrays.stream(values).boxed().toArray(Double[]::new))));
    }
}
