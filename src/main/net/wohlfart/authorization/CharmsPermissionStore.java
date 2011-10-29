package net.wohlfart.authorization;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.DEPLOYMENT;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authorization.entities.CharmsPermission;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Expressions;
import org.jboss.seam.core.Expressions.ValueExpression;
import org.jboss.seam.security.Role;
import org.jboss.seam.security.SimplePrincipal;
import org.jboss.seam.security.permission.IdentifierPolicy;
import org.jboss.seam.security.permission.Permission;
import org.jboss.seam.security.permission.PermissionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class becomes a part of the resolver chain inside the Permission Mapper
 * it is also used to store permissions in the database backing the methods have
 * to be called within a transaction
 * 
 * this is implemented in a similar way as the
 * CharmsIdentityStore/CharmsRoleIdentityStore classes
 * 
 * @author Michael Wohlfart
 * 
 * 
 *         NOTE: using this class doesn't allow us to use permission inheritance
 *         therefore we need a seperate action bean to manage non-inheritance
 *         permissions we assume all permissions here are being inherited from a
 *         role to its members
 * 
 *         FIXME: implement a query cache like in the JpaPermissionStore
 * 
 *         implement it according to the JPA permission store similar to the
 *         identityStore / IdentityRoleStore we also need a proxy that resolves
 *         permissions for string based targets
 * 
 *         leave objects to the rest of the resolver chain
 * 
 *         permissions for
 */

@Name("charmsPermissionStore")
@Install(precedence = DEPLOYMENT, value = false)
@Scope(APPLICATION)
@BypassInterceptors
@Startup
public class CharmsPermissionStore implements PermissionStore, Serializable {

    static final long serialVersionUID = -1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsPermissionStore.class);

    // we have a overridden IdentifierPolicy since the original is not serializable
    // which causes trouble on tomcat restarts
    private CharmsIdentifierPolicy identifierPolicy;

    // for dynamically resolving the hibernate session without bijection
    private ValueExpression<Session> hibernateSession;

    @Create
    public void init() {
        LOGGER.debug("Created component {}", this);
        initHibernateSession();
        //identifierPolicy = (CharmsIdentifierPolicy) Component.getInstance(CharmsIdentifierPolicy.class, true);
        //identifierPolicy = (CharmsIdentifierPolicy) Component.getInstance(IdentifierPolicy.class, true);
        identifierPolicy = (CharmsIdentifierPolicy) Component.getInstance("org.jboss.seam.security.identifierPolicy", true);
    }

    protected void initHibernateSession() {
        // EL expression for runtime evaluation
        hibernateSession = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class);
    }

    protected Session lookupHibernateSession() {
        return hibernateSession.getValue();
    }

    @Override
    public void clearPermissions(final Object target) {
        LOGGER.info("clearPermissions called for target: {}", target);
        final StringBuilder query = new StringBuilder();
        query.append("delete from ");
        query.append(CharmsPermission.class.getName());
        query.append(" p where");
        query.append(" p.target = :target");

        final String identifier = identifierPolicy.getIdentifier(target);

        lookupHibernateSession().createQuery(query.toString()).setParameter("target", identifier).executeUpdate();
    }

    /**
     * this method checks the database for a permission or creates a new
     * unpersisted one in case the permission is not in the database
     * 
     * @param recipient
     * @param target
     * @return
     */
    @SuppressWarnings("unchecked")
    protected CharmsPermission getCurrentOrNewPermission(final CharmsPermission permission) {
        CharmsPermission storedPermission = null;

        final List<CharmsPermission> charmsPermissions = lookupHibernateSession()
            .getNamedQuery(CharmsPermission.FIND_BY_RECIPIENT_DISCRIMINATOR_AND_TARGET)
            .setParameter("recipient", permission.getRecipient())
            .setParameter("discriminator", permission.getDiscriminator())
            .setParameter("target", permission.getTarget()) // the target needs an identifier or must be a  string
            .list();

        if ((charmsPermissions == null) || (charmsPermissions.size() == 0)) {
            // no permission for this role/target yet, create a new one
            storedPermission = permission;
        } else if (charmsPermissions.size() == 1) {
            // exactly the one permission we are looking for
            storedPermission = charmsPermissions.get(0);
        } else {
            LOGGER.warn("multiple permissions for role recipient: {} and target{} in database"
                    , permission.getRecipient(), permission.getTarget());
            throw new IllegalArgumentException("found multiple permissions in database, recipient is "
                    + permission.getRecipient() + " target is" + permission.getTarget() );
        }

        return storedPermission;
    }

    /**
     * CharmsPermission are database rows and contain multiple actions string in
     * a single database field
     * 
     * Permission are only for a single action and either for a user or for a
     * role
     * 
     * the target might be an object or a string in case of an object it can't
     * be constructed from the database string so we have to pass it from the
     * caller
     * 
     */
    protected List<Permission> translatePermissions(final Object target, final List<CharmsPermission> list) {
        final List<Permission> result = new ArrayList<Permission>();

        // loop for each charms permission
        for (final CharmsPermission p : list) {

            // we need to create a permissin for each action:
            final String allowedActions = p.getAction();
            String[] selected = new String[] {};
            if ((allowedActions != null) && (allowedActions.trim().length() > 0)) {
                selected = StringUtils.split(allowedActions, ",");
            }
            final Set<String> map = new HashSet<String>();
            for (final String string : selected) {
                map.add(string.trim());
            }

            // loop for each action inside the charmsPermission
            for (final String action : map) {
                Principal principal = null;
                if (p.isRolePermission()) {
                    principal = new Role(p.getRecipient());
                } else if (p.isUserPermission()) {
                    principal = new SimplePrincipal(p.getRecipient());
                    // } else if (p.isRoleOnlyPermission()) {
                    // principal = new Role(p.getRecipient());
                } else {
                    throw new IllegalArgumentException(
                            "can't find principal for permission, target: " + target 
                            + " action: " + action);
                }
                result.add(new Permission(target, action, principal));
            }

        } // end loop for each charmsPermission

        return result;
    }

    @Override
    public boolean grantPermission(final Permission permission) {
        LOGGER.debug("grantPermission called with permission: {}", permission);
        return grantPermissions(Arrays.asList(new Permission[] { permission }));
    }

    @Override
    public boolean revokePermission(final Permission oldPermission) {
        LOGGER.debug("revokePermission called with permission: {}", oldPermission);

        final CharmsPermission permission = new CharmsPermission();
        permission.setRecipient(oldPermission.getRecipient().getName());
        // permission.setAction(oldPermission.getAction());
        permission.setTarget(identifierPolicy.getIdentifier(oldPermission.getTarget()));
        final Principal recipient = oldPermission.getRecipient();
        if (recipient instanceof Role) {
            permission.setDiscriminator(CharmsPermission.ROLE);
        } else if (recipient instanceof SimplePrincipal) {
            permission.setDiscriminator(CharmsPermission.USER);
        } else {
            throw new IllegalArgumentException(
                    "unknown discriminator for recipient when revoking permission " + recipient
                    + "the recipient must be an instance of Role or SimplePrincipal");
        }

        final CharmsPermission storedPermission = getCurrentOrNewPermission(permission);

        // at this point we have a raw permission from database or a newly
        // created blank permission
        // for the recipient/target combination in the storedPermission variable

        // now we have to remove the action and then persist to the database
        final String allowedActions = storedPermission.getAction();

        String[] selected = new String[] {};
        if ((allowedActions != null) && (allowedActions.trim().length() > 0)) {
            selected = StringUtils.split(allowedActions, ",");
        }
        final Set<String> map = new HashSet<String>();
        for (final String string : selected) {
            map.add(string.trim());
        }

        // all the allowed actions are in map now nicely trimmed
        if (!map.contains(oldPermission.getAction())) {
            throw new IllegalArgumentException("permission is not granted for action: " + permission.getAction() 
                    + " for recipient: " + recipient 
                    + " target: " + permission.getTarget() 
                    + " unable to revoke this permission");
        } else {
            map.remove(oldPermission.getAction());
        }

        // put it back together and store it in the DB
        final String newActionString = StringUtils.join(map, ", ");
        storedPermission.setAction(newActionString);

        // if there are no actions strings left we can remove the whole database row
        if (map.size() == 0) {
            lookupHibernateSession().delete(storedPermission);
        } else {
            lookupHibernateSession().persist(storedPermission);
        }

        return true;
    }

    // FIXME: this method does not do the job for
    // roleOnly or membersOnly discriminator since the seam permissions are
    // limited
    @Override
    public boolean grantPermissions(final List<Permission> permissions) {
        LOGGER.debug("grantPermissions called with permissions: {}", permissions);

        // a list of modified permissions, since they are not flushed to
        // database
        // during the execution of this method we might have to modify them
        // again
        // a select into the database would get us a stalled value here...
        // note that the problem in the revoke method is different
        final HashMap<MultiKey, CharmsPermission> modified = new HashMap<MultiKey, CharmsPermission>();

        for (final Permission oldPermission : permissions) {

            final CharmsPermission permission = new CharmsPermission();
            permission.setRecipient(oldPermission.getRecipient().getName());
            // permission.setAction(oldPermission.getAction());
            // converts to a string :
            permission.setTarget(identifierPolicy.getIdentifier(oldPermission.getTarget())); 
            final Principal recipient = oldPermission.getRecipient();
            if (recipient instanceof Role) {
                permission.setDiscriminator(CharmsPermission.ROLE);
            } else if (recipient instanceof SimplePrincipal) {
                permission.setDiscriminator(CharmsPermission.USER);
            } else {
                throw new IllegalArgumentException("unknown discriminator for recipient when granting permission " + recipient
                        + "the recipient must be an instance of Role or SimplePrincipal");
            }

            // // this can be either a user or a role
            // Principal recipient = permission.getRecipient();
            // // a string definiing what exactly can be done when tis
            // permission is granted
            // String action = permission.getAction();
            // // an object (workflow step, machine instance, entity class,
            // entity instance) on
            // // which the action is performed
            // Object target = permission.getTarget();

            CharmsPermission storedPermission = null;
            // the Role subclass of SimplePrincipal uses the same hash method as
            // Principal
            // the Roles and Principals with the same name would collide in the
            // hashmap,
            // we have to add in the classname to really split Roles and
            // SimplePrincipals...
            final MultiKey key = new MultiKey(recipient.getClass().getName(), new MultiKey(recipient, permission.getTarget()));
            // this doen't work:
            // MultiKey key = new MultiKey(recipient, target);
            if (modified.containsKey(key)) {
                storedPermission = modified.get(key);
            } else {
                storedPermission = getCurrentOrNewPermission(permission);
            }

            // at this point we have a raw permission from database or a newly
            // created blank permission
            // for the recipient/target combination in the storedPermission
            // variable

            // now we have to add the action and then persist to the database
            final String allowedActions = storedPermission.getAction();

            String[] selected = new String[] {};
            if ((allowedActions != null) && (allowedActions.trim().length() > 0)) {
                selected = StringUtils.split(allowedActions, ",");
            }
            final Set<String> map = new HashSet<String>();
            for (final String string : selected) {
                map.add(string.trim());
            }

            // all the allowed actions are in map now, nicely trimmed
            if (map.contains(oldPermission.getAction())) {
                throw new IllegalArgumentException("permission already granted for action: " + permission.getAction() 
                        + " for recipient: " + recipient
                        + " target: " + permission.getTarget());
            } else {
                map.add(oldPermission.getAction());
            }

            // put it back together and store it in the DB
            final String newActionString = StringUtils.join(map, ", ");
            storedPermission.setAction(newActionString);

            LOGGER.debug("storing permission: recipient is {}", storedPermission.getRecipient());

            lookupHibernateSession().saveOrUpdate(storedPermission);
            modified.put(key, storedPermission);

        } // end permission loop

        return true;
    }

    @Override
    public boolean revokePermissions(final List<Permission> permissions) {
        LOGGER.debug("revokePermissions called with permissions: {}", permissions);
        boolean result = true;

        // FIXME: not sure if we have similar problmes like in the
        // grantPermissions method...
        for (final Permission p : permissions) {
            if (!revokePermission(p)) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Override
    public List<String> listAvailableActions(final Object target) {
        LOGGER.debug("listAvailableActions called with target: {} is not yet implemented", target);
        throw new IllegalArgumentException("method not yet implemented");
        // return new ArrayList<String>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Permission> listPermissions(final Object target) {
        LOGGER.debug("listPermissions called with target: {}", target);

        final List<CharmsPermission> list = lookupHibernateSession()
            .getNamedQuery(CharmsPermission.FIND_BY_TARGET)
            .setParameter("target", identifierPolicy.getIdentifier(target))
            .list();

        return translatePermissions(target, list);
    }

    // this is invoked by the hasPermission richfaces component
    @Override
    public List<Permission> listPermissions(final Object target, final String action) {
        LOGGER.debug("listPermissions called with target: {}, action: {}", target, action);

        return translatePermissions(target, listCharmsPermissions(target, action));
    }

    @SuppressWarnings("unchecked")
    public List<CharmsPermission> listCharmsPermissions(final Object target, final String action) {
        LOGGER.debug("listCharmsPermissions called with target: {}, action: {}", target, action);

        final List<CharmsPermission> list = lookupHibernateSession()
            .getNamedQuery(CharmsPermission.FIND_BY_TARGET_AND_ACTION)
            .setParameter("target", identifierPolicy.getIdentifier(target))
            .setParameter("action", action)
            .list();

        return list;
    }

    @Override
    public List<Permission> listPermissions(final Set<Object> targets, final String action) {
        LOGGER.debug("listPermissions called with targets: {}, action: {}", targets, action);

        // since we have different targets the resolved permissions will not collide
        final List<Permission> result = new ArrayList<Permission>();

        for (final Object target : targets) {
            result.addAll(listPermissions(target, action));
        }
        return result;
    }

}
