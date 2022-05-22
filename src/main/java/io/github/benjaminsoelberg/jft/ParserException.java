package io.github.benjaminsoelberg.jft;

public class ParserException extends Exception {
    public ParserException(String message) {
        super(message);
    }

    public ParserException(Throwable cause) {
        super(cause);
    }
}
