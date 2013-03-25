package no.finn.capturandro.exception;

public class CapturandroException extends Exception {
    private final String message;

    public CapturandroException(String message){
        this.message = message;
    }

    public String toString(){
        return message;
    }
}
