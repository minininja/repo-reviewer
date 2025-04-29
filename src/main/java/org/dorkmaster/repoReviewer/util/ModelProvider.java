package org.dorkmaster.repoReviewer.util;

import dev.langchain4j.model.chat.ChatLanguageModel;

public interface ModelProvider {
    ChatLanguageModel build();
}
