package no.finntech.capturandro;

import java.io.IOException;

public class CapturandroException extends Exception {
    public CapturandroException(String message) {
        super(message);
    }

    public CapturandroException(IOException e) {
        super(e);
    }
}
