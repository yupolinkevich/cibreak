package org.ypolin.cibreak.exception;

public class CiBreakException extends RuntimeException{
    public CiBreakException(String message) {
        super(message);
    }

    public CiBreakException(String message, Throwable cause) {
        super(message, cause);
    }
}
