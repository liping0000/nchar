package net.wohlfart.framework.i18n;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.ResourceBundle;
import org.jboss.seam.international.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * factory for creating the message maps/esource bundles this returns a
 * resourceBundleMap which is an instance of Map for a certain locale
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.STATELESS)
@BypassInterceptors
@Name("org.jboss.seam.international.messagesFactory")
@Install(precedence = APPLICATION)
public class CustomMessagesFab extends Messages {

    private final static Logger LOGGER            = LoggerFactory.getLogger(CustomMessagesFab.class);

    private transient BundleMap resourceBundleMap = null;

    /**
     * Create the Map and cache it in the EVENT scope. No need to cache it in
     * the SESSION scope, since it is inexpensive to create.
     * 
     * @return a Map that interpolates messages in the Seam ResourceBundle
     */
    @Override
    @Factory(value = "customMessages", autoCreate = true, scope = EVENT)
    public Map<String, String> getMessages() {
        // this method is called once per event (pageload or ajax request)
        LOGGER.debug("getMessages called");
        final java.util.ResourceBundle resourceBundle = ResourceBundle.instance();

        // we need a map adaptor for the page
        if ((resourceBundleMap == null) || !resourceBundleMap.getBundle().equals(resourceBundle)) {
            LOGGER.debug("creating new bundle map");
            resourceBundleMap = new BundleMap(resourceBundle);
        }
        return resourceBundleMap;

    }

}
