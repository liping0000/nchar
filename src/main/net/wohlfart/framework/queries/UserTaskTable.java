package net.wohlfart.framework.queries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.ChangeRequestAction;
import net.wohlfart.changerequest.entities.Priority;
import net.wohlfart.framework.AbstractTableQuery;
import net.wohlfart.framework.entities.CharmsWorkflowData;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jbpm.api.Execution;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * list of all tasks for a specific user, this gives us the open task list
 * 
 * @author Michael Wohlfart
 * @version 0.1
 * 
 */

@Scope(ScopeType.CONVERSATION)
// @Scope(ScopeType.SESSION)
@Name(UserTaskTable.USER_TASK_TABLE)
public class UserTaskTable extends AbstractTableQuery<UserTaskTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserTaskTable.class);

    // needed to keep reference integrity in other components
    public static final String USER_TASK_TABLE = "userTaskTable";

    private Boolean showDraft = false;

    private Boolean showBlocked = true;
    private Boolean showUnblocked = true;

    //private CharmsUser authenticatedUser;

    private List<String> groupActorIds;

    private String baseEjbqlExpression;

    @Override
    @Transactional
    public void setup() {

        Session session = (Session) Component.getInstance("hibernateSession");
        
        CharmsUser charmsUser = (CharmsUser) Component.getInstance("authenticatedUser");
        //final Session hibernateSession = getSession();
        charmsUser = (CharmsUser) session.get(CharmsUser.class, charmsUser.getId());

        groupActorIds = new ArrayList<String>();
        // hibernateSession.l.load(authenticatedUser,
        // authenticatedUser.getId());

        // this sets up groupActorIds with the actorIds of all implied roles
        collectGroupActorIds(charmsUser.getMemberships());

        baseEjbqlExpression = "select new "
                + UserTaskTable.Row.class.getName()
                + "( "
                + " t.dbid" // database id for the task
                + ", t.processInstance.dbid" // database id for the process
                                             // instance in case we want to link
                                             // it
                + ", t.createTime" + ", t.duedate" + ", t.assignee" + ", t.progress"
                + ", d.title " // title from the data container
                + ", d.priority " + ", t.state " + ", t.name" + ", t.execution.key"
                + ", t.activityName" // the node
                + ", t.execution.name"

                // select number of not ended subexecutions:
                + ", (select count(e.dbid) from " + ExecutionImpl.class.getName() + " e where " + "   e.parent.dbid = t.execution.dbid and e.state != '"
                + Execution.STATE_ENDED + "'" + "  )"

                + ") "

                + " from " + TaskImpl.class.getName() + " t "

                // link with the data bean
                + " , " + CharmsWorkflowData.class.getName() + " d " + " where " + "  (t.processInstance.dbid = d.processInstanceId) "               
                // the process id value must match the id in the data container
        ;

        if (groupActorIds.size() == 0) {
            // no groups for the current user, just check the assignee value
            baseEjbqlExpression = baseEjbqlExpression + "and ( " + "  ((t.assignee is not null) " + "    and (t.assignee = '" + charmsUser.getActorId()
                    + "'))"
                    // or the current user is in the participation list
                    + " or ((t.assignee is null) and (" + "  (select count(userId) from t.participations where userId = '" + charmsUser.getActorId()
                    + "')" + "   > 0 ))" + ")";
        } else {
            baseEjbqlExpression = baseEjbqlExpression
                    + "and ( "
                    // either assignee matches
                    + "  ((t.assignee is not null) " + "    and (t.assignee = '" + charmsUser.getActorId()
                    + "'))"
                    // or the current user is in the participation list
                    + " or ((t.assignee is null) and (" + "  (select count(userId) from t.participations where userId = '" + charmsUser.getActorId()
                    + "')"
                    + "   > 0 ))"
                    // or we have a group of the current user in the
                    // participation list
                    + " or ((t.assignee is null) and (" + "  (select count(groupId) from t.participations where groupId in ("
                    + StringUtils.collectionToCommaDelimitedString(groupActorIds) + " ))" + "   > 0 ))" + ")";
        }
        ;

        setOrderDirection("desc");
        setOrderColumn("t.execution.key");
        // setRestrictionLogicOperator("or");
        calculateSelect();
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

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("t.execution.key", "d.title", "d.priority", "t.createTime"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    private void calculateSelect() {
        String qlExpression = baseEjbqlExpression;

        // apply the fragment filter
        if ((fragment != null) && (fragment.trim().length() > 0)) {
            qlExpression += " and " + " ( " + "   ( lower(d.title) like concat('%', lower(#{" + USER_TASK_TABLE + ".fragment}), '%') ) " + " or"
                    + "   ( lower(t.execution.key) like concat('%', lower(#{" + USER_TASK_TABLE + ".fragment}), '%') ) " + " )";
        }

        // don't show drafts filter
        if (!showDraft) {
            // if no drafts are wanted in the view
            qlExpression = qlExpression
            // + " and ( data.submitDate is not null ) ";
            // exclude them:
                    + " and ( t.name != '" + ChangeRequestAction.COMPLETE_TASK + "' ) ";
        }

        // don't show drafts filter
        if (!showBlocked) { // blocked means there are more than zero
                            // "not-ended" subexecution instances
            qlExpression = qlExpression + " and " + " ((select count(e.dbid) from " + ExecutionImpl.class.getName() + " e where "
                    + "   e.parent.dbid = t.execution.dbid and e.state != '" + Execution.STATE_ENDED + "'" + "  ) = 0)";
        }

        // don't show drafts filter
        if (!showUnblocked) { // unblocked means there are no "not-ended"
                              // subexecution instances
            qlExpression = qlExpression + " and " + " ((select count(e.dbid) from " + ExecutionImpl.class.getName() + " e where "
                    + "   e.parent.dbid = t.execution.dbid and e.state != '" + Execution.STATE_ENDED + "'" + "  ) > 0)";
        }

        setEjbql(qlExpression);
    }

    // this method is called tons of times but it's cheap so don't worry
    @Override
    public void refresh() {
        LOGGER.debug("refresh called");
        super.refresh();
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(t) ");

        final int order = stringBuilder.lastIndexOf(" order by ");
        if (order > 0) {
            final String countQuery = stringBuilder.substring(0, order).toString().trim();
            LOGGER.debug("created count query: {0}", countQuery);
            return countQuery;
        } else {
            final String countQuery = stringBuilder.toString().trim();
            LOGGER.debug("created count query: {0}", countQuery);
            return countQuery;
        }
    }

    @Override
    public void setFragment(final String fragment) {
        super.setFragment(fragment);
        calculateSelect();
    }

    @BypassInterceptors
    public Boolean getShowBlocked() {
        return showBlocked;
    }

    @BypassInterceptors
    public void setShowBlocked(final Boolean showBlocked) {
        this.showBlocked = showBlocked;
        calculateSelect();
    }

    @BypassInterceptors
    public Boolean getShowUnblocked() {
        return showUnblocked;
    }

    @BypassInterceptors
    public void setShowUnblocked(final Boolean showUnblocked) {
        this.showUnblocked = showUnblocked;
        calculateSelect();
    }

    @BypassInterceptors
    public Boolean getShowDraft() {
        return showDraft;
    }

    @BypassInterceptors
    public void setShowDraft(final Boolean showDraft) {
        this.showDraft = showDraft;
        calculateSelect();
    }

    @BypassInterceptors
    public String setupBlocked() {
        showBlocked = true;
        showUnblocked = false;
        calculateSelect();
        return "/pages/user/taskList.html";
    }

    @BypassInterceptors
    public String setupUnblocked() {
        showBlocked = false;
        showUnblocked = true;
        calculateSelect();
        return "/pages/user/taskList.html";
    }

    public static class Row implements Serializable {


        private String         requestTitle = "unknown yet";
        private final Priority priority;

        private final Long     dbid;
        private final Long     procDbid;
        private final Date     createTime;
        private final Date     duedate;
        private final String   assignee;
        private final Integer  progress;
        private final String   state;
        private final String   taskName;
        private final String   executionKey;
        private final String   processDefinitionKey;
        private final String   executionName;
        private final Long     count;

        public Row(final Long dbid, final Long procDbid,

        final Date createTime, final Date duedate, final String assignee, final Integer progress, final String requestTitle, final Priority priority,
                final String state, final String taskName, final String executionKey, final String processDefinitionKey, final String executionName,
                final Long count) {

            this.dbid = dbid;
            this.procDbid = procDbid;
            this.createTime = createTime;
            this.duedate = duedate;
            this.assignee = assignee;
            this.progress = progress;
            this.requestTitle = requestTitle;
            this.priority = priority;
            this.state = state;
            this.taskName = taskName;
            this.executionKey = executionKey;
            this.processDefinitionKey = processDefinitionKey;
            this.executionName = executionName;
            this.count = count;
        }

        public String getRequestTitle() {
            return requestTitle;
        }

        public Priority getPriority() {
            return priority;
        }

        public Long getDbid() {
            return dbid;
        }

        public Long getProcDbid() {
            return procDbid;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public Date getDuedate() {
            return duedate;
        }

        public String getAssignee() {
            return assignee;
        }

        public Integer getProgress() {
            return progress;
        }

        public String getState() {
            return state;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getExecutionKey() {
            return executionKey;
        }

        public String getProcessDefinitionKey() {
            return processDefinitionKey;
        }

        public String getExecutionName() {
            return executionName;
        }

        public Long getCount() {
            return count;
        }
    }

}
