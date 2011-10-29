package net.wohlfart.jbpm4.mail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletContext;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.email.EmailAddressResolver;
import net.wohlfart.email.entities.CharmsEmailMessage;
import net.wohlfart.email.entities.CharmsEmailTemplate;
import net.wohlfart.email.freemarker.MailConfiguration;
import net.wohlfart.email.freemarker.MailDataModel;
import net.wohlfart.framework.RuntimeEnvDataProvider;

import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * this class does all the dirty work creating emails
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class AbstractMailProducer {

    final static Logger         LOGGER = LoggerFactory.getLogger(AbstractMailProducer.class);

    private static final String UTF8   = "UTF-8";

    /**
     * producing the mime message(s) for a single template, this method does the
     * real work of assembling zero or more messages from a provided template,
     * process execution and db session
     * 
     * @param template
     * @param execution
     * @param session
     * @return a set of created mime messages for sending
     */
    protected Collection<MimeMessage> produce(final CharmsEmailTemplate template, final ExecutionImpl execution, final org.hibernate.Session databaseSession) {

        final ArrayList<MimeMessage> result = new ArrayList<MimeMessage>();
        try {
            // the model is used as interface to the charms datastore
            // and provides the template with the data for the placeholders
            // the needed data are either in the execution or will be retrieved
            // from the DB or will be added to the model later
            LOGGER.debug("creating model");
            final MailDataModel model = new MailDataModel(execution, databaseSession);

            // configstore for setting up time & date formats and implements the
            // template store
            // config also resolves the receivers for a mail template
            // FIXME: get this preconfigured from spring
            LOGGER.debug("creating config");
            final MailConfiguration config = MailConfiguration.getInstance();

            // first we need the receivers
            final List<CharmsUser> receivers = config.getReceivers(template, execution, databaseSession);
            LOGGER.debug("resolved localizedReceivers : {}", receivers);

            // and the sender
            final CharmsUser sender = config.getSender(template, execution, databaseSession);
            LOGGER.debug("resolved localizedSender : {}", sender);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("list of receivers:");
                for (final Object object : receivers) {
                    LOGGER.debug("   receiver object: {} is: {}", object, object.getClass());
                }
            }

            // spiff up the model with some more data

            model.setSender(sender);

            // FIXME: use the application properties datastore for this and set
            // this value there on bootup
            // FIXME: this is the only place where we access server specific
            // classes....
            // this should be removed...
            final ServletContext servletContext = RuntimeEnvDataProvider.getServletContext();
            final String contextPath = servletContext.getContextPath();
            final String baseUrl = servletContext.getInitParameter("charms.baseUrl");
            model.setServletUrl(baseUrl + contextPath);

            // now we process the template for each receiver
            for (final CharmsUser receiver : receivers) {

                // keep track of errors
                boolean error = false;
                final StringBuilder emailDebugOutput = new StringBuilder();

                // second we need the template, this might be specific for the
                // receivers locale
                // the templates are cached in the freemarker config

                final StringWriter subject = new StringWriter();
                try {
                    final Template subjectTemplate = config.getSubject(template.getId().toString(), new Locale("de"), execution, databaseSession);
                    subjectTemplate.process(model, subject); // do the
                                                             // freemarker magic
                    subject.flush();
                } catch (final TemplateException ex) {
                    error = true;
                    emailDebugOutput.append(ex.toString() + "\n");
                    LOGGER.debug("TemplateException while processing subject: " + ex);
                }

                final StringWriter body = new StringWriter();
                try {
                    // we have non unique names for the templates in order
                    // to send multiple emails, this means we use the id as
                    // lookup in the template store...
                    // FIXME: locale lookup for receivers locale...
                    final Template contentTemplate = config.getBody(template.getId().toString(), new Locale("de")); // this
                                                                                                                    // hits
                                                                                                                    // the
                                                                                                                    // DatabaseMailTemplateLoader
                                                                                                                    // class
                    // there is a different receiver for each generated email
                    model.setReceiver(receiver);
                    contentTemplate.process(model, body);
                    body.flush();
                } catch (final TemplateException ex) {
                    error = true;
                    emailDebugOutput.append(ex.toString() + "\n");
                    LOGGER.debug("TemplateException while processing body: " + ex);
                }

                String receiverString = "unknown";
                if ((receiver.getEmail() != null) && (receiver.getEmail().trim().length() > 0)) {
                    receiverString = receiver.getEmail();
                } else {
                    error = true;
                    receiverString = "(" + receiver.getLabel() + ")";
                    emailDebugOutput.append("unknown emailaddress for receiver " + receiver.getLabel() + "\n");
                }

                String senderString = "unknown";
                if ((sender != null) && (sender.getEmail() != null) && (sender.getEmail().trim().length() > 0)) {
                    senderString = sender.getEmail();
                } else {
                    error = true;
                    if (sender == null) {
                        senderString = "( sender resolved to null )";
                        emailDebugOutput.append("sender is null " + "\n");
                    } else {
                        senderString = "(" + sender.getLabel() + ")";
                        emailDebugOutput.append("unknown emailaddress for sender " + sender.getLabel() + "\n");
                    }
                }

                // final check if the emails are ok:
                if (!error) {
                    if (!EmailAddressResolver.EMAIL_PATTERN.matcher(receiverString).matches()) {
                        error = true;
                        emailDebugOutput.append("receiver address is invalid" + "\n");
                    }
                    if (!EmailAddressResolver.EMAIL_PATTERN.matcher(senderString).matches()) {
                        // FIXME: set a defailt application configurable sender
                        // address if the
                        // user doesn't have one
                        error = true;
                        emailDebugOutput.append("sender address is invalid" + "\n");
                    }
                }

                // at this point we generated
                // subject, body, receiverString, senderString

                LOGGER.debug("generated email: " + body.toString());

                // persist what we got so far
                final CharmsEmailMessage email = persistEmail(databaseSession, senderString, receiverString, subject.toString(), body.toString(),
                        execution.getKey(), error, emailDebugOutput.toString() + "\n");

                // put the mail in the outbox if everything is ok so far
                if (!error) {
                    try {
                        final MimeMessage mimeMessage = create(email);
                        result.add(mimeMessage);
                    } catch (final AddressException ex) {
                        LOGGER.warn("skipping email {} because of exception {}", email, ex.toString());
                    } catch (final MessagingException ex) {
                        LOGGER.warn("skipping email {} because of exception {}", email, ex.toString());
                    }
                }

            } // end for loop for receivers

        } catch (final IOException ex) {
            LOGGER.warn("IOException while creating mails", ex);
        } // end try block for generating emails

        LOGGER.info("result contains {} message(s) which will be sent", result.size());
        return result;
    }

    private CharmsEmailMessage persistEmail(final org.hibernate.Session databaseSession, final String sender, final String receiver, final String subject,
            final String body, final String businessKey, final boolean isError, final String errorText) {

        final CharmsEmailMessage charmsEmailMessage = new CharmsEmailMessage();
        charmsEmailMessage.setSubject(subject);
        charmsEmailMessage.setKey(businessKey);
        charmsEmailMessage.setSender(sender);
        charmsEmailMessage.setReceiver(receiver);

        String errorHeader = "";
        if (isError) {
            errorHeader = "----------------------\n" + "          error       \n" + "----------------------\n" + errorText + "\n"
                    + "----------------------\n\n\n";
        }
        charmsEmailMessage.setContent(errorHeader + body);

        charmsEmailMessage.setSubject(subject);
        charmsEmailMessage.setCreate(Calendar.getInstance().getTime());

        databaseSession.persist(charmsEmailMessage);

        return charmsEmailMessage;
    }

    private MimeMessage create(final CharmsEmailMessage message) throws AddressException, MessagingException {
        // see:
        // http://www.coderanch.com/t/272659/Other-Java-APIs/java/send-email-HTML-using-SMTPMessage
        final MimeMessage mimeMessage = new MimeMessage((Session) null);

        LOGGER.debug("message.getSender() is: {}", message.getSender());
        LOGGER.debug("message.getReceiver() is: {}", message.getReceiver());
        LOGGER.debug("message.getSubject() is: {}", message.getSubject());

        mimeMessage.setFrom(new InternetAddress(message.getSender()));
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, message.getReceiver());
        // message.addRecipients(RecipientType.BCC, "michael@wohlfart.net");
        mimeMessage.setSubject(message.getSubject(), UTF8);

        // MimeMultipart mimeMultipart = new MimeMultipart( "mixed" );
        final MimeMultipart content = new MimeMultipart("alternative");
        final MimeBodyPart text = new MimeBodyPart();
        final MimeBodyPart html = new MimeBodyPart();

        html.setContent("<html>" + message.getContent().replace("\n", " <br />") + " <br />" + "</html>", "text/html; charset=utf-8");
        html.setHeader("MIME-Version", "1.0");
        html.setHeader("Content-Type", "text/html; charset=UTF-8");

        text.setText(message.getContent(), UTF8);
        text.setHeader("MIME-Version", "1.0");
        text.setHeader("Content-Type", "text/plain; charset=UTF-8");

        content.addBodyPart(html);
        content.addBodyPart(text);

        mimeMessage.setContent(content);
        mimeMessage.setContentID(message.getId().toString());

        return mimeMessage;
    }

}
