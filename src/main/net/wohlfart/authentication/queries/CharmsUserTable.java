package net.wohlfart.authentication.queries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.framework.AbstractTableQuery;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.CONVERSATION)
@Name("charmsUserTable")
public class CharmsUserTable extends AbstractTableQuery<CharmsUserTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsUserTable.class);

    @Override
    public void setup() {
        setEjbql("select new " 
                + CharmsUserTable.Row.class.getName() 
                + "(u.id, "
                + "u.firstname, u.lastname, u.name, " // name is the login name
                + "u.actorId, " 
                + "u.label, " 
                + "u.enabled, u.unlocked, u.credentialsExpire, u.accountExpire, " 
                + "u.memberships.size " 
                + ")" 
                + " from "
                + CharmsUser.class.getName() 
                + " u");
        setOrderDirection("asc");
        setOrderColumn("u.firstname");
        setRestrictionLogicOperator("or");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("u.id", "u.firstname", "u.lastname", "u.name"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(u) ");

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
            restrictions.add(" lower(u.name) like concat('%', lower(#{charmsUserTable.fragment}), '%') ");
            restrictions.add(" lower(u.lastname) like concat('%', lower(#{charmsUserTable.fragment}), '%') ");
            restrictions.add(" lower(u.firstname) like concat('%', lower(#{charmsUserTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
    }

    @Transactional
    public void setSuspend(final Long id) {
        final Session hibernateSession = getSession();
        final CharmsUser charmsUser = (CharmsUser) hibernateSession.get(CharmsUser.class, id);
        charmsUser.setEnabled(false);
        hibernateSession.flush();
        refresh();
    }

    @Transactional
    public void setResume(final Long id) {
        final Session hibernateSession = getSession();
        final CharmsUser charmsUser = (CharmsUser) hibernateSession.get(CharmsUser.class, id);
        charmsUser.setEnabled(true);
        charmsUser.setUnlocked(true);
        hibernateSession.flush();
        refresh();
    }


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
