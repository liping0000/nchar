package net.wohlfart.authentication;

import static org.jboss.seam.ScopeType.STATELESS;

import java.util.List;

import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authentication.entities.RoleClassification;
import net.wohlfart.authorization.CustomHash;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.entities.CharmsPermissionTarget;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * setup some test accounts on application startup
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(STATELESS)
@Name("accountSetup")
public class AccountSetup {

    private final static Logger LOGGER = LoggerFactory.getLogger(AccountSetup.class);

    private static final int    DEMO_USER_COUNT   = 0;
    private static final int    DEMO_ROLE_COUNT   = 0;

    private static final String ADMIN_LOGIN       = "admin";
    private static final String ADMIN_PASSWD      = "admin";

    private static final String DEVEL_LOGIN       = "devel";
    private static final String DEVEL_PASSWD      = "devel";

    private static final String DEMO_LOGIN        = "demo";
    private static final String DEMO_PASSWD       = "demo";

    private static final String USER_ROLE_STRING  = "user";
    private static final String ADMIN_ROLE_STRING = "admin";
    private static final String DEVEL_ROLE_STRING = "devel";
    private static final String TQM_ROLE_STRING   = "TQM";

    // @In(create=true)
    // EntityManager entityManager;
    @In(value = "hibernateSession")
    private Session             hibernateSession;



    /**
     * setup the devel account to make sure we can log in we don't need to
     * implement any interface for the transaction to work like in spring, the
     * transactional annotation works out of the box in seam, probably as long
     * as there is no @BypassInterceptors involved
     */

    @SuppressWarnings("unchecked")
    @Transactional
    public void startup() {

        // -------------- setup roles
        // make sure the user role exists
        final CharmsRole userRole = deployAuthorizationalRole(USER_ROLE_STRING);
        assert userRole != null : "user role must not be null on startup";

        // make sure the devel role exists
        final CharmsRole develRole = deployAuthorizationalRole(DEVEL_ROLE_STRING);
        assert develRole != null : "devel role must not be null on startup";

        // make sure the admin role exists
        final CharmsRole adminRole = deployAuthorizationalRole(ADMIN_ROLE_STRING);
        assert adminRole != null : "adminRole role must not be null on startup";

        // make sure the admin role exists
        final CharmsRole tqmRole = deployAuthorizationalRole(TQM_ROLE_STRING);
        assert tqmRole != null : "tqmRole role must not be null on startup";

        // flush for good meassure
        // ((Session)entityManager.getDelegate()).flush();
        hibernateSession.flush();

        // --------------- setup permissions

        // recipient = ADMIN_ROLE_STRING
        // target = CharmsChart.TARGET_STRING
        // action = StringUtils.join(CharmsChart.ALL_ACTIONS, ",")

        // get all available targets
        final List<CharmsPermissionTarget> list = hibernateSession.createQuery("from CharmsPermissionTarget").list();

        for (final CharmsPermissionTarget target : list) {
            if (StringUtils.isEmpty(target.getAllActionString())) {
                LOGGER.warn("no actions found for charmsPermission target with target string {}, " + "we don't assing a permission for this target",
                        target.getTargetString());
            } else {
                LOGGER.info("assigning permission to role/user {}, target {}, actions {} ",
                        new Object[] { ADMIN_ROLE_STRING, target.getTargetString(), target.getAllActionString() });
                deployPermissionToRole(ADMIN_ROLE_STRING, target.getTargetString(), target.getAllActionString());
            }
        }
        hibernateSession.flush();

        // ------------- setup accounts

        final CharmsUser develUser = deployUser(DEVEL_LOGIN, DEVEL_PASSWD, develRole, adminRole, tqmRole);
        develUser.setFirstname("Erwin");
        develUser.setLastname("Entwickler");
        hibernateSession.persist(develUser);

        hibernateSession.flush();

        final CharmsUser adminUser = deployUser(ADMIN_LOGIN, ADMIN_PASSWD, adminRole, tqmRole);
        adminUser.setFirstname("Alfred");
        adminUser.setLastname("Admin");
        hibernateSession.persist(adminUser);

        final CharmsUser demoUser = deployUser(DEMO_LOGIN, DEMO_PASSWD, tqmRole);
        demoUser.setFirstname("Doris");
        demoUser.setLastname("Demo");
        hibernateSession.persist(demoUser);

        // deploy roles:
        final int roleCount = DEMO_ROLE_COUNT;
        for (int i = 0; i < roleCount; i++) {
            final String name = "testrole" + i;
            deployAuthorizationalRole(name);
            if (i % 100 == 0) {
                LOGGER.debug("setup role: " + name);
            }
        }

        // deploy users:
        final int userCount = DEMO_USER_COUNT;
        for (int i = 0; i < userCount; i++) {
            final String name = "testuser" + i;
            deployUser(name, name);
            if (i % 100 == 0) {
                LOGGER.debug("setup user: " + name);
            }
        }

        hibernateSession.flush();
    }

    // deploy a user if the user doesn't exist
    private CharmsUser deployUser(final String username, final String passwd, final CharmsRole... roles) {
        final int resultCount = hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", username).list().size();
        if (resultCount < 1) {
            final CharmsUser dummyUser = new CharmsUser();
            dummyUser.setName(username);
            dummyUser.setPasswd(CustomHash.instance().generateSaltedHash(passwd, username));
            dummyUser.setFirstname(username);
            dummyUser.setLastname(username);
            hibernateSession.persist(dummyUser);
            for (final CharmsRole role : roles) {
                hibernateSession.persist(new CharmsMembership(dummyUser, role));
            }
            hibernateSession.flush();
        } else if (resultCount == 1) {
            // the user already exists we can just assign the roles
            final CharmsUser dummyUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", username).uniqueResult();
            for (final CharmsRole role : roles) {
                hibernateSession.persist(new CharmsMembership(dummyUser, role));
            }
            hibernateSession.flush();
        } else {
            LOGGER.warn("username {} is not unique ", username);
        }
        return (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", username).uniqueResult();
    }

    // deploy a role if the role doesn't exist
    // and return the persisted role
    private CharmsRole deployAuthorizationalRole(final String rolename) {
        final int resultCount = hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", rolename).list().size();
        if (resultCount < 1) {
            final CharmsRole role = new CharmsRole();
            role.setName(rolename);
            role.setClassification(RoleClassification.AUTHORIZATIONAL);
            hibernateSession.persist(role);
            hibernateSession.flush();
        } else {
            LOGGER.warn("rolename {} already exists", rolename);
        }
        return (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", rolename).uniqueResult();
    }

    // recipient = ADMIN_ROLE_STRING
    // target = CharmsChart.TARGET_STRING
    // action = StringUtils.join(CharmsChart.ALL_ACTIONS, ",")
    private void deployPermissionToRole(final String recipient, final String target, final String action) {
        // does the admin role already have the permission to see the charts
        // menu
        // FIXME: this permission might not include the permissions to write or
        // modify...
        final int resultCount = hibernateSession.getNamedQuery(CharmsPermission.FIND_BY_RECIPIENT_ROLE_AND_TARGET).setParameter("recipient", recipient)
                .setParameter("target", target).list().size();
        if (resultCount < 1) {
            final CharmsPermission rolePermission = new CharmsPermission();
            rolePermission.setAction(action);
            rolePermission.setDiscriminator(CharmsPermission.ROLE);
            rolePermission.setTarget(target);
            rolePermission.setRecipient(recipient);
            hibernateSession.persist(rolePermission);
        }

    }

}
