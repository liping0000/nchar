package net.wohlfart.framework;

import java.io.Serializable;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIInput;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

// see: http://www.jsftutorials.net/components/step5.html

@Name("attachmentGridBackingBean")
@Scope(ScopeType.SESSION)
@AutoCreate
public class AttachmentGridBackingBean implements Serializable {

    static final long serialVersionUID = -1L;

    public transient UIInput uIInput          = null;

    public void setInput(final UIInput uIInput) {
        //System.err.println("setting: " + uIInput);
        this.uIInput = uIInput;
    }

    public UIInput getInput() {
        //System.err.println("getting: " + uIInput);
        return uIInput;
    }

    public transient UIComponentBase uIComponentBase = null;

    public void setTable(final UIComponentBase uIComponentBase) {
        //System.err.println("setting uIComponentBase: " + uIComponentBase);
        this.uIComponentBase = uIComponentBase;
    }

    public UIComponentBase getTable() {
        //System.err.println("getting uIComponentBase: " + uIComponentBase);
        return uIComponentBase;
    }

}
