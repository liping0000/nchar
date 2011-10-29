package net.wohlfart.charms.test.components;

import net.wohlfart.authentication.CharmsIdentityManager;
import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.CharmsRoleIdentityStore;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.CustomHash;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PasswordEncryptionTest extends ComponentSessionBase {

    private CustomHash customHash;

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        customHash = CustomHash.instance();
    }

    @Test
    public void testSHAEncryption() {

        final String clearPasswd1 = "devel";
        final String clearPasswd2 = "admin";

        final String hashedPasword1 = customHash.generateSaltedHash("devel", "devel", CustomHash.ALGORITHM_SHA);
        final String hashedPasword2 = customHash.generateSaltedHash("admin", "admin", CustomHash.ALGORITHM_SHA);

        Assert.assertEquals(hashedPasword1, "{SHA}IabK+vDpsAgGktaZwVONDfpCtHo=");

        Assert.assertTrue(StringUtils.startsWith(hashedPasword1, CustomHash.BEGIN_TAG_SHA));
        Assert.assertTrue(StringUtils.startsWith(hashedPasword2, CustomHash.BEGIN_TAG_SHA));

        // default encryption is SSHA not SHA, so this will fail:
        Assert.assertFalse(customHash.checkPassword(clearPasswd1, "devel", hashedPasword1));
    }

    @Test
    public void testMD5Encryption() {

        final String clearPasswd1 = "devel";
        final String clearPasswd2 = "admin";

        final String hashedPasword1 = customHash.generateSaltedHash("devel", "devel", CustomHash.ALGORITHM_MD5);
        final String hashedPasword2 = customHash.generateSaltedHash("admin", "admin", CustomHash.ALGORITHM_MD5);

        Assert.assertEquals(hashedPasword1, "{MD5}2vmFQ8SHr2zrIwyuACyS/Q==");

        Assert.assertTrue(StringUtils.startsWith(hashedPasword1, CustomHash.BEGIN_TAG_MD5));
        Assert.assertTrue(StringUtils.startsWith(hashedPasword2, CustomHash.BEGIN_TAG_MD5));

        // default encryption is SSHA not MD5, so this will fail:
        Assert.assertFalse(customHash.checkPassword(clearPasswd1, "devel", hashedPasword1));
    }

    @Test
    public void testSSHAEncryption() {

        final String clearPasswd1 = "devel";
        final String clearPasswd2 = "admin";

        final String hashedPasword1 = customHash.generateSaltedHash("devel", "devel", CustomHash.ALGORITHM_SSHA);
        final String hashedPasword2 = customHash.generateSaltedHash("admin", "admin", CustomHash.ALGORITHM_SSHA);

        Assert.assertEquals(hashedPasword1, "{SSHA}IabK+vDpsAgGktaZwVONDfpCtHo=");

        Assert.assertTrue(StringUtils.startsWith(hashedPasword1, CustomHash.BEGIN_TAG_SSHA));
        Assert.assertTrue(StringUtils.startsWith(hashedPasword2, CustomHash.BEGIN_TAG_SSHA));

        Assert.assertTrue(customHash.checkPassword(clearPasswd1, "devel", hashedPasword1));
        Assert.assertTrue(customHash.checkPassword(clearPasswd2, "admin", hashedPasword2));
        Assert.assertFalse(customHash.checkPassword(clearPasswd1, "admin", hashedPasword2));
        Assert.assertFalse(customHash.checkPassword(clearPasswd2, "devel", hashedPasword1));

    }

    @Test
    public void testDefaultEncryption() {

        final String clearPasswd1 = "devel";
        final String clearPasswd2 = "admin";

        final String hashedPasword1 = customHash.generateSaltedHash("devel", "devel", CharmsUser.PASSWORD_HASH_FUNCTION);
        final String hashedPasword2 = customHash.generateSaltedHash("admin", "admin");

        Assert.assertEquals(hashedPasword1, "{SSHA}IabK+vDpsAgGktaZwVONDfpCtHo=");

        Assert.assertTrue(StringUtils.startsWith(hashedPasword1, CustomHash.BEGIN_TAG_SSHA));
        Assert.assertTrue(StringUtils.startsWith(hashedPasword2, CustomHash.BEGIN_TAG_SSHA));

        Assert.assertTrue(customHash.checkPassword(clearPasswd1, "devel", hashedPasword1));
        Assert.assertTrue(customHash.checkPassword(clearPasswd2, "admin", hashedPasword2));
        Assert.assertFalse(customHash.checkPassword(clearPasswd1, "admin", hashedPasword2));
        Assert.assertFalse(customHash.checkPassword(clearPasswd2, "devel", hashedPasword1));

    }

    // test lazy encryption with valid authentication
    @Test
    public void testLazyEncryption() {

        final String USER1 = "lazyEncryptionTest1";
        final String PASSWD1 = "lazypassword1";

        final String USER2 = "lazyEncryptionTest2";
        final String PASSWD2 = "lazypassword2";

        // setup an identityManager
        final CharmsIdentityManager charmsIdentityManager = new CharmsIdentityManager();

        final CharmsUserIdentityStore charmsUserIdentityStore = new CharmsUserIdentityStore();
        charmsUserIdentityStore.init();
        final CharmsRoleIdentityStore charmsRoleIdentityStore = new CharmsRoleIdentityStore();
        charmsRoleIdentityStore.init();

        charmsIdentityManager.setIdentityStore(charmsUserIdentityStore);
        charmsIdentityManager.setRoleIdentityStore(charmsRoleIdentityStore);

        // setup a hibernate session
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession");

        final CharmsUser user1 = new CharmsUser();
        user1.setName(USER1);
        user1.setPasswd(PASSWD1);
        hibernateSession.persist(user1);
        hibernateSession.flush();

        final CharmsUser user2 = new CharmsUser();
        user2.setName(USER2);
        user2.setPasswd(PASSWD2);
        hibernateSession.persist(user2);
        hibernateSession.flush();

        CharmsUser persistedUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", USER1).uniqueResult();

        Assert.assertEquals(persistedUser.getPasswd(), PASSWD1);

        final boolean auth1 = charmsUserIdentityStore.authenticate(USER1, PASSWD1);
        Assert.assertTrue(auth1);

        persistedUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", USER1).uniqueResult();

        final String hashedPassword1 = customHash.generateSaltedHash(PASSWD1, USER1);
        Assert.assertEquals(persistedUser.getPasswd(), hashedPassword1);

        final boolean auth2 = charmsUserIdentityStore.authenticate(USER2, "invalid");
        Assert.assertFalse(auth2);

        persistedUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", USER2).uniqueResult();

        final String hashedPassword2 = customHash.generateSaltedHash(PASSWD2, USER2);
        Assert.assertEquals(persistedUser.getPasswd(), hashedPassword2);

        final boolean auth3 = charmsUserIdentityStore.authenticate(USER2, PASSWD2);
        Assert.assertTrue(auth3);

    }

}
