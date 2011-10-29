package net.wohlfart.authentication;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.core.Expressions;
import org.jboss.seam.core.Expressions.ValueExpression;
import org.jboss.seam.security.management.IdentityStore;
import org.jboss.seam.security.management.NoSuchRoleException;

/**
 * this class provides some tools to implement IdentityStores and is intended to
 * be extended for a hibernate session specific implementation of a user and
 * role identityStore, the intention is to hide any Database specific details od
 * the entity store in this class
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public abstract class AbstractIdentityStore implements IdentityStore, Serializable {

    protected FeatureSet featureSet;

    // for dynamically resolving the hibernate session without bijection
    private ValueExpression<Session> hibernateSession;

    // FIXME: this is unused so far
    @Override
    public boolean supportsFeature(final Feature feature) {
        return featureSet.supports(feature);
    }

    protected void initHibernateSession() {
        // EL expression for runtime evaluation
        hibernateSession = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class);
    }

    protected Session lookupHibernateSession() {
        return hibernateSession.getValue();
    }

    // basic hibernate actions ///////////////////////////////

    protected void persistEntity(final Object entity) {
        lookupHibernateSession().persist(entity);
    }

    protected Object mergeEntity(final Object entity) {
        return lookupHibernateSession().merge(entity);
    }

    protected void removeEntity(final Object entity) {
        lookupHibernateSession().delete(entity);
    }

    // all SQL specific stuff below this comment ///////////////////////////////

    @SuppressWarnings("unchecked")
    protected CharmsUser lookupUser(final String username) {
        final StringBuilder query = new StringBuilder();
        query.append("select u from ");
        query.append(CharmsUser.class.getName());
        query.append(" u where");
        query.append(" u.name = :name");

        final List<CharmsUser> list = lookupHibernateSession()
            .createQuery(query.toString())
            .setParameter("name", username)
            .list();

        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            throw new IllegalArgumentException("can't find user by name '" + username + "'");
        }
    }

    @SuppressWarnings("unchecked")
    protected CharmsRole lookupRole(final String rolename) {
        final StringBuilder query = new StringBuilder();
        query.append("select r from ");
        query.append(CharmsRole.class.getName());
        query.append(" r where");
        query.append(" r.name = :name");

        final List<CharmsRole> list = lookupHibernateSession()
            .createQuery(query.toString())
            .setParameter("name", rolename)
            .list();

        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            // fixme: logger or exception here
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> listRoleMembers(final String rolename) {
        final CharmsRole roleEntity = lookupRole(rolename);

        final StringBuilder query = new StringBuilder();
        query.append("select r.name from ");
        query.append(CharmsRole.class.getName());
        query.append(" r where");
        query.append(" :role member of r.roles");

        return lookupHibernateSession()
            .createQuery(query.toString())
            .setParameter("role", roleEntity)
            .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listGrantableRoles() {
        final StringBuilder query = new StringBuilder();
        query.append("select r.name from ");
        query.append(CharmsRole.class.getName());
        query.append(" r where");
        query.append(" r.conditional = false");

        return lookupHibernateSession().createQuery(query.toString()).list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listUsers() {
        final StringBuilder query = new StringBuilder();
        query.append("select u.name from ");
        query.append(CharmsUser.class.getName());
        query.append(" u");

        return lookupHibernateSession().createQuery(query.toString()).list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listUsers(final String filter) {
        final StringBuilder query = new StringBuilder();
        query.append("select u.name from ");
        query.append(CharmsUser.class.getName());
        query.append(" u where");
        query.append(" lower(name) like :username");
                                                    
        final StringBuilder parameter = new StringBuilder();
        if (StringUtils.isEmpty(filter)) {
            parameter.append("%");
        } else {
            parameter.append("%");
            parameter.append(filter);
            parameter.append("%");
        }
        return lookupHibernateSession()
            .createQuery(query.toString())
            .setParameter("username", parameter)
            .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listRoles() {
        final StringBuilder query = new StringBuilder();
        query.append("select r.name from ");
        query.append(CharmsRole.class.getName());
        query.append(" r ");

        return lookupHibernateSession().createQuery(query.toString()).list();
    }

    @SuppressWarnings("unchecked")
    protected List<String> listUserMembers(final String rolename) {
        final Object roleEntity = lookupRole(rolename);

        final StringBuilder query = new StringBuilder();
        query.append("select x from ");
        query.append(CharmsMembership.class.getName());
        query.append(" x where ");
        query.append(" x.role = :role ");

        final List<CharmsMembership> memberships = lookupHibernateSession()
            .createQuery(query.toString())
            .setParameter("role", roleEntity)
            .list();

        final List<String> members = new ArrayList<String>();

        for (final CharmsMembership m : memberships) {
            final CharmsUser user = m.getCharmsUser();
            members.add(user.getName());
        }

        return members;
    }

    @Override
    public boolean deleteRole(final String role) {
        final CharmsRole roleToDelete = lookupRole(role);
        if (roleToDelete == null) {
            throw new NoSuchRoleException("Could not delete role, role '" + role + "' does not exist");
        }

        final StringBuilder query = new StringBuilder();
        query.append("delete ");
        query.append(CharmsMembership.class.getName());
        query.append(" m where");
        query.append(" m.charmsRole = :charmsRole");

        lookupHibernateSession().createQuery(query.toString())
            .setParameter("charmsRole", roleToDelete)
            .executeUpdate();

        final List<String> roles = listRoleMembers(role);
        for (final String r : roles) {
            removeRoleFromGroup(r, role);
        }

        removeEntity(roleToDelete);
        return true;
    }

    // / method that should be overridden by subclasses

    @Override
    public boolean addRoleToGroup(final String role, final String group) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean changePassword(final String name, final String password) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean createRole(final String role) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean createUser(final String username, final String password) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean createUser(final String username, final String password, final String firstname, final String lastname) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean deleteUser(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean disableUser(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean enableUser(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public List<String> getGrantedRoles(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public List<String> getImpliedRoles(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public List<String> getRoleGroups(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean grantRole(final String name, final String role) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean isUserEnabled(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public List<Principal> listMembers(final String role) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean removeRoleFromGroup(final String role, final String group) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean revokeRole(final String name, final String role) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean roleExists(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

    @Override
    public boolean userExists(final String name) {
        throw new IllegalArgumentException("Method not supported in AbstractIdentityStore, a subclass should implement this method");
    }

}
