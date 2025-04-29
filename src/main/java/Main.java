import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;
import org.dorkmaster.repoReviewer.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class Main {
    private static final String GIT_OPTION = "gitUrl";
    private static final String LOCATION_OPTION = "localRepo";
    private static final String CUSTOM_PROMPTS = "prompts";
    private static final String OUTPUT_FILE = "out";
    private static final String DEFAULT_OUTPUT_FILE = "Report.MD";
    private static final String MODE = "mode";
    private static final String RUNTIME = "runtime";
    private static final String MODEL = "model";

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    enum Runtimes {
        OLLAMA {
            @Override
            ModelProvider initializer(String runtime, String model) {
                OllamaProvider provider = new OllamaProvider();
                if (null != runtime) {
                    provider.setDockerImageName(runtime);
                }
                if (null != model) {
                    provider.setModel(model);
                }
                return provider;
            }
        };

        abstract ModelProvider initializer(String runtime, String model);
    }

    public static class Cli {
        String[] args;

        protected Options setupOptions() {
            return new Options()
                    .addOption(
                            Option.builder(MODE)
                                    .hasArg()
                                    .desc("Provider to use")
                                    .build()
                    )
                    .addOption(
                            Option.builder(RUNTIME)
                                    .hasArg()
                                    .desc("Provider to load")
                                    .build()
                    )
                    .addOption(
                            Option.builder(MODEL)
                                    .hasArg()
                                    .desc("Model to use for prompt execution")
                                    .build()
                    )
                    .addOption(
                            Option.builder("gitUrl")
                                    .hasArg()
                                    .desc("GIT url to use for a remote repository")
                                    .build()
                    )
                    .addOption(
                            Option.builder("localRepo")
                                    .hasArg()
                                    .desc("Path to a local repository")
                                    .build()
                    )
                    .addOption(
                            Option.builder("prompts")
                                    .hasArg()
                                    .desc("Custom prompts file to use (YAML format)")
                                    .build()
                    )
                    .addOption(
                            Option.builder(OUTPUT_FILE)
                                    .hasArg()
                                    .desc("Output folder/filename to use")
                                    .build()
                    );
        }

        protected Options options;
        protected CommandLine cli;
        protected CommandLineParser parser = new DefaultParser();
        protected HelpFormatter formatter = new HelpFormatter();

        public Cli(String[] args) {
            this.args = args;

            try {
                this.options = setupOptions();
                cli = parser.parse(options, args);
            } catch (ParseException e) {
                // do something better here
                throw new RuntimeException(e);
            }
        }

        public Pair<String, String> folder() {
            if (cli.hasOption(GIT_OPTION)) {
                return Pair.of(cli.getOptionValue(GIT_OPTION), GitDownloader.cloneRepo(cli.getOptionValue(GIT_OPTION)));
            } else if (cli.hasOption(LOCATION_OPTION)) {
                return Pair.of("local repo: " + cli.getOptionValue(LOCATION_OPTION), cli.getOptionValue(LOCATION_OPTION));
            } else {
                formatter.printHelp("java -jar repo-reviewer.jar", options);
                // throw an error, there's no repo
                System.exit(1);
            }
            return null;
        }

        public String outputFile() {
            if (cli.hasOption(OUTPUT_FILE)) {
                return cli.getOptionValue(OUTPUT_FILE);
            } else {
                return DEFAULT_OUTPUT_FILE;
            }
        }

        public PromptLoader.Prompts prompts() {
            PromptLoader.Prompts prompts = null;
            if (cli.hasOption(CUSTOM_PROMPTS)) {
                try (InputStream in = new FileInputStream(cli.getOptionValue(CUSTOM_PROMPTS))) {
                    prompts = PromptLoader.loadPrompts(in);
                } catch (IOException e) {
                    logger.error("Could not load prompts from \"{}\"", cli.getOptionValue(CUSTOM_PROMPTS), e);
                    System.exit(1);
                }
            } else {
                try (InputStream in = Main.class.getResourceAsStream("default-prompts.yaml")) {
                    prompts = PromptLoader.loadPrompts(in);
                } catch (IOException e) {
                    logger.error("Could not load default prompts", e);
                    System.exit(1);
                }
            }
            return prompts;
        }

        public ModelProvider provider() {
            ModelProvider provider;
            if (cli.hasOption(MODE)) {
                provider = (ModelProvider) Runtimes.valueOf(cli.getOptionValue(MODE))
                        .initializer(
                                cli.hasOption(RUNTIME) ? cli.getOptionValue(RUNTIME) : null,
                                cli.hasOption(MODEL) ? cli.getOptionValue(MODEL) : null
                        );
            } else {
                // fallback to ollama locally
                provider = new OllamaProvider();
            }
            return provider;
        }
    }

    public static void main(String[] args) {
        Cli cli = new Cli(args);
        PromptLoader.Prompts prompts = cli.prompts();
        if (prompts.getPrompts().size() > 0) {
            Pair<String, String> tmp = cli.folder();
            try {
                ChatLanguageModel model = cli.provider().build();

                // execute the prompts
                new ExecutePrompts().execute(model, tmp.getLeft(), tmp.getRight(), prompts.getPrompts(), cli.outputFile());
            } finally {
                new File(tmp.getRight()).delete();
            }
        }
    }
}