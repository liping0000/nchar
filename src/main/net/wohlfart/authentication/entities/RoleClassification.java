package net.wohlfart.authentication.entities;

/**
 * A simple enumeration for gender values for a CharmsUser property used with
 * 
 * @Enumerated(EnumType.STRING) to store the enum as string values in hibernate
 * 
 * @author Michael Wohlfart
 */
public enum RoleClassification {
    AUTHORIZATIONAL("role.classification.authorizational"), 
    LOCATIONAL("role.classification.locational"),
    ORGANISATIONAL("role.classification.organisational");
    
    // used as default classification for creating new roles
    public final static RoleClassification DEFAULT = RoleClassification.ORGANISATIONAL;
    
    // the classification in the listshuttle
    public final static RoleClassification SHUTTLE = RoleClassification.AUTHORIZATIONAL;
    

    /** the message code must be configured in some message properties */
    private final String msgCode;

    private RoleClassification(final String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgCode() {
        return msgCode;
    }
}
