package net.wohlfart.charms.test.components;

import java.util.List;

import net.wohlfart.authentication.CharmsIdentityManager;
import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.CharmsRoleIdentityStore;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.CharmsPermissionStore;
import net.wohlfart.authorization.entities.CharmsPermission;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.security.Role;
import org.jboss.seam.security.SimplePrincipal;
import org.jboss.seam.security.permission.Permission;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IdentityPermissionManagerTest extends ComponentSessionBase {

    protected CharmsIdentityManager charmsIdentityManager;
    protected CharmsPermissionStore charmsPermissionStore;

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();

        // setup an identityManager
        charmsIdentityManager = new CharmsIdentityManager();

        final CharmsUserIdentityStore charmsUserIdentityStore = new CharmsUserIdentityStore();
        charmsUserIdentityStore.init();
        final CharmsRoleIdentityStore charmsRoleIdentityStore = new CharmsRoleIdentityStore();
        charmsRoleIdentityStore.init();

        charmsIdentityManager.setIdentityStore(charmsUserIdentityStore);
        charmsIdentityManager.setRoleIdentityStore(charmsRoleIdentityStore);

        charmsPermissionStore = new CharmsPermissionStore();
        charmsPermissionStore.init();

        // setup a hibernate session
        hibernateSession = (Session) Component.getInstance("hibernateSession");

    }

    @AfterMethod
    @Override
    public void end() {
        super.end();
    }


    @Test
    public void testPermissionInheritance() {
        final String USER1 = "2u111sub";
        final String USER2 = "2u222sub";
        final String USER3 = "2u333sub";
        final String USER4 = "2u444sub";
        final String USER5 = "2u555sub";
        final String USER6 = "2u666sub";
        final String USER7 = "2u777sub";
        final String USERA = "2uAAAsub";

        final String ROLE1234 = "2r111sub";
        final String ROLE2345 = "2r222sub";
        final String ROLE3456 = "2r333sub";
        final String ROLE4567 = "2r444sub";

        final String GROUP12345 = "2g111sub";
        final String GROUP234567 = "2g222sub";
        final String GROUP3456 = "2g333sub";
        final String GROUP14567 = "2g444sub";

        final List<String> list;
        final CharmsRole role;

        Assert.assertTrue(charmsIdentityManager.createUser(USER1, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER2, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER3, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER4, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER5, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER6, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER7, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USERA, "pass"), "can't create user");

        Assert.assertTrue(charmsIdentityManager.createRole(ROLE1234), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE2345), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE3456), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE4567), "can't create role");

        Assert.assertTrue(charmsIdentityManager.createRole(GROUP12345), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(GROUP234567), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(GROUP3456), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(GROUP14567), "can't create role");
        // need a flush here for the assignments...
        hibernateSession.flush();

        Assert.assertTrue(charmsIdentityManager.grantRole(USER1, ROLE1234), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER2, ROLE1234), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER3, ROLE1234), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER4, ROLE1234), "can't grant role");

        Assert.assertTrue(charmsIdentityManager.grantRole(USER2, ROLE2345), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER3, ROLE2345), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER4, ROLE2345), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER5, ROLE2345), "can't grant role");

        Assert.assertTrue(charmsIdentityManager.grantRole(USER3, ROLE3456), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER4, ROLE3456), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER5, ROLE3456), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER6, ROLE3456), "can't grant role");

        Assert.assertTrue(charmsIdentityManager.grantRole(USER4, ROLE4567), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER5, ROLE4567), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER6, ROLE4567), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.grantRole(USER7, ROLE4567), "can't grant role");
        hibernateSession.flush();

        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(ROLE1234, GROUP12345), "can't grant role");
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(ROLE2345, GROUP12345), "can't grant role");
        hibernateSession.flush();


        Assert.assertTrue(charmsIdentityManager.grantRole(USERA, GROUP12345), "can't grant role");
        hibernateSession.flush();


        final String target1 = "target1";
        final String action1 = "action1";

        final CharmsPermission permission1 = new CharmsPermission();
        permission1.setTarget(target1);
        permission1.setAction(action1);
        permission1.setRecipient(ROLE1234);
        permission1.setDiscriminator(CharmsPermission.ROLE);
        hibernateSession.persist(permission1);
        hibernateSession.flush();


        // users 1-4 should inherit the permission
        // since they are members of the ROLE1234 groups

        final CharmsUser user1 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", USER1).uniqueResult();
        Assert.assertNotNull(user1);
        
        final List<Permission> permissions = charmsPermissionStore.listPermissions(target1, action1);
        // there are more permissions but only one for this target/action
        Assert.assertEquals(permissions.size(), 1);
        // this is a copy of what is happening inside the
        // PersistentPermissionResolver hasPermission method
        final Permission permission = permissions.get(0);
        // Role is a subclass of SimplePrincipal
        Assert.assertTrue(permission.getRecipient() instanceof SimplePrincipal, "permission not assigned to a SimplePrincipal");
        Assert.assertTrue(permission.getRecipient() instanceof Role, "permission not assigned to a Role");
        // the permission check is reduced to a hasRole call in CharmsIdentitys
        // super class
        Assert.assertTrue(charmsIdentityManager.getGrantedRoles(USER1).contains(ROLE1234));
        Assert.assertTrue(charmsIdentityManager.getGrantedRoles(USER2).contains(ROLE1234));
        Assert.assertTrue(charmsIdentityManager.getGrantedRoles(USER3).contains(ROLE1234));
        Assert.assertTrue(charmsIdentityManager.getGrantedRoles(USER4).contains(ROLE1234));


        // USERA is granted GROUP12345, so user A should inherit permissions
        // from ROLE1234
        Assert.assertFalse(charmsIdentityManager.getGrantedRoles(USERA).contains(ROLE1234)); // explicit
                                                                                             // granted
        Assert.assertTrue(charmsIdentityManager.getImpliedRoles(USERA).contains(ROLE1234)); // implicit
                                                                                            // granted


    }


}
