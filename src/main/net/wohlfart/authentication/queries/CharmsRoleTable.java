package net.wohlfart.authentication.queries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.RoleClassification;
import net.wohlfart.framework.AbstractTableQuery;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.CONVERSATION)
@Name("charmsRoleTable")
public class CharmsRoleTable extends AbstractTableQuery<CharmsRoleTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsRoleTable.class);
    
    // special attribute for the filter
    private RoleClassification classification = RoleClassification.AUTHORIZATIONAL;

    private String baseEjbqlExpression;
    
    @Override
    public void setup() {
        baseEjbqlExpression = "select new "
                + CharmsRoleTable.Row.class.getName() 
                + "(r.id," 
                + "r.name," 
                + "r.classification," 
                + "r.actorId," 
                + "r.label," 
                + "r.memberships.size," // the number of upstream roles from which we get the permissions                                                                                                                                                   // users
                + "r.upstream.size," 
                + "r.contained.size)"                                    
                + " from " + CharmsRole.class.getName() + " r";
        setOrderDirection("asc");
        setOrderColumn("r.name");
        // setRestrictionLogicOperator("and"); // important
        calculateSelect();
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("r.id", "r.name", "r.classification"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    private void calculateSelect() {
        String qlExpression = baseEjbqlExpression;
        boolean whereUsed = false;

        if ((fragment != null) && (fragment.trim().length() > 0)) {
            if (whereUsed) {
                qlExpression += " and ";
            } else {
                qlExpression += " where ";
                whereUsed = true;
            }
            qlExpression += "( lower(r.name) like concat('%', lower(#{charmsRoleTable.fragment}), '%')  )";
        }
    
        // do we have a classification filter
        if (classification != null) {
            if (whereUsed) {
                qlExpression += " and ";
            } else {
                qlExpression += " where ";
                whereUsed = true;
            }
            qlExpression += " (r.classification = #{charmsRoleTable.classification}) ";
        }
        
        setEjbql(qlExpression);
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

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(r) ");

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
      
    @Factory(value = "classificationTableSelects")  // FIXME: there is another fab creating the same for the entity, unify both
    public RoleClassification[] getClassificationSelects() {
        // return an enumeration
        return RoleClassification.values();
    }

    public void setClassification(final RoleClassification classification) {
        this.classification = classification;
        calculateSelect();
    }
    public RoleClassification getClassification() {
        return classification;
    }


    public static class Row implements Serializable {


        private final Long    id;
        private final String  name;
        private RoleClassification classification;

        private final String  actorId;
        private final String  label;

        private final Integer userCount;
        private final Integer upstreamCount;
        private final Integer containedCount;

        public Row(final Long id, 
                   final String name,
                   final RoleClassification classification,
                   final String actorId, 
                   final String label,
                   final Integer userCount, 
                   final Integer upstreamCount,
                   final Integer containedCount) {

            this.id = id;
            this.name = name;
            this.classification = classification;

            this.actorId = actorId;
            this.label = label;

            this.userCount = userCount;
            this.upstreamCount = upstreamCount;
            this.containedCount = containedCount;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        
        public RoleClassification getClassification() {
            return classification;
        }

        public String getActorId() {
            return actorId;
        }

        public String getLabel() {
            return label;
        }

        public Integer getUserCount() {
            return userCount;
        }

        public Integer getUpstreamCount() {
            return upstreamCount;
        }
        
        public Integer getContainedCount() {
            return containedCount;
        }
    }
}
