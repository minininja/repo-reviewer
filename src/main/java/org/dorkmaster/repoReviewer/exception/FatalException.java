package org.dorkmaster.repoReviewer.exception;

public class FatalException extends RuntimeException {
    int exitCode = -1;

    public FatalException(String message) {
        super(message);
    }

    public FatalException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

    public FatalException setExitCode(int exitCode) {
        this.exitCode = exitCode;
        return this;
    }
}
