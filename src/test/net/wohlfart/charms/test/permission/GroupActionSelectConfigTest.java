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
import net.wohlfart.jbpm4.node.GroupActionSelectConfig;

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

public class GroupActionSelectConfigTest extends AbstractPermissionTestBase {

    @Test
    public void testPermissionInheritanceTargetGroupInheritance() {
        
        GroupActionSelectConfig groupActionSelectConfig = new GroupActionSelectConfig();
        groupActionSelectConfig.setAction(ACTION1);
        String expression = groupActionSelectConfig.getQlExpression(null, user1);
        hibernateSession.createQuery(expression).list();
        
        // user1 in group1 has permission to do action on user2 in group2
        cleanupSession();// recipient, target, actions...
        createPermission(group1, group2, ACTION1, ACTION2);
        cleanupSession();// recipient, target, actions...
        List<CharmsRole> roles = CharmsPermissionQuery.findTargetGroupsWithPermission(hibernateSession, user1, ACTION1);
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
 
 
}
