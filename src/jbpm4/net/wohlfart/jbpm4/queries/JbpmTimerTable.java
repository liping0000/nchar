package net.wohlfart.jbpm4.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.framework.AbstractTableQuery;
import net.wohlfart.jbpm4.command.ResumeTimer;
import net.wohlfart.jbpm4.command.SuspendTimer;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.job.TimerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * this class implements access to the list of timer in jbpm
 * 
 * @author Michael Wohlfart
 * 
 */

@Scope(CONVERSATION)
@Name(JbpmTimerTable.JBPM_TIMER_TABLE)
public class JbpmTimerTable extends AbstractTableQuery<JbpmTimerTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmTimerTable.class);

    // needed to keep reference integrity in other components
    public static final String  JBPM_TIMER_TABLE = "jbpmTimerTable";

    @Override
    public void setup() {
        setEjbql("select new " 
                + JbpmTimerTable.Row.class.getName() 
                + "( " 
                + " t.dbid" 
                + ", t.dueDate" 
                + ", t.state" 
                + ", t.repeat" 
                + ", t.execution.dbid"
                + ", t.execution.key ) " 
                + " from " 
                + TimerImpl.class.getName() 
                + " t");

        setOrderDirection("asc");
        setOrderColumn("t.dueDate");
        setRestrictionLogicOperator("or");
    }

    private final Set<String> sortColumns = new HashSet<String>(
            Arrays.asList("t.dbid", "t.execution.key", "t.dueDate", "t.state", "t.repeat"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length())
            .append(ejbql)
            .replace(start, end, "select count(t) ");

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
        if ((fragment == null) || (fragment.trim().length() == 0)) {
            setRestrictionExpressionStrings(new ArrayList<String>());
        } else {
            final List<String> restrictions = new ArrayList<String>();
            restrictions.add(" lower(t.state) like concat('%', lower(#{" + JBPM_TIMER_TABLE + ".fragment}), '%') ");
            restrictions.add(" lower(t.repeat) like concat('%', lower(#{" + JBPM_TIMER_TABLE + ".fragment}), '%') ");
            restrictions.add(" lower(t.execution.key) like concat('%', lower(#{" + JBPM_TIMER_TABLE + ".fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
    }

    @Transactional
    public void setSuspend(final String pdid) {
        LOGGER.debug("suspending: {}, session {}", pdid, getSession());
        ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");
        processEngine.execute(new SuspendTimer(new Long(pdid)));
        getSession().flush();
        refresh();
    }

    @Transactional
    public void setResume(final String pdid) {
        LOGGER.debug("resuming: {}, session {}", pdid, getSession());
        ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");
        processEngine.execute(new ResumeTimer(new Long(pdid)));
        getSession().flush();
        refresh();
    }

    
    public static class Row implements Serializable {

        private final Long   dbid;
        private final Date   duedate;
        private final String state;
        private final String repeat;
        private final Long   executionDbid;
        private final String key;

        public Row(
                final Long dbid, 
                final Date duedate, 
                final String state, 
                final String repeat, 
                final Long executionDbid, 
                final String key) {

            this.dbid = dbid;
            this.duedate = duedate;
            this.state = state;
            this.repeat = repeat;
            this.executionDbid = executionDbid;
            this.key = key;
        }

        public Long getDbid() {
            return dbid;
        }

        public Date getDuedate() {
            return duedate;
        }

        public String getState() {
            return state;
        }

        public String getRepeat() {
            return repeat;
        }

        public Long getExecutionDbid() {
            return executionDbid;
        }

        public String getKey() {
            return key;
        }
    }

}
