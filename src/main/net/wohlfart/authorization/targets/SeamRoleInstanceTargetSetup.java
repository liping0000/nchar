package net.wohlfart.authorization.targets;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

import org.jboss.seam.security.permission.PermissionManager;

/**
 * the permission target object role/group and all its action,
 * note this is for a single group only
 * 
 * @author Michael Wohlfart
 */
public class SeamRoleInstanceTargetSetup implements IPermissionTargetFactory {

    public static final String TARGET_STRING = "seam.role.instance";

    // common actions
    public static final String READ_ACTION = "read";
    public static final String WRITE_ACTION = "write";
    public static final String CREATE_ACTION = "create";
    public static final String DELETE_ACTION = "delete";
    public static final String UPDATE_ACTION = "update";

    protected static final String READ_PERMISSION_ACTION   = PermissionManager.PERMISSION_READ;
    protected static final String GRANT_PERMISSION_ACTION  = PermissionManager.PERMISSION_GRANT;
    protected static final String REVOKE_PERMISSION_ACTION = PermissionManager.PERMISSION_REVOKE;

    private static final String[] ALL_ACTIONS 
    = new String[] { 
        READ_ACTION, 
        WRITE_ACTION, 
        CREATE_ACTION, 
        DELETE_ACTION, 
        UPDATE_ACTION,
        GRANT_PERMISSION_ACTION, 
        READ_PERMISSION_ACTION, 
        REVOKE_PERMISSION_ACTION 
    };

    private final CharmsPermissionTarget charmsPermissionTarget   = new CharmsPermissionTarget();

    public SeamRoleInstanceTargetSetup() {
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
     * @Override public String getTargetString() { return TARGET_STRING; }
     * 
     * @Override public String[] getAllActions() { return ALL_ACTIONS; }
     * 
     * @Override public String getDescription() { return "" + "<p>" +
     * "Mit diesem Recht können Sie die Aktionen einstellen die sich auf die Rollen in Charms beziehen."
     * + "<br />" +
     * "Über direkte Zuweisung an einen Benutzer oder indirekte Zuweisung über eine Rolle können Sie die folgenden Aktionen erlauben:."
     * + "<ul>" +
     * "<li><b>update</b> erlaubt das Ändern existierender Rollen</li>" +
     * "<li><b>delete</b> erlaubt das Löschen von Rollen</li>" +
     * "<li><b>write</b> erlaubt das Schreiben von Rollen (entspricht update)</li>"
     * +
     * "<li><b>seam.read-permissions</b> erlaubt die Rechte von Rollen zu lesen</li>"
     * + "<li><b>read</b> erlaubt das Lesen von Rollen</li>" +
     * "<li><b>seam.grant-permission</b> erlaubt Rechte an Rollen zu vergeben</li>"
     * + "<li><b>create</b> erlaubt das Anlegen von Rollen</li>" +
     * "<li><b>seam.revoke-permission</b> erlaubt Rollen Rechte zu entziehen</li>"
     * + "</ul>" +
     * "Entsprechend den erlaubten Aktionen werden Teile der Benutzungsoberfläche (Menüpunkte, Buttons) ein- oder ausgeblendet."
     * + "</p>" ; }
     */
}
