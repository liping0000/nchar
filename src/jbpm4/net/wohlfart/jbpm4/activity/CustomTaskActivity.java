package net.wohlfart.jbpm4.activity;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.TransitionNotFoundException;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.apache.commons.lang.StringUtils;
import org.jbpm.api.Execution;
import org.jbpm.api.JbpmException;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.job.Job;
import org.jbpm.api.job.Timer;
import org.jbpm.api.model.Activity;
import org.jbpm.api.model.Event;
import org.jbpm.api.model.Transition;
import org.jbpm.api.task.Participation;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.hibernate.DbSessionImpl;
import org.jbpm.pvm.internal.history.HistoryEvent;
import org.jbpm.pvm.internal.history.events.TaskActivityStart;
import org.jbpm.pvm.internal.history.events.TaskComplete;
import org.jbpm.pvm.internal.job.JobImpl;
import org.jbpm.pvm.internal.job.TimerImpl;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.EventImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl.Propagation;
import org.jbpm.pvm.internal.model.TransitionImpl;
import org.jbpm.pvm.internal.model.op.AtomicOperation;
import org.jbpm.pvm.internal.model.op.ExecuteActivity;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.session.TimerSession;
import org.jbpm.pvm.internal.task.ParticipationImpl;
import org.jbpm.pvm.internal.task.SwimlaneImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.jbpm.pvm.internal.util.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 
 * syntax example:
 * 
 * <customTaskActivity g="228,108,97,37" 
 *     form="changerequest/complete.html" <-- html for for human task 
 *     name="changerequest.complete" <-- name of the generated task 
 *     groupActorName="admin" <-- group name (not actorId!) of static
 * assign group assignSwimlane= dueDate= reminderDate= reminderRepeat= >
 * 
 * 
 * </customTask>
 */
public class CustomTaskActivity extends AbstractActivity {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomTaskActivity.class);

    public final static String TASK_COMPLETED = "completed";
    private final static String DUE_REMINDER_INTERVAL = "1 day";


    /* ------------------- custom events -------------------- */
    // note that transitions also trigger events
    // this is the new start event, its async
    public static final String TASK_NOTIFY_EVENT = "taskNotify";  
    // this is a remind event used to send emails to the assignee or candidate
    public static final String TASK_REMIND_EVENT = "taskRemind";  
    // this is the due event
    public static final String TASK_DUE_EVENT = "taskDue"; 
    // only used in the term method
    // this is the end event
    public static final String TASK_END_EVENT = "taskEnd"; 

    /* ------------------- XML tag properties for parsing -------------------- */
    // needed for xml parsing during the binding phase:

    /** the tag for the url of the human task form */
    public static final String ATTRIBUTE_FORM_NAME = "form";
    /** the parsed value of the human task form */
    private String form;

    /** the tag for the name of the generated task */
    // public static final String TASK_NAME = "name";
    // the name of the task is stored in the parent class, we don't need a field
    // for this here

    /**
     * the name of a static group to which this task might be assigned if there
     * is no special swimlane, note this is the group/role actor name not the
     * group actor id
     */
    public static final String GROUP_ASSIGN = "groupActorName";
    /** parsed value */
    private String groupActorName = null;

    /** transitions that create one or more subexecutions */
    public static final String SPAWN_SIGNALS = "spawnSignals";
    private String[] spawnSignals;

    /** transitions that terminate a subexecution or workflow */
    public static final String TERM_SIGNALS = "termSignals";
    private String[] termSignals;

    /*
     * ------------------- variable names needed for task creation --------------------
     */

    /**
     * variable names of the seam/action provided task properties,
     * 
     * whoever starts the execution is reponsible to set this variables when the
     * task is created the variable is not removed from the execution the caller
     * is also responsible for removing the variables
     */
    // the swimlane to assign the task to, fallback is to have a groupActorName
    // in the node definition to use for assignment, next fallback is to assign
    // the task to the current user
    public static final String ASSIGN_SWIMLANE = "assignSwimlane";
    // the due date of the next task, fallback is to have no due date at all
    public static final String DUE_DATE = "dueDate";
    // reminder date, fallback is no reminder at all
    public static final String REMINDER_DATE = "reminderDate";
    // reminder interval in a string ("1 day", "3 minutes" etc.)
    public static final String REMINDER_REPEAT = "reminderRepeat";

    /**
     * execute is called when the workflow reaches this activity, we create the
     * node specific tasks here
     */
    @Override
    public void execute(final ActivityExecution execution) {
        // DbSession is a wrapper for a real session object
        execute((ExecutionImpl) execution, EnvironmentImpl.getFromCurrent(DbSessionImpl.class));
    }

    /**
     * signal is called when the user interacts with the task/activity/workflow
     */
    @Override
    public void signal(final ActivityExecution execution, 
            final String signalName, final Map<String, ?> parameters) throws Exception {
        signal((ExecutionImpl) execution, signalName, parameters);
    }

    /**
     * - find the swimlane or create a swimlane to assign the task to - find
     * dueDate - find remindDate - find reminderInterval - call
     * customTaskExecute to create and fire up the task
     */
    public void execute(final ExecutionImpl execution, final DbSessionImpl dbSession) {
        LOGGER.info("entering execute for task activity {} in execution {}", getName(), execution.getId());

        // task name from the parent
        final String name = super.getName();
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null for custom tasks, check your process definition!");
        }

        // /// the variable to resolve the assignment for the next task:
        final Date dueDate = (Date) execution.getVariable(DUE_DATE);
        final Date reminderDate = (Date) execution.getVariable(REMINDER_DATE);
        final String reminderRepeat = (String) execution.getVariable(REMINDER_REPEAT);
        LOGGER.info("found variables: dueDate: {}, reminderDate: {}, reminderRepeat: {} ", 
                new Object[] { dueDate, reminderDate, reminderRepeat });

        // /// only thing left now is how to resolve the swimlane...
        final Object object = execution.getVariable(ASSIGN_SWIMLANE); 
        LOGGER.info("swimlane object is: {}, the execution is {}", object, execution);
        if ((object != null) && (object instanceof SwimlaneImpl)) {
            // 1) check if we have a swimlane in the environment, if we do, use it:
            // this makes sense for forwards or single user assignments where a swimlane is
            // in the session or conversation environment. It is also used for spawning
            // single user or swimlane executions/tasks, the assignee or group is set as swimlane
            // in the context of the execution and the transition.take() method is called ending
            // in this execute method and picking up the asignee(s)
            // a variable can be set directly in the execution like this:
            // concurrentExecution.setVariable(ASSIGN_SWIMLANE_VAR,
            // "some value here");
            LOGGER.info("assigning to provided swimlane, found a swimlane in environment");
            final SwimlaneImpl swimlane = (SwimlaneImpl) object;
            LOGGER.debug("removing from {}[{}] key: {}", 
                    new Object[]{execution, execution.hashCode(), ASSIGN_SWIMLANE});
            execution.removeVariable(ASSIGN_SWIMLANE); // cleanup, only remove if we actually got one
            customTaskExecute(execution, dbSession, swimlane, dueDate, reminderDate, reminderRepeat);

        } else if (groupActorName != null) {
            // 2) check if there is a static group assignment for this task in
            // the workflow definition
            // this makes sense for workflow specific assignments, where the
            // user doesn't decide
            // who has to take the next task, it doesn't make sense to assign
            // a task to a single static user since this user might not be
            // available or
            // this user might change, so we resolve the group by its provided
            // name
            // and create a swimlane with it
            LOGGER.info("assignment to static group found, name of group is: {}, resolving actorId for this group...", groupActorName);
            final String groupActorId = (String) dbSession.getSession()
            .getNamedQuery(CharmsRole.FIND_ACTOR_ID_BY_NAME)
            .setParameter("name", groupActorName)
            .uniqueResult();
            if (groupActorId == null) {
                LOGGER.warn("no role found for group name {}, there will be no task for this activity", groupActorName);
            } else {
                LOGGER.info("...resolved actorId for groupActorName: {} -> groupActorId: {}", groupActorName, groupActorId);
                final SwimlaneImpl swimlane = new SwimlaneImpl();
                swimlane.addCandidateGroup(groupActorId);
                customTaskExecute(execution, dbSession, swimlane, dueDate, reminderDate, reminderRepeat);
            }
        } else {
            // 3) check if there is a authenticated user for assigning this task
            // this is used as final fallback
            LOGGER.info("trying to assign task to the current user");
            // final CharmsUser charmsUser = (CharmsUser)
            // EnvironmentImpl.getCurrent().get("authenticatedUser");
            final CharmsUser charmsUser = net.wohlfart.jbpm4.CustomIdentityService.findAuthenticatedUser();
            if (charmsUser == null) {
                LOGGER.warn("no current user found, can't assign or create a task here");
            } else {
                LOGGER.info("assigning next task to authenticated user: {}", charmsUser.getLabel());
                final SwimlaneImpl swimlane = new SwimlaneImpl();
                swimlane.setAssignee(charmsUser.getActorId());
                customTaskExecute(execution, dbSession, swimlane, dueDate, reminderDate, reminderRepeat);
            }
        }

        LOGGER.info("removing variables: dueDate: {}, reminderDate: {}, reminderRepeat: {} ", 
                new Object[] { dueDate, reminderDate, reminderRepeat });
        execution.removeVariable(DUE_DATE);
        execution.removeVariable(REMINDER_DATE);
        execution.removeVariable(REMINDER_REPEAT);

        // needed to get the tasks in the MessageEntryFactory, the query doesn't
        // work without a flush here
        dbSession.flush();

        LOGGER.info("exiting execute for task activity '{}'", getName());
    }

    /**
     * Execute for CustomTaskActivity means creating a task and assigning a
     * user/group to it this is exactly what this method does
     * 
     * this method is responsible for creating the task for this activity
     * 
     * @param execution
     * @param dbSession
     * @param swimlane
     *            the candidate(s) for the next task
     */
    private void customTaskExecute(final ExecutionImpl execution, 
            final DbSessionImpl dbSession, final SwimlaneImpl swimlane, // swimlane to assign task to
            final Date dueDate, // duedate for the task or null
            final Date remindDate, // first reminder date or null
            final String reminderInterval) { // reminder interval or null

        LOGGER.info("running customTaskExecute method to create and assign a task" 
                + " swimlane: {} dueDate: {} remindDate: {} reminderInterval: {}", 
                new Object[] { swimlane, dueDate, remindDate, reminderInterval });
        // there is probably no current task ???
        // TaskImpl currentTask = execution.getTask();
        final TaskImpl task = dbSession.createTask();
        // this is needed since the script might want to pick up the task
        // Contexts.getEventContext().set("task", task);

        // we don't have a task definition
        // task.setTaskDefinition(taskDefinition);
        task.setExecution(execution);
        // we don't know the super task here
        // task.setSuperTask(currentTask);
        task.setProcessInstance(execution.getProcessInstance());
        task.setSignalling(true);
        task.setName(name);
        task.setFormResourceName(form);
        task.setDuedate(dueDate);

        // move the assignee and participation from the swimlane to the task:
        task.setAssignee(swimlane.getAssignee());
        for (final Participation participation : swimlane.getParticipations()) {
            // this creates candidates and other kinds of participations..
            task.addParticipation(participation.getUserId(), participation.getGroupId(), participation.getType());
        } // FIXME: we might have a case where the swimlane exists but is empty, 
        // meaning no participiants or assignee

        dbSession.save(task); // this is needed to be able to create the timers

        // timer stuff follows
        // TimerSession timerSession =
        // EnvironmentImpl.getFromCurrent(TimerSession.class);
        // usual way to create a time is timerSession.schedule(timer)
        // this sync the timer at the end of the transaction...
        // however we manually trigger the notification to sync
        /*  */
        // // create a task notify event for sending mails async to the owner of
        // the new task
        // TimerImpl timer1 = execution.createTimer();
        // TimerImpl timer1 = task.createTimer();

        final TimerSession timerSession = EnvironmentImpl.getCurrent().get(TimerSession.class);

        final TimerImpl notifyTimer = execution.createTimer();
        notifyTimer.setDueDate(Clock.getTime()); // now
        notifyTimer.setRepeat(null);
        notifyTimer.setEventName(TASK_NOTIFY_EVENT);
        notifyTimer.setExclusive(false);
        timerSession.schedule(notifyTimer);

        // // setup a timer as reminder for the task if we have a reminder date
        if (remindDate != null) {
            final TimerImpl remindTimer = execution.createTimer();
            remindTimer.setDueDate(remindDate);
            remindTimer.setRepeat(reminderInterval);
            remindTimer.setEventName(TASK_REMIND_EVENT);
            remindTimer.setExclusive(false);
            timerSession.schedule(remindTimer);
        }

        // // setup a timer as due reminder for good meassure
        if (dueDate != null) {
            final TimerImpl dueTimer = execution.createTimer();
            dueTimer.setDueDate(dueDate);
            dueTimer.setRepeat(DUE_REMINDER_INTERVAL);
            dueTimer.setEventName(TASK_DUE_EVENT);
            dueTimer.setExclusive(false);
            timerSession.schedule(dueTimer);
        }

        // Transaction transaction =
        // EnvironmentImpl.getFromCurrent(Transaction.class);
        /*
         * syncs are done when using schedule! org.hibernate.Transaction
         * transaction = dbSession.getSession().getTransaction(); JobExecutor
         * jobExecutor = EnvironmentImpl.getFromCurrent(JobExecutor.class);
         * transaction.registerSynchronization(new
         * JobAddedNotification(jobExecutor));
         */
        // we don't need that if we have the task context push on the context
        // stack
        // HashMap<String, Object> map = new HashMap<String, Object>();
        // map.put("task", task);
        // execution.setElContext(map); // a transient el context
        /*
         * EnvironmentImpl environment = EnvironmentImpl.getCurrent();
         * TaskContext taskContext = new TaskContext(task);
         * environment.setContext(taskContext); try { // in the script we can do
         * any custom stuff with the domain data // we just have to wrap a task
         * context around it new ScriptExpression(getScript(),
         * getScriptLanguage()).evaluateInScope(execution); // execution scope
         * is not used!! } finally { environment.removeContext(taskContext); }
         */

        scriptActivity.setTask(task);
        scriptActivity.perform(execution);

        HistoryEvent.fire(new TaskActivityStart(task), execution);

        // dbSession.save(task); already saved
        execution.waitForSignal();
    }

    // @Override
    public void signal(final ExecutionImpl execution, final String signalName, final Map<String, ?> parameters) throws Exception {

        LOGGER.info("signal called for custom task activity: " + " signalName: " + signalName + " execution: " + execution + " parameters: " + parameters);

        // dealing with spawns

        if ((spawnSignals != null) && (Arrays.asList(spawnSignals).contains(signalName))) {
            // need to do a spawn here, this requires special treatment with
            // some user input
            LOGGER.info("found a spawn: {}", signalName);
            spawnSignal(execution, signalName);
            LOGGER.info("end of signal method call, returning from spawn");
            return;
        } // end of spawn handling code

        // dealing with terms

        if ((termSignals != null) && (Arrays.asList(termSignals).contains(signalName))) {
            // need to terminate the execution here
            LOGGER.info("found a term: {}", signalName);
            termSignal(execution, signalName);
            LOGGER.info("end of signal method call, returning from term");
            return;
        } // end of term handling code

        // default is to do an ordinary transition...

        // canceling the timers here might get us a concurrent modification and
        // a stale object exception if we use the timer session...
        // cancelAllTimers(execution);

        // signal to take the transition
        LOGGER.info("found an ordinary signal for a transition: {}", signalName);
        customTaskSignal(execution, signalName, parameters); // only thing left
        // to do is an
        // ordinary signal
        LOGGER.info("end of signal method call, returning from signal/transition");

    }

    private void cancelAllTimers(final ExecutionImpl execution) {
        LOGGER.debug("terminating all timers for the current execution: {}", execution);
        final TimerSession timerSession = EnvironmentImpl.getFromCurrent(TimerSession.class, false);
        if (timerSession == null) {
            LOGGER.warn("timer session is null, can't cancel all timers");
            return;
        }

        LOGGER.debug("destroying timers for {}", this);
        final List<Timer> timers = timerSession.findTimersByExecution(execution);
        final Job job = EnvironmentImpl.getFromCurrent(JobImpl.class, false);
        for (final Timer timer : timers) {
            // not sure but this might check if the signal comes from the
            // current job so we
            // don't cancel ourselves here...
            if (timer != job) {
                if (((TimerImpl) timer).getLockOwner() == null) {
                    timerSession.cancel(timer); // just deletes the timer from
                    // the session
                } else {
                    // if the timer has no repeat its not a big thing
                    if (timer.getRepeat() == null) {
                        LOGGER.info("timer is locked, can't cancel timer {}, owner is {}, lock expires {}, repeat is {}, retries is {}", new Object[] { timer,
                                timer.getLockOwner(), timer.getLockExpirationTime(), timer.getRepeat(), timer.getRetries() });
                    } else {
                        LOGGER.warn("repeating timer is locked, can't cancel timer {}, owner is {}, lock expires {}, repeat is {}, retries is {}",
                                new Object[] { timer, timer.getLockOwner(), timer.getLockExpirationTime(), timer.getRepeat(), timer.getRetries() });

                    }
                    // can't do this:
                    // optimistic locking failed; nested exception is
                    // org.hibernate.StaleObjectStateException: Row was updated
                    // or deleted by another transaction (or unsaved-value
                    // mapping was incorrect):
                    // [org.jbpm.pvm.internal.job.TimerImpl#3]
                    // we can't modify the timer while it is being used in
                    // another transaction
                    // ((TimerImpl)timer) .setRepeat(null);
                    // ((TimerImpl)timer) .setRetries(0);
                }
            }
        }
    }

    /**
     * copied from the task activity, a minimalistic signaling method for taking
     * a transition
     * 
     * @param execution
     * @param signalName
     * @param parameters
     * @throws Exception
     */
    private void customTaskSignal(final ExecutionImpl execution, final String signalName, final Map<String, ?> parameters) throws Exception {
        final ActivityImpl activity = execution.getActivity();
        LOGGER.info("receiving signal {}", signalName);

        final DbSession dbSession = EnvironmentImpl.getFromCurrent(DbSession.class);
        final TaskImpl currentTask = dbSession.findTaskByExecution(execution);

        final TransitionData transitionData = (TransitionData) currentTask.getVariable(TransitionData.TRANSITION_DATA);
        /*
        // we need to roll back, if there is no transition data
        if (transitionData == null) {
            throw new JbpmException("no transition data found in current task " + currentTask 
                    + " invoked signal is " + signalName
                    + " execution is " + execution);
        }
         */
        if (transitionData == null) {
            LOGGER.warn("no transition data found in current task {} invoked signal is {} execution is {}, we continue without transition data",
                    new Object[] {currentTask, signalName, execution});
        } else {
            final SwimlaneImpl swimlane = transitionData.getNextSwimlane();
            execution.createVariable(ASSIGN_SWIMLANE, swimlane);
            LOGGER.info("created a swimlane variable in the execution, execution is {}", execution);
            final Date date = transitionData.getDueDate();
            execution.createVariable(DUE_DATE, date);
            LOGGER.info("created a duedate variable in the execution, execution is {}", execution);
            // currentTask.setVariable(key, swimlane)
        }

        // we don't allow unnamed signals/transitions
        final TransitionImpl outgoing = activity.findOutgoingTransition(signalName);
        if (outgoing == null) {
            throw new TransitionNotFoundException("No transition named '" + signalName + "' was found." + " spawnSignals are " + Arrays.toString(spawnSignals)
                    + " termSignals are " + Arrays.toString(termSignals));
        }

        LOGGER.debug("currentTask is: {}", currentTask);
        LOGGER.debug("dbSession is: {}, {} ", dbSession.hashCode(), dbSession);
        LOGGER.debug("execution is: {}, {} ", execution.hashCode(), execution);

        // this triggers the even listeners by hooking a atomic operation into
        // the queue
        // they are picked up from the queue after this method returns...
        execution.fire(signalName, activity);

        // mark the task in the history table as completed, otherwise the tasks
        // will pile up
        // in the history table
        currentTask.historyTaskComplete(signalName);

        // ---->
        // FIXME: we need to set up the swimlane that is picked up in the
        // execute method here!!

        // execution.take(outgoing);
        // this is the take call:
        execution.setPropagation(Propagation.EXPLICIT);
        execution.setTransition(outgoing);
        // this calls the nexts tasks's execute method...
        execution.fire(Event.END, execution.getActivity(), AtomicOperation.TRANSITION_END_ACTIVITY);

        // deleting the task from the db is enqueued as atomic operation so the
        // following script can access to the task
        // otherwise the task would have been removed here before the script is
        // called...
        execution.performAtomicOperationSync(new ExecuteActivity() {

            @Override
            public boolean isAsync(final ExecutionImpl execution) {
                throw new UnsupportedOperationException("should be called on performAtomicOperationSync");
            }

            @Override
            public void perform(final ExecutionImpl execution) {
                dbSession.delete(currentTask);
//                dbSession.update(execution);
            }
        });

        LOGGER.info("finished signal {}", signalName);
    }

    private void spawnSignal(final ExecutionImpl execution, final String signalName) throws Exception {
        final DbSessionImpl taskDbSession = EnvironmentImpl.getFromCurrent(DbSessionImpl.class);
        final TaskImpl currentTask = taskDbSession.findTaskByExecution(execution);

        // a task scoped variable containing the UI data for the next transition
        final TransitionData transitionData = (TransitionData) currentTask.getVariable(TransitionData.TRANSITION_DATA);
        // we need to roll back, if there is no transition data
        if (transitionData == null) {
            throw new JbpmException("no transition data found in current task " + currentTask);
        }
        final Set<ParticipationImpl> participations = transitionData.getNextParticipations();

        // we need to roll back, this is critical since some action might
        // already happend due to atomic operations...
        if ((participations == null) || (participations.size() == 0)) {
            throw new JbpmException("participations set is empty, there are no receiver for the spawned execution ");
        }

        // get the user input to do the right thing somehow:
        // * transitionData contains a swimlane configured from the user via UI
        // or hardcoded in the xhtml page for the signal
        // * only the set of protected Set<ParticipationImpl> participations =
        // new HashSet<ParticipationImpl>(); is used
        // * for each participation of type "owner" a new spawn is created,
        // there is a swimlane in the new
        // spawn with one owner(user) in each as the assignee of the swimlane so
        // it can simply be copied over to the task
        // which is created
        // * for all participations of type "candidate" a common spawn is
        // created, the spawn contains a swimlane with
        // only the candidates and no assignee

        // collecting candidates for the next task in this swimlane,
        // candidates might be users or groups whichever were selected in the UI
        // with the role "candidate", this lane is later added to spawnLanes if
        // there are any participiants
        final SwimlaneImpl candidateLane = new SwimlaneImpl();

        // collecting owners of the next task, this might require a spawn since  every owner
        // is a user and has its own instance of the task
        final Set<SwimlaneImpl> spawnLanes = new HashSet<SwimlaneImpl>();

        for (final ParticipationImpl participation : participations) {

            // it's an owner (group/user) we need at least a spawn for the user
            // and we need to resolve the group in order to spawn for each member...
            if (participation.getType().equals(Participation.OWNER)) {

                // single user lane
                final String userId = participation.getUserId();
                if (!StringUtils.isEmpty(userId)) {
                    final SwimlaneImpl ownerLane = new SwimlaneImpl();
                    ownerLane.setAssignee(participation.getUserId());
                    // one spawn for each owner
                    spawnLanes.add(ownerLane);
                    // spawn(execution, signalName, ownerLane);
                }

                final String groupId = participation.getGroupId();
                if (!StringUtils.isEmpty(groupId)) {
                    // resolve the role/group to get the users
                    final CharmsRole role = (CharmsRole) taskDbSession.getSession()
                    .getNamedQuery(CharmsRole.FIND_BY_ACTOR_ID)
                    .setParameter("actorId", groupId)
                    .uniqueResult();

                    final Set<CharmsUser> users = role.getMemberUsers();// .getAllMemberUsers();
                    for (final CharmsUser user : users) {
                        final SwimlaneImpl ownerLane = new SwimlaneImpl();
                        ownerLane.setAssignee(user.getActorId());
                        // one spawn for each member of an owning group
                        spawnLanes.add(ownerLane);
                        // spawn(execution, signalName, ownerLane);
                    }
                }

            // its not the owner so we don't need a multi spawn here    
            } else if (participation.getType().equals(Participation.CANDIDATE)) {
                // collect the candidates to create one single spawn for all of
                // them later...
                final String uid = participation.getUserId();
                final String gid = participation.getGroupId();
                if (StringUtils.isEmpty(uid) && StringUtils.isEmpty(gid)) {
                    LOGGER.warn("found a swimlane with empty uid/gid, as candidate, skipping this swimlane");
                } else {
                    candidateLane.addParticipation(uid, gid, Participation.CANDIDATE);
                }

            } else {
                LOGGER.warn("unknown participation type {}, " 
                        + " should be either " + Participation.OWNER 
                        + " or " + Participation.CANDIDATE,
                        participation.getType());
            }
        }

        // add the candidate lane for spawning if it is not empty
        if (candidateLane.getParticipations().size() > 0) {
            spawnLanes.add(candidateLane);
        }

        // check if after all the swimlane hackery we have some lanes at all
        // and we should do some spawning now...
        if (spawnLanes.size() == 0) {
            throw new JbpmException("candidateLane and ownerLanes set is empty, there are no receiver for the spawned execution ");
        }

        final Set<ExecutionImpl> spawnedExecutions = new HashSet<ExecutionImpl>();
        // do the spawn for each user/group member or whatever:
        final Date date = transitionData.getDueDate();
        for (final SwimlaneImpl lane : spawnLanes) {
            // this is where the spawn happens:
            final ExecutionImpl subExecution = spawn(execution, signalName, lane, date);           
            spawnedExecutions.add(subExecution);
        }

        // set the spawns as a variable
        // currentTask.setVariable("spawns", spawnedExecutions);

        // fire custom event for our mail and script listeners right after we  did the spawns
        // the signal is enqueued and performed as atomic operation in line
        execution.fire(signalName, execution.getActivity(), new ExecuteActivity() {
            @Override
            public void perform(final ExecutionImpl execution) {
                for (final ExecutionImpl subExecution : spawnedExecutions) {
                    subExecution.setState(Execution.STATE_ACTIVE_CONCURRENT);
                }
            }
        });

    }

    /**
     * spawns a single new executions and returns the new execution, the swimlane is
     * for the next task
     * 
     * @param execution the parent execution
     * @param signal the spawn signal
     * @param swimlane will be set as process variable in the new execution
     * @param dueDate will be set as process variable in the new execution
     * @return
     * @throws Exception
     */
    private synchronized ExecutionImpl spawn(
            final ExecutionImpl execution, 
            final String signal, 
            final SwimlaneImpl swimlane,
            final Date dueDate) throws Exception {
        LOGGER.info("spawn called");

        final Activity currentActivity = execution.getActivity();
        final Transition transition = currentActivity.getOutgoingTransition(signal);
        final Activity target = transition.getDestination();

        // creating the subexecution, the subexecution can be picked up with
        // ((ArrayList<ExecutionImpl>)
        // execution.getExecutions()).get(execution.getExecutions().size());
        // we need the name to pick it up in the script
        final ExecutionImpl concurrentExecution = execution.createExecution(); 
        concurrentExecution.setSuperProcessExecution(execution);
        // inherit the key
        concurrentExecution.setKey(execution.getKey());
        concurrentExecution.setActivity(target);
        // we set the state to created here and set it to
        // Execution.STATE_ACTIVE_CONCURRENT
        // in a completeAtomicActionLater ...
        concurrentExecution.setState(/* Execution.STATE_ACTIVE_CONCURRENT */Execution.STATE_CREATED);
        // .setVariable only sets the variable in the parent execution, so we
        // need create here
        LOGGER.debug("creating variables in concurrentExecution {}[{}]", 
                concurrentExecution, concurrentExecution.hashCode());
        LOGGER.debug("creating a swimlane variable {} -> {}", ASSIGN_SWIMLANE, swimlane);
        concurrentExecution.createVariable(ASSIGN_SWIMLANE, swimlane);
        LOGGER.info("created a duedate variable {} -> {}", DUE_DATE, dueDate);
        concurrentExecution.createVariable(DUE_DATE, dueDate);

        // concurrentExecution.createVariable(DUE_DATE, new Date());
        // concurrentExecution.createVariable(REMINDER_DATE,new Date());
        // concurrentExecution.createVariable(REMINDER_REPEAT, "10 mins");

        // to create a variable in the subexecution:
        // execution.createVariable("messageEntry", value, "hi-long", false);

        // this initiates a call to the execute() method of the activity in the
        // spawned execution
        // so the execute calls in the next taskActivity are called earlier than
        // the signal in the spawning task!
        concurrentExecution.take(transition);
        // the swimlane is no longer needed after the transition and creation of
        // the spaned execution and task assignment, the execute method takes care of removing the swimlane...
        // concurrentExecution.removeVariable(ASSIGN_SWIMLANE);

        LOGGER.debug("returned from take {}[{}] ", 
                new Object[] {concurrentExecution, concurrentExecution.hashCode()});

        LOGGER.info("returning spawned execution: " + concurrentExecution);
        return concurrentExecution;

    }

    /**
     * ends an execution if it is not a process instance returns the ended
     * execution
     * 
     * @param execution
     * @param signal
     * @return
     * @throws Exception
     */
    private ExecutionImpl termSignal(final ExecutionImpl execution, final String signalName) throws JbpmException {
        LOGGER.info("terminating execution {} with signal {}", execution, signalName);

        // only end the execution if it is not a process instance
        if (execution.getIsProcessInstance()) {
            throw new JbpmException("Can't terminate Process instance, you need an end node to terminate a process instance");
        }
        // FIXME: check for subexecutions here

        cancelAllTimers(execution);

        // end the task and the execution here
        final DbSession dbSession = EnvironmentImpl.getFromCurrent(DbSession.class);
        final TaskImpl task = dbSession.findTaskByExecution(execution);
        // TaskImpl task = (TaskImpl) execution.getTask();
        if (task != null) {

            // set to false so we don't signal another transition
            // but might need the signal to trigger an email...
            // having this set to true would call execution.signal in the
            // task.complete() method
            task.setSignalling(false);
            // in Signal.perform() before calling activityBehaviour.signal()
            // (this method)
            // Propagation is set to Propagation.UNSPECIFIED and proceed() is
            // called checking for non-ended execution
            // setting Propagation.EXPLICIT prevents the call of proceed() after
            // the return of this method
            execution.setPropagation(Propagation.EXPLICIT);
            // fire custom event for our mail listeners
            execution.fire(CustomTaskActivity.TASK_END_EVENT, execution.getActivity());

            // this just marks the history task (which is already in the history
            // table) as completed:
            HistoryEvent.fire(new TaskComplete(TASK_COMPLETED), execution);

            // we don't delete the execution, we just mark it as ended, the
            // execution will be deleted when the process finished
            execution.setState(Execution.STATE_ENDED);

            // enqueue an operation to delete the task and update the execution
            // when all the other operations completed, this way we still have
            // the task available
            // the the script is signaled to run and ScriptActivity can wrap a
            // task context
            execution.performAtomicOperationSync(new ExecuteActivity() {

                @Override
                public boolean isAsync(final ExecutionImpl execution) {
                    throw new UnsupportedOperationException("should be called on performAtomicOperationSync");
                }

                @Override
                public void perform(final ExecutionImpl execution) {
                    dbSession.delete(task);
                    dbSession.update(execution);
                }
            });
        } else {
            LOGGER.warn("no task found, this is suspicious, there should be a task to terminate");
        }
        return execution;
    }

    @Override
    public EventImpl getEvent(final String eventName) {
        final EventImpl event = super.getEvent(eventName);
        LOGGER.warn("resolved event {} for key {}, implement me", event, eventName);
        return event;
    }

    @Override
    public Map<String, ? extends Event> getEvents() {
        final Map<String, ? extends Event> events = super.getEvents();
        LOGGER.warn("resolved events {}, implement me", events);
        return events;
    }

    // ------------- getter and setter ---------------

    public String getForm() {
        return form;
    }

    public void setForm(final String form) {
        this.form = form;
    }

    public String getGroupActorName() {
        return groupActorName;
    }

    public void setGroupActorName(final String groupActorName) {
        this.groupActorName = groupActorName;
    }

    public void setSpawnSignals(final String[] spawnSignals) {
        //copy the array to avoid unintended side effects
        this.spawnSignals = Arrays.copyOf(spawnSignals, spawnSignals.length);
    }
    public String[] getSpawnSignals() {
        return spawnSignals;
    }

    public void setTermSignals(final String[] termSignals) {
        this.termSignals = Arrays.copyOf(termSignals, termSignals.length);
    }
    public String[] getTermSignals() {
        return termSignals;
    }

}
