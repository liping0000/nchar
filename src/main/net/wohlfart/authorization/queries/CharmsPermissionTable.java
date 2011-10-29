package net.wohlfart.authorization.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.framework.AbstractTableQuery;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 
 * JSF component sets like Trinidad, Tomahawk and IceFaces give you an enhanced
 * h:dataTable which has built-in sorting, paging and other goodies. However,
 * there is one big flaw - the entire data set is loaded and sorted/paged in
 * memory, when really you want to get your persistence layer (be it an ORM or
 * just a database) to do this for - a sort translates naturally to an ORDER BY,
 * and paging translates naturally to a LIMIT clauses.
 * 
 * Seam on the other hand provides tight integration to your persistence layer,
 * and supports paging and sorting of queries through the Query object in the
 * Seam Application Framework - but you have to write a load more JSF to get it
 * integrated. We can get the best of both worlds by using the enhanced
 * DataModel in Trinidad, which supports paging and sorting backed by a Query.
 * 
 * The jboss-seam-trinidad.jar (built using the build file in the trinidad
 * directory in Seam CVS) provides a DataModel which, when backed by a Query,
 * provides lazy loading of data for paging, sorting in the persistence context
 * and strong row keys. You can use it by adding the jboss-seam-trinidad.jar to
 * your WEB-INF/lib - no need to alter your facelet. One caveat is that you must
 * ensure the rows property on the Query is the same as the maxResults property
 * on the Query. Take a look at the seamdiscs example in the trinidad/examples
 * directory to see it in action.
 */

/**
 * The EntityQuery component manages a JPQL query result set.
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(CONVERSATION)
@Name("charmsPermissionTable")
public class CharmsPermissionTable extends AbstractTableQuery<CharmsPermissionTable.Row> {


    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsPermissionTable.class);

    @Override
    public void setup() {
        setEjbql("select new " + CharmsPermissionTable.Row.class.getName() + "(p.id, " + "p.recipient, " + "p.target, " + "p.discriminator, " + "p.action )"
                + " from " + CharmsPermission.class.getName() + " p");

        setOrderDirection("asc");
        setOrderColumn("p.target");
        setRestrictionLogicOperator("or");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("p.id", "p.recipient", "p.target", "p.discriminator"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(p) ");

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
            restrictions.add(" lower(p.recipient) like concat('%', lower(#{charmsPermissionTable.fragment}), '%') ");
            restrictions.add(" lower(p.target) like concat('%', lower(#{charmsPermissionTable.fragment}), '%') ");
            restrictions.add(" lower(p.action) like concat('%', lower(#{charmsPermissionTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
    }

    @Transactional
    public Long getUserIdForPermission(final String s) {
        // FIXME: we might get an exception here
        final Long id = Long.parseLong(s);
        final Session hibernateSession = getSession();
        LOGGER.debug("getUserIdForPermission id: {}", id);
        final Long result = (Long) hibernateSession.getNamedQuery(CharmsPermission.FIND_USER_ID_BY_ID).setParameter("id", id).uniqueResult();
        LOGGER.debug("getUserIdForPermission: {}", result);
        return result;
    }

    @Transactional
    public Long getRoleIdForPermission(final String s) {
        // FIXME: we might get an exception here
        final Long id = Long.parseLong(s);
        final Session hibernateSession = getSession();
        LOGGER.debug("getRoleIdForPermission id: {}", id);
        final Long result = (Long) hibernateSession.getNamedQuery(CharmsPermission.FIND_ROLE_ID_BY_ID).setParameter("id", id).uniqueResult();
        LOGGER.debug("getRoleIdForPermission: {}", result);
        return result;
    }

    public static class Row implements Serializable {


        private final Long   id;
        private final String recipient;
        private final String target;
        private final String action;
        private Boolean      userPermission     = false;
        private Boolean      rolePermission     = false;
        //private Boolean      roleOnlyPermission = false;

        public Row(final Long id, final String recipient, final String target, final String discriminator, final String action) {

            this.id = id;
            this.recipient = recipient;
            this.target = target;
            // action is a comma separated list of strings
            // we insert blanks after the commas so we can have
            // a linebreak in the table, just need to make sure there are no
            // blanks inserted during save...
            final String[] values = StringUtils.split(action, ",");
            this.action = StringUtils.join(values, ", ");

            if (discriminator.equals(CharmsPermission.ROLE)) {
                rolePermission = true;
            }

            if (discriminator.equals(CharmsPermission.USER)) {
                userPermission = true;
            }

//            if (discriminator.equals(CharmsPermission.ROLEONLY)) {
//                roleOnlyPermission = true;
//            }

        }

        public Long getId() {
            return id;
        }

        public String getRecipient() {
            return recipient;
        }

        public String getTarget() {
            return target;
        }

        public Boolean getUserPermission() {
            return userPermission;
        }

        public Boolean getRolePermission() {
            return rolePermission;
        }

//        public Boolean getRoleOnlyPermission() {
//            return roleOnlyPermission;
//        }

        public String getAction() {
            return action;
        }
    }
}
