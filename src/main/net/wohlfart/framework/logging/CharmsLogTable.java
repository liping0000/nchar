package net.wohlfart.framework.logging;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.framework.AbstractTableQuery;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(CONVERSATION)
@Name("charmsLogTable")
public class CharmsLogTable extends AbstractTableQuery<CharmsLogEntry> {


    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsLogTable.class);

    @Override
    public void setup() {
        setEjbql("select l from " + CharmsLogEntry.class.getName() + " l ");
        setMaxResults(10);
        setRestrictionLogicOperator("or");
        setUseWildcardAsCountQuerySubject(true);

        setOrderDirection("asc");
        setOrderColumn("id");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("date", "logger", "level", "message"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(l) ");

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
            restrictions.add(" lower(level) like concat('%', lower(#{charmsLogTable.fragment}), '%') ");
            restrictions.add(" lower(logger) like concat('%', lower(#{charmsLogTable.fragment}), '%') ");
            restrictions.add(" lower(message) like concat('%', lower(#{charmsLogTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
        // go to the first page
        first();
    }

}
