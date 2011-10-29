package net.wohlfart.authentication;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * The jboss Identity instance implements various methods for checking roles,
 * permission and authorization, the real user data are stored as POJO in the
 * User object or in the authenticatedUser session object,
 * 
 * we don't want any domain data in this object besides the username, this
 * object is only for the identity framework for resolving permissions, roles
 * and authorization stuff not for the business logic
 * 
 * for business stuff there is the authenticatedUser object in the session which
 * is actually a CharmsUser with all the properties there
 * 
 * useful links:
 * http://odyssi.blogspot.com/2008/01/intro-to-jboss-seam-security-part-1.html
 * http://shane.bryzak.com/blog/articles/seam_security_gets_an_upgrade
 * 
 * about extending Identity:
 * http://docs.jboss.com/seam/2.0.1.GA/reference/en/html/security.html#d0e8142
 * 
 * This implements a subject in JAAS terms a subject can be a user or another
 * application or part of code or a user group some other important members
 * fields are:
 * 
 * 
 * protected static boolean securityEnabled = true;
 * 
 * public static final String ROLES_GROUP = "Roles";
 * 
 * private Credentials credentials; contains username/password
 * 
 * private MethodExpression authenticateMethod; authenticating user, fallback is
 * the IdentityManagers.authenticate
 * 
 * 
 * private Principal principal; private Subject subject; private RememberMe
 * rememberMe;
 * 
 * private transient ThreadLocal<Boolean> systemOp; system operation bypassing
 * security checks
 * 
 * private String jaasConfigName = null;
 * 
 * private PermissionMapper permissionMapper;
 * 
 * @author Michael Wohlfart
 * 
 */

// TODO: check how this fits in with Charms User maybe we can reduce usage of
// one during a session

@Startup
@Name("org.jboss.seam.security.identity")
@Install(precedence = Install.APPLICATION)
@BypassInterceptors
@Scope(ScopeType.SESSION)
public class CharmsIdentity extends Identity {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsIdentity.class);

    // private static final String ADMIN_IP = "127.0.0.1";

    // additional methods not supported by seam but used in our app
    // public static final String EVENT_POST_LOGIN =
    // "net.wohlfart.authentication.CharmsIdentity.EventPostLogin";

    // this is needed because listening to the EVENT_LOGGED_OUT event doesn't
    // give us a
    // identity...
    public static final String  EVENT_PRE_LOGOUT = "net.wohlfart.authentication.CharmsIdentity.EventPreLogout";

    public CharmsIdentity() {
        super();
    }

    @Override
    public boolean isLoggedIn() {
        LOGGER.debug("is logged in called");
        return super.isLoggedIn();
    }

    // FIXME: what does this method that authenticate doesn't ?
    @Override
    public String login() {
        final Object requestObject = FacesContext.getCurrentInstance().getExternalContext().getRequest();

        if (!(requestObject instanceof HttpServletRequest)) {
            LOGGER.warn("Request object is not of type HttpServletRequest, login will fail, request type is: {}", requestObject.getClass().getName());
            return null; // indicates a failed login
        }

        final HttpServletRequest httpServletRequest = (HttpServletRequest) requestObject;
        final String remoteAddr = httpServletRequest.getRemoteAddr();
        LOGGER.debug("login from: {} calling parent to perform login...", remoteAddr);

        // // do a automatic login if the user comes from the admin host
        // if (ADMIN_IP.equals(remoteAddr)) {
        // // TODO
        // //authenticate(LoginContext loginContext);
        // }

        final String result = super.login(); // result: "loggedIn"

        // if ("loggedIn".equals(result)) {
        // LOGGER.debug("raising event: " + EVENT_POST_LOGIN);
        // Events.instance().raiseEvent(EVENT_POST_LOGIN);
        // }

        LOGGER.info("... login return from parent, result is: {}", result);
        return result;
    }

    @Override
    protected void preAuthenticate() {
        super.preAuthenticate();
    };

    @Override
    protected void postAuthenticate() {
        super.postAuthenticate();
    };

    @Override
    public void logout() {
        LOGGER.info("logout called");
        Events.instance().raiseEvent(EVENT_PRE_LOGOUT);
        super.logout(); // this deletes our current session leading to some ajax
                        // problems on the client side
    }

    // -----------------------------------------------------------------------


    // this is the #{s:hasPermission(target, string)} entry point
    // defined with a jsf function, see:
    // http://docs.jboss.org/seam/1.1.5.GA/reference/en/html/security.html
    // FIXME: we could implement a permission cache at this level
    // only caching none rules permissions...
    @Override
    public boolean hasPermission(final Object target, final String action) {
        boolean result = false;
        try {
            result = super.hasPermission(target, action);
            LOGGER.info("hasPermission returned for {} with target: {}, action: {}, resolved to: {}", new Object[] { this, target, action, result });

        } catch (final Exception ex) {
            result = false;
        }
        return result;
    }

    /*
     * public boolean hasAnyPermission(Object target) { super.get }
     */

    public boolean hasAnyPermission(final Object target, final String actions) {
        boolean result = false;
        try {
            final String[] actionSet = StringUtils.split(actions, ',');
            for (final String action : actionSet) {
                result = super.hasPermission(target, action) || result;
                if (result) {
                    break;
                }
            }
            LOGGER.info("hasAnyPermission returned for {} with target: {}, actions: {}, resolved to: {}", new Object[] { this, target, actions, result });

        } catch (final Exception ex) {
            LOGGER.warn("can't resolve permission ", ex);
            result = false;
        }
        return result;
    }

    /**
     * This is the entry point for the <s:hasRole(role) /> seam UI component, we
     * try to stay with permissions and avoid role based security whenever
     * possible, this method checks if the user is a member of the specified
     * role. The roles are added during the login process in the
     * UserSessionInitializer.authenticate() method
     */
    @Override
    public boolean hasRole(final String role) {
        final boolean result = super.hasRole(role);
        LOGGER.info("hasRole returned for {} with: {}, resolved to:  {}", new Object[] { this, role, result });
        return result;
    }

    // -----------------------------------------------------------------------

    @Override
    @Deprecated
    public boolean hasPermission(final String name, final String action, final Object... arg) {
        final Boolean result = super.hasPermission(name, action, arg);
        LOGGER.warn("Deprecated hasPermission returned for {} with name {}, action: {}, resolved to: {}", new Object[] { this, name, action, result });
        return result;
    }

    /**
     * nice toString method for logging
     */
    @Override
    public String toString() {
        final Credentials credentials = getCredentials();
        return "[" + ((credentials == null) ? "no credentials" : credentials.getUsername()) + "] " + this.getClass().getName() + "@" + hashCode();
    }
}
