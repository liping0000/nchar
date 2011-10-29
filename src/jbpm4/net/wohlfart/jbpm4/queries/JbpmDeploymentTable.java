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

import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.cmd.ResumeDeploymentCmd;
import org.jbpm.pvm.internal.cmd.SuspendDeploymentCmd;
import org.jbpm.pvm.internal.repository.DeploymentImpl;
import org.jbpm.pvm.internal.repository.DeploymentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * this class implements a process definition query and also methods for
 * uploading process definitions
 * 
 * 
 * 
 * @author Michael Wohlfart
 * @version 0.1
 * 
 */

@Scope(CONVERSATION)
@Name(value = JbpmDeploymentTable.JBPM_DEPLOYMENT_TABLE)
public class JbpmDeploymentTable extends AbstractTableQuery<JbpmDeploymentTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmDeploymentTable.class);

    // needed to keep reference integrity in other components
    public static final String JBPM_DEPLOYMENT_TABLE = "jbpmDeploymentTable";

    @Override
    public void setup() {
        setEjbql("select new " 
                + JbpmDeploymentTable.Row.class.getName() 
                + "(" 
                + " p.deployment.dbid" 
                + ", p.deployment.name" 
                + ", p.deployment.timestamp"
                + ", p.deployment.state" 
                + ", p.stringValue" 
                + ")" 
                + " from " 
                + DeploymentProperty.class.getName() 
                + " p" 
                + " where " 
                + " p.key = '" + DeploymentImpl.KEY_PROCESS_DEFINITION_ID 
                + "'");

        setOrderDirection("asc");
        setOrderColumn("p.deployment.timestamp");
        setRestrictionLogicOperator("or");
    }

    private final Set<String> sortColumns = new HashSet<String>(
            Arrays.asList( "p.deployment.dbid", 
                           "p.deployment.name", 
                           "p.deployment.timestamp",
                           "p.deployment.state"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    public void refresh() {
        LOGGER.debug("calling refresh on: " + this);
        super.refresh();
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(d) ");

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
            restrictions.add(" lower(d.name) like concat('%', lower(#{processDeploymentTable.fragment}), '%') ");
            restrictions.add(" lower(d.state) like concat('%', lower(#{processDeploymentTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
    }

    @Transactional
    @BypassInterceptors
    public void setSuspend(final String pdid) {
        LOGGER.debug("suspending: " + pdid);
        ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");
        processEngine.execute(new SuspendDeploymentCmd(pdid));
        getSession().flush(); // manual flushing needed
        refresh();
    }

    @Transactional
    @BypassInterceptors
    public void setResume(final String pdid) {
        LOGGER.debug("resuming: " + pdid);
        ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");
        processEngine.execute(new ResumeDeploymentCmd(pdid));
        getSession().flush(); // manual flushing needed
        refresh();
    }

    public static class Row implements Serializable {

        private final Long   dbid;
        private final String name;
        private final Date   timestamp;
        private final String state;
        private final String processDefinitionId;

        public Row(
                final Long dbid, 
                final String name, 
                final Long timestamp, 
                final String state, 
                final String processDefinitionId) {

            this.dbid = dbid;
            this.name = name;
            this.timestamp = new Date(timestamp);
            this.state = state;
            this.processDefinitionId = processDefinitionId;
        }

        public Long getDbid() {
            return dbid;
        }

        public String getName() {
            return name;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getState() {
            return state;
        }

        public String getProcessDefinitionId() {
            return processDefinitionId;
        }
    }

}
