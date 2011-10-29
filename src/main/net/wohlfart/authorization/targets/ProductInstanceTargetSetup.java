package net.wohlfart.authorization.targets;

import org.jboss.seam.security.permission.PermissionManager;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

/**
 * the permission target object user and all its actions,
 * note this is for a sigle user only
 * 
 * @author Michael Wohlfart
 */
public class ProductInstanceTargetSetup implements IPermissionTargetFactory {

    public static final String TARGET_STRING = "changerequest.product.instance";

    // common actions
    public static final String READ_ACTION = "read";
    public static final String WRITE_ACTION = "write";
    public static final String CREATE_ACTION = "create";
    public static final String DELETE_ACTION = "delete";
    public static final String UPDATE_ACTION = "update";

    private static final String[] ALL_ACTIONS = new String[] { 
        READ_ACTION, 
        WRITE_ACTION, 
        CREATE_ACTION, 
        DELETE_ACTION, 
        UPDATE_ACTION
    };

    private final CharmsPermissionTarget charmsPermissionTarget   = new CharmsPermissionTarget();

    public ProductInstanceTargetSetup() {
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
     * "Mit diesen Einstellungen können Sie die Rechte für Aktionen einstellen die sich auf die Benutzer in Charms beziehen."
     * + "<br />" +
     * "Über direkte Zuweisung an einen Benutzer oder indirekte Zuweisung über eine Rolle können Sie die folgenden Aktionen erlauben:."
     * + "<ul>" +
     * "<li><b>update</b> erlaubt das Ändern existierender Benutzer</li>" +
     * "<li><b>delete</b> erlaubt das Löschen von Benutzern</li>" +
     * "<li><b>write</b> erlaubt das Schreiben von Benutzern (entspricht update)</li>"
     * +
     * "<li><b>seam.read-permissions</b> erlaubt die Rechte von Benutzern zu lesen</li>"
     * + "<li><b>read</b> erlaubt das Lesen von Benutzern</li>" +
     * "<li><b>seam.grant-permission</b> erlaubt Rechte an Benutzer zu vergeben</li>"
     * + "<li><b>create</b> erlaubt das Anlegen von Benutzer</li>" +
     * "<li><b>seam.revoke-permission</b> erlaubt Benutzern Rechte zu entziehen</li>"
     * + "</ul>" +
     * "Entsprechend den erlaubten Aktionen werden Teile der Benutzungsoberfläche (Menüpunkte, Buttons) ein- oder ausgeblendet."
     * + "</p>" ;
     * 
     * }
     */

}
