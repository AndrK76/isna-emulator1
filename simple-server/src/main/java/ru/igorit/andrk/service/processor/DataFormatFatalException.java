package ru.igorit.andrk.service.processor;

public class DataFormatFatalException extends RuntimeException{
    public DataFormatFatalException(String message) {
        super(message);
    }

    public DataFormatFatalException(Throwable cause) {
        super(cause);
    }
}
