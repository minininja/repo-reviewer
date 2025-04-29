package org.dorkmaster.repoReviewer.providers;

import dev.langchain4j.model.chat.ChatLanguageModel;

public interface ModelProvider {
    ChatLanguageModel build();
}
