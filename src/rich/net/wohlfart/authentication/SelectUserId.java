package net.wohlfart.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsRoleItem;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authentication.entities.CharmsUserItem;
import net.wohlfart.authorization.CharmsPermissionStore;
import net.wohlfart.authorization.entities.CharmsPermission;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backend for a user selectbox FIXME: we have too many db lookups here
 * 
 * @author Michael Wohlfart
 * 
 */
@AutoCreate
@Scope(ScopeType.APPLICATION)
@Name(value = "selectUserId")
public class SelectUserId implements Serializable {


    private final static Logger   LOGGER = LoggerFactory.getLogger(SelectUserId.class);

    @In(value = "hibernateSession")
    private Session               hibernateSession;

    @In(create = true)
    private CharmsPermissionStore charmsPermissionStore;

    @SuppressWarnings("unchecked")
    @Transactional
    public List<SelectItem> getAll() {
        LOGGER.debug("getAll called");
        final List<SelectItem> result = hibernateSession.createQuery(
                "select new " + CharmsUserItem.class.getName() + "(u.id, u.firstname, u.lastname, u.name) " + " from CharmsUser u" + " order by u.lastname")
                .list();
        return result;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<SelectItem> getForRole(final String roleName) {
        LOGGER.debug("getForRole called: " + roleName);
        // FIXME: include the role query for subroles
        final List<SelectItem> result = hibernateSession
                .createQuery(
                        "select distinct new " + CharmsUserItem.class.getName() + "(u.id, u.firstname, u.lastname, u.name) " + " from CharmsUser u "
                                + " join u.memberships m " + " where m.charmsRole.name = :roleName " + " order by u.lastname")
                .setParameter("roleName", roleName).list();
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<SelectItem> getForPermission(final String target, final String action) {
        LOGGER.debug("getForPermission called target: " + target + " action: " + action);
        List<SelectItem> result = new ArrayList<SelectItem>();

        // PermissionStore permissionStore = (PermissionStore)
        // Component.getInstance(JpaPermissionStore.class, true);
        // LOGGER.debug("permissionStore: " + permissionStore);
        final List<CharmsPermission> permissions = charmsPermissionStore.listCharmsPermissions(target, action);

        if ((permissions == null) || (permissions.size() == 0)) {
            LOGGER.warn("no permissions found, target is: " + target + " action is: " + action + " user list will be empty!!");
            return result;
        }

        // we have some permissions that fit target and action
        // we collect the users ids, must use a set here
        final Set<Long> userIds = new HashSet<Long>();

        for (final CharmsPermission permission : permissions) {

            final String name = permission.getRecipient();
            // System.err.println("recipients name is : " + name);

            // if the recipient is a role, we have to find the actor ids for the
            // role
            // role is an instance of SimplePrincipal !! so the order of this
            // if..else is important
            if (permission.isRolePermission()) {
                final List<Long> ids = hibernateSession.getNamedQuery(CharmsUser.FIND_IDS_BY_GROUP_NAME).setParameter("name", name).list();
                userIds.addAll(ids);
                if (ids.size() == 0) {
                    LOGGER.warn("no user found for Role with name: " + name + " action was: " + action + " target was: " + target + " result is empty");
                }

                // FIXME: implement subrole -> subuser adding here

                // we have to find the actor id for the principal
            } else if (permission.isUserPermission()) {
                final List<Long> ids = hibernateSession.getNamedQuery(CharmsUser.FIND_ID_BY_NAME).setParameter("name", name).list();
                if (ids.size() == 1) {
                    userIds.add(ids.get(0));
                } else {
                    LOGGER.warn("no (single) user found for SimplePrincipal with name: " + name + " action was: " + action + " target was: " + target
                            + " result is: " + ids);
                }
                // } else if (permission.isRoleOnlyPermission()) {
                // LOGGER.info("found a roleonly permission, not returning the users ");
            } else {
                LOGGER.warn("unknown discriminator: " + permission.getDiscriminator());
            }
        }

        if (userIds.size() == 0) {
            return result;
        }

        // FIXME: we need a popup and no pulldown!
        // limit is 1000 for oracle:
        // ORA-01795: maximum number of expressions in a list is 1000
        LOGGER.info(" useridcount: {}", userIds.size());
        if (userIds.size() > 500) {
            result = hibernateSession.createQuery(
                    "select new " + CharmsUserItem.class.getName() + "(u.id, u.firstname, u.lastname, u.name) " + "    from CharmsUser u"
                            + " order by u.lastname").list();
        } else {
            result = hibernateSession
                    .createQuery(
                            "select new " + CharmsUserItem.class.getName() + "(u.id, u.firstname, u.lastname, u.name) " + "    from CharmsUser u"
                                    + " where u.id in ( :ids )" + " order by u.lastname").setParameterList("ids", userIds).list();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<SelectItem> getRolesForPermission(final String target, final String action) {
        LOGGER.debug("getRoleForPermission called target: " + target + " action: " + action);
        List<SelectItem> result = new ArrayList<SelectItem>();

        // PermissionStore permissionStore = (PermissionStore)
        // Component.getInstance(JpaPermissionStore.class, true);
        // LOGGER.debug("permissionStore: " + permissionStore);
        final List<CharmsPermission> permissions = charmsPermissionStore.listCharmsPermissions(target, action);

        if (permissions.size() == 0) {
            LOGGER.info("no permissions found, target is: " + target + " action is: " + action + " role list will be empty!!");
            return result;
        }

        /*
         * debugged values: recipient: admin (role) recipient class: class
         * org.jboss.seam.security.Role recipient: devel (user) recipient class:
         * class org.jboss.seam.security.SimplePrincipal
         */

        // we collect the users ids
        final List<Long> roleIds = new ArrayList<Long>();

        for (final CharmsPermission permission : permissions) {
            final String name = permission.getRecipient();

            // we have to find the role ids for the role
            // role is an instance of SimplePrincipal !! so the order of this
            // if..else is important
            if (permission.isRolePermission()) {
                LOGGER.info("found recipient as role: {}", name);

                final CharmsRole role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", name)
                // .setParameter("org", true)
                        .uniqueResult();

                // the role might not be an org role, skip it
                if (role != null) {
                    roleIds.add(role.getId());
                }

                if (permission.isRolePermission()) {
                    // FIXME: implement subrole addition
                }
            }
        } // end recipient loop

        if (roleIds.size() == 0) {
            LOGGER.warn("no permissions found, target is: " + target + " action is: " + action + " role list will be empty!!!");
            return result;
        }

        result = hibernateSession
                .createQuery(
                        "select new " + CharmsRoleItem.class.getName() + "(r.id,r.name) " + "    from CharmsRole r" + " where r.id in ( :ids )"
                                + " order by r.name").setParameterList("ids", roleIds).list();
        return result;
    }

}
