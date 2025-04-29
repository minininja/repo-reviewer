import org.dorkmaster.repoReviewer.util.PromptLoader;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class PromptLoaderTests {
    protected static final String HAPPY_PROMPT = "Tell me a joke";

    @Test
    public void happy() {
        // look into generating the content dynamically rather from a resource
        // which will ensure that changing the prompt only once
        try (InputStream in = this.getClass().getResourceAsStream("/prompt/happy.yaml")){
            PromptLoader.Prompts prompts = PromptLoader.loadPrompts(in);
            assertEquals(prompts.getPrompts().size(), 1);
            assertEquals(HAPPY_PROMPT, prompts.getPrompts().iterator().next());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
