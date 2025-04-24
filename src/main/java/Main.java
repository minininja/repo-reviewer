import org.apache.commons.cli.*;
import org.dorkmaster.repoReviewer.util.ExecutePrompts;
import org.dorkmaster.repoReviewer.util.GitDownloader;
import org.dorkmaster.repoReviewer.util.PromptLoader;
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
    private static final String DEFAULT_OUTPUT_FILE = "report.md";

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Options options = new Options()
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

        String folder = null;
        Collection<String> prompts = null;

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(options, args);

            // figure out where the repo to review is
            if (cmd.hasOption(GIT_OPTION)) {
                folder = GitDownloader.cloneRepo(cmd.getOptionValue(GIT_OPTION));
            } else if (cmd.hasOption(LOCATION_OPTION)) {
                folder = cmd.getOptionValue(LOCATION_OPTION);
            } else {
                formatter.printHelp("java -jar repo-reviewer.jar", options);
                System.exit(1);
            }

            // load the standard prompts or a custom set
            if (cmd.hasOption(CUSTOM_PROMPTS)) {
                try (InputStream in = new FileInputStream(cmd.getOptionValue(CUSTOM_PROMPTS))) {
                    prompts = PromptLoader.loadPrompts(in);
                } catch (IOException e) {
                    logger.error("Could not load prompts from \"{}\"", cmd.getOptionValue(CUSTOM_PROMPTS), e);
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

            // execute the prompts
            if (prompts.size() > 0) {
                new ExecutePrompts().execute(cmd.hasOption(GIT_OPTION) ? cmd.getOptionValue(GIT_OPTION) : cmd.getOptionValue(LOCATION_OPTION), folder, prompts, DEFAULT_OUTPUT_FILE);
            } else {
                logger.info("No prompts to execute, shutting down");
            }


        } catch(ParseException e ) {
            e.printStackTrace();
        } finally {
            // cleanup
            if (folder != null) {
                new File(folder).delete();
            }
        }
    }
}