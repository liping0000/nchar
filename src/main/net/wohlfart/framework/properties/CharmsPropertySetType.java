package net.wohlfart.framework.properties;

// needed to avoid name collission between application specific properties and
// user defined...
public enum CharmsPropertySetType {
    APPLICATION, 
    TEMPLATE, 
    USER;

    private CharmsPropertySetType() {
    }

}
