package net.wohlfart.jbpm4.activity;

import java.util.Map;

import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.binding.AutomaticTaskBinding;

import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.JbpmException;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.model.Transition;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.history.HistoryEvent;
import org.jbpm.pvm.internal.history.events.TaskActivityStart;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * automatic task activity creates a task without timer and without form its
 * only propose is to assign a users to a dummy task, in order to refere to it
 * later for email generation or for bookkeeping, any transition to the next
 * activity has to be triggered programaticaly...
 * 
 * 
 * in a process definition a automaticTaskActivity looks like this:
 * 
 * <automaticTaskActivity g="72,60,145,36" name="changerequest.initialize">
 * <transition name="toCreateBusinessKey" to="changerequest.createBusinessKey"
 * /> <transition name="toComplete" to="changerequest.complete" /> </
 * automaticTaskActivity>
 * 
 * @author Michael Wohlfart
 * 
 */
public class AutomaticTaskActivity extends AbstractActivity {


    private final static Logger LOGGER = LoggerFactory.getLogger(AutomaticTaskActivity.class);

    @Override
    public void execute(final ActivityExecution execution) {
        execute((ExecutionImpl) execution);
    }

    public void execute(final ExecutionImpl execution) {
        LOGGER.debug("entering execute for task activity {} in execution {}", getName(), execution.getId());
        // DbSession is a wrapper for a real session object
        final DbSession dbSession = EnvironmentImpl.getFromCurrent(DbSession.class);

        // check parameters before we create anything in db
        final String name = getName(); // the name of the task to be generated,
                                       // must not be null
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null for automatic tasks, " + " check your process definition, you need an attribute "
                    + ATTRIBUTE_TASK_NAME + " in " + AutomaticTaskBinding.TAG);
        }

        // ////////////////////////////////////////
        // creating and persisting a task
        final TaskImpl task = dbSession.createTask();
        task.setExecution(execution);
        task.setProcessInstance(execution.getProcessInstance());
        task.setSignalling(true);
        task.setName(name);
        // things we can't do here:
        // no task definition
        // task.setTaskDefinition(taskDefinition);
        // no parent task
        // task.setSuperTask(currentTask);

        // assign the automatic task to the current logged in user
        // FIXME: do this in the script!
        final CharmsUser charmsUser = (CharmsUser) Contexts.lookupInStatefulContexts(CharmsUserIdentityStore.AUTHENTICATED_USER);
        if (charmsUser != null) {
            task.setAssignee(charmsUser.getActorId());
        } else {
            LOGGER.warn("no authenticated user found for task assignment in automatic task, "
                    + "there should be at least an authenticated user in the session, the automatic task can not be assigned " + "the task might get lost");
        }
        LOGGER.debug("saving created task with name {}, assigned to {}", name, charmsUser);
        dbSession.save(task);

        scriptActivity.setTask(task);
        scriptActivity.perform(execution);

        final TaskActivityStart taskActivityStartEvent = new TaskActivityStart(task);
        HistoryEvent.fire(taskActivityStartEvent, execution);

        execution.waitForSignal();
        LOGGER.debug("exiting execute for task/activity {}, called waitForSignal() on the execution", name);
    }

    @Override
    public void signal(final ActivityExecution execution, final String signalName, final Map<String, ?> parameters) throws Exception {
        signal((ExecutionImpl) execution, signalName, parameters);
    }

    /**
     * this method is invoced by: ExecutionService executionService =
     * processEngine.getExecutionService();
     * executionService.signalExecutionById(processInstance.getId(),
     * signalAfterInit);
     * 
     * the job is to complete the task and move the execution to the next node
     * through the requested transition
     * 
     * @param execution
     * @param signalName
     * @param parameters
     * @throws Exception
     */
    public void signal(final ExecutionImpl execution, final String signalName, final Map<String, ?> parameters) throws Exception {
        final ActivityImpl activity = execution.getActivity();
        LOGGER.debug("received signal {} for activity {}", signalName, activity);
        final DbSession dbSession = EnvironmentImpl.getFromCurrent(DbSession.class);

        // there shouldn't be many variables here
        if (parameters != null) {
            execution.setVariables(parameters);
        }

        // to notify any listeners
        execution.fire(signalName, activity);

        // complete the task
        final TaskImpl task = dbSession.findTaskByExecution(execution);
        if (task != null) {
            task.setSignalling(false);
            task.complete(signalName);
        } else {
            throw new JbpmException("No task was found in execution at " + AutomaticTaskBinding.TAG);
        }

        // resolving which transition to take
        final Transition transition = activity.findOutgoingTransition(signalName);
        if (transition != null) {
            execution.take(transition);
        } else {
            throw new JbpmException("No transition named '" + signalName + "' was found in " + AutomaticTaskBinding.TAG);
        }
    } // end of the signal method

}
