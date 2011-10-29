package net.wohlfart.jbpm4.mail;

public class MailserverUnavailableException extends Exception {


    public MailserverUnavailableException(final String message) {
        super(message);
    }

    public MailserverUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
