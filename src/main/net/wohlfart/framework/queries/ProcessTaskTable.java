package net.wohlfart.framework.queries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.pvm.internal.history.model.HistoryTaskImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.ParticipationImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * list of all tasks for a process instance, this class generates two different
 * lists of tasks:
 * 
 * - all tasks for the current process instance regardless if they are assigned
 * to the current user, closed, terminated or still ongoing
 * 
 * - all open tasks that are assigned to the current user
 * 
 * this class also provides some statistical data for the current process: -
 * childCount: - openCount: - totalCount:
 * 
 * we have to provide either a taskid and the process id is resolved or provide
 * a processid directly...
 * 
 * this component is not instanciated by seam, we have to do all manual
 * lookups....
 * 
 * @author Michael Wohlfart
 * @version 0.1
 * 
 * 
 *          FIXME: check some hibernate tips&tricks:
 *          http://docs.jboss.org/hibernate
 *          /core/3.3/reference/en/html/queryhql.html#queryhql-tipstricks
 * 
 */

@Scope(ScopeType.CONVERSATION)
@Name(ProcessTaskTable.PROCESS_TASK_TABLE)
public class ProcessTaskTable implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProcessTaskTable.class);

    // needed to keep reference integrity in other components
    public static final String  PROCESS_TASK_TABLE = "processTaskTable";

    private Long taskInstanceId;
    private Long processInstanceId;

    private List<Row1> allTasks;
    private List<Row2> userTasks;

    private Long openCount;
    private Long totalCount;
    private Long childCount;

    public void setTaskInstanceId(final Long dbid) {
        taskInstanceId = dbid;
        processInstanceId = null;
        refresh();
    }

    public void setProcessInstanceId(final Long dbid) {
        processInstanceId = dbid;
        taskInstanceId = null;
        refresh();
    }

    @SuppressWarnings("unchecked")
    public void refresh() {

        CharmsUser charmsUser = (CharmsUser) Component.getInstance("authenticatedUser");
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);

        // find the process instance id
        if (processInstanceId == null) {
            processInstanceId = (Long) hibernateSession
                    .createQuery("select t.processInstance.dbid " + " from " + TaskImpl.class.getName() + " t " + " where t.dbid = :tid")
                    .setParameter("tid", taskInstanceId).uniqueResult();
        }

        // find the execution id if we have a taskid, use the process instance
        // otherwise
        Long executionInstanceId = processInstanceId;
        if (taskInstanceId != null) {
            executionInstanceId = (Long) hibernateSession
                    .createQuery("select t.execution.dbid " + " from " + TaskImpl.class.getName() + " t " + " where t.dbid = :tid")
                    .setParameter("tid", taskInstanceId).uniqueResult();
        }

        LOGGER.debug("process instance id is: {}", processInstanceId);

        // calculate groups of the current user
        // FIXME: this is ugly and groups might be empty causing the query to fail here
        charmsUser = (CharmsUser) hibernateSession.get(CharmsUser.class, charmsUser.getId());
        final Set<CharmsMembership> memberships = charmsUser.getMemberships();
        final List<String> groups = new ArrayList<String>();
        for (final CharmsMembership membership : memberships) {
            // groups.add("'" + membership.getCharmsRole().getGroupActorId() +
            // "'");
            groups.add(membership.getCharmsRole().getActorId());
        }
        if (groups.size() == 0) {
            groups.add("empty"); // quickfix here
        }

        LOGGER.debug("groups are: {}", groups);

        allTasks = (List<Row1>) hibernateSession
                .createQuery(
                        "select new "
                                + ProcessTaskTable.Row1.class.getName()
                                + "( "
                                + " t.dbid"
                                + ", t.createTime"
                                + ", t.duedate"
                                + ", t.endTime"
                                // used for checking if the user already has the
                                // task:
                                + ", t.assignee"
                                + ", (select firstname from "
                                + CharmsUser.class.getName()
                                + " where actorId = t.assignee)"
                                + ", (select lastname from "
                                + CharmsUser.class.getName()
                                + " where actorId = t.assignee)"
                                // we also check if the current user can take
                                // the task:
                                + ", (select count(groupId) from "
                                + ParticipationImpl.class.getName()
                                + " p where "
                                + " p.task.dbid = t.dbid " // the history task
                                                           // we use to get also
                                                           // the closed tasks
                                                           // do not have a
                                                           // participation
                                                           // column
                                + " and groupId in ( :groups )" // this is the
                                                                // set of the
                                                                // current users
                                                                // groups and
                                                                // the actorId
                                + "  )" 
                                + ", t.state " 
                                + ", t.name" 
                                + ") " 
                                + " from " 
                                + HistoryTaskImpl.class.getName() 
                                + " t " 
                                + " where ( "
                                + "  t.processInstance.dbid = :processInstanceId " 
                                + " ) "
                                + " order by t.createTime")
                                .setParameterList("groups", groups)
                                .setParameter("processInstanceId", processInstanceId)
                // + StringUtils.collectionToCommaDelimitedString(groups) //
                // FIXME: use a parameter, example should be somewhere in the
                // source already
                // .setParameter("userId", authenticatedUser.getId())
                .list();

        // FIXME: the group tasks are not fetched here
        userTasks = (List<Row2>) hibernateSession
                .createQuery(
                        "select new " 
                        + ProcessTaskTable.Row2.class.getName() 
                        + "( " 
                        + " t.dbid" 
                        + ", t.createTime" 
                        + ", t.duedate" 
                        + ", t.endTime"
                        + ", t.state " 
                        + ", t.name" 
                        + ") " 
                        + " from " 
                        + HistoryTaskImpl.class.getName() 
                        + " t " 
                        + " where " 
                        + "( "
                        + "  t.processInstance.dbid = :processInstanceId " 
                        + "  and t.assignee = :assignee " 
                        + " ) " 
                        + " and " 
                        + " t.endTime is null "
                        + " order by t.createTime").setParameter("assignee", charmsUser.getActorId())
                .setParameter("processInstanceId", processInstanceId)
                // + StringUtils.collectionToCommaDelimitedString(groups) //
                // FIXME: use a parameter, example should be somewhere in the
                // source already
                // .setParameter("userId", authenticatedUser.getId())
                .list();

        // count of open tasks for the whole process instance
        openCount = (Long) hibernateSession
                .createQuery(
                        "select count(*) from " + HistoryTaskImpl.class.getName() + " t " + " where (" + "  ( (t.state is null) "
                                + "    or (t.state not in (:notOpen)) )" + " and t.processInstance.dbid = :processInstanceId" + " )")
                .setParameterList("notOpen", Arrays.asList(new String[] { "completed" })).setParameter("processInstanceId", processInstanceId).uniqueResult();

        // total task count for the process instance
        totalCount = (Long) hibernateSession
                .createQuery(
                        "select count(*) from " + HistoryTaskImpl.class.getName() + " t " + " where (" + " t.processInstance.dbid = :processInstanceId" + " )")
                .setParameter("processInstanceId", processInstanceId).uniqueResult();

        // child execution instances for the execution of the current task, or
        // the
        // process instance if we have no task...
        childCount = (Long) hibernateSession
                .createQuery(
                        "select count(*) from " + ExecutionImpl.class.getName() + " e" + " where (" + "    ( e.parent.dbid is not null ) " + " and "
                                + "    ( e.parent.dbid = :executionInstanceId ) " + " and " + "  ( (e.state is null) " + "    or (e.state not in (:notOpen)) )"
                                + " )").setParameterList("notOpen", Arrays.asList(new String[] { "completed", "ended" }))
                .setParameter("executionInstanceId", executionInstanceId).uniqueResult();

        LOGGER.debug("found " + allTasks.size() + " elements " + " openCount: " + openCount + " totalCount: " + totalCount);
    }

    @BypassInterceptors
    public List<Row1> getAllTaskList() {
        LOGGER.debug("calling getResultList in ProcessTaskTable");
        return allTasks;
    }

    @BypassInterceptors
    public List<Row2> getUserTaskList() {
        LOGGER.debug("calling getResultList in ProcessTaskTable");
        return userTasks;
    }

    @BypassInterceptors
    public Long getOpenCount() {
        LOGGER.debug("calling getOpenCount in ProcessTaskTable");
        return openCount;
    }

    @BypassInterceptors
    public Long getTotalCount() {
        LOGGER.debug("calling getTotalCount in ProcessTaskTable");
        return totalCount;
    }

    @BypassInterceptors
    public Long getChildCount() {
        LOGGER.debug("calling getTotalCount in ProcessTaskTable");
        return childCount;
    }

    public static class Row1 implements Serializable {

        private final Long   dbid;
        private final Date   createTime;
        private final Date   duedate;
        private final Date   endTime;
        private final String assignee;
        private final String firstname;
        private final String lastname;
        private Integer      progress;
        private final Long   assignCount;
        private final String state;
        private final String taskName;

        public Row1(final Long dbid, final Date createTime, final Date duedate, final Date endTime, final String assignee, final String firstname,
                final String lastname,
                // Integer progress,
                final Long assignCount, final String state, final String taskName) {

            this.dbid = dbid;
            this.createTime = createTime;
            this.duedate = duedate;
            this.endTime = endTime;
            this.assignee = assignee;

            this.firstname = firstname;
            this.lastname = lastname;

            this.assignCount = assignCount;
            // this.progress = progress;
            this.state = state;
            this.taskName = taskName;
        }

        public Long getDbid() {
            return dbid;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public Date getDuedate() {
            return duedate;
        }

        public String getAssignee() {
            return assignee;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public Integer getProgress() {
            return progress;
        }

        public Long getAssignCount() {
            return assignCount;
        }

        public String getState() {
            return state;
        }

        public String getTaskName() {
            return taskName;
        }
    }

    public static class Row2 implements Serializable {

        private final Long   dbid;
        private final Date   createTime;
        private final Date   duedate;
        private final Date   endTime;
        private final String state;
        private final String taskName;

        public Row2(final Long dbid, final Date createTime, final Date duedate, final Date endTime, final String state, final String taskName) {

            this.dbid = dbid;
            this.createTime = createTime;
            this.duedate = duedate;
            this.endTime = endTime;
            this.state = state;
            this.taskName = taskName;
        }

        public Long getDbid() {
            return dbid;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public Date getDuedate() {
            return duedate;
        }

        public String getState() {
            return state;
        }

        public String getTaskName() {
            return taskName;
        }
    }

}
