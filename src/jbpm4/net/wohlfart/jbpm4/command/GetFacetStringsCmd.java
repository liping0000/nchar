package net.wohlfart.jbpm4.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.jbpm4.activity.CustomTaskActivity;

import org.jbpm.api.activity.ActivityBehaviour;
import org.jbpm.api.cmd.Environment;
import org.jbpm.api.model.Transition;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.session.RepositorySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this gets the strings that are needed for the facets in the UI, containing: -
 * transition - existing term signals
 * 
 * @author Michael Wohlfart
 * 
 */
public class GetFacetStringsCmd extends AbstractCommand<Set<String>> {


    private final static Logger LOGGER = LoggerFactory.getLogger(GetFacetStringsCmd.class);

    String                      processDefinitionId;
    String                      activityName;

    public GetFacetStringsCmd(final String processDefinitionId, final String activityName) {
        this.processDefinitionId = processDefinitionId;
        this.activityName = activityName;
    }

    @Override
    public Set<String> execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");

        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
        if (processDefinition == null) {
            LOGGER.warn("no process definition found with processDefinitionId {} returning empty set", processDefinitionId);
            return new HashSet<String>();
        }

        final Set<String> result = new HashSet<String>();

        // add the transitions
        final ActivityImpl activity = processDefinition.getActivity(activityName);
        final List<? extends Transition> outgoing = activity.getOutgoingTransitions();
        // add one data panel for each outgoing transition
        for (final Transition transition : outgoing) {
            result.add(transition.getName());
        }

        // add the term signals if we have a CustomTaskActivity
        final ActivityBehaviour behaviour = activity.getActivityBehaviour();
        if (behaviour instanceof CustomTaskActivity) {
            final CustomTaskActivity customTaskActivity = (CustomTaskActivity) behaviour;
            final String[] term = customTaskActivity.getTermSignals();
            if (term != null) {
                for (final String signal : term) {
                    result.add(signal);
                }
            }
        }

        return result;
    }

}
