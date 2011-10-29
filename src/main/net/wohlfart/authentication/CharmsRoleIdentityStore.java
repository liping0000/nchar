package net.wohlfart.authentication;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.security.Role;
import org.jboss.seam.security.SimplePrincipal;
import org.jboss.seam.security.management.IdentityManagementException;
import org.jboss.seam.security.management.NoSuchRoleException;
import org.jboss.seam.security.management.NoSuchUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * backend store for roles, 
 * the methods have to be called within a transaction
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
@Name(CharmsRoleIdentityStore.CHARMS_ROLE_IDENTITY_STORE)
@Install(precedence = Install.APPLICATION)
@Scope(APPLICATION)
@BypassInterceptors
@Startup
public class CharmsRoleIdentityStore extends AbstractIdentityStore {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsRoleIdentityStore.class);

    public final static String CHARMS_ROLE_IDENTITY_STORE = "charmsRoleIdentityStore";

    public static final String EVENT_PRE_PERSIST_USER_ROLE = "org.jboss.seam.security.management.prePersistUserRole";

    @Create
    public void init() {
        LOGGER.debug("created {}", this.getClass().getName());
        if (featureSet == null) {
            featureSet = new FeatureSet();
            featureSet.addFeature(Feature.createRole);
            featureSet.addFeature(Feature.deleteRole);
            featureSet.addFeature(Feature.grantRole);
            featureSet.addFeature(Feature.revokeRole);
        }

        super.initHibernateSession();
    }

    @Override
    public List<String> listGrantableRoles() {
        return super.listGrantableRoles();
    }

    @Override
    public List<String> listRoles() {
        return super.listRoles();
    }

    @Override
    public boolean roleExists(final String name) {
        return lookupRole(name) != null;
    }

    /**
     * this method does provide any roles for a username by recursively scanning
     * the subroles
     */
    @Override
    public List<String> getImpliedRoles(final String username) {
        final CharmsUser user = lookupUser(username);
        if (user == null) {
            throw new NoSuchUserException("No such user '" + username + "'");
        }

        final Set<String> roles = new HashSet<String>();
        final Collection<CharmsMembership> memberships = user.getMemberships();
        if (memberships != null) {
            for (final CharmsMembership m : memberships) {
                addRoleAndMemberships(m.getCharmsRole().getName(), roles);
            }
        }
        return new ArrayList<String>(roles);
    }

    /**
     * recursive method to collect all roles in a set, used in the get
     * ImpliedRoles method
     * 
     * @param role
     * @param roles
     */
    protected void addRoleAndMemberships(final String role, final Set<String> roles) {

        // returns false if the role is already in the set
        if (roles.add(role)) {
            final CharmsRole instance = lookupRole(role);

            final Collection<CharmsRole> groups = instance.getUpstream();

            if (groups != null) {
                for (final CharmsRole group : groups) {
                    addRoleAndMemberships(group.getName(), roles);
                }
            }
        }
    }

    /**
     * this delivers the first level of subroles of a role with the provided
     * name
     */
    @Override
    public List<String> getRoleGroups(final String name) {
        final CharmsRole role = lookupRole(name);
        if (role == null) {
            throw new NoSuchRoleException("No such role '" + name + "'");
        }

        final List<String> groups = new ArrayList<String>();

        final Set<CharmsRole> roleGroups = role.getUpstream();
        for (final CharmsRole group : roleGroups) {
            groups.add(group.getName());
        }

        return groups;
    }

    @Override
    public List<Principal> listMembers(final String role) {
        final List<Principal> members = new ArrayList<Principal>();

        for (final String user : listUserMembers(role)) {
            members.add(new SimplePrincipal(user));
        }

        for (final String roleName : listRoleMembers(role)) {
            members.add(new Role(roleName));
        }
        return members;
    }

    @Override
    public boolean grantRole(final String username, final String role) {

        final CharmsUser user = lookupUser(username);
        if (user == null) {
            throw new IdentityManagementException("Could not grant role - user does not exist");
        }

        final CharmsRole roleToGrant = lookupRole(role);
        if (roleToGrant == null) {
            throw new NoSuchRoleException("Could not grant role, role '" + role + "' does not exist");
        }

        final CharmsMembership membershipToGrant = new CharmsMembership(user, roleToGrant);

        // Otherwise we need to insert a cross-reference entity instance
        try {
            // CharmsMembership membership = new CharmsMembership(user,
            // roleToGrant);
            Events.instance().raiseEvent(EVENT_PRE_PERSIST_USER_ROLE, membershipToGrant);
            persistEntity(membershipToGrant);
        } catch (final Exception ex) {
            throw new IdentityManagementException("Error creating cross-reference role record.", ex);
        }

        return true;
    }

    @Override
    public boolean revokeRole(final String username, final String rolename) {
        final CharmsUser user = lookupUser(username);
        if (user == null) {
            throw new NoSuchUserException("Could not revoke role, no such user '" + username + "'");
        }

        final CharmsRole roleToRevoke = lookupRole(rolename);
        if (roleToRevoke == null) {
            throw new NoSuchRoleException("Could not revoke role, role '" + rolename + "' does not exist");
        }

        boolean success = false;
        int wasLinked = 0;

        // collection of the memberships of the user:
        Collection<CharmsMembership> memberships;
        memberships = user.getMemberships();
        // walk through all memberships of the user
        for (final CharmsMembership m : memberships) {
            // and find the membership for the role we want to remove
            if (m.getCharmsRole().getName().equals(rolename)) {
                // now remove entity and
                // the membership from the users memberships
                wasLinked++;
                removeEntity(m);
                success |= memberships.remove(m); // no shortcircuit!
                break;
            }
        }

        // remove from role
        memberships = roleToRevoke.getMemberships();
        for (final CharmsMembership m : memberships) {
            if (m.getCharmsUser().getName().equals(username)) {
                wasLinked++;
                removeEntity(m);
                success |= memberships.remove(m); // no shortcircuit!
                break;
            }
        }

        if (wasLinked < 2) {
            throw new IdentityManagementException("Error can't revoke a role that hasn't been properly granted: '" + rolename + "' for user '" + username
                    + "' was linked: " + wasLinked);
        }

        return success;
    }

    /**
     * assigning group as a subrole to role
     */
    @Override
    public boolean addRoleToGroup(final String role, final String group) {

        final CharmsRole targetRole = lookupRole(role);
        if (targetRole == null) {
            throw new NoSuchUserException("Could not add role to group, no such role '" + role + "'");
        }

        final CharmsRole targetGroup = lookupRole(group);
        if (targetGroup == null) {
            throw new NoSuchRoleException("Could not add role to group, group '" + group + "' does not exist");
        }

        final Set<CharmsRole> groups = targetGroup.getUpstream();
        if (groups.contains(targetRole)) {
            throw new IdentityManagementException("role '" + role + "' already in group '" + group + "'");
        }

        boolean changed = groups.add(targetRole);
        targetGroup.calculateContainedRoles(lookupHibernateSession());
        return changed;
    }

    @Override
    public boolean removeRoleFromGroup(final String role, final String group) {

        final CharmsRole roleToRemove = lookupRole(role);
        if (roleToRemove == null) {
            throw new NoSuchUserException("Could not remove role from group, no such role '" + role + "'");
        }

        final CharmsRole targetGroup = lookupRole(group);
        if (targetGroup == null) {
            throw new NoSuchRoleException("Could not remove role from group, no such group '" + group + "'");
        }

        final Set<CharmsRole> groups = targetGroup.getUpstream();
        if (!groups.contains(roleToRemove)) {
            throw new IdentityManagementException("role '" + role + "' is not in group '" + group + "'");
        }

        boolean changed = groups.remove(roleToRemove);
        targetGroup.calculateContainedRoles(lookupHibernateSession());
        return changed;
    }

    @Override
    public boolean createRole(final String role) {
        try {

            if (roleExists(role)) {
                throw new IdentityManagementException("Could not create role, already exists");
            }

            final CharmsRole instance = new CharmsRole();
            instance.setName(role);
            persistEntity(instance);

            return true;
        } catch (final Exception ex) {
            if (ex instanceof IdentityManagementException) {
                throw (IdentityManagementException) ex;
            } else {
                throw new IdentityManagementException("Could not create role", ex);
            }
        }
    }

    @Override
    public boolean deleteRole(final String rolename) {
        final CharmsRole role = lookupRole(rolename);
        if (role == null) {
            throw new NoSuchRoleException("Could not delete, role '" + rolename + "' does not exist");
        }

        // remove all memberships:
        final Iterator<CharmsMembership> memberships = role.getMemberships().iterator();
        while (memberships.hasNext()) {
            final CharmsMembership next = memberships.next();
            removeEntity(next);
            next.getCharmsUser().getMemberships().remove(next);
        }

        role.getMemberships().clear();
        removeEntity(role);
        return true;
    }

    @Override
    public List<String> getGrantedRoles(final String username) {
        final CharmsUser user = lookupUser(username);
        if (user == null) {
            throw new NoSuchUserException("No such user '" + username + "'");
        }

        final List<String> roles = new ArrayList<String>();
        final Collection<CharmsMembership> memberships = user.getMemberships();
        if (memberships != null) {
            for (final CharmsMembership m : memberships) {
                roles.add(m.getCharmsRole().getName());
            }
        }
        return roles;
    }

}
