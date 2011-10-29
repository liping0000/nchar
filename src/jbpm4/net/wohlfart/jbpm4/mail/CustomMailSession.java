package net.wohlfart.jbpm4.mail;

import java.util.Collection;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.jbpm.pvm.internal.email.spi.MailSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 
 * this might be called from one of the threads from the thread pool for the job
 * executor
 * 
 * this class is needed for the jbpm config
 * 
 * @author Michael Wohlfart
 * 
 */
public class CustomMailSession implements MailSession {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomMailSession.class);

    /**
     * this is configured in the jbpm4.cfg.xml like this:
     * 
     * <object class="net.wohlfart.jbpm4.mail.CustomMailSession" > <field
     * name="mailSender"><ref object="mailSender" /></field> </object>
     * 
     * will be an instance of:
     * org.springframework.mail.javamail.JavaMailSenderImpl
     */
    private JavaMailSenderImpl  springMailSender;

    /**
     * the interface method to send emails out
     */
    @Override
    public void send(final Collection<Message> emails) {

        LOGGER.debug("send called for email collection, count is: {}", emails.size());
        LOGGER.debug("mail sender was set to: " + springMailSender);

        try {
            for (final Message email : emails) {
                if (email instanceof MimeMessage) {
                    final MimeMessage mimeMessage = (MimeMessage) email;
                    LOGGER.debug("got a mime message for sending: " + mimeMessage);
                    send(mimeMessage);
                } else {
                    LOGGER.warn("email is not of expected type (should be MimeMessage) but is " + email.getClass().getName());
                }
            }
        } catch (final MailserverUnavailableException ex) {
            LOGGER.warn("can't send email, skipping rest of messages, mailcount is {}, reason is \"{}\","
                    + " enable info logging for this class if you want to see the stacktrace", emails.size(), ex.toString());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.warn("error stacktrace is ", ex);
            }
        }
    }

    // the actual work of sending a single email from as preconfigured
    // javax.mail.Message
    // FIXME: mark the email as sent in the DB
    private void send(final MimeMessage message) throws MailserverUnavailableException {
        LOGGER.info("sending Message: " + message);

        if (springMailSender == null) {
            throw new MailserverUnavailableException("springMailSender not injected, it is either not configured in the spring-beans.config "
                    + " or not injected in jbpm.cfg.xml");
        }

        // this is the situation:
        // - we have message of type javax.mail.Message in message
        // - we have a spring mail sender in mailSender of type
        // org.springframework.mail.javamail.JavaMailSenderImpl

        final javax.mail.Session mailSession = springMailSender.getSession();
        final String mailProtocol = springMailSender.getProtocol();

        Transport transport = null;
        try {
            // If there is someone to send it to, then send it.
            final Address[] recipients = message.getAllRecipients();
            if (recipients.length > 0) {
                transport = mailSession.getTransport(mailProtocol);
                // the connection properties (port/user/host are configured in
                // the properties of the spring bean)
                transport.connect();

                final String id = message.getContentID();
                transport.sendMessage(message, recipients);

                LOGGER.debug("content id {} has been sent", id);
            } else {
                LOGGER.warn("no recipients for email message {} ", message);
            }

        } catch (final NoSuchProviderException ex) {
            throw new MailserverUnavailableException("Can't send Email message", ex);
        } catch (final MessagingException ex) {
            throw new MailserverUnavailableException("Can't send Email message", ex);
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (final MessagingException ex) {
                    LOGGER.warn("error while closing mail transport layer, ignored", ex);
                }
            }
        }
    }

}
