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
package org.jtaccuino.extension.deepnetts;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class DeepNettsExtension implements JShellExtension {

    private static final List<String> EXTENSIONS = List.of(
            "DF_LIB_EXTENSION",
            "FX_CHARTS_EXTENSION"
    );

    private static final List<String> DEPS = List.of(
            "com.deepnetts:deepnetts-core-pro:3.2.0",
            "com.deepnetts:deepnetts-license:1.0"
    );

    private static final List<String> IMPORTS = List.of(
            "deepnetts.data.DataSets",
            "deepnetts.data.TabularDataSet",
            "deepnetts.data.norm.MaxScaler",
            "deepnetts.eval.ClassifierEvaluator",
            "deepnetts.eval.ConfusionMatrix",
            "javax.visrec.ml.eval.EvaluationMetrics",
            "deepnetts.net.FeedForwardNetwork",
            "deepnetts.net.layers.activation.ActivationType",
            "deepnetts.net.loss.LossType",
            "deepnetts.net.train.BackpropagationTrainer",
            "deepnetts.net.train.opt.OptimizerType",
            "javax.visrec.ml.data.DataSet",
            "javax.visrec.ml.data.preprocessing.Scaler"
    );

    private DeepNettsExtension() {
        // prevent instantiation
    }

    @Override
    public Optional<String> initCodeSnippet() {
        var extensions = EXTENSIONS.stream().map(dep -> "use(JTExtension." + dep + ");").collect(Collectors.joining("\n"));
        var dependencies = DEPS.stream().map(dep -> "addDependency(\"" + dep + "\");").collect(Collectors.joining("\n"));
        var imports = IMPORTS.stream().map(imp -> "import " + imp + ";").collect(Collectors.joining("\n"));
        return Optional.of(extensions + "\n" + dependencies + "\n" + imports);
    }

    @Descriptor(mode = Mode.ON_DEMAND, type = DeepNettsExtension.class)
    public static class FactoryImpl implements Factory {

        @Override
        public DeepNettsExtension createExtension(ReactiveJShell jshell) {
            return new DeepNettsExtension();
        }
    }
}
