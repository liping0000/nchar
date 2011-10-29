package net.wohlfart.jbpm4.queries;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.node.ISelectConfig;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.CONVERSATION)
@Name(JbpmTransitionRoleTable.JBPM_TRANSITION_ROLE_TABLE)
public class JbpmTransitionRoleTable extends AbstractTransitionTableQuery<JbpmTransitionRoleTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmTransitionRoleTable.class);

    protected static final String JBPM_TRANSITION_ROLE_TABLE = "jbpmTransitionRoleTable";

    @Override
    public void setup() {
        LOGGER.info("setup called");
        
        setOrderDirection("asc");
        setOrderColumn("r.name");
        //setRestrictionLogicOperator("or");
        calculateSelect();
    }

    private void calculateSelect() {    
        if (transitionConfig == null) {
            // FIXME: no transition config means we don't have a transition panel
            // and don't need a popup or a select
            LOGGER.debug("no transition config provided, this might only happen in the init phase");
            setEjbql(" select u from " + CharmsRole.class.getName() + " u where 1 = 2");
        } else {
            ISelectConfig groupConfig = transitionConfig.getGroupSelectConfig();
            final CharmsUser authenticatedUser = (CharmsUser) Contexts.getSessionContext().get("authenticatedUser");
            final String qlExpression = groupConfig.getQlExpression(fragment, authenticatedUser);       
            LOGGER.debug("select is: {}", qlExpression);
            setEjbql(qlExpression);     
        }
    }


    @Override
    public void setFragment(final String fragment) {
        super.setFragment(fragment);
        calculateSelect();
    }

    @Override
    @BypassInterceptors
    public void setTransitionName(final String transitionName) {
        super.setTransitionName(transitionName);
        calculateSelect();
    }


    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("r.id", "r.name"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
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


    public static class Row implements Serializable {


        private final Long    id;
        private final String  name;

        private final String  actorId;
        private final String  label;

        private final Integer userCount;
        private final Integer roleCount;

        public Row(final Long id, final String name,

                final String actorId, final String label,

                final Integer userCount, final Integer roleCount) {

            this.id = id;
            this.name = name;

            this.actorId = actorId;
            this.label = label;

            this.userCount = userCount;
            this.roleCount = roleCount;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
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

        public Integer getRoleCount() {
            return roleCount;
        }
    }
}
