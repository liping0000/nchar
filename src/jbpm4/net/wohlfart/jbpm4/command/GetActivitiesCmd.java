package net.wohlfart.jbpm4.command;

import java.util.ArrayList;
import java.util.List;

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
public class GetActivitiesCmd extends AbstractCommand<List<ActivityImpl>> {


    private final static Logger LOGGER = LoggerFactory.getLogger(GetActivitiesCmd.class);

    String                      processDefinitionId;

    public GetActivitiesCmd(final String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ActivityImpl> execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        final List<ActivityImpl> activities = new ArrayList<ActivityImpl>();

        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
        if (processDefinition == null) {
            LOGGER.warn("no process definition found with processDefinitionId {} returning empty activities list", processDefinitionId);
            return activities;
        }

        for (final ActivityImpl activity : (List<ActivityImpl>) processDefinition.getActivities()) {
            activities.add(activity);
        }

        return activities;
    }

}
