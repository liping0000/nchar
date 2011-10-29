package net.wohlfart.jbpm4.command;

import org.jbpm.api.cmd.Environment;
import org.jbpm.jpdl.internal.activity.MailActivity;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.email.spi.MailProducer;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.wire.usercode.UserCodeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class GetMailProducerCmd extends AbstractCommand<MailProducer> {


    private final static Logger LOGGER = LoggerFactory.getLogger(GetMailProducerCmd.class);

    ProcessDefinitionImpl       processDefinition;
    MailActivity                mailActivity;

    public GetMailProducerCmd(final ProcessDefinitionImpl processDefinition, final MailActivity mailActivity) {
        this.processDefinition = processDefinition;
        this.mailActivity = mailActivity;
    }

    @Override
    public MailProducer execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        final UserCodeReference mailProducerReference = mailActivity.getMailProducerReference();
        final MailProducer mailProducer = (MailProducer) mailProducerReference.getObject(processDefinition);
        return mailProducer;
    }

}
