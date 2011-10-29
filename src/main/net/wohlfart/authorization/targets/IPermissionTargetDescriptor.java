package net.wohlfart.authorization.targets;

import java.io.Serializable;

/**
 * 
 * 
 * 
 * @author Michael Wohlfart
 */
public interface IPermissionTargetDescriptor extends Serializable {

    // common actions
    public static final String READ_ACTION   = "read";
    public static final String WRITE_ACTION  = "write";
    public static final String CREATE_ACTION = "create";
    public static final String DELETE_ACTION = "delete";
    public static final String UPDATE_ACTION = "update";

    // the string used as target in the permission
    public String getTargetString();

    // the id of the target object
    //public Long getTargetId();

    // the actions for the permission
    public String[] getAllActions();

    // user friendly description of the target and the permissions
    public String getDescription();

}
