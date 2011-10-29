package net.wohlfart.framework.properties;

import java.io.Serializable;


public class CharmsPropertyItem implements Serializable {

    private String name;
    private CharmsPropertyType type;
    private String value;
    
    
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    
    public void setType(CharmsPropertyType type) {
        this.type = type;
    }
    public CharmsPropertyType getType() {
        return type;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    
}
