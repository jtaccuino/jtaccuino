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
package org.jtaccuino.core.ui.documentation;

import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;

/**
 * Renders a javadoc string plus an optional header (enclosing type + member
 * signature) into a {@link SimpleViewOnlyStyledModel} for the incubator
 * RichTextArea. The javadoc is converted to markdown per
 * {@code docs/javadoc-preview-format.md} and then rendered via
 * {@link MarkdownToStyledModel}.
 */
final class JavadocRenderer {

    private JavadocRenderer() {
        // prevent instantiation
    }

    static SimpleViewOnlyStyledModel render(String javadoc, String typeName, String signature) {
        var model = new SimpleViewOnlyStyledModel();
        renderHeader(model, typeName, signature);
        var markdown = JavadocHtmlToMarkdown.convert(javadoc);
        MarkdownToStyledModel.renderInto(markdown, model);
        return model;
    }

    private static void renderHeader(SimpleViewOnlyStyledModel model, String typeName, String signature) {
        if (typeName != null && !typeName.isBlank()) {
            model.addSegment(typeName, JavadocStyles.typeStyle());
            model.nl();
        }
        if (signature != null && !signature.isBlank()) {
            model.addSegment(signature, JavadocStyles.signatureStyle());
            model.nl();
        }
    }
}
