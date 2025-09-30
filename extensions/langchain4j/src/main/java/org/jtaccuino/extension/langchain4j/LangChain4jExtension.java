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
package org.jtaccuino.extension.langchain4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jtaccuino.jshell.ReactiveJShell;
import org.jtaccuino.jshell.extensions.JShellExtension;

public class LangChain4jExtension implements JShellExtension {

    private static final String VERSION = "1.6.0";

    private static final List<String> DEPS = List.of(
            "dev.langchain4j:langchain4j:" + VERSION,
            "dev.langchain4j:langchain4j-core:" + VERSION,
            "dev.langchain4j:langchain4j-ollama:" + VERSION
    );

    private static final List<String> IMPORTS = List.of(
            "dev.langchain4j.chain.ConversationalRetrievalChain",
            "dev.langchain4j.data.document.Document",
            "dev.langchain4j.data.document.DocumentSplitter",
            "dev.langchain4j.data.document.loader.FileSystemDocumentLoader",
            "dev.langchain4j.data.document.loader.UrlDocumentLoader",
            "dev.langchain4j.data.document.parser.TextDocumentParser",
            "dev.langchain4j.data.document.splitter.DocumentSplitters",
            "dev.langchain4j.data.embedding.Embedding",
            "dev.langchain4j.data.message.AiMessage",
            "dev.langchain4j.data.segment.TextSegment",
            "dev.langchain4j.memory.chat.MessageWindowChatMemory",
            "dev.langchain4j.model.embedding.EmbeddingModel",
            "dev.langchain4j.model.ollama.OllamaEmbeddingModel",
            "dev.langchain4j.model.ollama.OllamaChatModel",
            "dev.langchain4j.rag.DefaultRetrievalAugmentor",
            "dev.langchain4j.rag.content.retriever.ContentRetriever",
            "dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever",
            "dev.langchain4j.rag.query.transformer.CompressingQueryTransformer",
            "dev.langchain4j.store.embedding.EmbeddingMatch",
            "dev.langchain4j.store.embedding.EmbeddingStore",
            "dev.langchain4j.store.embedding.EmbeddingStoreIngestor",
            "dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore",
            "dev.langchain4j.service.AiServices"
    );

    private LangChain4jExtension() {
        // prevent instantiation
    }

    @Override
    public Optional<String> initCodeSnippet() {
        var dependencies = DEPS.stream().map(dep -> "addDependency(\"" + dep + "\");").collect(Collectors.joining("\n"));
        var imports = IMPORTS.stream().map(imp -> "import " + imp + ";").collect(Collectors.joining("\n"));
        return Optional.of(dependencies + "\n" + imports);
    }

    @Descriptor(mode = Mode.ON_DEMAND, type = LangChain4jExtension.class)
    public static class FactoryImpl implements Factory {

        @Override
        public LangChain4jExtension createExtension(ReactiveJShell jshell) {
            return new LangChain4jExtension();
        }
    }
}
