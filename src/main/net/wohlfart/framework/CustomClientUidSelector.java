package net.wohlfart.framework;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.ui.ClientUidSelector;
import org.jboss.seam.util.RandomStringUtils;

/**
 * uuid for the s:token tag
 * 
 * @author Michael Wohlfart
 * 
 */

@Name("org.jboss.seam.ui.clientUidSelector")
@Install(precedence = Install.DEPLOYMENT)
public class CustomClientUidSelector extends ClientUidSelector {

    private static final long serialVersionUID = -4923235748771706010L;
    private String            clientUid;

    @Override
    @Create
    public void onCreate() {
        setCookiePath(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath());
        setCookieMaxAge(-1);
        setCookieEnabled(true);
        clientUid = getCookieValue();
    }

    @Override
    public void seed() {
        if (!isSet()) {

            clientUid = RandomStringUtils.random(50, true, true); // Fixed
            setCookieValueIfEnabled(clientUid);
        }
    }

    @Override
    public boolean isSet() {
        return clientUid != null;
    }

    @Override
    public String getClientUid() {
        return clientUid;
    }

    @Override
    protected String getCookieName() {
        return "javax.faces.ClientToken";
    }

}
