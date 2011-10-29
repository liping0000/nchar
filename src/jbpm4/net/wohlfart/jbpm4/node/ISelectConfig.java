package net.wohlfart.jbpm4.node;

import org.jbpm.api.task.Participation;

import net.wohlfart.authentication.entities.CharmsUser;


public interface ISelectConfig {
 
    
    /**
     * workflow configured role/group names, all users in the specified group
     * or in a parent group of the specified group(s) will be selectable
     */
    public static final String GROUP_NAMES = "groupNames";
    /**
     * all users with the specified permission (with the permission to perform
     * the specified action on the specified target) will be listed in the select
     * plus all users which belong to a group or a parent group with the
     * specified permission...
     */
    public static final String PERMISSION_TARGET = "permissionTarget";
    public static final String PERMISSION_ACTION = "permissionAction";
    /**
     * the action the current user must have on a target, all targets
     * will show up in the select list, groups will be resolved to users
     * etc... targets are groups or users, the receiver is a user or a group
     */
    public static final String ACTION = "action";  
    /**
     * participationRole can be "owner" or "candidate" see
     * Participation.CANDIDATE, Participation.OWNER for the string
     * 
     * if a group is owner each group member gets its own spawn
     */
    public static final String PARTICIPATION_ROLE = "participationRole";

//    @Deprecated
//    String getQlExpression(String baseEjbqlExpression, String fragment);

    
    // FIXME:
    // we need to refactor this, the user is not needed in all  implementations
    // the hql queryies might have sql-injection problems!
    String getQlExpression(String fragment, CharmsUser charmsUser);

    /**
     * 
     * @return
     */
    String getParticipationRole();

}
