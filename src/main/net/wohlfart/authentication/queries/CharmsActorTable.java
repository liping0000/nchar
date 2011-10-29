package net.wohlfart.authentication.queries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.IActorIdHolder;
import net.wohlfart.framework.AbstractTableQuery;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * an actor is either a group or a user
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.CONVERSATION)
@Name("charmsActorTable")
public class CharmsActorTable extends AbstractTableQuery<CharmsActorTable.Row> {


    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsActorTable.class);

    @Override
    public Set<String> getColumnsForOrder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setup() {
        setEjbql("select new " + CharmsActorTable.Row.class.getName() + "(a.actorId, " + " a.label)" + " from " + IActorIdHolder.class.getName() + " a");
        setOrderDirection("asc");
        setOrderColumn("a.label");
        setRestrictionLogicOperator("or");
    }

    /**
     * Seam tries to create a count query automatically, however this doesn't
     * work for some databases and ejb queries we use We just modify the
     * rendered ejb query and conserve the restriction clauses
     */
    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(a) ");

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
            restrictions.add(" lower(a.label) like concat('%', lower(#{charmsActorTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
    }

    public static class Row implements Serializable {


        private final String actorId;
        private final String label;

        public Row(final String actorId, final String label) {
            this.actorId = actorId;
            this.label = label;
        }

        public String getActorId() {
            return actorId;
        }

        public String getLabel() {
            return label;
        }
    }

}
