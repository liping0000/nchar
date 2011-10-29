package net.wohlfart.framework.properties;


public enum CharmsPropertyType {
    STRING("propertytype.string"), 
    DATE("propertytype.date"),
    BOOLEAN("propertytype.boolean"),
    INTEGER("propertytype.integer"), 
    MEMENTO("propertytype.memento");
 
    /** the message code must be configured in some message properties */
    private final String msgCode;

    private CharmsPropertyType(final String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgCode() {
        return msgCode;
    }
}
