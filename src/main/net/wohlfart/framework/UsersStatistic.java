package net.wohlfart.framework;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.ChangeRequestAction;
import net.wohlfart.framework.entities.CharmsWorkflowData;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jbpm.api.Execution;
import org.jbpm.pvm.internal.history.model.HistoryProcessInstanceImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * this class creates some statistic data that are shown in the home page
 * 
 * we calculate data for * open tasks -- directly assigned -- assigned as
 * candidate -- assigned without open subtasks -- assigned with open subtasks
 * 
 * * new requests -- number of drafts
 * 
 * * my requests -- draft -- ongoing -- completed
 * 
 */
@Scope(CONVERSATION)
@Name(value = "usersStatistics")
public class UsersStatistic implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(UsersStatistic.class);

    @In(value = "hibernateSession")
    private Session             hibernateSession;

    @In
    private CharmsUser          authenticatedUser;

    // a session variable set during login, might be empty if the user logs in
    // for the first time
    @In(scope = ScopeType.SESSION, required = false)
    private Date                lastLogin;

    // this is being calculated and contains the actorIds of any group where
    // this user
    // is a member of
    private List<String>        groupActorIds;

    // number of tasks that are assigned to the current user
    private Long                allDraftTasks;

    // number of tasks that are assigned to the current user
    private Long                allAssignedTasks;

    // ass assigned tasks without any open subexecutions
    private Long                allWorkableTasks;

    // number of tasks that are not assigned to the current user but
    // can be taken by the current user
    private Long                allCandidateTasks;

    // number of change requests from the current user that are still worked on
    private Long                allOngoingRequests;

    // number of change requests of the current user that are completed
    private Long                allCompletedRequests;

    // ass assigned tasks without any open subexecutions
    private Long                blockedTaskCount;

    // ass assigned tasks without any open subexecutions
    private Long                unblockedTaskCount;

    @Transactional
    public void refresh() {
        LOGGER.info("refresh called for usersStatistics, last login is: {} ", lastLogin);

        groupActorIds = new ArrayList<String>();
        collectGroupActorIds(authenticatedUser.getMemberships());
        // FIXME: this is a hack in case the user is in no group at all
        if (groupActorIds.size() == 0) {
            groupActorIds.add("'empty'");
        }

        LOGGER.info(" group actorIds of the current user to be used in the select: {} ", groupActorIds);

        calculateMyRequests();

        LOGGER.info("allAssignedTasks: {}", allAssignedTasks);
        LOGGER.info("allCandidateTasks: {}", allCandidateTasks);
        LOGGER.info("allWorkableTasks: {}", allWorkableTasks);
        LOGGER.info("allDraftTasks: {}", allDraftTasks);

        LOGGER.info("allOngoingRequests: {}", allOngoingRequests);
        LOGGER.info("allCompletedRequests: {}", allCompletedRequests);

        LOGGER.info("blockedTaskCount: {}", blockedTaskCount);
        LOGGER.info("unblockedTaskCount: {}", unblockedTaskCount);
    }

    @BypassInterceptors
    public Long getAllDraftTasks() {
        return allDraftTasks;
    }

    @BypassInterceptors
    public Long getAllAssignedTasks() {
        return allAssignedTasks;
    }

    @BypassInterceptors
    public Long getAllWorkableTasks() {
        return allWorkableTasks;
    }

    @BypassInterceptors
    public Long getAllCandidateTasks() {
        return allCandidateTasks;
    }

    @BypassInterceptors
    public Long getAllOngoingRequests() {
        return allOngoingRequests;
    }

    @BypassInterceptors
    public Long getAllCompletedRequests() {
        return allCompletedRequests;
    }

    @BypassInterceptors
    public Long getBlockedTaskCount() {
        return blockedTaskCount;
    }

    @BypassInterceptors
    public Long getUnblockedTaskCount() {
        return unblockedTaskCount;
    }

    // calculate all ActorId for the current user and store them in
    // groupActorIds
    private void collectGroupActorIds(final Set<CharmsMembership> memberships) {
        if ((memberships == null) || (memberships.size() == 0)) {
            return;
        }

        for (final CharmsMembership membership : memberships) {
            final String groupActorId = "'" + membership.getCharmsRole().getActorId() + "'";
            if (!groupActorIds.contains(groupActorId)) {
                groupActorIds.add(groupActorId);
                collectGroupActorIds(membership.getCharmsRole().getMemberships());
            }
        }

    }

    private void calculateMyRequests() {
        allAssignedTasks = (Long) hibernateSession.createQuery(getAllAssignedTasksQuery()).uniqueResult();
        allCandidateTasks = (Long) hibernateSession.createQuery(getAllCandidateTasksQuery()).uniqueResult();
        allWorkableTasks = (Long) hibernateSession.createQuery(getAllWorkableTasksQuery()).uniqueResult();
        allDraftTasks = (Long) hibernateSession.createQuery(getAllDraftTasksQuery()).uniqueResult();

        allOngoingRequests = (Long) hibernateSession.createQuery(getAllOngoingRequestsQuery()).uniqueResult();
        allCompletedRequests = (Long) hibernateSession.createQuery(getAllCompletedRequestsQuery()).uniqueResult();

        unblockedTaskCount = (Long) hibernateSession.createQuery(getUnblockedTaskCountQuery()).uniqueResult();
        blockedTaskCount = (Long) hibernateSession.createQuery(getBlockedTaskCountQuery()).uniqueResult();
    }

    // ------------------ methods to create sql strings
    // -----------------------------
    private String getUnblockedTaskCountQuery() {
        return "select count(t.dbid)" + " from "
                + TaskImpl.class.getName()
                + " t "

                // link with the data bean
                + " , "
                + CharmsWorkflowData.class.getName()
                + " d "
                + " where "
                + "  (t.processInstance.dbid = d.processInstanceId) " // the
                                                                      // process
                                                                      // id
                                                                      // value
                                                                      // must
                                                                      // match
                                                                      // the id
                                                                      // in the
                                                                      // data
                                                                      // container

                // exclude the drafts
                + " and ( t.name != '"
                + ChangeRequestAction.COMPLETE_TASK
                + "' ) "

                // exclude not directly assigned tasks (e.g. TQM group)
                // + " and (t.assignee is not null) "

                // either the task is assigned directly or via a participation
                // member, if it is assigned via a participation member
                // it must not be directly assigned to anyone else
                + " and " + " ( " + "  (t.assignee = '" + authenticatedUser.getActorId() + "')" + " or " + " ((t.assignee is null) and "
                + "  ( (select count(groupId) from t.participations where groupId in (" + StringUtils.collectionToCommaDelimitedString(groupActorIds)
                + " ))  > 0 )" + " )" + " or " + " ((t.assignee is null) and " + "  ( (select count(userId) from t.participations where userId = '"
                + authenticatedUser.getActorId() + "' )  > 0 )"
                + " )"
                + " ) " // end and clause

                // select number of not ended subexecutions:
                + " and ( " + " (select count(e.dbid) from " + ExecutionImpl.class.getName() + " e where "
                + "   e.parent.dbid = t.execution.dbid and e.state != '" + Execution.STATE_ENDED + "'" + "  ) = 0 )";
    }

    private String getBlockedTaskCountQuery() {
        return "select count(t.dbid)" + " from "
                + TaskImpl.class.getName()
                + " t "

                // link with the data bean
                + " , "
                + CharmsWorkflowData.class.getName()
                + " d "
                + " where "
                + "  (t.processInstance.dbid = d.processInstanceId) " // the
                                                                      // process
                                                                      // id
                                                                      // value
                                                                      // must
                                                                      // match
                                                                      // the id
                                                                      // in the
                                                                      // data
                                                                      // container

                // exclude the drafts
                + " and ( t.name != '" + ChangeRequestAction.COMPLETE_TASK + "' ) "

                + " and (t.assignee is not null) " + " and (t.assignee = '" + authenticatedUser.getActorId() + "')"

                // select number of not ended subexecutions:
                + " and ( " + " (select count(e.dbid) from " + ExecutionImpl.class.getName() + " e where "
                + "   e.parent.dbid = t.execution.dbid and e.state != '" + Execution.STATE_ENDED + "'" + "  ) > 0 )";
    }

    private String getAllCompletedRequestsQuery() {
        return "select count(h.dbid)" + " from " + HistoryProcessInstanceImpl.class.getName() + " h "

        // link with the data bean
                + " , " + CharmsWorkflowData.class.getName() + " d " + " where (h.dbid = d.processInstanceId) " // the
                                                                                                                // process
                                                                                                                // id
                                                                                                                // value
                                                                                                                // must
                                                                                                                // match
                                                                                                                // the
                                                                                                                // id
                                                                                                                // in
                                                                                                                // the
                                                                                                                // data
                                                                                                                // container

                + " and ( d.submitUser.id = " + authenticatedUser.getId() + ")"

                // there must be an end date in the history row in order to be
                // complete
                + " and (h.endTime is not null) ";
    }

    private String getAllOngoingRequestsQuery() {
        return "select count(h.dbid)" + " from " + HistoryProcessInstanceImpl.class.getName() + " h "

        // link with the data bean
                + " , " + CharmsWorkflowData.class.getName() + " d " + " where (h.dbid = d.processInstanceId) " // the
                                                                                                                // process
                                                                                                                // id
                                                                                                                // value
                                                                                                                // must
                                                                                                                // match
                                                                                                                // the
                                                                                                                // id
                                                                                                                // in
                                                                                                                // the
                                                                                                                // data
                                                                                                                // container

                + " and ( d.submitUser.id = " + authenticatedUser.getId() + ")"

                // there must be an end date in the history row in order to be
                // complete
                + " and (h.endTime is null) ";
    }

    private String getAllAssignedTasksQuery() {
        return "select count(t.dbid)" + " from " + TaskImpl.class.getName() + " t "

        // link with the data bean
                + " , " + CharmsWorkflowData.class.getName() + " d " + " where " + "  (t.processInstance.dbid = d.processInstanceId) " // the
                                                                                                                                       // process
                                                                                                                                       // id
                                                                                                                                       // value
                                                                                                                                       // must
                                                                                                                                       // match
                                                                                                                                       // the
                                                                                                                                       // id
                                                                                                                                       // in
                                                                                                                                       // the
                                                                                                                                       // data
                                                                                                                                       // container

                // exclude the drafts
                + " and ( t.name != '" + ChangeRequestAction.COMPLETE_TASK + "' ) "

                + " and (t.assignee is not null) " + " and (t.assignee = '" + authenticatedUser.getActorId() + "')";
    }

    private String getAllCandidateTasksQuery() {
        return "select count(t.dbid)"
                + " from "
                + TaskImpl.class.getName()
                + " t "

                // link with the data bean
                + " , "
                + CharmsWorkflowData.class.getName()
                + " d "
                + " where "
                + "  (t.processInstance.dbid = d.processInstanceId) " // the
                                                                      // process
                                                                      // id
                                                                      // value
                                                                      // must
                                                                      // match
                                                                      // the id
                                                                      // in the
                                                                      // data
                                                                      // container

                // exclude the drafts
                + " and ( t.name != '"
                + ChangeRequestAction.COMPLETE_TASK
                + "' ) "

                + " and (t.assignee is null) "

                // the current user is a candidate for this task, meaning the
                // user is somehow in the participations list of the task,
                // either one of his groups or his actorId as userId in a
                // candidate set entry
                + " and ( " + "  ( (select count(groupId) from t.participations where groupId in ("
                + StringUtils.collectionToCommaDelimitedString(groupActorIds) + " ))  > 0 )" + " or "
                + "  ( (select count(userId) from t.participations where userId = '" + authenticatedUser.getActorId() + "' )  > 0 )" + " ) " // end
                                                                                                                                             // and
                                                                                                                                             // clause
        ;
    }

    private String getAllWorkableTasksQuery() {
        return "select count(t.dbid)" + " from "
                + TaskImpl.class.getName()
                + " t "

                // link with the data bean
                + " , "
                + CharmsWorkflowData.class.getName()
                + " d "
                + " where "
                + "  (t.processInstance.dbid = d.processInstanceId) " // the
                                                                      // process
                                                                      // id
                                                                      // value
                                                                      // must
                                                                      // match
                                                                      // the id
                                                                      // in the
                                                                      // data
                                                                      // container

                // exclude the drafts
                + " and ( t.name != '" + ChangeRequestAction.COMPLETE_TASK + "' ) "

                + " and (t.assignee is not null) " + " and (t.assignee = '" + authenticatedUser.getActorId() + "')"

                // select number of not ended subexecutions:
                + " and ( " + " (select count(e.dbid) from " + ExecutionImpl.class.getName() + " e where "
                + "   e.parent.dbid = t.execution.dbid and e.state != '" + Execution.STATE_ENDED + "'" + "  ) = 0 )";
    }

    private String getAllDraftTasksQuery() {
        return "select count(t.dbid)" + " from " + TaskImpl.class.getName() + " t "

        // link with the data bean
                + " , " + CharmsWorkflowData.class.getName() + " d " + " where " + "  (t.processInstance.dbid = d.processInstanceId) " // the
                                                                                                                                       // process
                                                                                                                                       // id
                                                                                                                                       // value
                                                                                                                                       // must
                                                                                                                                       // match
                                                                                                                                       // the
                                                                                                                                       // id
                                                                                                                                       // in
                                                                                                                                       // the
                                                                                                                                       // data
                                                                                                                                       // container

                // only the drafts
                + " and ( t.name = '" + ChangeRequestAction.COMPLETE_TASK + "' ) "

                + " and (t.assignee is not null) " + " and (t.assignee = '" + authenticatedUser.getActorId() + "')"

        /*
         * this disables the blocked drafts: // number of not ended
         * subexecutions must be zero + " and ( " +
         * " (select count(e.dbid) from " + ExecutionImpl.class.getName() +
         * " e where " + "   e.parent.dbid = t.execution.dbid and e.state != '"
         * + Execution.STATE_ENDED + "'" + "  ) = 0 )"
         */
        ;
    }

}
