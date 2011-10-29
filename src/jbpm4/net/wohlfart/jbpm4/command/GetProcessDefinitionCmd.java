package net.wohlfart.jbpm4.command;

import org.jbpm.api.ProcessDefinition;
import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.session.RepositorySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class GetProcessDefinitionCmd extends AbstractCommand<ProcessDefinition> {


    private final static Logger LOGGER = LoggerFactory.getLogger(GetProcessDefinitionCmd.class);

    String                      processDefinitionId;

    public GetProcessDefinitionCmd(final String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public ProcessDefinition execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        // we have the processDefinitionID and want the process definition
        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
        return processDefinition;
    }

}
