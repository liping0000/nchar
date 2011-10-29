package net.wohlfart.charms.test.permission;

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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class AbstractPermissionTestBase extends SeamTest {

    protected CharmsIdentityManager charmsIdentityManager;
    protected CharmsPermissionStore charmsPermissionStore;

    protected CharmsUser user1;
    protected CharmsUser user2;
    protected CharmsUser user3;
    protected CharmsUser user4;
    protected CharmsUser user5;

    protected CharmsRole group1;
    protected CharmsRole group2;
    protected CharmsRole group3;
    protected CharmsRole group4;
    protected CharmsRole group5;
    
    protected String TARGET1 = "target1Ptst";
    protected String ACTION1 = "action1Ptst";
    protected String ACTION2 = "action2Ptst";
    protected String ACTION3 = "action3Ptst";

    
    protected Session hibernateSession;

    @BeforeClass
    @Override
    public void begin() {
        super.begin();
        Identity.setSecurityEnabled(false);
        Lifecycle.beginCall();
        Lifecycle.beginSession(session.getAttributes(), null);

        // setup an identityManager
        charmsIdentityManager = new CharmsIdentityManager();

        final CharmsUserIdentityStore charmsUserIdentityStore = new CharmsUserIdentityStore();
        charmsUserIdentityStore.init();
        final CharmsRoleIdentityStore charmsRoleIdentityStore = new CharmsRoleIdentityStore();
        charmsRoleIdentityStore.init();

        charmsIdentityManager.setIdentityStore(charmsUserIdentityStore);
        charmsIdentityManager.setRoleIdentityStore(charmsRoleIdentityStore);

        // setup a permission store
        charmsPermissionStore = new CharmsPermissionStore();
        charmsPermissionStore.init();

        // setup a hibernate session
        hibernateSession = (Session) Component.getInstance("hibernateSession");
        
        // setup 5 users
        user1 = createUser("permUser1x");
        user2 = createUser("permUser2x");
        user3 = createUser("permUser3x");
        user4 = createUser("permUser4x");
        user5 = createUser("permUser5x");

        // setup 5 groups
        group1 = createGroup("permGroup1x");
        group2 = createGroup("permGroup2x");
        group3 = createGroup("permGroup3x");
        group4 = createGroup("permGroup4x");
        group5 = createGroup("permGroup5x");
        
        cleanupSession();
    };

    @AfterClass
    @Override
    public void end() {

        cleanupSession();
       
        deleteUser(user1);
        deleteUser(user2);
        deleteUser(user3);
        deleteUser(user4);
        deleteUser(user5);
        
        deleteGroup(group1);
        deleteGroup(group2);
        deleteGroup(group3);
        deleteGroup(group4);
        deleteGroup(group5);
        
        hibernateSession.flush();
        
        session.invalidate();
        // super.end();
    };
    


    
    
    
    
    
    protected void cleanupSession() {
        hibernateSession.flush();
        hibernateSession.clear();
        group1 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group1.getName()).uniqueResult();
        group2 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group2.getName()).uniqueResult();
        group3 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group3.getName()).uniqueResult();
        group4 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group4.getName()).uniqueResult();
        group5 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group5.getName()).uniqueResult();
           
        group1.calculateContainedRoles(hibernateSession);
        group2.calculateContainedRoles(hibernateSession);
        group3.calculateContainedRoles(hibernateSession);
        group4.calculateContainedRoles(hibernateSession);
        group5.calculateContainedRoles(hibernateSession);
        hibernateSession.flush();
        hibernateSession.clear();

        group1 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group1.getName()).uniqueResult();
        group2 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group2.getName()).uniqueResult();
        group3 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group3.getName()).uniqueResult();
        group4 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group4.getName()).uniqueResult();
        group5 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", group5.getName()).uniqueResult();
      
        user1 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", user1.getName()).uniqueResult();
        user2 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", user2.getName()).uniqueResult();
        user3 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", user3.getName()).uniqueResult();
        user4 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", user4.getName()).uniqueResult();
        user5 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", user5.getName()).uniqueResult();       
    }
    
    
    
    
    

    // https://forum.hibernate.org/viewtopic.php?t=969465&highlight=path+expected+join
    // http://jumpingbean.co.za/blogs/mark/hibernate_hql_inner_join_on_clause
    // https://forum.hibernate.org/viewtopic.php?t=964492&highlight=path+expected+join
   
    

    protected void createPermission(CharmsRole recipient, CharmsUser target, String... actions) {
        CharmsPermission charmsPermission = new CharmsPermission();
        charmsPermission.setRecipient(recipient.getName());
        charmsPermission.setDiscriminator(CharmsPermission.ROLE);
        charmsPermission.setTarget(SeamUserInstanceTargetSetup.TARGET_STRING);
        charmsPermission.setTargetId(target.getId());
        final StringBuffer buf = new StringBuffer();
        // String result = "";
        for (final String action : actions) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(action);
        }
        charmsPermission.setAction(buf.toString());
        hibernateSession.persist(charmsPermission);
        hibernateSession.flush();
    }

    protected void createPermission(CharmsRole recipient, CharmsRole target, String... actions) {
        CharmsPermission charmsPermission = new CharmsPermission();
        charmsPermission.setRecipient(recipient.getName());
        charmsPermission.setDiscriminator(CharmsPermission.ROLE);
        charmsPermission.setTarget(SeamRoleInstanceTargetSetup.TARGET_STRING);
        charmsPermission.setTargetId(target.getId());
        final StringBuffer buf = new StringBuffer();
        // String result = "";
        for (final String action : actions) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(action);
        }
        charmsPermission.setAction(buf.toString());
        hibernateSession.persist(charmsPermission);
        hibernateSession.flush();
    }

    protected void createPermission(CharmsUser recipient, CharmsUser target, String... actions) {
        CharmsPermission charmsPermission = new CharmsPermission();
        charmsPermission.setRecipient(recipient.getName());
        charmsPermission.setDiscriminator(CharmsPermission.USER);
        charmsPermission.setTarget(SeamUserInstanceTargetSetup.TARGET_STRING);
        charmsPermission.setTargetId(target.getId());
        final StringBuffer buf = new StringBuffer();
        // String result = "";
        for (final String action : actions) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(action);
        }
        charmsPermission.setAction(buf.toString());
        hibernateSession.persist(charmsPermission);
        hibernateSession.flush();
    }

    protected void createPermission(CharmsUser recipient, CharmsRole target, String... actions) {
        CharmsPermission charmsPermission = new CharmsPermission();
        charmsPermission.setRecipient(recipient.getName());
        charmsPermission.setDiscriminator(CharmsPermission.USER);
        charmsPermission.setTarget(SeamRoleInstanceTargetSetup.TARGET_STRING);
        charmsPermission.setTargetId(target.getId());
        final StringBuffer buf = new StringBuffer();
        // String result = "";
        for (final String action : actions) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(action);
        }
        charmsPermission.setAction(buf.toString());
        hibernateSession.persist(charmsPermission);
        hibernateSession.flush();
    }

    protected void createPermission(CharmsUser recipient, String target, String... actions) {
        CharmsPermission charmsPermission = new CharmsPermission();
        charmsPermission.setRecipient(recipient.getName());
        charmsPermission.setDiscriminator(CharmsPermission.USER);
        charmsPermission.setTarget(target);
        final StringBuffer buf = new StringBuffer();
        // String result = "";
        for (final String action : actions) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(action);
        }
        charmsPermission.setAction(buf.toString());
        hibernateSession.persist(charmsPermission);
        hibernateSession.flush();
    }

    protected void createPermission(CharmsRole recipient, String target, String... actions) {
        CharmsPermission charmsPermission = new CharmsPermission();
        charmsPermission.setRecipient(recipient.getName());
        charmsPermission.setDiscriminator(CharmsPermission.ROLE);
        charmsPermission.setTarget(target);
        final StringBuffer buf = new StringBuffer();
        // String result = "";
        for (final String action : actions) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(action);
        }
        charmsPermission.setAction(buf.toString());
        hibernateSession.persist(charmsPermission);
        hibernateSession.flush();
    }
    
    protected void removePermissions(CharmsRole recipient) {
        hibernateSession
            .getNamedQuery(CharmsPermission.REMOVE_FOR_ROLE_NAME)
            .setParameter("name", recipient.getName())
            .executeUpdate();
    }

    protected void removePermissions(CharmsUser recipient) {
        hibernateSession
            .getNamedQuery(CharmsPermission.REMOVE_FOR_USER_NAME)
            .setParameter("name", recipient.getName())
            .executeUpdate();
    }


    protected CharmsUser createUser(String username) {
        CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName(username);
        charmsUser.setFirstname(username);
        charmsUser.setLastname(username);
        charmsUser.setEnabled(true);
        charmsUser.setPasswd(username);
        hibernateSession.persist(charmsUser);
        hibernateSession.flush();

        charmsUser = (CharmsUser) hibernateSession
        .getNamedQuery(CharmsUser.FIND_BY_NAME)
        .setParameter("name", username)
        .uniqueResult();
        Assert.assertNotNull(charmsUser);
        Assert.assertNotNull(charmsUser.getId());
        return charmsUser;
    }
    
    protected void deleteUser(CharmsUser charmsUser) {       
        hibernateSession.delete(charmsUser);
        hibernateSession.flush();
    }

    protected CharmsRole createGroup(String username) {
        CharmsRole charmsRole = new CharmsRole();
        charmsRole.setName(username);
        charmsRole.calculateContainedRoles(hibernateSession);
        hibernateSession.persist(charmsRole);
        hibernateSession.flush();

        charmsRole = (CharmsRole) hibernateSession
        .getNamedQuery(CharmsRole.FIND_BY_NAME)
        .setParameter("name", username)
        .uniqueResult();
        Assert.assertNotNull(charmsRole);
        Assert.assertNotNull(charmsRole.getId());
        return charmsRole;
    }
    
    protected void deleteGroup(CharmsRole charmsGroup) {       
        hibernateSession.delete(charmsGroup);
    }
    
    

}
