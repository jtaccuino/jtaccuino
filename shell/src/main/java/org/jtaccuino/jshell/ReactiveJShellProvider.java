/*
 * Copyright 2024 JTaccuino Contributors
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
package org.jtaccuino.jshell;

import java.nio.file.Path;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.UUID;
import org.jtaccuino.jshell.extensions.ExtensionManager;
import org.jtaccuino.jshell.extensions.JShellExtension;

/**
 * Works as a factory for an async (reactive) extensible JShell Wrapper
 */
public class ReactiveJShellProvider {

    private static final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    private static final boolean IS_WINDOWS = osName.contains("windows");

    public static ReactiveJShell createReactiveShell(UUID uuid, Path path) {
        ReactiveJShell rjs = ReactiveJShell.create(uuid);
        return initShell(rjs, uuid, path);
    }

    private static ReactiveJShell initShell(ReactiveJShell rjs, UUID uuid, Path path) {

        var uuidInitSource = "var _$jsci$uuid = java.util.UUID.fromString(\"" + uuid + "\");";

        ReactiveJShell.EvaluationResult evalResult = rjs.eval(uuidInitSource);
        rjs.getWrappedShell().onSnippetEvent((t) -> {
            if (t.snippet().source().equals(uuidInitSource)) {
                System.out.println("Init uuid status changed from " + t.previousStatus() + " to : " + t.status());
                System.out.println("Caused by: " + t.causeSnippet() == null ? "Empty" : t.causeSnippet().source());
            }
        });
        if (evalResult.status().isSuccess()) {
            System.out.println("JSciUUID successfully set");
        } else {
            System.out.println("JSciUUID failed to set");
            System.out.println(evalResult.snippetEventsCurrent());
        }
        if (path != null) {
            var pathString = path.toString();
            if (IS_WINDOWS) {
                pathString = path.toString().replaceAll("\\\\", "//");
            }
            var cwdInitSource = "var cwd = java.nio.file.Paths.get(\"" + pathString + "\");";
            ReactiveJShell.EvaluationResult cwdDefResult = rjs.eval(cwdInitSource);

            if (cwdDefResult.status().isSuccess()) {
                System.out.println("cwd successfully set to " + pathString);
            } else {
                System.out.println("cwd failed to set to " + pathString);
                System.out.println(cwdDefResult.snippetEventsCurrent());
            }
        }
        addDefaultExtensions(rjs, uuid);
        return postInitShell(rjs, uuid);
    }

    private static ReactiveJShell postInitShell(ReactiveJShell rjs, UUID uuid) {
        rjs.eval("import static org.jtaccuino.jshell.extensions.fx.ChartsFx.*");

        return rjs;
    }

    private static ReactiveJShell addDefaultExtensions(ReactiveJShell rjs, UUID uuid) {
        var factories = ServiceLoader.load(JShellExtension.Factory.class);
        factories.forEach(f -> {
            JShellExtension extension = f.createExtension(rjs);
            ExtensionManager.register(extension, uuid);
            String extensionVarInit = "var " + extension.shellVariableName() + " = org.jtaccuino.jshell.extensions.ExtensionManager.lookup(" + extension.getClass().getName() + ".class, _$jsci$uuid)";
            rjs.eval(extensionVarInit);
            rjs.getWrappedShell().onSnippetEvent((t) -> {
                if (t.snippet().source().equals(extensionVarInit)) {
                    System.out.println("Init extensionVar extension.shellVariableName() status changed from " + t.previousStatus() + " to : " + t.status());
                    System.out.println("Caused by: " + t.causeSnippet() == null ? "Empty" : t.causeSnippet().source());
                }
            });
            ReactiveJShell.EvaluationResult evalResult = rjs.eval(extension.initCodeSnippet());
            if (evalResult.status().isSuccess()) {
                System.out.println("Extension " + extension + " init code registered successfully");
            } else {
                System.out.println("Extension " + extension + " failed to load init code!");
                System.out.println(evalResult.snippetEventsCurrent());
            }
        });
        return rjs;
    }
}
