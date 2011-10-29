package net.wohlfart.authorization.targets;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

/**
 * devel permission
 * 
 * 
 * @author Michael Wohlfart
 */
public class CharmsDevelTargetSetup implements IPermissionTargetFactory {

    public static final String TARGET_STRING = "charms.devel";

    private static final String[] ALL_ACTIONS 
    = new String[] { 
        "all" 
    };

    private final CharmsPermissionTarget charmsPermissionTarget = new CharmsPermissionTarget();

    public CharmsDevelTargetSetup() {
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
