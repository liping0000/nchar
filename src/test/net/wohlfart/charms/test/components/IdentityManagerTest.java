package net.wohlfart.charms.test.components;

import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.CharmsIdentityManager;
import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.CharmsRoleIdentityStore;
import net.wohlfart.authentication.entities.CharmsRole;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.seam.Component;
import org.jboss.seam.security.management.IdentityManagementException;
import org.jboss.seam.security.management.NoSuchRoleException;
import org.jboss.seam.security.management.NoSuchUserException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IdentityManagerTest extends ComponentSessionBase {

    protected CharmsIdentityManager charmsIdentityManager;

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
    }

    @AfterMethod
    @Override
    public void end() {
        hibernateSession.clear();
        hibernateSession.flush();
        super.end();
    }

    // test methods

    @Test
    public void testComponent() {
        // isEnabled checks for both identity stores
        Assert.assertTrue( charmsIdentityManager.isEnabled(), "identity stores are not set up in IdentityManager");
    }

    @Test
    public void testCreateRole() {

        final String TESTROLE = "testrole";

        // create a testrole
        Assert.assertTrue( charmsIdentityManager.createRole(TESTROLE), "can't create testrole");
        hibernateSession.flush();

        // try to create the same user again, this should fail since rolenames
        // are unique
        try {
            charmsIdentityManager.createRole(TESTROLE);
            Assert.fail("multiple roles with identitcal names created");
        } catch (final IdentityManagementException ex) {
            // exception expected
        }

        Assert.assertTrue(charmsIdentityManager.deleteRole(TESTROLE), "can't delete role");
        hibernateSession.flush();
        try {
            charmsIdentityManager.deleteRole(TESTROLE);
            Assert.fail("multiple deletes for same role possible");
        } catch (final NoSuchRoleException ex) {
            // exception expected
        }
        Assert.assertTrue( charmsIdentityManager.createRole(TESTROLE), "can't create role after delete");
        hibernateSession.flush();
        Assert.assertTrue( charmsIdentityManager.deleteRole(TESTROLE), "can't delete role");
        hibernateSession.flush();

        hibernateSession.clear();
    }

    @Test
    public void testCreateUser() {

        final String TESTUSER = "testuser";
        final String TESTPASSWD = "testpasswd";

        /*
        // create a testuser
        Assert.assertTrue( charmsIdentityManager.createUser(TESTUSER, TESTPASSWD), "can't create testuser");
        hibernateSession.flush();

        // try to create the same user again, this should fail since usernames
        // are unique
        try {
            charmsIdentityManager.createUser(TESTUSER, TESTPASSWD);
            Assert.fail("multiple users with identitcal names created");
        } catch (final IdentityManagementException ex) {
            // exception expected
        }

        Assert.assertTrue( charmsIdentityManager.deleteUser(TESTUSER), "can't delete user");
        hibernateSession.flush();
        try {
            charmsIdentityManager.deleteUser(TESTUSER);
            Assert.fail("multiple deletes for same user possible");
        } catch (final NoSuchUserException ex) {
            // exception expected
        }
        
        Assert.assertTrue( charmsIdentityManager.createUser(TESTUSER, TESTPASSWD), "can't create user after delete");
        hibernateSession.flush();
        Assert.assertTrue( charmsIdentityManager.deleteUser(TESTUSER), "can't delete user");
        hibernateSession.flush();
        hibernateSession.clear();
        */
    }

    @Test
    public void testEnableUser() {

        final String TESTUSER = "test2";

        Assert.assertTrue( charmsIdentityManager.createUser(TESTUSER, "test2", "firstname", "lastname"), "can't create user with names");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.userExists(TESTUSER), "user doesn't exists after createUser");
        Assert.assertTrue( charmsIdentityManager.isUserEnabled(TESTUSER), "user is not enabled after createUser");
        Assert.assertTrue( charmsIdentityManager.disableUser(TESTUSER), "disabled user broken");
        hibernateSession.flush();
        Assert.assertFalse( charmsIdentityManager.isUserEnabled(TESTUSER), "user is not disabled after disableUser");
        Assert.assertTrue( charmsIdentityManager.enableUser(TESTUSER), "enable user broken");
        Assert.assertTrue( charmsIdentityManager.isUserEnabled(TESTUSER), "user is not enabled after enableUser");
        Assert.assertTrue( charmsIdentityManager.deleteUser(TESTUSER), "delete user failed");
        hibernateSession.flush();
        Assert.assertFalse( charmsIdentityManager.userExists(TESTUSER), "user exists after deleteUser");

        // a user with empty password should be disabled
        Assert.assertTrue( charmsIdentityManager.createUser(TESTUSER, null), "can't create user with empty password");
        hibernateSession.flush();
        Assert.assertTrue( charmsIdentityManager.userExists(TESTUSER), "user with empty password doesn't exists after createUser");
        Assert.assertFalse( charmsIdentityManager.isUserEnabled(TESTUSER), "user without password is enabled, should be disabled");
        Assert.assertTrue( charmsIdentityManager.enableUser(TESTUSER), "can't enable user without password");
        Assert.assertTrue( charmsIdentityManager.isUserEnabled(TESTUSER), "user is not enabled after enableUser");
        Assert.assertTrue( charmsIdentityManager.deleteUser(TESTUSER), "delete user failed");

        hibernateSession.clear();
    }

    @Test
    public void testPasswordUser() {

        final String USER1 = "u11";
        final String USER2 = "u22";

        Assert.assertTrue( charmsIdentityManager.createUser(USER1, "p1"), "can't create user");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.changePassword(USER1, "p2"), "can't change password");

        Assert.assertTrue( charmsIdentityManager.authenticate(USER1, "p2"), "can't authenticate");

        Assert.assertFalse( charmsIdentityManager.authenticate(USER1, "p3"), "can authenticate with wrong password");

        Assert.assertTrue( charmsIdentityManager.createUser(USER2, "p4"), "can't create user with names");
        hibernateSession.flush();

        Assert.assertFalse( charmsIdentityManager.authenticate(USER2, "p3"), "can authenticate with wrong password");

        Assert.assertTrue( charmsIdentityManager.deleteUser(USER1));
        Assert.assertTrue( charmsIdentityManager.deleteUser(USER2));
        hibernateSession.flush();

        hibernateSession.clear();
    }

    @Test
    public void testRoleAssignment() {
        final String USER1 = "u1";
        final String USER2 = "u2";
        final String ROLE1 = "r1";
        final String ROLE2 = "r2";
        final String ROLE3 = "r3";
        List<String> list;

        hibernateSession.clear();

        Assert.assertTrue(charmsIdentityManager.createUser(USER1, "p1"), "can't create user");
        hibernateSession.flush();
        list = charmsIdentityManager.getGrantedRoles(USER1);
        Assert.assertEquals(list.size(), 0, "new user shouldn't have any roles yet");
        list = charmsIdentityManager.getImpliedRoles(USER1);
        Assert.assertEquals(list.size(), 0, "new user shouldn't have any implied roles yet");

        Assert.assertTrue(charmsIdentityManager.createRole(ROLE1), "can't create role");
        hibernateSession.flush();
        hibernateSession.clear();

        Assert.assertTrue(charmsIdentityManager.grantRole(USER1, ROLE1), "can't grant role");
        hibernateSession.flush();
        list = charmsIdentityManager.getGrantedRoles(USER1);
        Assert.assertEquals(list.size(), 1, "new user shouldn't have 1 role");
        list = charmsIdentityManager.getImpliedRoles(USER1);
        Assert.assertEquals(list.size(), 1, "new user shouldn't have 1 implied role");

        Assert.assertTrue(charmsIdentityManager.createRole(ROLE2), "can't create role");
        hibernateSession.flush();

        Assert.assertTrue(charmsIdentityManager.grantRole(USER1, ROLE2), "can't grant role");
        hibernateSession.flush();
        list = charmsIdentityManager.getGrantedRoles(USER1);
        Assert.assertEquals(list.size(), 2, "new user1 shouldn't have 2 role");
        list = charmsIdentityManager.getImpliedRoles(USER1);
        Assert.assertEquals(list.size(), 2, "new user1 shouldn't have 2 implied role");

        list = charmsIdentityManager.getGrantedRoles(USER1);
        Assert.assertEquals(2, list.size(), "role count missmatch");
        Assert.assertTrue( list.contains(ROLE1), "role 1 missing");
        Assert.assertTrue( list.contains(ROLE2), "role 2 missing");

        try {
            charmsIdentityManager.grantRole(USER1, ROLE3);
            Assert.fail("granted an unknown role");
        } catch (final NoSuchRoleException ex) {
            // expected exception
            hibernateSession.clear();
        }

        try {
            charmsIdentityManager.grantRole(USER1, ROLE2);
            Assert.fail("granted the same role twice");
        } catch (final IdentityManagementException ex) {
            // expected exception
            hibernateSession.clear();
        }

        try {
            charmsIdentityManager.grantRole(USER2, ROLE2);
            Assert.fail("granted a role to an unknown user");
        } catch (final IdentityManagementException ex) {
            // expected exception
            hibernateSession.clear();
        }

        Assert.assertTrue( charmsIdentityManager.createRole(ROLE3), "can't create role");
        Assert.assertTrue( charmsIdentityManager.createUser(USER2, "p1"), "can't create user");
        hibernateSession.flush();
        hibernateSession.clear();
        Assert.assertTrue( charmsIdentityManager.grantRole(USER2, ROLE3), "can't grant role");
        hibernateSession.flush();
        hibernateSession.clear();

        list = charmsIdentityManager.getGrantedRoles(USER2);
        Assert.assertEquals(1, list.size(), "role count missmatch");
        Assert.assertTrue( list.contains(ROLE3), "role 2 missing");

        Assert.assertTrue( charmsIdentityManager.grantRole(USER2, ROLE2), "can't grant role");
        hibernateSession.flush();
        hibernateSession.clear();
        list = charmsIdentityManager.getGrantedRoles(USER2);
        Assert.assertEquals(2, list.size(), "role count missmatch");
        Assert.assertTrue( list.contains(ROLE2), "role 2 missing");
        Assert.assertTrue( list.contains(ROLE3), "role 3 missing");

        Assert.assertTrue( charmsIdentityManager.revokeRole(USER2, ROLE2), "can't revoke role");
        hibernateSession.flush();
        list = charmsIdentityManager.getGrantedRoles(USER2);
        Assert.assertEquals(list.size(), 1, "role count missmatch");
        Assert.assertTrue( list.contains(ROLE3), "role 3 missing");
        hibernateSession.flush();
        hibernateSession.clear();
        list = charmsIdentityManager.getGrantedRoles(USER2);
        Assert.assertEquals(list.size(), 1, "role count missmatch");
        Assert.assertTrue( list.contains(ROLE3), "role 3 missing");

        hibernateSession.clear();

        list = charmsIdentityManager.getImpliedRoles(USER2);
        Assert.assertEquals(list.size(), 1, "role count missmatch");
        Assert.assertTrue(list.contains(ROLE3));

        try {
            charmsIdentityManager.revokeRole(USER2, ROLE2);
            Assert.fail("revoked the same role twice");
        } catch (final IdentityManagementException ex) {
            // expected exception
            hibernateSession.clear();
        }

        list = charmsIdentityManager.getImpliedRoles(USER2);
        Assert.assertEquals(list.size(), 1, "role count missmatch");
        Assert.assertTrue( list.contains(ROLE3), "role 3 missing");

        charmsIdentityManager.deleteRole(ROLE1);
        hibernateSession.flush();
        charmsIdentityManager.deleteRole(ROLE2);
        hibernateSession.flush();

        charmsIdentityManager.deleteRole(ROLE3);
        hibernateSession.flush();

        charmsIdentityManager.deleteUser(USER1);
        hibernateSession.flush();
        charmsIdentityManager.deleteUser(USER2);
        hibernateSession.flush();

        hibernateSession.clear();
    }

    @Test
    public void testUpstreamDownstreamRoles() {
        final String ROLE1 = "sr111";
        final String ROLE2 = "sr222";
        final String ROLE3 = "sr333";

        Assert.assertTrue(charmsIdentityManager.createRole(ROLE1),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE2),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE3),"can't create role");
        hibernateSession.flush();
        hibernateSession.clear();


        charmsIdentityManager.addRoleToGroup(ROLE1, ROLE2);  // adding role1 as upstream to role2
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLE2, ROLE3);  // adding role2 as upstream to role3
        hibernateSession.flush();
        hibernateSession.clear(); // we need to clear in order to get the entities out of the session
        // otherwise the stale downstream will give us no elements

        // role3 ---> roles2 ---> role1  role1 is contained in role2 is contained in role3

        CharmsRole role1 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE1)
        .uniqueResult();       
        Assert.assertEquals(role1.getUpstream().size(), 0);
        Assert.assertEquals(role1.getDownstream().size(), 1);

        CharmsRole role3 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE3)
        .uniqueResult();
        Assert.assertEquals(role3.getUpstream().size(), 1);
        Assert.assertEquals(role3.getDownstream().size(), 0);

        CharmsRole role2 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE2)
        .uniqueResult();
        Assert.assertEquals(role2.getUpstream().size(), 1);
        Assert.assertEquals(role2.getDownstream().size(), 1);


        Set<CharmsRole> upstreams = role2.getUpstream();
        Set<CharmsRole> downstreams = role2.getDownstream();

        Assert.assertEquals(upstreams.size(), 1);
        Assert.assertEquals(downstreams.size(), 1);

        Assert.assertEquals(upstreams.iterator().next().getName(), ROLE1);
        Assert.assertEquals(downstreams.iterator().next().getName(), ROLE3);
    }

    @Test
    public void testContainedRoles() {
        final String ROLE1 = "csr111";
        final String ROLE2 = "csr222";
        final String ROLE3 = "csr333";
        final String ROLE4 = "csr444";
        final String ROLE5 = "csr555";
        final String ROLE6 = "csr666";

        Assert.assertTrue(charmsIdentityManager.createRole(ROLE1),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE2),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE3),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE4),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE5),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE6),"can't create role");
        hibernateSession.flush();
        hibernateSession.clear();


        charmsIdentityManager.addRoleToGroup(ROLE1, ROLE2);  // adding role1 as upstream to role2
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLE2, ROLE3);  // adding role2 as upstream to role3
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLE3, ROLE4);  // adding role3 as upstream to role4
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLE4, ROLE5);  // adding role4 as upstream to role5
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLE5, ROLE6);  // adding role5 as upstream to role6
        hibernateSession.flush();
        hibernateSession.clear(); // we need to clear in order to get the entities out of the session
        // otherwise the stale downstream will give us no elements

        // role3 ---> roles2 ---> role1  role1 is contained in role2 is contained in role3



        CharmsRole role1 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE1)
        .uniqueResult();       
        role1.calculateContainedRoles(hibernateSession);

        CharmsRole role2 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE2)
        .uniqueResult();       
        role2.calculateContainedRoles(hibernateSession);

        CharmsRole role3 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE3)
        .uniqueResult();       
        role3.calculateContainedRoles(hibernateSession);

        CharmsRole role4 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE4)
        .uniqueResult();       
        role4.calculateContainedRoles(hibernateSession);

        CharmsRole role5 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE5)
        .uniqueResult();       
        role5.calculateContainedRoles(hibernateSession);

        
        CharmsRole role6 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE6)
        .uniqueResult();       
        role6.calculateContainedRoles(hibernateSession);

        hibernateSession.flush();
        hibernateSession.clear();

        role6 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE6)
        .uniqueResult();   
        Assert.assertEquals(role6.getContained().size(), 5);

        role1 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE1)
        .uniqueResult();       
        Assert.assertEquals(role1.getUpstream().size(), 0);
        Assert.assertEquals(role1.getDownstream().size(), 1);
        Assert.assertEquals(role1.getContained().size(), 0);
        
        role4 = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLE4)
        .uniqueResult();   
        Assert.assertEquals(role4.getContained().size(), 3);
        Assert.assertEquals(role4.getUpstream().size(), 1);
        Assert.assertEquals(role4.getDownstream().size(), 1);      
    }
    
    
    @Test
    public void testTreeContainedRoles() {
        final String ROLEA = "ROLEAtree";
        final String ROLEAA = "ROLEAAree";
        final String ROLEB = "ROLEBree";
        final String ROLEC = "ROLECree";
        final String ROLEAB = "ROLEABree";
        final String ROLEBC = "ROLEBCree";
        final String ROLECA = "ROLECAree";
        final String ROLEABC = "ROLEABCree";

        Assert.assertTrue(charmsIdentityManager.createRole(ROLEA),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLEAA),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLEB),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLEC),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLEAB),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLEBC),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLECA),"can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLEABC),"can't create role");
        hibernateSession.flush();
        hibernateSession.clear();


        charmsIdentityManager.addRoleToGroup(ROLEA, ROLEAB);  
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLEB, ROLEAB); 
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLEB, ROLEBC); 
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLEC, ROLEBC);  
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLEC, ROLECA); 
        hibernateSession.flush();
        charmsIdentityManager.addRoleToGroup(ROLEA, ROLECA);  
        hibernateSession.flush();
        
        charmsIdentityManager.addRoleToGroup(ROLEAA, ROLEA);
        hibernateSession.flush();
        hibernateSession.clear(); // we need to clear in order to get the entities out of the session
        // otherwise the stale downstream will give us no elements

        // role3 ---> roles2 ---> role1  role1 is contained in role2 is contained in role3



        CharmsRole roleA = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLEA)
        .uniqueResult();       

        CharmsRole roleB = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLEB)
        .uniqueResult();       
        Assert.assertNotNull(roleB);

        CharmsRole roleC = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLEC)
        .uniqueResult();       
        Assert.assertNotNull(roleC);

        CharmsRole roleAB = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLEAB)
        .uniqueResult();       
        
        roleAB.calculateContainedRoles(hibernateSession);

        
        Set<CharmsRole> containedAB = roleAB.getContained();
        Assert.assertEquals(containedAB.size(), 3);  // roleA, roleB, roleAA (inside roleA)
 
        Set<CharmsRole> upstreamAB = roleAB.getUpstream();
        Assert.assertEquals(upstreamAB.size(), 2);
        
        roleAB.calculateContainedRoles(hibernateSession);
        containedAB = roleAB.getContained();
        Assert.assertEquals(containedAB.size(), 3);
          
        hibernateSession.flush();
        hibernateSession.clear();
       
        roleA = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLEA)
        .uniqueResult(); 
 
        Set<CharmsRole> upstreamA = roleA.getUpstream();
        Assert.assertEquals(upstreamA.size(), 1);
        roleA.calculateContainedRoles(hibernateSession);

        Set<CharmsRole> containedA = roleA.getContained();
        Assert.assertEquals(containedA.size(), 1);
        
        roleAB = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLEAB)
        .uniqueResult(); 
        
        containedAB = roleAB.getContained();
        Assert.assertEquals(containedAB.size(), 3);
            
        
        charmsIdentityManager.addRoleToGroup(ROLEAB, ROLEABC);  // adding role5 as upstream to role6
        charmsIdentityManager.addRoleToGroup(ROLEBC, ROLEABC);  // adding role5 as upstream to role6
        charmsIdentityManager.addRoleToGroup(ROLECA, ROLEABC);  // adding role5 as upstream to role6
        hibernateSession.flush();
        hibernateSession.clear();
        
        CharmsRole roleABC = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", ROLEABC)
        .uniqueResult(); 
        roleABC.calculateContainedRoles(hibernateSession);
        
        Set<CharmsRole> containedABC = roleABC.getContained();
        Assert.assertEquals(containedABC.size(), 7);

    }



    @Test
    public void testRoleGroups() {
        final String USER1 = "u111";
        final String USER2 = "u222";
        final String ROLE1 = "r111";
        final String ROLE2 = "r222";
        final String ROLE3 = "r333";
        final String ROLE4 = "r444";
        final String GROUP1 = "g111";
        final String GROUP2 = "g222";
        final String GROUP3 = "g333";
        final String GROUP4 = "g444";
        List<String> list;

        Assert.assertTrue( charmsIdentityManager.createRole(ROLE1), "can't create role");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.createRole(GROUP1), "can't create role");
        Assert.assertTrue( charmsIdentityManager.createRole(GROUP2), "can't create role");
        Assert.assertTrue( charmsIdentityManager.createRole(GROUP3), "can't create role");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.addRoleToGroup(GROUP1, ROLE1), "can't assign role to group");

        Assert.assertTrue( charmsIdentityManager.addRoleToGroup(GROUP2, ROLE1), "can't assign role to group");

        Assert.assertTrue( charmsIdentityManager.addRoleToGroup(GROUP3, ROLE1), "can't assign role to group");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.createUser(USER2, "p1"), "can't create user");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.grantRole(USER2, ROLE1), "can't assign role");
        hibernateSession.flush();

        list = charmsIdentityManager.getImpliedRoles(USER2); // :
        // "can't resolve implied roles";
        Assert.assertEquals(4,list.size());
        Assert.assertTrue( list.contains(ROLE1));
        Assert.assertTrue( list.contains(GROUP1));
        Assert.assertTrue( list.contains(GROUP2));
        Assert.assertTrue( list.contains(GROUP3));

        try {
            charmsIdentityManager.addRoleToGroup(GROUP3, GROUP4);
            Assert.fail("assigning an unknown group");
        } catch (final NoSuchRoleException ex) {
            // expected exception
        }

        Assert.assertTrue(charmsIdentityManager.createRole(GROUP4), "can't create role");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.addRoleToGroup(GROUP4, GROUP3), "can't assign role to group");
        hibernateSession.flush();

        list = charmsIdentityManager.getRoleGroups(ROLE1);
        Assert.assertTrue( list.contains(GROUP2), "missing group in role");

        Assert.assertTrue( charmsIdentityManager.removeRoleFromGroup(GROUP2, ROLE1), "can't remove role from group");

        list = charmsIdentityManager.getImpliedRoles(USER2); // :
        // "can't resolve implied roles";
        Assert.assertEquals(4, list.size());
        Assert.assertTrue( list.contains(ROLE1));
        Assert.assertTrue( list.contains(GROUP1));
        Assert.assertTrue( list.contains(GROUP4));
        Assert.assertTrue( list.contains(GROUP3));

        Assert.assertTrue( charmsIdentityManager.removeRoleFromGroup(GROUP1, ROLE1), "can't remove role from group");
        Assert.assertTrue( charmsIdentityManager.removeRoleFromGroup(GROUP3, ROLE1), "can't remove role from group");
        Assert.assertTrue( charmsIdentityManager.removeRoleFromGroup(GROUP4, GROUP3), "can't remove role from group");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.deleteUser(USER2), "can't remove user with groups");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.createRole(ROLE2), "can't create role");
        hibernateSession.flush();
        Assert.assertTrue( charmsIdentityManager.addRoleToGroup(ROLE2, ROLE1), "can't assign role to group");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.createRole(ROLE3), "can't create role");
        hibernateSession.flush();
        Assert.assertTrue( charmsIdentityManager.addRoleToGroup(ROLE3, ROLE2), "can't assign role to group");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.createRole(ROLE4), "can't create role");
        hibernateSession.flush();
        Assert.assertTrue( charmsIdentityManager.addRoleToGroup(ROLE4, ROLE3), "can't assign role to group");
        hibernateSession.flush();

        Assert.assertTrue( charmsIdentityManager.createUser(USER1, "p1"), "can't create user");
        hibernateSession.flush();
        Assert.assertTrue( charmsIdentityManager.grantRole(USER1, ROLE1), "can't assign role");
        hibernateSession.flush();

        list = charmsIdentityManager.getImpliedRoles(USER1); // :
        // "can't resolve implied roles";
        Assert.assertEquals(list.size(), 4, "size should be 4 but is " + list.size());
        Assert.assertTrue( list.contains(ROLE1));
        Assert.assertTrue( list.contains(ROLE2));
        Assert.assertTrue( list.contains(ROLE3));
        Assert.assertTrue( list.contains(ROLE4));
    }

    /**
     * this test is checking the user inheritance along the roles/groups this is
     * implemented the getAllMemberUsers() method, which collects all users of
     * all upstream roles which is pretty pointless
     * 
     * since the permissions are forwarded downstream, if we woudl do the same
     * with the users, all users would have all permissions in the root node...
     * 
     * so the get allMemberUsers method should be deprecated and not used at all
     * 
     */
    @Test
    public void testGetAllMemberUsers() {
        final String USER1 = "u111sub";
        final String USER2 = "u222sub";
        final String USER3 = "u333sub";
        final String USER4 = "u444sub";
        final String USER5 = "u555sub";
        final String USER6 = "u666sub";
        final String USER7 = "u777sub";

        final String ROLE1234 = "r111sub";
        final String ROLE2345 = "r222sub";
        final String ROLE3456 = "r333sub";
        final String ROLE4567 = "r444sub";

        final String GROUP12345 = "g111sub";
        final String GROUP234567 = "g222sub";
        final String GROUP3456 = "g333sub";
        final String GROUP14567 = "g444sub";

        final List<String> list;
        CharmsRole role;
        
        Assert.assertTrue(tx.isActive());

        Assert.assertTrue(charmsIdentityManager.createUser(USER1, "pass"), "can't create user");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());
        Assert.assertTrue(charmsIdentityManager.createUser(USER2, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER3, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER4, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER5, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER6, "pass"), "can't create user");
        Assert.assertTrue(charmsIdentityManager.createUser(USER7, "pass"), "can't create user");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

        Assert.assertTrue(charmsIdentityManager.createRole(ROLE1234), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE2345), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE3456), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(ROLE4567), "can't create role");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

        Assert.assertTrue(charmsIdentityManager.createRole(GROUP12345), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(GROUP234567), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(GROUP3456), "can't create role");
        Assert.assertTrue(charmsIdentityManager.createRole(GROUP14567), "can't create role");
        // need a flush here for the assignments...
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

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
        Assert.assertTrue(tx.isActive());

        // set test each user only once
        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 5);

        // adding a user that is already in the set shouldn't change anything
        Assert.assertTrue(charmsIdentityManager.grantRole(USER5, GROUP12345), "can't grant role");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 5);

        // adding a user tha is not yet in the set shoul be reflected
        Assert.assertTrue(charmsIdentityManager.grantRole(USER7, GROUP12345), "can't grant role");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 6);

        // removing the user should get us the old count
        Assert.assertTrue(charmsIdentityManager.revokeRole(USER7, GROUP12345), "can't revoke role");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 5);

        // removing a user that is in both roles shouldn't change anything
        Assert.assertTrue(charmsIdentityManager.revokeRole(USER3, ROLE1234), "can't revoke role");
        hibernateSession.flush();

        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 5);

        // removing from the other role should dec the count by one now
        Assert.assertTrue(charmsIdentityManager.revokeRole(USER3, ROLE2345), "can't revoke role");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 4);

        // adding again should get us the old count again
        Assert.assertTrue(charmsIdentityManager.grantRole(USER3, ROLE1234), "can't revoke role");
        hibernateSession.flush();

        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 5);

        Assert.assertTrue(charmsIdentityManager.grantRole(USER3, ROLE2345), "can't revoke role");
        hibernateSession.flush();
        Assert.assertTrue(tx.isActive());

        role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", GROUP12345).uniqueResult();
        Assert.assertEquals(role.getUpstream().size(), 2);
        Assert.assertEquals(role.getAllMemberUsers().size(), 5);
        Assert.assertTrue(tx.isActive());

    }

}
