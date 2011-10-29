package net.wohlfart.email.freemarker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.email.CandidateAssigneeTokenResolver;
import net.wohlfart.email.DebugAddressResolver;
import net.wohlfart.email.EmailAddressResolver;
import net.wohlfart.email.PreviousAssigneeTokenResolver;
import net.wohlfart.email.PreviousExecutionTokenResolver;
import net.wohlfart.email.SingleAssigneeTokenResolver;
import net.wohlfart.email.entities.CharmsEmailTemplate;

import org.hibernate.Session;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jbpm.pvm.internal.model.ExecutionImpl;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;

/**
 * Singleton configuration for freemarker
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class MailConfiguration extends Configuration {

    private static MailConfiguration  instance;

    // enhancing the config with a receiver loader
    private DatabaseMailAddressLoader addressLoader;
    // and a subject loader
    private DatabaseSubjectLoader     subjectLoader;

    private MailConfiguration() {
        super();
        setTemplateLoader(new DatabaseMailTemplateLoader());

        // custom extensions for charms email processing:
        final DatabaseMailAddressLoader addressLoader = new DatabaseMailAddressLoader();
        // the order is important!
        addressLoader.add(new SingleAssigneeTokenResolver());
        addressLoader.add(new CandidateAssigneeTokenResolver());
        addressLoader.add(new PreviousAssigneeTokenResolver());
        addressLoader.add(new PreviousExecutionTokenResolver());
        addressLoader.add(new EmailAddressResolver());

        addressLoader.add(new DebugAddressResolver());

        setReceiverLoader(addressLoader);

        setSubjectLoader(new DatabaseSubjectLoader());
        // see:
        // http://freemarker.sourceforge.net/docs/pgui_datamodel_objectWrapper.html
        setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);

        // try to keep everything in UTF-8 to simplify things
        clearEncodingMap();
        setDefaultEncoding("UTF-8");

        // default number format
        setNumberFormat("0.######");
        // default date format
        setDateFormat("EEEEE, d. MMMMM, ''yy");
        // default date format
        setTimeFormat("HH:mm");
    }

    public synchronized static MailConfiguration getInstance() {
        if (instance == null) {
            instance = new MailConfiguration();
        }
        return instance;
    }

    // the main methods:

    // resolve a template for the body of an email (superclass takes care of
    // that)
    // @Override
    @Transactional(TransactionPropagationType.MANDATORY)
    public Template getBody(final String templateName, final Locale locale) throws IOException {
        return super.getTemplate(templateName, locale, getDefaultEncoding(), true);
    }

    // resolve the template for the subject of an email
    @Transactional(TransactionPropagationType.MANDATORY)
    public Template getSubject(final String templateId, final Locale locale, final ExecutionImpl execution, final Session session) throws IOException {
        return subjectLoader.getSubjectAsTemplate(templateId, locale, execution, session, this);
    }

    // resolve the receivers for a template
    @Transactional(TransactionPropagationType.MANDATORY)
    public List<CharmsUser> getReceivers(final CharmsEmailTemplate template, final ExecutionImpl execution, final Session session) {
        return addressLoader.getReceivers(template, execution, session);
    }

    // resolve the sender for a template
    @Transactional(TransactionPropagationType.MANDATORY)
    public CharmsUser getSender(final CharmsEmailTemplate template, final ExecutionImpl execution, final Session session) {
        return addressLoader.getSender(template, execution, session);
    }

    // ------ internal stuff

    public void setReceiverLoader(final DatabaseMailAddressLoader addressLoader) {
        this.addressLoader = addressLoader;
    }

    public DatabaseMailAddressLoader getReceiverLoader() {
        return addressLoader;
    }

    public void setSubjectLoader(final DatabaseSubjectLoader subjectLoader) {
        this.subjectLoader = subjectLoader;
    }

    public DatabaseSubjectLoader getSubjectLoader() {
        return subjectLoader;
    }

    // we don't want the default encoding map
    @Override
    public void loadBuiltInEncodingMap() {
        throw new IllegalArgumentException("no predefined encoding map used in charms, we use UTF-8");
    }

}
