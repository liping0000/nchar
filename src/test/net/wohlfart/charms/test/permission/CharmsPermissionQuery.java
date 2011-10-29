package net.wohlfart.charms.test.permission;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.targets.SeamRoleInstanceTargetSetup;
import net.wohlfart.authorization.targets.SeamUserInstanceTargetSetup;
import net.wohlfart.jbpm4.node.GroupActionSelectConfig;
import net.wohlfart.jbpm4.node.GroupPermissionSelectConfig;
import net.wohlfart.jbpm4.node.UserActionSelectConfig;
import net.wohlfart.jbpm4.node.UserPermissionSelectConfig;

/**
 * helper for testing permission queries
 * 
 * @author michael
 *
 */
public class CharmsPermissionQuery implements Serializable {
    
    
    public final static String findTargetGroupsWithPermission =
        " select distinct tr " // this is the target of the permission not the recipient!
        + " from "
        
        + CharmsRole.class.getSimpleName() + " tr "       // targetRole 
         + " left join tr.container tc "                  // targteContainer
        + " , "
        
        + CharmsPermission.class.getSimpleName() + " p "  // the permission links the recipient to the target
        + " , "
        
        + CharmsUser.class.getSimpleName() + " ru "       // recipientUser  
        + " left join ru.memberships rm "                 // recipientMembership
        + " left join rm.charmsRole.contained rc "        // recipientContainer
        
        
        // the permission is for a user and the name matches
        + " where "
        
        
        // --- --- link to the target, might be user, user as member of a group or subgroup... --- ---
        
        
        // target links directly to the selected role..
        + "( "
        + " ( p.targetId = tr.id "
        // the target is a user instance
        + " and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' )"

        + " or "          
        // target link to a container group which contains the user
        + "   ( p.targetId = tc.id "
        // the target is a role instance
        + " and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' ) "
        + ")"

        
        
        // --- --- link to the recipient --- ---
        // the recipient might get the permission indirect via a group...
        
        + " and  ( "
        
        // the recipient got the permission directly
        + "  ( ( p.discriminator = '" + CharmsPermission.USER + "' ) and ( p.recipient = :recipient ) )  " 
        
        + "    or "
        // a group of which the recipient is a member of got the permission
        + " ( ( rc.name = p.recipient or rm.charmsRole.name = p.recipient )"              
        + " and ( p.discriminator = '" + CharmsPermission.ROLE + "') "
        + " and ( ru.name = :recipient) )"
       
        
        + ")"
                      
        
        // --- --- link the action --- ---
        // receiver 
        // the action matches
        + " and (  ( p.action like concat('%,', :action, ',%') )" 
        + "    or  ( p.action like concat('%,', :action) )"
        + "    or  ( p.action like concat(:action, ',%') )" 
        + "    or  ( p.action like :action )  )"  ;
        
    
    
    public final static String findTargetUsersWithPermission = 
        // u is the target of the permission not the recipient of the permission
        " select distinct tu " 
        + " from "
        
        // the user matched with all container roles for that user:
        + CharmsUser.class.getSimpleName() + " tu "        // targetUser  
        + " left join tu.memberships tm "                  // targetMembership
        + " left join tm.charmsRole.container tc, "        // targteContainer

        + CharmsPermission.class.getSimpleName() + " p, "  // the permission links the recipient to the target
        
        + CharmsUser.class.getSimpleName() + " ru "        // recipientUser  
        + " left join ru.memberships rm "                  // recipientMembership
        + " left join rm.charmsRole.contained rc "         // recipientContainer               

        + " where "
                
        // --- --- link to the target, might be user, user as member of a group or subgroup... --- ---       
        // permission target links directly to the target user..
        + "( "
        + " ( p.targetId = tu.id "
        // the target is a user instance
        + " and p.target = '" + SeamUserInstanceTargetSetup.TARGET_STRING + "' )"
        
        + " or "
        // the permission links to a group of which the target user is a direct member of
        + "   ( p.targetId = tm.charmsRole.id "
        // the target is a role instance
        + " and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' ) "

        + " or "          
        // target link to a container group which contains one of the users member groups
        + "   ( p.targetId = tc.id "
        // the target is a role instance
        + " and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' ) "
        + ")"
        
        
        // --- --- link to the recipient might get the permission indirect via a group... --- ---     
        + " and  ( "       
        // the recipient got the permission directly
        + "  ( ( p.discriminator = '" + CharmsPermission.USER + "' ) and ( p.recipient = :recipient ) )  "        
        + "    or "
        // a group of which the recipient is a member of got the permission
        + " ( ( rc.name = p.recipient or rm.charmsRole.name = p.recipient )"              
        + " and ( p.discriminator = '" + CharmsPermission.ROLE + "') )"           
        + ")"
        
              
        // --- --- link the action --- ---
        // the action matches
        + " and (  ( p.action like concat('%,', :action, ',%') )" 
        + "    or  ( p.action like concat('%,', :action) )"
        + "    or  ( p.action like concat(:action, ',%') )" 
        + "    or  ( p.action like :action )  )"                ;
    
    
    
    
    public final static String findRecipientUsersWithPermission =
        " select distinct u "
        + " from "
        
        + CharmsUser.class.getSimpleName() + " u "
        + " left join u.memberships m "
        + " left join m.charmsRole.contained r2 "
        + ","
        
        + CharmsPermission.class.getSimpleName() + " p "
        + " where "

        // -- -- link the recipient --- ---
        // the permission is for a contained role and the name matches
        //  or for a membership role and the name matches
        + " ( ( ( r2.name = p.recipient or m.charmsRole.name = p.recipient )"              
        + " and p.discriminator = '" + CharmsPermission.ROLE + "' )"
        + " or "
        // the permission is for a user and the name matches
        + "   ( ( u.name = p.recipient ) "
        + " and p.discriminator = '" + CharmsPermission.USER + "' ) )"
        
        // --- --- link the target --- ---
        // the target matches
        + " and p.target = :target"
        
        // --- --- link the action --- ---
        // the action matches
        + " and (  ( p.action like concat('%,', :action, ',%') )" 
        + "    or  ( p.action like concat('%,', :action) )"
        + "    or  ( p.action like concat(:action, ',%') )" 
        + "    or  ( p.action like :action )  )"   ;
    
    
    public final static String findRecipientGroupsWithPermission =
        " select distinct r1 "
        + " from "
        
        + CharmsRole.class.getSimpleName() + " r1 "
        + " left join r1.contained r2 "
        + ","
        
        + CharmsPermission.class.getSimpleName() + " p "
        + " where "

        + "  ( r2.name = p.recipient or r1.name = p.recipient )"              
        + " and p.discriminator = '" + CharmsPermission.ROLE + "' "
        
        + " and p.target = :target"
        + " and (  ( p.action like concat('%,', :action, ',%') )" 
        + "    or  ( p.action like concat('%,', :action) )"
        + "    or  ( p.action like concat(:action, ',%') )" 
        + "    or  ( p.action like :action )  )"   ;             
        
    
    
    
    @SuppressWarnings("unchecked")
    public static List<CharmsRole> findRecipientGroupsWithPermission(
            Session hibernateSession, 
            String targetString, 
            String action) {
        /*
        List<CharmsRole> roles = hibernateSession.createQuery(findRecipientGroupsWithPermission)
        .setParameter("target", targetString)
        .setParameter("action", action)        
        .list();
        */
               
        GroupPermissionSelectConfig groupPermissionSelectConfig = new GroupPermissionSelectConfig();
        groupPermissionSelectConfig.setPermissionAction(action);
        groupPermissionSelectConfig.setPermissionTarget(targetString);
        String expression = groupPermissionSelectConfig.getQlExpression(null, null);
        List<CharmsRole> roles = hibernateSession.createQuery(expression).list();
        return roles;  
    }
    
    
    @SuppressWarnings("unchecked")
    public static List<CharmsUser> findRecipientUsersWithPermission(
            Session hibernateSession, 
            String targetString, 
            String action) {
        /*
        List<CharmsUser> users = hibernateSession.createQuery(findRecipientUsersWithPermission)
        .setParameter("target", targetString)
        .setParameter("action", action)        
        .list();
        */
        
        UserPermissionSelectConfig userPermissionSelectConfig = new UserPermissionSelectConfig();
        userPermissionSelectConfig.setPermissionAction(action);
        userPermissionSelectConfig.setPermissionTarget(targetString);
        String expression = userPermissionSelectConfig.getQlExpression(null, null);
        List<CharmsUser> users = hibernateSession.createQuery(expression).list();
        return users;  
    }


    /**
     * we want all target users of any permission with the specified action string here
     * 
     * @param hibernateSession
     * @param recipient  the owner of the permission, direct or indirect
     * @param action  the action defined in the permission for the recipient to perform on the user we are looking for
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<CharmsUser> findTargetUsersWithPermission(
            Session hibernateSession, 
            CharmsUser recipient,  // 
            String action) {
        /*
        List<CharmsUser> users = hibernateSession.createQuery(findTargetUsersWithPermission)
        .setParameter("recipient", recipient.getName())
        .setParameter("action", action)        
        .list();        
        */
        
        UserActionSelectConfig userActionSelectConfig = new UserActionSelectConfig();
        userActionSelectConfig.setAction(action);
        String expression = userActionSelectConfig.getQlExpression(null, recipient);
        List<CharmsUser> users = hibernateSession.createQuery(expression).list();       
        return users;
    }
 
 
    /**
     * we want all target users here...
     * 
     * @param hibernateSession
     * @param recipient
     * @param action
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<CharmsRole> findTargetGroupsWithPermission(
            Session hibernateSession, 
            CharmsUser recipient,  // the owner of the permission, direct or indirect
            String action) {
        
        /*
        List<CharmsRole> roles = hibernateSession.createQuery(findTargetGroupsWithPermission)
        .setParameter("recipient", recipient.getName())
        .setParameter("action", action)        
        .list();
        */
        
        GroupActionSelectConfig groupActionSelectConfig = new GroupActionSelectConfig();
        groupActionSelectConfig.setAction(action);
        String expression = groupActionSelectConfig.getQlExpression(null, recipient);
        List<CharmsRole> roles = hibernateSession.createQuery(expression).list();    
        return roles;
    }

}
