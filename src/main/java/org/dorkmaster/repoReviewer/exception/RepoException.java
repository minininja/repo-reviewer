package org.dorkmaster.repoReviewer.exception;

public class RepoException extends FatalException {
    protected String remoteRepo;
    protected String localFolder;

    public RepoException(String message, String remoteRepo, String localFolder) {
        super(message, -2);
        this.remoteRepo = remoteRepo;
        this.localFolder = localFolder;
    }

    public String getRemoteRepo() {
        return remoteRepo;
    }

    public String getLocalFolder() {
        return localFolder;
    }
}
