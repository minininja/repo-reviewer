package org.dorkmaster.repoReviewer.util;

import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class PromptLoader {
    protected static Yaml yaml = new Yaml();

    public static Prompts loadPrompts(InputStream in) {
        Prompts prompts = yaml.loadAs(in, Prompts.class);
        return prompts;
    }

    public static class Prompts {
        protected Collection<String> prompts = Collections.EMPTY_LIST;

        public void setPrompts(Collection<String> prompts) {
            this.prompts = prompts;
        }

        public Collection<String> getPrompts() {
            return prompts;
        }
    }
}
