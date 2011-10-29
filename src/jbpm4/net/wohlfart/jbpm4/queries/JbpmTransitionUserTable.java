package net.wohlfart.jbpm4.queries;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
@Name(JbpmTransitionUserTable.JBPM_TRANSITION_USER_TABLE)
public class JbpmTransitionUserTable extends AbstractTransitionTableQuery<JbpmTransitionUserTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmTransitionUserTable.class);

    protected static final String JBPM_TRANSITION_USER_TABLE = "jbpmTransitionUserTable";


    @Override
    public void setup() {
        LOGGER.info("setup called");

        setOrderDirection("asc");
        setOrderColumn("tu.firstname");
        // setRestrictionLogicOperator("or");
        calculateSelect();
    }

    private void calculateSelect() {
        if (transitionConfig == null) {
            // FIXME: no transition config means we don't have a transition panel
            // and don't need a popup or a select
            LOGGER.debug("no transition config provided, this might only happen in the init phase");
            setEjbql(" select tu from " + CharmsUser.class.getName() + " tu where 1 = 2");
        } else {
            ISelectConfig userConfig = transitionConfig.getUserSelectConfig();       
            final CharmsUser authenticatedUser = (CharmsUser) Contexts.getSessionContext().get("authenticatedUser");
            final String qlExpression = userConfig.getQlExpression(fragment, authenticatedUser);       
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


    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("tu.id", "tu.firstname", "tu.lastname", "tu.name"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }


    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(tu) ");

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

    /*
     * @Override public void setFragment(String fragment) {
     * super.setFragment(fragment); if ((fragment == null) ||
     * (fragment.trim().length() == 0)) { fragmentRestrictions = new
     * ArrayList<String>(); //setRestrictionExpressionStrings(new
     * ArrayList<String>()); } else { fragmentRestrictions = new
     * ArrayList<String>(); fragmentRestrictions.add(
     * " lower(u.name) like concat('%', lower(#{jbpmTransitionUserTable.fragment}), '%') "
     * ); fragmentRestrictions.add(
     * " lower(u.lastname) like concat('%', lower(#{jbpmTransitionUserTable.fragment}), '%') "
     * ); fragmentRestrictions.add(
     * " lower(u.firstname) like concat('%', lower(#{jbpmTransitionUserTable.fragment}), '%') "
     * ); //setRestrictionExpressionStrings(restrictions); }
     * updateRestrictions(); }
     */


    public static class Row implements Serializable {


        private final Long    id;
        private final String  firstname;
        private final String  lastname;
        private final String  name;     // login

        private final String  actorId;
        private final String  label;

        private final Boolean active;
        private final Integer roleCount;

        public Row(

                final Long id, final String firstname, final String lastname, final String name,

                final String actorId, final String label,

                final Boolean enabled, final Boolean unlocked, final Date credentialsExpire, final Date accountExpire, final Integer roleCount) {

            this.id = id;
            this.firstname = firstname;
            this.lastname = lastname;
            this.name = name;
            this.actorId = actorId;
            this.label = label;
            final Date now = Calendar.getInstance().getTime();
            active = enabled && unlocked && ((credentialsExpire == null) || (!credentialsExpire.before(now)))
            && ((accountExpire == null) || (!accountExpire.before(now)));
            this.roleCount = roleCount;
        }

        public Long getId() {
            return id;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getLastname() {
            return lastname;
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

        public Boolean getActive() {
            return active;
        }

        public Integer getRoleCount() {
            return roleCount;
        }
    }
}
