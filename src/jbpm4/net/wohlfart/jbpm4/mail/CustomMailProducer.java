package net.wohlfart.jbpm4.mail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.mail.Message;

import net.wohlfart.email.entities.CharmsEmailTemplate;

import org.jbpm.api.Execution;
import org.jbpm.api.listener.EventListener;
import org.jbpm.api.listener.EventListenerExecution;
import org.jbpm.pvm.internal.email.spi.MailProducer;
import org.jbpm.pvm.internal.email.spi.MailSession;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.env.TaskContext;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class CustomMailProducer extends AbstractMailProducer implements MailProducer, Serializable, EventListener {


    final static Logger LOGGER = LoggerFactory.getLogger(CustomMailProducer.class);

    private String      templateName;                                              // this
                                                                                    // is
                                                                                    // set
                                                                                    // by
                                                                                    // jbpm4s
                                                                                    // xml
                                                                                    // parser

    /**
     * the only thing we have to do here is to generate the list of Messages,
     * the messages are handed over to the send method in the CustomMailSession
     * object by jbpm... finding the receiver/sender/subject/body is handled by
     * the config object which contains the MailAddresseLoader for this task
     * 
     * note that calling this method migh be very expensive, so we use the job
     * executor to trigger an async job execution for this method
     */
    @Override
    public Collection<Message> produce(final Execution execution) {

        LOGGER.debug("produce called in CustomMailProducer");

        final ExecutionImpl executionImpl = (ExecutionImpl) execution;

        LOGGER.debug("event triggered, name is: {}", (executionImpl.getEvent().getName()));
        LOGGER.debug("event triggered, getObservableElement is: {}", (executionImpl.getEvent().getObservableElement()));
        LOGGER.debug("event triggered, getObservableElement class is: {}", (executionImpl.getEvent().getObservableElement().getClass().getName()));

        LOGGER.debug(" activity instance id: {}", executionImpl.getHistoryActivityInstanceDbid());
        LOGGER.debug(" template name is: {}", templateName);

        return doProduce(executionImpl);
    }

    /**
     * implementing the event listener interface for jbpm this way we can use
     * the mail producer in transitions
     * 
     */
    @Override
    public void notify(final EventListenerExecution execution) throws Exception {
        LOGGER.info("notify called in CustomMailProducer");
        final ExecutionImpl executionImpl = (ExecutionImpl) execution;

        // find current task
        final EnvironmentImpl environment = EnvironmentImpl.getCurrent();
        final DbSession dbSession = environment.get(DbSession.class);
        final TaskImpl task = dbSession.findTaskByExecution(execution);

        // make task available to mail templates through task context
        final TaskContext taskContext = new TaskContext(task);
        environment.setContext(taskContext);
        try {
            final Collection<Message> messages = doProduce(executionImpl);
            environment.get(MailSession.class).send(messages);
        } finally {
            environment.removeContext(taskContext);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Message> doProduce(final ExecutionImpl executionImpl) {

        final org.hibernate.Session databaseSession = EnvironmentImpl.getFromCurrent(org.hibernate.Session.class);
        LOGGER.debug("session is: {}", databaseSession);

        LOGGER.debug("doProduce method called on CustomMailProducer session is: " + databaseSession);

        // we might have multiple templates with the same name, we use them all
        final List<CharmsEmailTemplate> list = databaseSession.getNamedQuery(CharmsEmailTemplate.FIND_BY_NAME).setParameter("name", templateName).list();

        if (list.size() == 0) {
            LOGGER.info("can't find email template for name {} returning empty template list", templateName);
            return new ArrayList<Message>();
        }

        LOGGER.debug("found {} email(s) for template name {}, assembling emails now", list.size(), templateName);

        final Collection<Message> messages = new ArrayList<Message>();
        for (final CharmsEmailTemplate template : list) {
            // check if the template is enabled
            if (template.getEnabled()) {
                // the real work is done in the superclass
                messages.addAll(super.produce(template, executionImpl, databaseSession));
            } else {
                LOGGER.info("template is disabled: {} ", template);
            }
        }

        LOGGER.debug("generated {} emails from the template", messages.size());
        return messages;
    }

    /**
     * @param templateName
     *            the templateName to set
     */
    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    /**
     * @return the templateName
     */
    public String getTemplateName() {
        return templateName;
    }

}
