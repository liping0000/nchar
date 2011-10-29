package net.wohlfart.charms.test.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.wohlfart.authentication.CharmsIdentityManager;
import net.wohlfart.authentication.CharmsRoleIdentityStore;
import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.CharmsPermissionStore;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.targets.SeamRoleInstanceTargetSetup;
import net.wohlfart.authorization.targets.SeamUserInstanceTargetSetup;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.mock.SeamTest;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.Role;
import org.jboss.seam.security.SimplePrincipal;
import org.jboss.seam.security.permission.Permission;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PermissionQueryTest extends AbstractPermissionTestBase {

    @Test
    public void testPermissionInheritanceTargetGroupInheritance() {
        List<CharmsRole> roles;
        
        // user1 in group1 has permission to do action on user2 in group2
        cleanupSession();// recipient, target, actions...
        createPermission(group1, group2, ACTION1, ACTION2);
        cleanupSession();// recipient, target, actions...
        roles = CharmsPermissionQuery.findTargetGroupsWithPermission(hibernateSession, user1, ACTION1);
        Assert.assertEquals(roles.size(), 0);

        createPermission(user1, group2, ACTION1, ACTION2);
        cleanupSession();// recipient, target, actions...
        roles = CharmsPermissionQuery.findTargetGroupsWithPermission(hibernateSession, user1, ACTION1);
        Assert.assertEquals(roles.size(), 1);
        
        // cleanup
        //Assert.assertTrue(charmsIdentityManager.revokeRole(user4.getName(), group4.getName()));
        removePermissions(group1);
        removePermissions(group2);
        removePermissions(group3);
        removePermissions(group4);
        removePermissions(user1);
        //charmsIdentityManager.removeRoleFromGroup(group3.getName(), group2.getName());
    }
 
    
    /**
     * this is for testing if a permission on a group is inherited to the members and subgroups
     * and if all the users/members that are targets of the permission will be found
     */
    @Test
    public void testPermissionInheritanceTargetUserInheritance() {
        // user1 in group1 has permission to do action on user2 in group2
        cleanupSession();// recipient, target, actions...
        createPermission(group1, group2, ACTION1, ACTION2);
        cleanupSession();// recipient, target, actions...
        Assert.assertTrue(charmsIdentityManager.grantRole(user1.getName(), group1.getName()));
        cleanupSession(); 
        Assert.assertTrue(charmsIdentityManager.grantRole(user2.getName(), group2.getName()));
        cleanupSession(); 
         
        List<CharmsUser> users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION1);
        Assert.assertEquals(users.size(), 1);
        Assert.assertEquals(users.get(0).getName(), user2.getName());
        
        Assert.assertTrue(charmsIdentityManager.grantRole(user3.getName(), group3.getName()));
        cleanupSession(); 
        
        charmsIdentityManager.addRoleToGroup(group3.getName(), group2.getName());
        cleanupSession(); 
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION1);
        Assert.assertEquals(users.size(), 2, "users: " + users);
        
        // user 4 is a member of group4 which is a subgroup of group1 which has right on group2&group3 which have 2 users:
        Assert.assertTrue(charmsIdentityManager.grantRole(user4.getName(), group4.getName()));
        cleanupSession(); 
        charmsIdentityManager.addRoleToGroup(group4.getName(), group1.getName());
        cleanupSession(); 
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user4, ACTION1);
        Assert.assertEquals(users.size(), 2, "users: " + users);
        
        // group4 gets permission to act on user5
        createPermission(group4, user5, ACTION1);
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user4, ACTION1);
        Assert.assertEquals(users.size(), 3, "users: " + users);
        
        
        // cleanup:
        Assert.assertTrue(charmsIdentityManager.revokeRole(user1.getName(), group1.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user2.getName(), group2.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user3.getName(), group3.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user4.getName(), group4.getName()));
        removePermissions(group1);
        removePermissions(group2);
        removePermissions(group3);
        removePermissions(group4);
        charmsIdentityManager.removeRoleFromGroup(group3.getName(), group2.getName());
        charmsIdentityManager.removeRoleFromGroup(group4.getName(), group1.getName());
    } 

  
    
   
    /**
     * this is for testing if a permission on a group is inherited to the mebers and subgroups
     * and if all the users/members that are targets of the permission will be found
     */
    @Test
    public void testPermissionTargetUserInheritance() {
        
        // user1 has permission to do action on user2
        cleanupSession();// recipient, target, actions...
        createPermission(user1, user2, ACTION1, ACTION2);
        
        cleanupSession();       
        List<CharmsUser> users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION1);
        Assert.assertEquals(users.size(), 1);
        Assert.assertEquals(users.get(0).getName(), user2.getName());
  
        // user1 has permission to do action on user3
        cleanupSession();// receiver, target, actions...
        createPermission(user1, user3, ACTION1, ACTION3);
        cleanupSession();       
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION1);
        Assert.assertEquals(users.size(), 2);
        
        // user1 has permission to do action on user4
        cleanupSession();// receiver, target, actions...
        createPermission(user1, user4, ACTION1, ACTION2);
        cleanupSession();       
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION1);
        Assert.assertEquals(users.size(), 3);
        
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION3);
        Assert.assertEquals(users.size(), 1);
        
        Assert.assertEquals(users.get(0).getName(), user3.getName());
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 2);
        
        
        Assert.assertEquals(group1.getMemberships().size(), 0, "membership should be emty, but is: " + group1.getMemberships());
        
        // give user 1 permission to action on group1
        cleanupSession();// recipient, target, actions...
        createPermission(user1, group1, ACTION1, ACTION2);
        cleanupSession();       
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        
        Assert.assertEquals(users.size(), 2, " users: " + users);  // group1 is empty so no change
        
        // add user5 to group1
        Assert.assertTrue(charmsIdentityManager.grantRole(user5.getName(), group1.getName()));
        cleanupSession();       
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 3, " users: " + users); // one more target to act opon..
       
        // add user3 to group1
        Assert.assertTrue(charmsIdentityManager.grantRole(user3.getName(), group1.getName()));
        cleanupSession();       
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 4, " users: " + users); // one more target to act opon..
               
        // add user4 to group1, user 4 is already a target, so no change
        Assert.assertTrue(charmsIdentityManager.grantRole(user4.getName(), group1.getName()));
        cleanupSession();       
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 4, " users: " + users); // one more target to act opon..
       
        // remove all permissions on user1
        removePermissions(user1);
        cleanupSession();       
        createPermission(user1, group1, ACTION2); // only the permission on group1 with 3 members is left
        cleanupSession();       
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 3, " users: " + users); // one more target to act opon..
        
        // now a user 0 layers later:
        Assert.assertTrue(charmsIdentityManager.grantRole(user2.getName(), group1.getName()));
        cleanupSession(); 
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 4, " users: " + users); 
        cleanupSession(); 
        Assert.assertTrue(charmsIdentityManager.revokeRole(user2.getName(), group1.getName()));
        cleanupSession(); 
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 3, " users: " + users); 
        
        
        // now a user 1 layers later:
        Assert.assertTrue(charmsIdentityManager.grantRole(user2.getName(), group2.getName()));
        cleanupSession(); 
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group2.getName(), group1.getName()));  
        cleanupSession(); 
        // user2 is an indirect member of group1 now, user1 has action2 permission on group1 which should include
        // user2 via group2 now
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 4, " users: " + users); 
        cleanupSession(); 
        Assert.assertTrue(charmsIdentityManager.revokeRole(user2.getName(), group2.getName()));
        cleanupSession(); 
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 3, " users: " + users); 
        
        
        // now a user 2 layers later:
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group3.getName(), group2.getName()));
        cleanupSession(); 
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 3, " users: " + users); // one more target to act opon..
        cleanupSession(); 
        Assert.assertTrue(charmsIdentityManager.grantRole(user2.getName(), group3.getName()));
        cleanupSession(); 
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user1, ACTION2);
        Assert.assertEquals(users.size(), 4, " users: " + users); // one more target to act opon..
        
        users = CharmsPermissionQuery.findTargetUsersWithPermission(hibernateSession, user2, ACTION2);
        Assert.assertEquals(users.size(), 0, " users: " + users); // one more target to act opon..
        
        
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group2.getName(), group1.getName()));
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group3.getName(), group2.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user2.getName(), group3.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user3.getName(), group1.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user4.getName(), group1.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user5.getName(), group1.getName()));
        removePermissions(user1);
        removePermissions(group1);
    }
 
        
    
    @Test
    public void testPermissionRecipientUserInheritance() {
 
        cleanupSession();

        // create two permissions to user1 and group1
        createPermission(user1, TARGET1, ACTION1, ACTION2);
        createPermission(group1, TARGET1, ACTION1, ACTION2);  
        cleanupSession();
        
        // only user with explicit granted permission
        List<CharmsUser> users = CharmsPermissionQuery.findRecipientUsersWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(users.size(), 1);
        Assert.assertEquals(users.get(0).getName(), user1.getName());
        
        // granting group1 to user1, adding an indirect permission
        Assert.assertTrue(charmsIdentityManager.grantRole(user2.getName(), group1.getName()));
        cleanupSession();
        users = CharmsPermissionQuery.findRecipientUsersWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(users.size(), 2);

        // 
        Assert.assertTrue(charmsIdentityManager.grantRole(user3.getName(), group1.getName()));
        cleanupSession();
        users = CharmsPermissionQuery.findRecipientUsersWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(users.size(), 3);
        
        Assert.assertTrue(charmsIdentityManager.grantRole(user4.getName(), group2.getName()));
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group1.getName(), group2.getName()));
        cleanupSession();
        users = CharmsPermissionQuery.findRecipientUsersWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(users.size(), 4);
                
        createPermission(user5, TARGET1, ACTION2);
        cleanupSession();
        users = CharmsPermissionQuery.findRecipientUsersWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(users.size(), 4);
        
        cleanupSession();
        users = CharmsPermissionQuery.findRecipientUsersWithPermission(hibernateSession, TARGET1, ACTION2);
        Assert.assertEquals(users.size(), 5);
        
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group1.getName(), group2.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user2.getName(), group1.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user3.getName(), group1.getName()));
        Assert.assertTrue(charmsIdentityManager.revokeRole(user4.getName(), group2.getName()));
       
        removePermissions(user1);
        removePermissions(group1);
        removePermissions(user5);
        
    }
    

    
    
    
    @Test
    public void testPermissionRecipientGroupInheritance() {
    
        cleanupSession();

        // create two permissions to user1 and group1
        createPermission(user1, TARGET1, ACTION1, ACTION2);
        createPermission(group1, TARGET1, ACTION1, ACTION2);
  
        cleanupSession();     

        // make sure group1 has the permission
        Assert.assertEquals(group1.getContained().size(), 0);
        List<CharmsRole> groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals(groups.get(0).getName(), group1.getName());
      
        // add group1 (with permission) to group2 as upstream/contained group
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group1.getName(), group2.getName()));
        cleanupSession();

        // group1 has no upstream/contained groups
        Assert.assertEquals(group1.getUpstream().size(), 0);
        Assert.assertEquals(group1.getContained().size(), 0);
        // group 2 has group1 as upstream/contained group
        Assert.assertEquals(group2.getUpstream().size(), 1);
        Assert.assertEquals(group2.getContained().size(), 1);
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 2);
   
        // add group1 to group3, group3 has group1 as upstream/contained group
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group1.getName(), group3.getName()));
        cleanupSession();

        // no upstream/contained
        Assert.assertEquals(group1.getUpstream().size(), 0);
        Assert.assertEquals(group1.getContained().size(), 0);     
        // group2 has group1 as upstream/ contained group
        Assert.assertEquals(group2.getUpstream().size(), 1);
        Assert.assertEquals(group2.getContained().size(), 1);
        // group3 has group1 as upstream/ contained group
        Assert.assertEquals(group3.getUpstream().size(), 1);
        Assert.assertEquals(group3.getContained().size(), 1);
        // group2, group3 inherit from group1, group1 has anyways..
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 3);
       
        // add group1 to group4
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group1.getName(), group4.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 4);
        
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group5.getName(), group1.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 4);
        
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group1.getName(), group3.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 3);        
        
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group1.getName(), group2.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 2);        
        
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group1.getName(), group4.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 1);        
        
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group5.getName(), group1.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 1);        
          
        // now chain the groups
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group1.getName(), group2.getName()));
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group2.getName(), group3.getName()));
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group3.getName(), group4.getName()));
        Assert.assertTrue(charmsIdentityManager.addRoleToGroup(group4.getName(), group5.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 5);        

        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group2.getName(), group3.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 2);        
       
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group1.getName(), group2.getName()));
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group3.getName(), group4.getName()));
        Assert.assertTrue(charmsIdentityManager.removeRoleFromGroup(group4.getName(), group5.getName()));
        cleanupSession();
        groups = CharmsPermissionQuery.findRecipientGroupsWithPermission(hibernateSession, TARGET1, ACTION1);
        Assert.assertEquals(groups.size(), 1);        
 
        removePermissions(user1);
        removePermissions(group1);
    }

}
