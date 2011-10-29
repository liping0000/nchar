package net.wohlfart.jbpm4.command;

import java.util.List;

import net.wohlfart.jbpm4.activity.CustomTaskActivity;

import org.jbpm.api.Execution;
import org.jbpm.api.JbpmException;
import org.jbpm.api.cmd.Command;
import org.jbpm.api.cmd.Environment;
import org.jbpm.api.job.Job;
import org.jbpm.api.job.Timer;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.job.JobImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.session.TimerSession;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminateExecution implements Command<Void> {


    private final static Logger LOGGER         = LoggerFactory.getLogger(TerminateExecution.class);

    public final static String  TASK_COMPLETED = "completed";

    private final String        taskId;

    public TerminateExecution(final String taskId) {

        this.taskId = taskId;
    }

    @Override
    public Void execute(final Environment environment) throws Exception {
        final DbSession dbSession = environment.get(DbSession.class);

        if ((taskId == null) || "".equals(taskId)) {
            throw new JbpmException("Cannot complete a task with a null or empty taskId");
        }

        final TaskImpl task = dbSession.get(TaskImpl.class, Long.parseLong(taskId));

        if (task == null) {
            throw new JbpmException("No task with id " + taskId + " was found");
        }

        LOGGER.debug("terminating execution for taskId: " + taskId);

        final TimerSession timerSession = EnvironmentImpl.getFromCurrent(TimerSession.class, false);
        if (timerSession != null) {
            LOGGER.debug("destroying timers of " + this);
            final List<Timer> timers = timerSession.findTimersByExecution(task.getExecution());
            for (final Timer timer : timers) {
                final Job job = EnvironmentImpl.getFromCurrent(JobImpl.class, false);
                if (timer != job) {
                    LOGGER.debug("cancel timer: " + timer);
                    timerSession.cancel(timer);
                }
            }
        }

        // set to false so we don't signal another transition
        task.setSignalling(false);
        // changes in jbpm4.4: we need to specify the end signal
        task.complete(TASK_COMPLETED);
        dbSession.delete(task);
        final ExecutionImpl execution = (ExecutionImpl) dbSession.findExecutionById(task.getExecutionId());
        execution.setState(Execution.STATE_ENDED);
        // fire custom event for our mail listeners...
        execution.fire(CustomTaskActivity.TASK_END_EVENT, execution.getActivity());
        dbSession.save(execution);
        return null;
    }

}
