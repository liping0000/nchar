package net.wohlfart.jbpm4.command;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jbpm.api.Execution;
import org.jbpm.api.JbpmException;
import org.jbpm.api.cmd.Command;
import org.jbpm.api.cmd.Environment;
import org.jbpm.api.model.Activity;
import org.jbpm.api.model.Transition;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * no longer needed, this is implemented in the CustomTaskActivity
 * class now by handling the spawn signals
 * 
 * @author Michael Wohlfart
 */
@Deprecated
public class SpawnExecution implements Command<ExecutionImpl> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SpawnExecution.class);

    private final String transitionName;
    private final String executionId;
    private final Map<String, ?> variables;

    public SpawnExecution(final String executionId, final String spawnTransitionName) {

        LOGGER.info("SpawnExecution constructor, executionId is {}, spawnTransitionName is {} "
                , executionId, spawnTransitionName);

        transitionName = spawnTransitionName;
        this.executionId = executionId;
        variables = new HashMap<String, Object>();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SpawnExecution(final String executionId, final String spawnTransitionName, final Map<String, ?> variables) {

        LOGGER.info("SpawnExecution constructor, executionId is {}, spawnTransitionName is {} "
                , executionId, spawnTransitionName);

        transitionName = spawnTransitionName;
        this.executionId = executionId;
        this.variables = variables;

        final Set set = variables.entrySet();
        for (final Entry<String, Object> entry : (Set<Entry<String, Object>>) set) {
            LOGGER.debug(" #### found variable: {} value is: ", entry.getKey(), entry.getValue());
        }
    }

    @Override
    public ExecutionImpl execute(final Environment environment) throws Exception {
        ExecutionImpl execution = null;

        final DbSession dbSession = environment.get(DbSession.class);
        execution = (ExecutionImpl) dbSession.findExecutionById(executionId);
        if (execution == null) {
            throw new JbpmException("execution " + executionId + " does not exist");
        }

        final ActivityImpl currentActivity = execution.getActivity();
        if (!currentActivity.hasOutgoingTransition(transitionName)) {
            throw new JbpmException("outgoing transition  " + transitionName + " does not exist");
        } else {
            final Transition transition = currentActivity.getOutgoingTransition(transitionName);
            final Activity target = transition.getDestination();
            // creating the subexecution
            final ExecutionImpl concurrentExecution = execution.createExecution();
            concurrentExecution.setSuperProcessExecution(execution);

            // FIXME: check using: concurrentExecution.setVariables(variables);
            final Iterator<String> keys = variables.keySet().iterator();
            while (keys.hasNext()) {
                final String key = keys.next();
                LOGGER.debug(" *** found variable: " + key + " value is: " + variables.get(key));
                concurrentExecution.createVariable(key, variables.get(key));
            }

            // inherit the key
            concurrentExecution.setKey(execution.getKey());
            concurrentExecution.setActivity(target);
            concurrentExecution.setState(Execution.STATE_ACTIVE_CONCURRENT);
            concurrentExecution.take(transition);

            // fire the task start event for our custom listeners...
            //
            // guess we don't need that since the custom tasks have their own
            // start events being fired...
            // LOGGER.warn("firing task start event...");
            // concurrentExecution.fire(CustomTaskActivity.TASK_START_EVENT,
            // concurrentExecution.getActivity());

            return concurrentExecution;
        }
    }

}
