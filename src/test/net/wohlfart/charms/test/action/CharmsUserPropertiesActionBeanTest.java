package net.wohlfart.charms.test.action;

import net.wohlfart.admin.CharmsUserActionBean;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.refdata.entities.ChangeRequestProduct;

import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CharmsUserPropertiesActionBeanTest extends AbstractHomeActionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup the component under test
        abstractEntityHome = (CharmsUserActionBean) Component.getInstance("charmsUserActionBean");
        Assert.assertNotNull(abstractEntityHome, "abstractEntityHome is null");
    }

    @Test
    public void testSimpleCreateAllProps() {
        String outcome;

        // ---------------- create a new user --------------------

        final CharmsUserActionBean userAction = (CharmsUserActionBean) abstractEntityHome;

        outcome = userAction.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting userid");

        CharmsUser user = userAction.getCharmsUser();
        user.setName("loginNameTest1");
        user.setDescription("some description here");
        user.setExternalId1("external1");
        user.setExternalId2("external2");
        user.setFirstname("firstname");
        user.setLastname("lastname");
        userAction.setPasswd("sn00py00");
        userAction.setPasswdConfirm("sn00py00");
        outcome = userAction.persist();
        Assert.assertEquals(outcome, "persisted", "error persisting user");
        hibernateSession.flush(); // persist the user

        final CharmsUser persistedUser = userAction.getCharmsUser();
        Assert.assertNotNull(persistedUser.getId(), "error persisting user, got null id");

        // cleaning the component
        outcome = userAction.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting null userid");

        outcome = userAction.setEntityId("" + persistedUser.getId());
        Assert.assertEquals(outcome, "valid", "error setting userid");

        final CharmsUser refreshedUser = userAction.getCharmsUser();
        Assert.assertNotNull(refreshedUser, "error persisted user not found");
        // now check the users properties:
        Assert.assertEquals(refreshedUser.getFirstname(), "firstname");
        Assert.assertNotNull(refreshedUser.getName());
        Assert.assertEquals(refreshedUser.getName(), "loginNameTest1");
        Assert.assertEquals(refreshedUser.getLastname(), "lastname");
        Assert.assertEquals(refreshedUser.getExternalId1(), "external1");
        Assert.assertEquals(refreshedUser.getExternalId2(), "external2");
        Assert.assertEquals(refreshedUser.getDescription(), "some description here");

        hibernateSession.flush();
        hibernateSession.clear(); // reset session
        abstractEntityHome.clearInstance();

        user = (CharmsUser) hibernateSession.get(CharmsUser.class, persistedUser.getId());
        hibernateSession.delete(user);

        hibernateSession.flush();
    }

}
