package net.wohlfart.authorization.targets;

import java.io.Serializable;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;


public interface IPermissionTargetFactory extends Serializable {
    
    // collect all implementing classes here
    public static String[] permissionTargetFactories = {
        CharmsAdminTargetSetup.class.getName(),
        CharmsChartTargetSetup.class.getName(),
        CharmsDevelTargetSetup.class.getName(),
        CharmsReportTargetSetup.class.getName(),
        CharmsSearchTargetSetup.class.getName(),
        CharmsWorkflowTargetSetup.class.getName(),
        ProductInstanceTargetSetup.class.getName(),
        SeamRoleInstanceTargetSetup.class.getName(),
        SeamRoleTargetSetup.class.getName(),
        SeamUserInstanceTargetSetup.class.getName(),
        SeamUserTargetSetup.class.getName()
    };
     
    CharmsPermissionTarget getPermissionTarget();

}
