package org.dorkmaster.repoReviewer.util;

import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class PromptLoader {
    public static Collection<String> loadPrompts(InputStream in) {
        Yaml yaml = new Yaml();
        try {
            Prompts prompts = yaml.loadAs(in, Prompts.class);
            return prompts.prompts;
        } catch(Throwable t) {
            t.printStackTrace();;
        }

        return Collections.emptyList();
    }

    public static class Prompts {
        protected Collection<String> prompts;

        public void setPrompts(Collection<String> prompts) {
            this.prompts = prompts;
        }
    }
}
