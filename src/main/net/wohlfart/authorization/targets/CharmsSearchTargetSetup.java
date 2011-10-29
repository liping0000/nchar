package net.wohlfart.authorization.targets;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

/**
 * permission and actions to work with search, there are 3 levels of
 * search access
 * 
 * 
 * 
 * @author Michael Wohlfart
 */
public class CharmsSearchTargetSetup implements IPermissionTargetFactory {

    public static final String TARGET_STRING = "charms.search";

    public static final String ALL = "all";
    public static final String PARTICIPATED = "participated";
    public static final String SUBMITTED = "submitted";

    private static final String[] ALL_ACTIONS 
    = new String[] { 
        SUBMITTED,     // only search changerequests submitted by the user
        PARTICIPATED,  // only search changerequests participated by the user
        ALL            // search all changerequests
    };

    private final CharmsPermissionTarget charmsPermissionTarget = new CharmsPermissionTarget();

    public CharmsSearchTargetSetup() {
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

}
