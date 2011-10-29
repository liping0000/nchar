package net.wohlfart.authentication;

import java.io.Serializable;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.framework.i18n.CustomLocaleSelector;
import net.wohlfart.framework.logging.CharmsLogEntry;
import net.wohlfart.framework.logging.CharmsLogLevel;
import net.wohlfart.framework.logging.CharmsLogger;
import net.wohlfart.framework.properties.CharmsPropertySet;
import net.wohlfart.framework.properties.PropertiesManager;
import net.wohlfart.framework.theme.CustomThemeSelector;
import net.wohlfart.framework.timezone.CustomTimezoneSelector;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.management.IdentityManager;
import org.jbpm.api.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the actions to setup the session data for a user login an
 * instance of this class is created in the event context and destroyed
 * afterwards
 * 
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
@Name("userSessionInitializer")
@Scope(ScopeType.EVENT)
public class UserSessionInitializer implements Serializable {

    private static final long serialVersionUID = -1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(UserSessionInitializer.class);

    @In(value = "redirect")
    private org.jboss.seam.faces.Redirect redirect;

    @In(value = "identity")
    private CharmsIdentity identity;

    @In(value = "sessionContext")
    private transient Context sessionContext;

    @In(value = "hibernateSession")
    private Session hibernateSession;

    @In(value = "processEngine")
    // spring bean
    protected ProcessEngine processEngine;

    @In(value = CharmsUserIdentityStore.CHARMS_USER_IDENTITY_STORE)
    private CharmsUserIdentityStore charmsUserIdentityStore;

    @In(value = "propertiesManager")
    private PropertiesManager propertiesManager;

    @In(value = "localeSelector")
    private CustomLocaleSelector localeSelector;

    @In(value = "timezoneSelector")
    private CustomTimezoneSelector timezoneSelector;

    @In(value = "themeSelector")
    private CustomThemeSelector themeSelector;

    @Transactional
    public boolean authenticate() {

        final Credentials credentials = identity.getCredentials();
        final String username = credentials.getUsername();
        final String password = credentials.getPassword();

        // this is the fallback auth in case nothing is configured in
        // components.xml
        // it raised the events for user authentication
        final boolean success = charmsUserIdentityStore.authenticate(username, password);

        if (!success) {
            return false;
        }

        // check the redirect to make sure we don't send the user to the login
        // page again
        final String redirectView = redirect.getViewId();
        if (redirectView == null) {
            redirect.setViewId("/pages/user/home.xhtml");
        }

        // get all roles and add them to the identity
        final IdentityManager identityManager = IdentityManager.instance();
        for (final String role : identityManager.getImpliedRoles(username)) {
            identity.addRole(role);
        }

        return true;
    }

    // --- event after the user is authenticated

    @Observer(Identity.EVENT_POST_AUTHENTICATE)
    @Transactional
    public void userLogon() {
        final FacesMessages facesMessages = FacesMessages.instance();

        final Principal principal = identity.getPrincipal();
        if (principal == null) {
            LOGGER.error("No Principal available on post login");
            facesMessages.add(Severity.FATAL, "Internal Error, principal is null in userLogon");
            return;
        }

        // get the charms user from the DB
        final CharmsUser charmsUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", principal.getName())
                .uniqueResult();

        // set some session parameters from the user values
        localeSelector.selectLocaleId(charmsUser.getLocaleId());
        themeSelector.selectThemeId(charmsUser.getThemeId());
        timezoneSelector.selectTimeZoneId(charmsUser.getTimezoneId());

        // custom welcome message, org.jboss.seam.loginSuccessful is nulled in
        // the message bundle
        facesMessages.addFromResourceBundle("net.wohlfart.loginSuccessful", charmsUser.getLabel());
        LOGGER.info("user logged in: " + principal.getName());

        // display last login message
        final CharmsPropertySet properties = propertiesManager.getUserProperties(charmsUser);
        final Date lastLogin = properties.getPropertyAsDate(PropertiesManager.LAST_LOGIN_PROPERTY_NAME, null);
        final DateFormat deafaultDateFormat = properties.getPropertyAsDateFormat(PropertiesManager.DEFAULT_DATE_FORMAT, new SimpleDateFormat("dd.MM.yyyy"));

        // we might need this for some statistic views, however it might be null
        // when
        // the user never logged in before
        sessionContext.set(PropertiesManager.LAST_LOGIN_PROPERTY_NAME, lastLogin);
        sessionContext.set(PropertiesManager.DEFAULT_DATE_FORMAT, deafaultDateFormat);

        if (lastLogin != null) {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(lastLogin);
            facesMessages.addFromResourceBundle(Severity.INFO, "net.wohlfart.lastLogin",
            // the weekeday in the users locale:
                    cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, localeSelector.getLocale()),
                    // the formatted date according to the format string
                    deafaultDateFormat.format(lastLogin));
        } else {
            facesMessages.addFromResourceBundle(Severity.INFO, "net.wohlfart.firstLogin");
        }

        // persist the new logon date in the user properties
        propertiesManager.persistProperty(properties, PropertiesManager.LAST_LOGIN_PROPERTY_NAME, new Date());

        // log the logon event

        // the external context might be a servlet or portlet and depends on the
        // application server
        // and however this application is configured, we try to play save here
        // and check before casting the stuff
        // so we don't risk any exceptions...
        final ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        final Object request = externalContext.getRequest();
        String userAgent = " - unknown - ";
        String remoteAddr = " - unknown - ";
        if (request instanceof HttpServletRequest) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            userAgent = httpServletRequest.getHeader("user-agent");
            remoteAddr = httpServletRequest.getRemoteAddr();
        }

        final Object session = externalContext.getSession(false);
        String sessionId = " - unknown - ";
        if (session instanceof HttpSession) {
            final HttpSession httpSession = (HttpSession) externalContext.getSession(false);
            sessionId = httpSession.getId();
        }

        hibernateSession
                .persist(new CharmsLogEntry(CharmsLogger.AUTH, CharmsLogLevel.INFO, new StringBuffer().append("postLogin Event from ").append(remoteAddr)
                        .append(" principal name: \"").append(principal.getName()).append("\"").append(" fullName is \"").append(charmsUser.getLabel())
                        .append("\"").append(" sessionid:  ").append(sessionId).append(" remoteAddress:  ").append(remoteAddr).toString()));

        hibernateSession.persist(new CharmsLogEntry(CharmsLogger.STATS, CharmsLogLevel.FINE, new StringBuffer().append("Client data from ").append(remoteAddr)
                .append(" user-agent:  ").append(userAgent).toString()));

        hibernateSession.flush();

        // set the user for a jbpm session, this will be stored in a
        // thread local variable,
        // we don't need this atm since we do all the query stuff on our own
        // processEngine.setAuthenticatedUserId(charmsUser.getActorId());
    }

    // --- our custom event before the user logs off

    @Observer(CharmsIdentity.EVENT_PRE_LOGOUT)
    @Transactional
    public void userLogoff() {
        final Principal principal = identity.getPrincipal();
        LOGGER.info("user logged off: {}", principal.getName());

        hibernateSession.persist(new CharmsLogEntry(CharmsLogger.AUTH, CharmsLogLevel.INFO, new StringBuffer().append("preLogout Event for user ")
                .append(principal.getName()).toString()));
        hibernateSession.flush();
    }

}
