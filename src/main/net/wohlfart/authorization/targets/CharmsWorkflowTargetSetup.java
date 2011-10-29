package net.wohlfart.authorization.targets;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

/**
 * enables the view all menu item
 * 
 * @author Michael Wohlfart
 */
public class CharmsWorkflowTargetSetup implements IPermissionTargetFactory {

    public static final String TARGET_STRING = "charms.workflow";

    private static final String[] ALL_ACTIONS = new String[] { 
        "viewAll" 
    };

    private final CharmsPermissionTarget charmsPermissionTarget = new CharmsPermissionTarget();

    public CharmsWorkflowTargetSetup() {
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
     * "Mit diesem Recht können Sie die Aktionen einstellen die sich auf die Charts in Charms beziehen."
     * + "<br />" +
     * "Über direkte Zuweisung an einen Benutzer oder indirekte Zuweisung über eine Rolle können Sie die folgenden Aktionen erlauben:."
     * + "<ul>" +
     * "<li><b>view</b> erlaubt den lesenden Zugriff auf die Chart-Diagramme</li>"
     * + "</ul>" +
     * "Entsprechend den erlaubten Aktionen werden Teile der Benutzungsoberfläche (Menüpunkte, Buttons) ein- oder ausgeblendet."
     * + "</p>" ; }
     */

}
