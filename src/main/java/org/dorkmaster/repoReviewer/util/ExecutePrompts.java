package org.dorkmaster.repoReviewer.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutePrompts {
    private static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    private static final String MODEL = "GandalfBaum/llama3.2-claude3.7";
    private static final String DOCKER_IMAGE_NAME = "GandalfBaum/llama3.2-claude3.7";

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected String model = DOCKER_IMAGE_NAME;

    public static interface Assistant {
        String chat(String userMessage);
    }

    public ExecutePrompts() {
    }

    public ExecutePrompts(String model) {
        super();
        this.model = model;
    }

    public void execute(String repo, String folder, Collection<String> prompts, String output) {
        // get ollama running
        DockerImageName dockerImageName = DockerImageName.parse(OLLAMA_IMAGE);
        DockerClient dockerClient = DockerClientFactory.instance().client();
        dockerClient.listImagesCmd().withReferenceFilter(DOCKER_IMAGE_NAME).exec();
        List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(DOCKER_IMAGE_NAME).exec();
        OllamaContainer ollama;
        if (images.isEmpty()) {
            ollama = new OllamaContainer(dockerImageName);
        } else {
            ollama = new OllamaContainer(DockerImageName.parse(DOCKER_IMAGE_NAME).asCompatibleSubstituteFor(OLLAMA_IMAGE));
        }
        ollama.start();

        // start the model
        try {
            logger.info("Start pulling the '{}' model ... would take several minutes ...", this.model);
            Container.ExecResult r = ollama.execInContainer("ollama", "pull", this.model);
            logger.info("Model pulling competed! {}", r);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error pulling model", e);
        }
        ollama.commitToImage(this.model);

        // build the chat model
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(ollama.getEndpoint())
                .temperature(0.0)
                .logRequests(true)
                .logResponses(true)
                .modelName(this.model)
                .build();

        // load the repo into the context
        logger.info("Loading repo files from {}", folder);
        // Filter out empty files - They seem fine when run via the ide but not when via CLI
        List<Document> documents =  FileSystemDocumentLoader.loadDocuments(folder).stream().filter(a -> a.toTextSegment().text().trim().length() > 0).collect(Collectors.toList());
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        Assistant chatAssistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();


        // setup the report
        try (PrintStream out = new PrintStream(new FileOutputStream(output))) {
            out.println(String.format("# AI review of %s by %s\n--", repo, this.model));
            out.println(String.format("Generated at: %s\n", new Date().toString()));
            out.flush();

            for (String prompt : prompts) {
                logger.info("Executing prompt \"{}\"", prompt);
                out.println(String.format("## PROMPT: %s\n", prompt));
                out.println(chatAssistant.chat(prompt));
                out.flush();
            }
        } catch (FileNotFoundException e) {
            logger.error("Error writing output file", e);
            System.exit(1);
        }
    }
}
