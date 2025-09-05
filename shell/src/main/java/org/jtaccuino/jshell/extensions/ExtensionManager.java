/*
 * Copyright 2024-2025 JTaccuino Contributors
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
package org.jtaccuino.jshell.extensions;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provide access to extension instances for shell usage
 */
public final class ExtensionManager {

    private ExtensionManager() {}

    private static final Map<UUID, Map<Class<? extends JShellExtension>, JShellExtension>> EXTENSION_REGISTRY = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends JShellExtension> T lookup(Class<T> extensionClass, UUID shellUuid) {
        return (T) EXTENSION_REGISTRY.getOrDefault(shellUuid, Collections.emptyMap()).get(extensionClass);
    }

    public static void register(JShellExtension extension, UUID shellUuid) {
        EXTENSION_REGISTRY.computeIfAbsent(shellUuid, uuid -> new ConcurrentHashMap<>()).put(extension.getClass(), extension);
    }

    public static void cleanup(UUID shellUuid) {
        EXTENSION_REGISTRY.remove(shellUuid);
    }
}
