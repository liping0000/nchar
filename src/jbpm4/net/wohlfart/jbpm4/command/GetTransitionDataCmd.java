package net.wohlfart.jbpm4.command;

import java.util.List;

import net.wohlfart.jbpm4.node.TransitionConfig;

import org.jbpm.api.cmd.Environment;
import org.jbpm.api.listener.EventListener;
import org.jbpm.api.model.Event;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.EventImpl;
import org.jbpm.pvm.internal.model.EventListenerReference;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.model.TransitionImpl;
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
public class GetTransitionDataCmd extends AbstractCommand<TransitionConfig> {

    /**
     * 
     */
    private static final long   serialVersionUID = 1L;

    private final static Logger LOGGER           = LoggerFactory.getLogger(GetTransitionDataCmd.class);

    private final String        processDefinitionId;
    private final String        activityName;
    private final String        transitionName;

    public GetTransitionDataCmd(final String processDefinitionId, final String activityName, final String transitionName) {
        this.processDefinitionId = processDefinitionId;
        this.activityName = activityName;
        this.transitionName = transitionName;
    }

    @Override
    public TransitionConfig execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        // the result
        TransitionConfig transitionConfig = null;

        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
        final ActivityImpl activity = processDefinition.getActivity(activityName);
        final TransitionImpl transition = activity.getOutgoingTransition(transitionName);
        if (transition != null) {
            final EventImpl event = transition.getEvent(Event.TAKE);
            final List<EventListenerReference> listeners = event.getListenerReferences();
            // the config for the transition is hidden in the listener config
            if (listeners != null) {
                for (final EventListenerReference reference : listeners) {
                    final EventListener instance = reference.getEventListener();
                    if (instance instanceof TransitionConfig) {
                        transitionConfig = (TransitionConfig) instance;
                        LOGGER.debug("found a transitionConfig: " + transitionConfig);
                        break;
                    }
                }
            }
        }
        return transitionConfig;
    }

}
