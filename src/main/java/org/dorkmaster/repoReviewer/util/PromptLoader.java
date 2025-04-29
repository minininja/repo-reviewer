package org.dorkmaster.repoReviewer.util;

import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class PromptLoader {
    public static Prompts loadPrompts(InputStream in) {
        Yaml yaml = new Yaml();
        try {
            Prompts prompts = yaml.loadAs(in, Prompts.class);
            return prompts;
        } catch(Throwable t) {
            t.printStackTrace();;
        }

        return new Prompts();
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
