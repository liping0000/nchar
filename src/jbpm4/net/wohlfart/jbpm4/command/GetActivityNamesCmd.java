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


public class GetActivityNamesCmd extends AbstractCommand<List<String>> {

    private final static Logger LOGGER = LoggerFactory.getLogger(GetActivityNamesCmd.class);

    String processDefinitionId;

    public GetActivityNamesCmd(final String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        final List<String> activityNames = new ArrayList<String>();

        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);

        for (final ActivityImpl activity : (List<ActivityImpl>) processDefinition.getActivities()) {
            activityNames.add(activity.getName());
        }
        return activityNames;
    }

}
