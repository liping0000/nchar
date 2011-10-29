package net.wohlfart.authentication.entities;

/**
 * A simple enumeration for gender values for a CharmsUser property used with
 * 
 * @Enumerated(EnumType.STRING) to store the enum as string values in hibernate
 * 
 * @author Michael Wohlfart
 */
public enum Gender {
    FEMALE("user.gender.female"), 
    MALE("user.gender.male");

    /** the message code must be configured in some message properties */
    private final String msgCode;

    private Gender(final String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgCode() {
        return msgCode;
    }
}
