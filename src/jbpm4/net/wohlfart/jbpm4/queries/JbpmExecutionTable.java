package net.wohlfart.jbpm4.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.framework.AbstractTableQuery;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jbpm.pvm.internal.model.ExecutionImpl;
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
@Name(JbpmExecutionTable.JBPM_EXECUTION_TABLE)
public class JbpmExecutionTable extends AbstractTableQuery<JbpmExecutionTable.Row> {


    private final static Logger LOGGER               = LoggerFactory.getLogger(JbpmExecutionTable.class);

    // needed to keep reference integrity in other components
    public static final String  JBPM_EXECUTION_TABLE = "jbpmExecutionTable";

    @Override
    public void setup() {
        setEjbql("select new " + JbpmExecutionTable.Row.class.getName() + "( " + " e.dbid" + ", e.id" + ", e.name" + ", e.key" + ", e.state ) " + " from "
                + ExecutionImpl.class.getName() + " e");

        setOrderDirection("asc");
        setOrderColumn("e.key");
        setRestrictionLogicOperator("or");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("e.dbid", "e.id", "e.name", "e.key", "e.state"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
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
            restrictions.add(" lower(e.name) like concat('%', lower(#{" + JBPM_EXECUTION_TABLE + ".fragment}), '%') ");
            restrictions.add(" lower(e.key) like concat('%', lower(#{" + JBPM_EXECUTION_TABLE + ".fragment}), '%') ");
            restrictions.add(" lower(e.state) like concat('%', lower(#{" + JBPM_EXECUTION_TABLE + ".fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
    }

    public static class Row implements Serializable {


        private Long         dbid;
        private String       id;
        private final String name;
        private String       key;
        private String       state;

        public Row(final String name) {
            this.name = name;
        }

        public Row(final Long dbid, final String id, final String name, final String key, final String state) {

            this.dbid = dbid;
            this.id = id;
            this.name = name;
            this.key = key;
            this.state = state;
        }

        public Long getDbid() {
            return dbid;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public String getState() {
            return state;
        }
    }

}
