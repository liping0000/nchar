package net.wohlfart.jbpm4;

import net.wohlfart.authentication.entities.CharmsUser;

import org.jboss.seam.contexts.Contexts;
import org.jbpm.pvm.internal.svc.IdentityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * we extend the jbpm4 identity service with some extra methods this class is
 * configured in the jbpm4.cfg.xml file to override <identity-service />
 * 
 * @author Michael Wohlfart
 * 
 */
public class CustomIdentityService extends IdentityServiceImpl {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomIdentityService.class);

    /**
     * use this in the environment config:
     * 
     * <object name='authenticatedUser'
     * class='net.wohlfart.jbpm4.CustomIdentityService'
     * method='findAuthenticatedUser' />
     * 
     * it is used to return the authenticated user from the session
     */
    public static CharmsUser findAuthenticatedUser() {
        final CharmsUser authenticatedUser = (CharmsUser) Contexts.getSessionContext().get("authenticatedUser");
        LOGGER.debug("resolving authenticated user {}", authenticatedUser);
        return authenticatedUser;
    }

}
