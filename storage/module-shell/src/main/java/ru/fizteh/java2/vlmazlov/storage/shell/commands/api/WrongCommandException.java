package ru.fizteh.java2.vlmazlov.storage.shell.commands.api;

public class WrongCommandException extends Exception {
    public WrongCommandException() {
        super();
    }

    public WrongCommandException(String message) {
        super(message);
    }

    public WrongCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongCommandException(Throwable cause) {
        super(cause);
    }
}
