package net.wohlfart.jbpm4.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
import net.wohlfart.changerequest.entities.MessageType;
import net.wohlfart.jbpm4.CustomIdentityService;
import net.wohlfart.jbpm4.activity.CustomTaskActivity;
import net.wohlfart.terminal.commands.PerformFixExecutionVariables;

import org.apache.commons.lang.StringUtils;
import org.jbpm.api.Execution;
import org.jbpm.api.TaskService;
import org.jbpm.api.task.Swimlane;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.hibernate.DbSessionImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.ParticipationImpl;
import org.jbpm.pvm.internal.task.SwimlaneImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// entryFactory in groovy
public class MessageEntryFactory implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(MessageEntryFactory.class);

    // maps a transition name to the type of the message
    private final static HashMap<String, MessageType> typeHash = new HashMap<String, MessageType>();
    {
        typeHash.put("forward", MessageType.FORWARD);
        typeHash.put("toComplete", MessageType.INITIAL_SAVE);
        typeHash.put("toCreateBusinessKey", MessageType.SUBMIT);
        typeHash.put("toCreateBusinessKey2", MessageType.SUBMIT2);
        typeHash.put("toDiscard", MessageType.DISCARD);
        typeHash.put("toCancel1", MessageType.CANCEL);
        typeHash.put("toProcess", MessageType.PROCESS);
        typeHash.put("toCancel2", MessageType.CANCEL);
        typeHash.put("toRealize", MessageType.REALIZE);
        typeHash.put("toFinish", MessageType.FINISH);
        
        typeHash.put("transfer", MessageType.TRANSFER);

        typeHash.put("implement", MessageType.IMPLEMENT);
        typeHash.put("implemented", MessageType.IMPLEMENT_REPLY);

        typeHash.put("review", MessageType.REVIEW);
        typeHash.put("reviewed", MessageType.REVIEW_REPLY);

        typeHash.put("handle", MessageType.HANDLE);
        typeHash.put("handled", MessageType.HANDLE_REPLY);
    }

    /**
     * this method is pure magic - creates a new message entry - pulls the
     * current "messageEntry" variable from the execution and appends the newly
     * created message entry at the end of its children - if there are
     * subexecutions with status "started" a reference to the newly created message entry is
     * also stored as "messageEntry" in the new subexecutions
     * 
     * the user/group is picked from the transitiondata UI component:
     *     final Long uid = transitionData.getReceiverUserId();
     *     final Long gid = transitionData.getReceiverGroupId();
     *
     * 
     * --> in process definition just use
     * entryFactory.createMessageEntry(transitionData, execution);
     * 
     * if you need to modify and data in a workflow script use the returned message entry
     * 
     * 
     * @param transitionData
     * @param execution
     * @return
     */
    public ChangeRequestMessageEntry createMessageEntry(
            final TransitionData transitionData, final ExecutionImpl execution) {
        // maybe we should add a parameter to enable/disable receiver lookup for the messages
        // since some messages require a receiver and some don't...

        final DbSessionImpl dbSession = EnvironmentImpl.getFromCurrent(DbSessionImpl.class);

        // find all executions that are just started and put them in a set
        final Collection<ExecutionImpl> subexecutions = execution.getExecutions();
        final Set<ExecutionImpl> started = new HashSet<ExecutionImpl>();
        for (final ExecutionImpl subexecution : subexecutions) {
            // we only want the fresh ones here:
            if (subexecution.getState().equals(Execution.STATE_CREATED)) {
                started.add(subexecution);
            }
        }

        LOGGER.info("started execution count: {}", started.size());

        // create a new message entry
        final ChangeRequestMessageEntry entry = new ChangeRequestMessageEntry();
        // check if we can deduct the type of the transition by the name
        final String transitionName = transitionData.getTransitionName();
        if (typeHash.containsKey(transitionName)) {
            entry.setType(typeHash.get(transitionName));
        } else {
            LOGGER.warn("transiton name {} has no messageentry type configured, there will be no message in the workflow for this transition", transitionName);
        }

        // current timestamp and user
        entry.setTimestamp(org.jbpm.pvm.internal.util.Clock.getTime());
        entry.setAuthor(CustomIdentityService.findAuthenticatedUser());

        // set the message content if there is one
        entry.setContent(transitionData.getMessage());

        // check if we have a single user receiver
        final Long uid = transitionData.getReceiverUserId();
        final Long gid = transitionData.getReceiverGroupId();

        if ((uid == null) && (gid == null)) {
            // might be an end transition
            LOGGER.debug("no user or groupd id found in transitionData, the transitionName is {}", transitionName);
        }

        if ((uid != null) && (gid != null)) {
            LOGGER.warn("found user and group id in transition data using both!");
        }

        if (uid != null) {
            final CharmsUser receiver = dbSession.get(CharmsUser.class, uid);
            entry.setReceiver(receiver);
            LOGGER.debug("setting user receiver {}", receiver);
        }

        if (gid != null) {
            final CharmsRole receiver = dbSession.get(CharmsRole.class, gid);
            entry.setReceiverGroup(receiver);
            LOGGER.debug("setting group receiver {}", receiver);
        }



        // append as child to the parent entry:
        ChangeRequestMessageEntry parentMessageEntry;
        parentMessageEntry = (ChangeRequestMessageEntry) execution
            .getVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE);
        if (parentMessageEntry == null) {
            // this is a fallback in case we can't find the variable we are looking for
            parentMessageEntry = (ChangeRequestMessageEntry) dbSession.getSession()
                .getNamedQuery(ChangeRequestMessageEntry.FIND_CURRENT_BY_EID)
                .setParameter("eid", execution.getDbid())
                .uniqueResult(); // its not always unique here!!   
        }
        // we have a serious problem if this is still null here 
        if (parentMessageEntry == null) {
            throw new IllegalArgumentException("can't find the parent message entry, the execution data are most likely corrupted,"
                    + " run '" + PerformFixExecutionVariables.COMMAND_STRING + "' ");
        }
        parentMessageEntry.addChild(entry); // sets the parent

        // push in the newly created executions:
        // note: we push the same instance in each spawn, since we have just one instance
        // the createMessageEntries method creates new instances for each spawn
        for (final ExecutionImpl exec : started) {
            exec.createVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE, entry);
        }

        return entry;
    }


    /**
     * this method creates multiple message entries for each spawned subexecution
     * 
     * the user data is picked from the execution by analyzing the task in the execution
     * and resolving assignee and participations
     * 
     * @param transitionData
     * @param execution
     * @return
     */
    public List<ChangeRequestMessageEntry> createMessageEntries(
            final TransitionData transitionData, final ExecutionImpl execution) {

        List<ChangeRequestMessageEntry> result = new ArrayList<ChangeRequestMessageEntry>();
        final DbSessionImpl dbSession = EnvironmentImpl.getFromCurrent(DbSessionImpl.class);

        // find all executions that are just started and put them in a set
        final Collection<ExecutionImpl> subexecutions = execution.getExecutions();
        final Set<ExecutionImpl> started = new HashSet<ExecutionImpl>();
        for (final ExecutionImpl subexecution : subexecutions) {
            // we only want the fresh ones here:
            if (subexecution.getState().equals(Execution.STATE_CREATED)) {
                started.add(subexecution);
            }
        }

        MessageType type = null;
        LOGGER.info("started execution count: {}", started.size());
        // check if we can deduct the type of the transition by the name
        final String transitionName = transitionData.getTransitionName();
        if (typeHash.containsKey(transitionName)) {
            type = typeHash.get(transitionName);
        } else {
            LOGGER.warn("transiton name {} has no messageentry type configured", transitionName);
        }


        // create a new message entry for each subexecution, we find the task in the
        // execution and use the assignee as recipient of the message...
        for (final ExecutionImpl exec : started) {
            final ChangeRequestMessageEntry entry = new ChangeRequestMessageEntry();

            // get the data from the execution, the problem here is that the swimlane variable is removed 
            // in the custom execute method already so this doesn't  work:
            // final Object object = exec.getVariableObject(CustomTaskActivity.ASSIGN_SWIMLANE); 
            // we try to get the task in the execution
            // if (object == null) {
            //    LOGGER.warn("no swimlane for assigning in execution {}[{}]: {} --> {}", 
            //            new Object[] {exec, exec.hashCode(), CustomTaskActivity.ASSIGN_SWIMLANE, object});
            //    continue;
            //}
            //final SwimlaneImpl swimlane = (SwimlaneImpl) object;
            //String actorId = swimlane.getAssignee();
            // tis returns null, hardcoded
            //TaskImpl task = exec.getTask();
            LOGGER.debug("exec.getId(): {}", exec.getId());
            // TaskImpl task = dbSession.findTaskByExecution(exec);
            TaskImpl task = (TaskImpl) dbSession.createTaskQuery().executionId(exec.getId()).uniqueResult();
            if (task == null) {
                LOGGER.warn("no task found in execution, dbsession is: {}", dbSession.getSession());
                continue;
            }      


            // the simplest case is if we have a plain assignee
            String actorId = task.getAssignee();
            if (actorId != null) {
                final CharmsUser receiver = (CharmsUser) dbSession
                .getSession()
                .getNamedQuery(CharmsUser.FIND_BY_ACTOR_ID)
                .setParameter("actorId", actorId)
                .uniqueResult();
                if (receiver == null) {
                    LOGGER.warn ("no receiver found for actorId, transition is {}, actorId is {}", transitionName, actorId);
                } else {
                    entry.setReceiver(receiver);
                }
            } else {
                // we might have a participation here
                Set<ParticipationImpl> participations = task.getParticipations();
                // we only support exactly one participation so far
                if ( participations.size() > 1 ) {
                    LOGGER.warn("we have multiple participations for a single task, this is not yet supported");
                } else if (participations.size() == 1) {
                    ParticipationImpl participator = (ParticipationImpl) participations.toArray()[0];
                    String userActorId = participator.getUserId();
                    String groupActorId = participator.getGroupId();
                    
                    if (StringUtils.isNotEmpty(userActorId)) {
                        final CharmsUser receiver = (CharmsUser) dbSession
                        .getSession()
                        .getNamedQuery(CharmsUser.FIND_BY_ACTOR_ID)
                        .setParameter("actorId", userActorId)
                        .uniqueResult();
                        if (receiver == null) {
                            LOGGER.warn ("no receiver found for userActorId, transition is {}, userActorId is {}", transitionName, userActorId);
                        } else {
                            entry.setReceiver(receiver);
                        }
                    }
                    
                    if (StringUtils.isNotEmpty(groupActorId)) {
                        final CharmsRole receiver = (CharmsRole) dbSession
                        .getSession()
                        .getNamedQuery(CharmsRole.FIND_BY_ACTOR_ID)
                        .setParameter("actorId", groupActorId)
                        .uniqueResult();
                        if (receiver == null) {
                            LOGGER.warn ("no receiver found for groupActorId, transition is {}, groupActorId is {}", transitionName, groupActorId);
                        } else {
                            entry.setReceiverGroup(receiver);
                        }             
                    }
                }
            }

            // current timestamp and user
            entry.setTimestamp(org.jbpm.pvm.internal.util.Clock.getTime());
            entry.setAuthor(CustomIdentityService.findAuthenticatedUser());
            entry.setType(type);

            // set the message content if there is one
            entry.setContent(transitionData.getMessage());

            // append as child to the parent entry:
            final ChangeRequestMessageEntry parentMessageEntry = (ChangeRequestMessageEntry) execution
            .getVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE);
            parentMessageEntry.addChild(entry);  // sets the parent

            // push in the newly created executions:
            exec.createVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE, entry);
        }

        return result;
    }

}
