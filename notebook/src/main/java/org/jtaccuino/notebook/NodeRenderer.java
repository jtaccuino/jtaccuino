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
import javafx.scene.Node;

/**
 * Turns an object returned from a cell into something displayable.
 *
 * <p>This is a service provider interface: implementations live wherever the
 * types they understand live (the IDE ships renderers for collections, arrays,
 * {@code BufferedImage} and so on) and are discovered at runtime. A consumer
 * with no renderers on the classpath still works, it just cannot display
 * anything.
 */
public interface NodeRenderer<T> {

    Optional<Node> render(T object);

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Descriptor {

        Class<?> type();
    }
}
