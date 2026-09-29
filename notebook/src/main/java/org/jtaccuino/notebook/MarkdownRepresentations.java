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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Comparator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Finds the {@link MarkdownRepresentable} that best fits an object and applies
 * it.
 *
 * <p>The lookup mirrors the one {@code DisplayExtension} uses for
 * {@link NodeRenderer}: providers are discovered through the
 * {@link ServiceLoader}, filtered to those whose declared type is assignable
 * from the object, then ordered so the most specific type wins — otherwise
 * {@code Object.class} would match everything and the general renderer could
 * shadow the one written for the actual type.
 */
public final class MarkdownRepresentations {

    private static final Logger LOG = Logger.getLogger(MarkdownRepresentations.class.getName());

    @SuppressWarnings("rawtypes")
    private static final Comparator<ServiceLoader.Provider<MarkdownRepresentable>> MOST_SPECIFIC_FIRST
            = Comparator.comparing(
                    (ServiceLoader.Provider<MarkdownRepresentable> p) -> p.type().getAnnotation(MarkdownRepresentable.Descriptor.class).type(),
                    (Class<?> o1, Class<?> o2) -> o1.isAssignableFrom(o2) ? 1 : -1);

    private MarkdownRepresentations() {
    }

    /**
     * Renders an object as markdown, if anything on the classpath can.
     *
     * @param object the object that was displayed, may be {@code null}
     * @return the markdown rendering of the object, or empty when nothing on the
     * classpath handles its type, or the renderer that matched declines it
     */
    @SuppressWarnings("rawtypes")
    public static Optional<String> of(Object object) {
        if (null == object) {
            return Optional.empty();
        }
        return ServiceLoader.load(MarkdownRepresentable.class)
                .stream()
                .filter(p -> p.type().getAnnotation(MarkdownRepresentable.Descriptor.class)
                        .type()
                        .isAssignableFrom(object.getClass()))
                .sorted(MOST_SPECIFIC_FIRST)
                .findFirst()
                .flatMap(p -> render(p, object));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<String> render(ServiceLoader.Provider<MarkdownRepresentable> provider, Object object) {
        Class<?> argumentType = provider.type().getAnnotation(MarkdownRepresentable.Descriptor.class).type();
        try {
            MethodHandle toMarkdown = MethodHandles.publicLookup()
                    .findVirtual(provider.type(), "toMarkdown", MethodType.methodType(Optional.class, argumentType));
            return (Optional<String>) toMarkdown.invoke(provider.get(), object);
        } catch (Throwable ex) {
            // a broken renderer must not take the export down with it; the
            // caller falls back to whatever representation it has
            LOG.log(Level.SEVERE, "markdown rendering failed for " + object.getClass(), ex);
            return Optional.empty();
        }
    }
}
