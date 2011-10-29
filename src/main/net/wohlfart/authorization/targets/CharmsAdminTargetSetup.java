package net.wohlfart.authorization.targets;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

/**
 * This class implements some admin permissions like the permission and action
 * to view the admin menu, to edit workflow or reference datat...
 * 
 * @author Michael Wohlfart
 */
public class CharmsAdminTargetSetup implements IPermissionTargetFactory {

    public static final String TARGET_STRING = "charms.admin";

    private static final String[] ALL_ACTIONS 
    = new String[] { 
        "view",          // see the adminmenu
        "wfl-edit",      // edit a workflow definition
        "refdata-edit"   // edit the refdata
    };

    private final CharmsPermissionTarget charmsPermissionTarget = new CharmsPermissionTarget();

    public CharmsAdminTargetSetup() {
        charmsPermissionTarget.setTargetString(TARGET_STRING);
        for (final String actionName : ALL_ACTIONS) {
            final CharmsTargetAction action = new CharmsTargetAction();
            action.setName(actionName);
            charmsPermissionTarget.addAction(action);
        }
    }

    public CharmsPermissionTarget getPermissionTarget() {
        return charmsPermissionTarget;
    }

    /*
     * 
     * @Override public String getTargetString() { return TARGET_STRING; }
     * 
     * @Override public String[] getAllActions() { return ALL_ACTIONS; }
     * 
     * @Override public String getDescription() { return "" + "<p>" +
     * "Mit diesem Recht wird die Sichtbarkeit des Admin Menüs geregelt." +
     * "<br />" + "</p>" ; }
     */

}
