package net.wohlfart.authentication;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.util.List;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.CustomHash;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.management.IdentityManagementException;
import org.jboss.seam.security.management.NoSuchUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Identity Store for use with hibernate sessions, this implements the user
 * features for the IdentityManager, it can be replaced by an ldap lookup or
 * something similar without changing the group implementation
 * 
 * FIXME: remove the suppress warning here and parameterize for our custom
 * entities CharmsUser/Membership and CharmsRole 
 * 
 * FIXME: check version from
 * JpaIndetityStore including the new password encryption
 * 
 */
@Name(CharmsUserIdentityStore.CHARMS_USER_IDENTITY_STORE)
@Install(precedence = Install.APPLICATION)
@Scope(APPLICATION)
@BypassInterceptors
@Startup
public class CharmsUserIdentityStore extends AbstractIdentityStore {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsUserIdentityStore.class);
    
    public final static String CHARMS_USER_IDENTITY_STORE = "charmsUserIdentityStore";

    public static final String AUTHENTICATED_USER        = "org.jboss.seam.security.management.authenticatedUser";

    public static final String EVENT_USER_CREATED        = "org.jboss.seam.security.management.userCreated";
    public static final String EVENT_PRE_PERSIST_USER    = "org.jboss.seam.security.management.prePersistUser";
    public static final String EVENT_USER_AUTHENTICATED  = "org.jboss.seam.security.management.userAuthenticated";

    @Create
    public void init() {
        LOGGER.debug("created {}", this.getClass().getName());
        if (featureSet == null) {
            featureSet = new FeatureSet();
            featureSet.addFeature(Feature.changePassword);
            featureSet.addFeature(Feature.createUser);
            featureSet.addFeature(Feature.deleteUser);
            featureSet.addFeature(Feature.enableUser);
        }

        super.initHibernateSession();
    }

    @Override
    public List<String> listUsers() {
        return super.listUsers();
    }

    @Override
    public List<String> listUsers(final String filter) {
        return super.listUsers(filter);
    }

    @Override
    public boolean userExists(final String name) {
        return lookupUser(name) != null;
    }

    @Override
    public boolean isUserEnabled(final String name) {
        final CharmsUser user = lookupUser(name);
        return (user != null) && user.getEnabled();
    }

    /**
     * By default, we'll use the user's username as the password salt return
     * userPrincipalProperty.getValue(user).toString();
     * 
     * @param user
     * @return
     */
    protected String getUserAccountSalt(final CharmsUser user) {
        return user.getName();
    }


    @Override
    public boolean createUser(
            final String username, 
            final String password, 
            final String firstname, 
            final String lastname) {

        try {

            if (userExists(username)) {
                throw new IdentityManagementException("Could not create account, already exists");
            }

            final CharmsUser user = new CharmsUser();

            user.setName(username);
            user.setFirstname(firstname);
            user.setLastname(lastname);

            if (password == null) {
                // user.setPasswd(CharmsUser.LOCKED_PASSWD);
                user.setEnabled(false);
            } else {
                user.setPasswd(CustomHash.instance().generateSaltedHash(password, username));
                user.setEnabled(true);
            }

            // events not used in charms since this breaks testability here
            if (Events.exists()) {
                Events.instance().raiseEvent(EVENT_PRE_PERSIST_USER, user);
            }

            persistEntity(user);
            
            // events not used in charms since this breaks testability here
            if (Events.exists()) {
                Events.instance().raiseEvent(EVENT_USER_CREATED, user);
            }

            return true;

        } catch (final Exception ex) {
            if (ex instanceof IdentityManagementException) {
                throw (IdentityManagementException) ex;
            } else {
                throw new IdentityManagementException("Could not create account", ex);
            }
        }
    }

    @Override
    public boolean createUser(final String username, final String password) {
        return createUser(username, password, null, null);
    }

    @Override
    public boolean deleteUser(final String username) {
        final CharmsUser user = lookupUser(username);
        if (user == null) {
            throw new NoSuchUserException("Could not delete, user '" + username + "' does not exist");
        }
        removeEntity(user);
        return true;
    }

    @Override
    public boolean enableUser(final String name) {
        final CharmsUser user = lookupUser(name);
        if (user == null) {
            throw new NoSuchUserException("Could not enable user, user '" + name + "' does not exist");
        }

        // Can't enable an already-enabled user, return false
        if (user.getEnabled()) {
            return false;
        }

        user.setEnabled(true);
        return true;
    }

    @Override
    public boolean disableUser(final String name) {
        final CharmsUser user = lookupUser(name);
        if (user == null) {
            throw new NoSuchUserException("Could not disable user, user '" + name + "' does not exist");
        }

        // Can't disable an already-disabled user, return false
        if (!user.getEnabled()) {
            return false;
        }

        user.setEnabled(false);
        return true;
    }

    @Override
    public boolean changePassword(
            final String username, 
            final String password) {
        final CharmsUser user = lookupUser(username);
        if (user == null) {
            throw new NoSuchUserException("Could not change password, user '" + username + "' does not exist");
        }

        user.setPasswd(CustomHash.instance().generateSaltedHash(password, username));
        return true;
    }

    @Override
    public boolean authenticate(
            final String username, 
            final String plainPassword) {
        final CharmsUser user = lookupUser(username);

        if ((user == null) || StringUtils.isEmpty(user.getName())) {
            LOGGER.info("user.getName() is empty or user not found while trying to authenticate; " 
                    + " username was: '{}' the user will not be authenticated", username);
            return false;
        }

        if ((user != null) && (!user.getEnabled())) {
            LOGGER.info("User is disabled: {} login rejected ", user.getName());
            return false;
        }

        // check if the password is encrypted, if not encrypt it
        final CustomHash passwordCheck = CustomHash.instance();
        final String password = user.getPasswd();
        if (!passwordCheck.isPasswordEncryptedProperly(password)) {
            LOGGER.info("encrypting users password, username is {}", user.getName());
            final String encryptedPassword = passwordCheck.generateSaltedHash(password, user);
            user.setPasswd(encryptedPassword);
            persistEntity(user);
            // the caller is responsible for doing the flush...
            // NOTE: there is no flush done if the users password doesn't match
        }

        final boolean success = passwordCheck.checkPassword(plainPassword, user.getName(), user.getPasswd());

        if (success && Events.exists()) {
            if (Contexts.isEventContextActive()) {
                // push user into the event context
                Contexts.getEventContext().set(AUTHENTICATED_USER, user);
            }
            Events.instance().raiseEvent(EVENT_USER_AUTHENTICATED, user);
        }
        return success;
    }

    @Observer(Identity.EVENT_POST_AUTHENTICATE)
    public void putAuthenticatedUserIntoSession() {
        if (Contexts.isEventContextActive() && Contexts.isSessionContextActive()) {
            // push user into the session context
            Contexts.getSessionContext().set(AUTHENTICATED_USER, Contexts.getEventContext().get(AUTHENTICATED_USER));
        } else {
            LOGGER.warn("event or session context is not available, can't put user into session context" 
                    + " Contexts.isEventContextActive(): " + Contexts.isEventContextActive() 
                    + " Contexts.isSessionContextActive(): " + Contexts.isSessionContextActive());
        }
    }

}
