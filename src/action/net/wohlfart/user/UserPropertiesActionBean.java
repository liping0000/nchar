package net.wohlfart.user;

import java.io.Serializable;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.CustomHash;
import net.wohlfart.framework.i18n.CustomLocaleSelector;
import net.wohlfart.framework.theme.CustomThemeSelector;
import net.wohlfart.framework.timezone.CustomTimezoneSelector;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("userPropertiesActionBean")
public class UserPropertiesActionBean extends AbstractActionBean implements Serializable {


    private final static Logger    LOGGER = LoggerFactory.getLogger(UserPropertiesActionBean.class);

    @In(scope = ScopeType.SESSION, value = "authenticatedUser")
    @Out(scope = ScopeType.SESSION, value = "authenticatedUser")
    private CharmsUser             charmsUser;

    @In(value = "hibernateSession")
    private Session                hibernateSession;

    @In(value = "localeSelector")
    private CustomLocaleSelector   localeSelector;

    @In(value = "timezoneSelector")
    private CustomTimezoneSelector timezoneSelector;

    @In(value = "themeSelector")
    private CustomThemeSelector    themeSelector;

    private String                 localeId;
    private String                 themeId;
    private String                 timezoneId;

    private String                 email;

    private String                 oldPasswd;
    private String                 passwd;
    private String                 passwdConfirm;

    @Create
    @Transactional
    public void initialize() {

        LOGGER.debug("hibernateSession.isConnected(): " + hibernateSession.isConnected());
        LOGGER.debug("hibernateSession.isOpen(): " + hibernateSession.isOpen());
        LOGGER.debug("hibernateSession.getTransaction(): " + hibernateSession.getTransaction());
        LOGGER.debug("hibernateSession.getTransaction().isActive(): " + hibernateSession.getTransaction().isActive());

        localeId = charmsUser.getLocaleId();
        themeId = charmsUser.getThemeId();
        timezoneId = charmsUser.getTimezoneId();

        email = charmsUser.getEmail();

        LOGGER.debug("initialized userPropertiesActionBean: " + " " + localeId + " " + themeId + " " + timezoneId);
    }

    @Transactional
    public String save() {
        LOGGER.info("save called");

        // FIXME: this is the way to deal with faces messages, performance wise
        // that is
        final FacesMessages facesMessages = FacesMessages.instance();

        // only persist if all data are correct and valid
        final boolean old = ((oldPasswd != null) && (oldPasswd.trim().length() > 0));
        final boolean new1 = ((passwd != null) && (passwd.trim().length() > 0));
        final boolean new2 = ((passwdConfirm != null) && (passwdConfirm.trim().length() > 0));

        if (old && new1 && new2) {
            LOGGER.debug("we have three new passwds");
            if (CustomHash.instance().checkPassword(oldPasswd, charmsUser)) {
                LOGGER.debug("old passwd check is ok");
                if (passwd.equals(passwdConfirm)) {
                    LOGGER.debug("new paswords are identical");
                    // everything is ok
                    facesMessages.addFromResourceBundle("page.user.properties.changed");
                    charmsUser.setPasswd(CustomHash.instance().generateSaltedHash(passwd, charmsUser.getName()));
                    return merge();

                } else {
                    LOGGER.debug("new paswords are NOT identical");
                    // new passwords dont match
                    facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.properties.confirmWrong", passwdConfirm);
                    return "invalid";
                }
            } else {
                LOGGER.debug("old passwd wrong, checkPassword failed");
                // old passwd wrong
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.properties.oldPasswdWrong", oldPasswd);
                return "invalid";
            }
        } else if (!old && !new1 && !new2) {
            LOGGER.debug("all passwds are empty");
            // no passwd filled out, now changes to do
            // set the view properties
            facesMessages.addFromResourceBundle("page.user.properties.changed");
            return merge();
        } else {
            LOGGER.debug("not all passwords are empty, but nt all passwords are filled out");
            // at least one of the passwds is missing
            if (!old) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.properties.oldPasswdMissing", oldPasswd);
            }
            if (!new1) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.properties.newPasswdMissing", passwd);
            }
            if (!new2) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.properties.confirmPasswdMissing", passwdConfirm);
            }
            return "invalid";
        }
    }

    private String merge() {

        charmsUser.setEmail(email);
        charmsUser.setThemeId(themeId);
        charmsUser.setTimezoneId(timezoneId);
        charmsUser.setLocaleId(localeId);

        // merge the user to the DB, the user is outjected to session
        charmsUser = (CharmsUser) hibernateSession.merge(charmsUser);
        hibernateSession.flush();

        // set the session parameters from the user values
        localeSelector.selectLocaleId(charmsUser.getLocaleId());
        themeSelector.selectThemeId(charmsUser.getThemeId());
        timezoneSelector.selectTimeZoneId(charmsUser.getTimezoneId());

        // ((Session)entityManager.getDelegate()).flush();
        return "saved";
    }

    @BypassInterceptors
    public String getLocaleId() {
        LOGGER.debug("getLocaleId called: " + oldPasswd);
        return localeId;
    }

    @BypassInterceptors
    public void setLocaleId(final String localeId) {
        LOGGER.debug("setLocaleId called: " + oldPasswd);
        this.localeId = localeId;
    }

    @BypassInterceptors
    public String getThemeId() {
        LOGGER.debug("getThemeId called: " + oldPasswd);
        return themeId;
    }

    @BypassInterceptors
    public void setThemeId(final String themeId) {
        LOGGER.debug("setThemeId called: " + oldPasswd);
        this.themeId = themeId;
    }

    @BypassInterceptors
    public String getTimezoneId() {
        LOGGER.debug("getTimezoneId called: " + oldPasswd);
        return timezoneId;
    }

    @BypassInterceptors
    public void setTimezoneId(final String timezoneId) {
        LOGGER.debug("setTimezoneId called: " + oldPasswd);
        this.timezoneId = timezoneId;
    }

    @BypassInterceptors
    public String getEmail() {
        LOGGER.debug("getEmail called: " + oldPasswd);
        return email;
    }

    @BypassInterceptors
    public void setEmail(final String email) {
        LOGGER.debug("setEmail called: " + email);
        this.email = email;
    }

    @BypassInterceptors
    public String getOldPasswd() {
        LOGGER.debug("getOldPasswd called: " + email);
        return oldPasswd;
    }

    @BypassInterceptors
    public void setOldPasswd(final String oldPasswd) {
        LOGGER.debug("setOldPasswd called: " + oldPasswd);
        this.oldPasswd = oldPasswd;
    }

    @BypassInterceptors
    public String getPasswd() {
        LOGGER.debug("getPasswd called: " + oldPasswd);
        return passwd;
    }

    @BypassInterceptors
    public void setPasswd(final String passwd) {
        LOGGER.debug("setOldPasswd called: " + oldPasswd);
        this.passwd = passwd;
    }

    @BypassInterceptors
    public String getPasswdConfirm() {
        LOGGER.debug("getPasswdConfirm: " + oldPasswd);
        return passwdConfirm;
    }

    @BypassInterceptors
    public void setPasswdConfirm(final String passwdConfirm) {
        LOGGER.debug("setPasswdConfirm: " + oldPasswd);
        this.passwdConfirm = passwdConfirm;
    }

    // ------------------ notes

    /*
     * 
     * this is one way to retrieve the user data but since the user is in
     * session scope anyways we don't need this:
     * 
     * Principal principal = identity.getPrincipal();
     * 
     * // get the charms user from the DB CharmsUser charmsUser = (CharmsUser)
     * entityManager.createNamedQuery(CharmsUser.FIND_BY_NAME)
     * .setParameter("name", principal.getName()) .getSingleResult();
     * 
     * 
     * 
     * Here is the exact semantic of merge():
     * 
     * if there is a managed instance with the same identifier currently
     * associated with the persistence context, copy the state of the given
     * object onto the managed instance
     * 
     * if there is no managed instance currently associated with the persistence
     * context, try to load it from the database, or create a new managed
     * instance the managed instance is returned
     * 
     * the given instance does not become associated with the persistence
     * context, it remains detached and is usually discarded
     */

    /**
     * the faces trace API uses the toString Method to display some information
     * about the components in the UI we need to make sure Seam's Bijection
     * doesn't kick in and gives us an exception
     */
    @Override
    @BypassInterceptors
    public String toString() {
        return super.toString();
    }

}
