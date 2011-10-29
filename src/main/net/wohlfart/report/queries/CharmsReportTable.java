package net.wohlfart.report.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.framework.AbstractTableQuery;
import net.wohlfart.report.entities.CharmsReport;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The EntityQuery component manages a JPQL query result set.
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(CONVERSATION)
@Name("charmsReportTable")
public class CharmsReportTable extends AbstractTableQuery<CharmsReportTable.Row> {


    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsReportTable.class);

    @Override
    public void setup() {
        setEjbql("select new " + CharmsReportTable.Row.class.getName() + "(r.id, r.sortIndex, " + " r.messageCode, r.defaultName," + "   r.lastModified) "
                + " from " + CharmsReport.class.getName() + " r");
        setMaxResults(10);
        setRestrictionLogicOperator("or");
        setOrderDirection("asc");
        setOrderColumn("sortIndex");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("r.sortIndex", "r.defaultName", "r.lastModified"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf(" new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, " count(r) ");

        final int order = stringBuilder.lastIndexOf(" order by ");
        if (order > 0) {
            return stringBuilder.substring(0, order).toString().trim();
        } else {
            return stringBuilder.toString().trim();
        }
    }

    @Override
    public void setFragment(final String fragment) {
        LOGGER.debug("setting new fragmnet, was '{}' setting to '{}'", this.fragment, fragment);
        super.setFragment(fragment);
        if ((fragment == null) || (fragment.trim().length() == 0)) {
            setRestrictionExpressionStrings(new ArrayList<String>());
        } else {
            final List<String> restrictions = new ArrayList<String>();
            restrictions.add(" lower(r.defaultName) like concat('%', lower(#{charmsReportTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
        // go to the first page
        first();
    }

    public static class Row implements Serializable {


        private final Long    id;
        private final Integer index;
        private final String  msgCode;
        private final String  defaultName;
        private final Date    lastModified;

        public Row(final Long id, final Integer index, final String msgCode, final String defaultName, final Date lastModified) {
            this.id = id;
            this.index = index;
            this.msgCode = msgCode;
            this.defaultName = defaultName;
            this.lastModified = lastModified;
        }

        public Long getId() {
            return id;
        }

        public Integer getIndex() {
            return index;
        }

        public String getMsgCode() {
            return msgCode;
        }

        public String getDefaultName() {
            return defaultName;
        }

        public Date getLastModified() {
            return lastModified;
        }
    }
}
