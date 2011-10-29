package net.wohlfart.jbpm4.command;

import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.session.RepositorySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class GetActivityCmd extends AbstractCommand<ActivityImpl> {


    private final static Logger LOGGER = LoggerFactory.getLogger(GetActivityCmd.class);

    String                      processDefinitionId;
    String                      activityName;

    public GetActivityCmd(final String processDefinitionId, final String activityName) {
        this.processDefinitionId = processDefinitionId;
        this.activityName = activityName;
    }

    @Override
    public ActivityImpl execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");

        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
        if (processDefinition == null) {
            LOGGER.warn("no process definition found with processDefinitionId {} returning null", processDefinitionId);
            return null;
        }

        final ActivityImpl activity = processDefinition.getActivity(activityName);
        if (activity == null) {
            LOGGER.warn("no activity found with name {} returning null", activityName);
            return null;
        }

        return activity;
    }

}
