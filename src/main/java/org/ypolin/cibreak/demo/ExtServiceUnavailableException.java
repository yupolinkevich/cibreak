package org.ypolin.cibreak.demo;

public class ExtServiceUnavailableException extends RuntimeException {

    public ExtServiceUnavailableException(String message) {
        super(message);
    }

    public ExtServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
