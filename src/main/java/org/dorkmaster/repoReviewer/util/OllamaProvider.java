package org.dorkmaster.repoReviewer.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;

public class OllamaProvider implements ModelProvider {
    private static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    private static final String MODEL = "GandalfBaum/llama3.2-claude3.7";
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String dockerImageName = OLLAMA_IMAGE;
    protected String model = MODEL;

    public OllamaProvider setDockerImageName(String dockerImageName) {
        this.dockerImageName = dockerImageName;
        return this;
    }

    public OllamaProvider setModel(String model) {
        this.model = model;
        return this;
    }

    @Override
    public ChatLanguageModel build() {
        // get ollama running
        DockerImageName dockerImageName = DockerImageName.parse(this.dockerImageName);
        DockerClient dockerClient = DockerClientFactory.instance().client();
        dockerClient.listImagesCmd().withReferenceFilter(model).exec();
        List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(model).exec();
        OllamaContainer ollama;
        if (images.isEmpty()) {
            ollama = new OllamaContainer(dockerImageName);
        } else {
            ollama = new OllamaContainer(DockerImageName.parse(model).asCompatibleSubstituteFor(dockerImageName));
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

        return model;
    }
}
