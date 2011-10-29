package net.wohlfart.charms.test.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.wohlfart.authorization.CharmsPermissionStore;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.security.Role;
import org.jboss.seam.security.SimplePrincipal;
import org.jboss.seam.security.permission.Permission;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PermissionStoreTest extends ComponentSessionBase {

    protected CharmsPermissionStore charmsPermissionStore;

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();

        // setup a permission store
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

    /**
     * this tests shows some weakness of seam Principal implementation...
     */
    @Test
    public void testMultiKey() {
        // this is the problem:
        Assert.assertTrue( new SimplePrincipal("xxx").hashCode() == new Role("xxx").hashCode() );
        Assert.assertTrue( new SimplePrincipal("xxx").equals(new Role("xxx")) );
    }

    @Test
    public void testUserAndRolePermissions() {
        List<Permission> list;
        final SimplePrincipal userRecipient = new SimplePrincipal("xxx");
        final Role roleRecipient = new Role("xxx");

        final Object targetA = "targetA";

        final String action = "action";

        final Permission userPermission = new Permission(targetA, action, userRecipient);
        final Permission rolePermission = new Permission(targetA, action, roleRecipient);

        list = charmsPermissionStore.listPermissions("targetA");
        Assert.assertTrue( list.size() == 0 , "wrong list size, should be 0 but is " + list.size());

        final List<Permission> pList = Arrays.asList(new Permission[] { userPermission, rolePermission });
        Assert.assertTrue( charmsPermissionStore.grantPermissions(pList), "can't grant mixed permission list");
        hibernateSession.flush();

        list = charmsPermissionStore.listPermissions("targetA");
        Assert.assertEquals( list.size(), 2, "wrong list size, should be 2 but is " + list.size());

        Assert.assertTrue( charmsPermissionStore.revokePermission(new Permission("targetA", "action", new Role("xxx"))), "can't revoke permission");
        hibernateSession.flush();

        list = charmsPermissionStore.listPermissions("targetA");
        Assert.assertEquals( list.size(), 1, "wrong list size, should be 1 but is " + list.size());

        Assert.assertTrue( charmsPermissionStore.revokePermission(new Permission("targetA", "action", new SimplePrincipal("xxx"))), "can't revoke permission");
        hibernateSession.flush();

        list = charmsPermissionStore.listPermissions("targetA");
        Assert.assertEquals( list.size(), 0, "wrong list size, should be 0 but is " + list.size());

    }

    // test methods
    @Test
    public void testMultiUserTargetPermissions() {
        List<Permission> list;

        final SimplePrincipal recipient1 = new SimplePrincipal("user1");
        final SimplePrincipal recipient2 = new SimplePrincipal("user2");
        final Object targetA = "targetA";
        final Object targetB = "targetB";
        final Object targetC = "targetC";
        final Object targetD = "targetD";

        final Permission permissionAA1 = new Permission(targetA, "actionA", recipient1);
        final Permission permissionBB1 = new Permission(targetB, "actionB", recipient1);
        final Permission permissionCC2 = new Permission(targetC, "actionC", recipient2);
        final Permission permissionDD2 = new Permission(targetD, "actionD", recipient2);
        final Permission permissionAE2 = new Permission(targetA, "actionE", recipient2);
        final Permission permissionBE1 = new Permission(targetB, "actionE", recipient1);
        final Permission permissionAC1 = new Permission(targetA, "actionC", recipient1);
        final Permission permissionAD1 = new Permission(targetA, "actionD", recipient1);

        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionAA1), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionBB1), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionCC2), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionDD2), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionAE2), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionBE1), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionAC1), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionAD1), "can't perform a simple grant" );
        hibernateSession.flush();

        // check if all permissions are there:
        list = charmsPermissionStore.listPermissions(targetA);
        assert list.size() == 4 : "permissions list size doesn't match, should be 4 but is actually: " + list.size();
        for (final Permission p : list) {
            Assert.assertTrue( charmsPermissionStore.revokePermission(p) );
            hibernateSession.flush();
        }
        list = charmsPermissionStore.listPermissions(targetA);
        assert list.size() == 0 : "permissions list size doesn't match, should be 0 but is actually: " + list.size();

        // check if both permissions are there:
        list = charmsPermissionStore.listPermissions(targetB);
        assert list.size() == 2 : "permissions list size doesn't match, should be 2 but is actually: " + list.size();
        for (final Permission p : list) {
            Assert.assertTrue( charmsPermissionStore.revokePermission(p) );
            hibernateSession.flush();
        }
        list = charmsPermissionStore.listPermissions(targetB);
        assert list.size() == 0 : "permissions list size doesn't match, should be 0 but is actually: " + list.size();

        // check if both permissions are there:
        list = charmsPermissionStore.listPermissions(targetC);
        assert list.size() == 1 : "permissions list size doesn't match, should be 1 but is actually: " + list.size();
        for (final Permission p : list) {
            Assert.assertTrue( charmsPermissionStore.revokePermission(p) );
            hibernateSession.flush();
        }
        list = charmsPermissionStore.listPermissions(targetC);
        assert list.size() == 0 : "permissions list size doesn't match, should be 0 but is actually: " + list.size();

        // check if both permissions are there:
        list = charmsPermissionStore.listPermissions(targetD);
        assert list.size() == 1 : "permissions list size doesn't match, should be 1 but is actually: " + list.size();
        for (final Permission p : list) {
            Assert.assertTrue( charmsPermissionStore.revokePermission(p) );
            hibernateSession.flush();
        }
        list = charmsPermissionStore.listPermissions(targetD);
        assert list.size() == 0 : "permissions list size doesn't match, should be 0 but is actually: " + list.size();

        // permissionAA1 and permissionAC1
        // permissionBB1 and permissionBE1
        // have a conflict since they have the same
        // unique key out of recipient and target, just different actions
        // so they must be grouped before persisting!
        final Permission[] pArray = new Permission[] { permissionAA1, // letter
                                                                      // schema:
                                                                      // target
                                                                      // action
                                                                      // recipient
                permissionBB1, permissionCC2, permissionDD2, permissionAE2, permissionBE1, permissionAC1
        // not in the list:
        // permissionAD1
        };

        final List<Permission> pList = Arrays.asList(pArray);

        try {
            charmsPermissionStore.revokePermissions(pList);
            assert false : "can revoke a list of unpersisted permissions, shouldn't be possible";
        } catch (final IllegalArgumentException ex) {
            // expected exception
            // rollback all changes in the session
            hibernateSession.clear();
            hibernateSession.flush();
        }

        Assert.assertTrue( charmsPermissionStore.grantPermissions(pList), "can't grant permission list" );
        hibernateSession.flush();

        list = charmsPermissionStore.listPermissions(targetA);
        assert list.size() == 3 : "wrong list size, should be 3, but is: " + list.size();
        // probably can't use list.contains here since it uses the hashvalues of
        // the object:
        // assert !list.contains(permissionAD1) :
        // "wrong permission after list grant, list size is: " + list.size();
        // assert list.contains(permissionAE2) :
        // "missing permission after list grant";

        try {
            // plist is unmodifiable
            // pList.add(permissionAD1);
            final ArrayList<Permission> mList = new ArrayList<Permission>();
            mList.addAll(pList);
            mList.add(permissionAD1);
            charmsPermissionStore.revokePermissions(mList);
            assert false : "can revoke a list with one unpersisted permission, shouldn't work";
        } catch (final IllegalArgumentException ex) {
            // expected exception
            // rollback all changes in the session
            hibernateSession.clear();
            hibernateSession.flush();
        }

        assert charmsPermissionStore.revokePermissions(pList) : "can not revoke a list with one persisted permission";
        list = charmsPermissionStore.listPermissions(targetA);
        assert list.size() == 0 : "permissions list size doesn't match, should be 0 but is actually: " + list.size();
        hibernateSession.flush();

    }

    @Test
    public void testMultiUserSingleTargetPermissions() {
        List<Permission> list;
        hibernateSession.clear();
        hibernateSession.flush();

        final SimplePrincipal recipient1 = new SimplePrincipal("user1");
        final SimplePrincipal recipient2 = new SimplePrincipal("user2");
        final Object target = "targetA";

        final Permission permissionA1 = new Permission(target, "actionA", recipient1);
        final Permission permissionB1 = new Permission(target, "actionB", recipient1);
        final Permission permissionC2 = new Permission(target, "actionC", recipient2);
        final Permission permissionD2 = new Permission(target, "actionD", recipient2);
        final Permission permissionE2 = new Permission(target, "actionE", recipient2);
        final Permission permissionE1 = new Permission(target, "actionE", recipient1);

        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionA1), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionB1), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionC2), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionD2), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionE2), "can't perform a simple grant" );
        hibernateSession.flush();
        Assert.assertTrue( charmsPermissionStore.grantPermission(permissionE1), "can't perform a simple grant" );
        hibernateSession.flush();

        // check if both permissions are there:
        list = charmsPermissionStore.listPermissions(target);
        assert list.size() == 6 : "permissions list size doesn't match, should be 6 but is actually: " + list.size();

        for (final Permission p : list) {
            Assert.assertTrue( charmsPermissionStore.revokePermission(p) );
            hibernateSession.flush();
        }

        // check if both permissions are there:
        list = charmsPermissionStore.listPermissions(target);
        assert list.size() == 0 : "permissions list size doesn't match, should be 0 but is actually: " + list.size();

    }

    @Test
    public void testSingleUserTargetPermissions() {
        List<Permission> list;

        final SimplePrincipal recipient = new SimplePrincipal("user");
        final Object target = "target";

        final Permission permission1 = new Permission(target, "action1", recipient);

        Assert.assertTrue( charmsPermissionStore.grantPermission(permission1), "can't perform a simple grant" );
        hibernateSession.flush();

        // grant the same permission again should throw an exception:
        try {
            Assert.assertTrue( charmsPermissionStore.grantPermission(permission1) );
            assert false : "granting same permission twice should htrow an exception";
        } catch (final IllegalArgumentException ex) {
            // exception expected
        }

        // grant another action
        final Permission permission2 = new Permission(target, "action2", recipient);
        Assert.assertTrue( charmsPermissionStore.grantPermission(permission2), "can't perform a simple grant" );
        hibernateSession.flush();

        // check if both permissions are there:
        list = charmsPermissionStore.listPermissions(target);
        assert list.size() == 2 : "permissions list size doesn't match, should be 2 but is actually: " + list.size();

        // grant another action
        final Permission permission3 = new Permission(target, "action3", recipient);
        Assert.assertTrue( charmsPermissionStore.grantPermission(permission3), "can't perform a simple grant" );
        hibernateSession.flush();

        // check if three permissions are there:
        list = charmsPermissionStore.listPermissions(target);
        assert list.size() == 3 : "permissions list size doesn't match, should be 3 but is actually: " + list.size();

        // revoking a permission that doesn't exist should throw an exception
        final Permission permissionX = new Permission(target, "actionX", recipient);
        try {
            assert charmsPermissionStore.revokePermission(permissionX);
            assert false : "revoking an unknoen permission works but should throw an exception";
        } catch (final IllegalArgumentException ex) {
            // exception expected
        }

        // revoke a permission (this is permission1)
        final Permission permissionY = new Permission(target, "action1", recipient);
        Assert.assertTrue( charmsPermissionStore.revokePermission(permissionY), "can't perform a simple revoke" );
        hibernateSession.flush();

        list = charmsPermissionStore.listPermissions(target);
        assert list.size() == 2 : "permissions list size doesn't match, should be 2 but is actually: " + list.size();

        // revoking the same permission twice should fail
        try {
            Assert.assertTrue( charmsPermissionStore.revokePermission(permission1) );
            assert false : "granting same permission twice should throw an exception";
        } catch (final IllegalArgumentException ex) {
            // exception expected
        }

        list = charmsPermissionStore.listPermissions(target);
        assert list.size() == 2 : "permissions list size doesn't match, should be 2 but is actually: " + list.size();

        Assert.assertTrue( charmsPermissionStore.revokePermission(permission2) );
        Assert.assertTrue( charmsPermissionStore.revokePermission(permission3) );

        // empty permission list
        list = charmsPermissionStore.listPermissions(target);
        assert list.size() == 0 : "permissions list size doesn't match, should be 0 but is actually: " + list.size();

    }

}
