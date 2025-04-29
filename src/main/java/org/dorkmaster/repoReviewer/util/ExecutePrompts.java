package org.dorkmaster.repoReviewer.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.*;
import java.util.*;

public class ExecutePrompts {
    private static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    private static final String MODEL = "GandalfBaum/llama3.2-claude3.7";
    private static final String DOCKER_IMAGE_NAME = "GandalfBaum/llama3.2-claude3.7";

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected String model = DOCKER_IMAGE_NAME;
    protected Set<String> ignoredDirectories = Set.of(".git");

    public static interface Assistant {
        String chat(String userMessage);
    }

    public ExecutePrompts() {
    }

    public ExecutePrompts(String model) {
        super();
        this.model = model;
    }

    public void execute(ChatLanguageModel model, String displayName, String localFolder, Collection<String> prompts, String output) {
        // load the repo into the context
        logger.info("Loading repo files from {}", localFolder);

        List<Document> documents = new LinkedList<>();
        walk(documents, new File(localFolder));

        if (documents.size() == 0) {
            logger.info("Repo does not contain any files which can be processed");
            System.exit(1);
        }

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        Assistant chatAssistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();

        // setup the report
        try (PrintStream out = new PrintStream(new FileOutputStream(output))) {
            out.println(String.format("# AI review of %s by %s\n--", displayName, this.model));
            out.println(String.format("Generated at: %s\n", new Date().toString()));
            out.flush();

            for (String prompt : prompts) {
                logger.info("Executing prompt \"{}\"", prompt);
                out.println(String.format("## PROMPT: %s\n", prompt));
                for (int i = 0; i < 3; i++) {
                    try {
                        out.println(chatAssistant.chat(prompt));
                        out.flush();
                        break;
                    } catch (Throwable t) {
                        // ignore errors, they'll most likely be timeouts.  just try again.
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("Error writing output file", e);
            System.exit(1);
        }
    }

    public void walk(Collection<Document> documents, File item) {
        if (item.isFile()) {
            try {
                // ignore any zero length files (post parsing)
                Document doc = FileSystemDocumentLoader.loadDocument(item.getAbsolutePath());
                if (doc.toTextSegment().text().trim().length() > 0) {
                    documents.add(doc);
                }
            } catch(Throwable t) {
                // skip over this file as it's probably not text
                logger.info("Error loading {}", item, t);
            }
        } else if (item.isDirectory()) {
            logger.info("Looking for files in {}", item);
            // not every folder needs to be looked at, for example .git
            if (!ignoredDirectories.contains(item.getName())) {
                for (File file : item.listFiles()) {
                    walk(documents, file);
                }
            }
        }
    }
}
