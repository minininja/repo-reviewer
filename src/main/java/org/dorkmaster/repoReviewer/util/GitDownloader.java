package org.dorkmaster.repoReviewer.util;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.dorkmaster.repoReviewer.exception.RepoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class GitDownloader {
    private static Logger logger = LoggerFactory.getLogger(GitDownloader.class);

    public static String cloneRepo(String repoUrl) {
        return cloneRepo(repoUrl, 1);
    }

    public static String cloneRepo(String repoUrl, int maxMinutes) {
        String tempFolder = "repo-" + UUID.randomUUID().toString();

        try {
            File f = new File(tempFolder);
            if (f.mkdir()) {
                f.deleteOnExit();
            }

            CommandLine cmdLine = new CommandLine("git");
            cmdLine.addArgument("clone");
            cmdLine.addArgument(repoUrl);
            cmdLine.addArgument(tempFolder);

            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            Executor executor = new DefaultExecutor();
            executor.execute(cmdLine, resultHandler);

            int cnt = 0;
            while (cnt < maxMinutes && !resultHandler.hasResult()) {
                logger.debug("Waiting on git clone");
                resultHandler.waitFor(60 * 1000);
                cnt++;
            }

            if (!resultHandler.hasResult()) {
                logger.error("Git clone timed out from \"{}\"", repoUrl);
                throw new RepoException("Git clone timed out", tempFolder, repoUrl);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Git process died unexpectedly while cloning from \"{}\"", repoUrl, e);
            throw new RepoException("Git process died unexpectedly", tempFolder, repoUrl);
        }

        return tempFolder;
    }
}
