package net.wohlfart.framework.queries;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.ChangeRequestAction;
import net.wohlfart.framework.AbstractTableQuery;
import net.wohlfart.framework.entities.CharmsWorkflowData;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jbpm.pvm.internal.history.model.HistoryProcessInstanceImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * list of all tasks for a certain user or all users (admin role)
 * 
 * @author Michael Wohlfart
 * @version 0.1
 * 
 */

@Scope(ScopeType.CONVERSATION)
@Name(ProcessTable.PROCESS_TABLE)
public class ProcessTable extends AbstractTableQuery<ProcessTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProcessTable.class);

    // needed to keep reference integrity in other components
    public static final String  PROCESS_TABLE = "processTable";

    private Boolean showDraft     = true;
    private Boolean showOngoing   = true;
    private Boolean showComplete  = true;

    // no setter for this, always false for now..
    private final Boolean showDiscarded = false;

    private Long userId;

    private String baseEjbqlExpression;

    public String setupDraft() {
        LOGGER.info("setupDraft called");
        showDraft = true;
        showOngoing = false;
        showComplete = false;

        // this is the view id for the user proc list
        return "/pages/wfl/changerequest/userProcList.html";
    }

    public String setupComplete() {
        LOGGER.info("setupDraft called");
        showDraft = false;
        showOngoing = false;
        showComplete = true;

        // this is the view id for the user proc list
        return "/pages/wfl/changerequest/userProcList.html";
    }

    public String setupOngoing() {
        LOGGER.info("setupDraft called");
        showDraft = false;
        showOngoing = true;
        showComplete = false;

        // this is the view id for the user proc list
        return "/pages/wfl/changerequest/userProcList.html";
    }

    @Override
    // called when oncreate
    public void setup() {
        LOGGER.info("setup called");
        baseEjbqlExpression = "select new " + ProcessTable.Row.class.getName() 
            + "( " 
            + " h.dbid" 
            + ", h.key" // business key
            + ", d.submitDate" 
            + ", h.endTime" 
            + ", d.title" // title from the data container
            + " ) " 
            + " from " + HistoryProcessInstanceImpl.class.getName() + " h"
            + " , " + CharmsWorkflowData.class.getName() + " d "
                // dbid is long, processInstanceId is long
                + " where (h.dbid = d.processInstanceId) " // the process id value must match the id in the data container
        ;

        setOrderDirection("desc");
        setOrderColumn("h.key");
        // setRestrictionLogicOperator("or");
        calculateSelect();
    }

    private final Set<String> sortColumns = new HashSet<String>(
            Arrays.asList("h.dbid", "h.key", "d.submitDate", "h.endTime", "d.title"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(h) ");

        final int order = stringBuilder.lastIndexOf(" order by ");
        if (order > 0) {
            final String countQuery = stringBuilder.substring(0, order).toString().trim();
            LOGGER.debug("created count query: {}", countQuery);
            return countQuery;
        } else {
            final String countQuery = stringBuilder.toString().trim();
            LOGGER.debug("created count query: {}", countQuery);
            return countQuery;
        }
    }

    // @Override
    // public void refresh() {
    // calculateSelect();
    // super.refresh();
    // }

    @Override
    public void setFragment(final String fragment) {
        super.setFragment(fragment);
        calculateSelect();
    }

    private void calculateSelect() {
        String qlExpression = baseEjbqlExpression;

        if ((fragment != null) && (fragment.trim().length() > 0)) {
            qlExpression += " and " // a where clause is always present since we
                                    // join the domain data
                    + "(  ( lower(d.title) like concat('%', lower(#{" + PROCESS_TABLE + ".fragment}), '%') ) "
                    + " or"
                    + "   ( lower(h.key) like concat('%', lower(#{" + PROCESS_TABLE + ".fragment}), '%') ) " + ") ";
        }

        // drafts are any without business key
        if (!showDraft) {
            // if we don't want to see the drafts we have to filter out all with
            // business key
            qlExpression += " and (h.key is not null) ";
        }

        // ongoing are any without an end date but with a business key
        if (!showOngoing) {
            // if we don't want to see the ongoing we have to filter out all
            // with an end date and without a business key
            qlExpression += " and ((h.endTime is not null) or (h.key is null))";
        }

        // finished are any with an end date
        if (!showComplete) {
            // if we don't want to see the completed, we have to filter out all
            // with a null end date
            qlExpression += " and (h.endTime is null) ";
        }

        // we never show discarded
        if (!showDiscarded) {
            qlExpression += " and ";
            qlExpression += "("; // we don't want discarded processes in the
                                 // list
            qlExpression += "(h.endActivityName != '" + ChangeRequestAction.DISCARD_TASK + "') ";
            qlExpression += " or "; // we want unfinished
            qlExpression += "(h.endActivityName is null) ";
            qlExpression += ")"; //
        }

        // only show own processed FIXME: we have to check the complete task
        // here!!
        // * d is the data bean for the process instance
        // *
        if (userId != null) {
            qlExpression = qlExpression
                    + " and ( " // the request is alread submitted and we have
                                // set the submit user
                    + "    ( d.submitUser.id = "
                    + userId
                    + ")" // userid is long, no quotes for derby!
                    // + " or ( d.submitUser is null) " // FIXME: everyone sees
                    // all drafts here
                    + "   or ("
                    // there is still a complete task open, we didn't set the
                    // submittor in the data object
                    + " ( d.submitUser is null) " + " and " + " h.dbid in ( " + "        select t.processInstance.dbid from " + TaskImpl.class.getName()
                    + " t " + "        where t.name = '" + ChangeRequestAction.COMPLETE_TASK + "'" + "        and t.assignee in ( "
                    + "                  select u.actorId from " + CharmsUser.class.getName() + " u " + "                  where id = " + userId
                    + "                ) " + "     ) " + "   )" + " )";
        }

        LOGGER.debug("created qlExpression query: {}", qlExpression);
        setEjbql(qlExpression);
    }

    public Boolean getShowDraft() {
        return showDraft;
    }

    public void setShowDraft(final Boolean showDraft) {
        LOGGER.debug("setShowDraft: {}", showDraft);
        this.showDraft = showDraft;
        setFirstResult(0);
        calculateSelect();
    }

    public Boolean getShowOngoing() {
        return showOngoing;
    }

    public void setShowOngoing(final Boolean showOngoing) {
        LOGGER.debug("setShowOngoing: {}", showOngoing);
        this.showOngoing = showOngoing;
        setFirstResult(0);
        calculateSelect();
    }

    public Boolean getShowComplete() {
        return showComplete;
    }

    public void setShowComplete(final Boolean showComplete) {
        LOGGER.debug("setShowComplete: {}", showComplete);
        this.showComplete = showComplete;
        setFirstResult(0);
        calculateSelect();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        LOGGER.debug("setUserId: {}", userId);
        this.userId = userId;
        calculateSelect();
    }

    public static class Row implements Serializable {


        private final Long   dbid;
        private final String key;
        private final Date   start;
        private final Date   end;
        private final String title;

        public Row(final Long dbid, final String key, final Date start, final Date end, final String title) {

            this.dbid = dbid;
            this.key = key;
            this.start = start;
            this.end = end;
            this.title = title;
        }

        public Long getDbid() {
            return dbid;
        }

        public String getKey() {
            return key;
        }

        public Date getStart() {
            return start;
        }

        public Date getEnd() {
            return end;
        }

        public String getTitle() {
            return title;
        }
    }

}
