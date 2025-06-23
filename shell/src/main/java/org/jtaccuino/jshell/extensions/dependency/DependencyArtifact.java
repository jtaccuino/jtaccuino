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
package org.jtaccuino.jshell.extensions.dependency;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public record DependencyArtifact(String groupId, String artifactId, String classifier,
        String baseVersion, String version, Path path, List<DependencyArtifact> dependencies) {

    public Stream<DependencyArtifact> stream() {
        return Stream.concat(Stream.of(this),
                dependencies().stream()
                        .flatMap(DependencyArtifact::stream)
        );
    }

    public String identifier() {
        return groupId() + ":" + artifactId();
    }
}
