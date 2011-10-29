package net.wohlfart.charms.test.action;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.admin.CharmsUserActionBean;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsRoleItem;
import net.wohlfart.authentication.entities.CharmsUser;

import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CharmsUserActionBeanTest extends AbstractHomeActionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup the component under test
        abstractEntityHome = (CharmsUserActionBean) Component.getInstance("charmsUserActionBean");
        Assert.assertNotNull(abstractEntityHome, "abstractEntityHome is null");
    }

    @Test
    public void testSetEntityIdAction() {
        String outcome;

        // ---------------- testing missing required field --------------------

        outcome = abstractEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");

        outcome = abstractEntityHome.persist();
        Assert.assertEquals(outcome, "invalid", "outcome is: " + outcome + " should be invalid because of missing name");

        hibernateSession.clear(); // reset session
        abstractEntityHome.clearInstance();

        // ---------------- testing a random invalid id
        // -------------------------

        outcome = abstractEntityHome.setEntityId("blabla");
        Assert.assertEquals(outcome, "invalid", "error setting roleid to an invalid value is possible");

        outcome = abstractEntityHome.persist();
        Assert.assertEquals(outcome, "invalid", "error setting roleid to an invalid value is possible");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // ---------------- testing random unused id --------------------------

        // id too high wont find anything
        outcome = abstractEntityHome.setEntityId("3000");
        Assert.assertEquals(outcome, "invalid", "error setting userid to an invalid value is possible");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // ---------------- testing id < 0 ------------------------------------

        // negative ids won't find anything
        outcome = abstractEntityHome.setEntityId("-3");
        Assert.assertEquals(outcome, "invalid", "error setting userid to an invalid value is possible");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // ---------------- some long parsing behaviour checks ----------------

        // and now for something completely different:
        Long l;
        try {
            l = new Long("-0");
            assert l == 0;
        } catch (final NumberFormatException e) {
            assert false : "number format exception";
        }

        try {
            l = new Long("+0");
            assert l == 0;
            assert false : "no number format exception";
        } catch (final NumberFormatException e) {
            // expected exception
        }

        // ---------------- testing id == -0 ----------------------------------

        outcome = abstractEntityHome.setEntityId("-0");
        Assert.assertEquals(outcome, "invalid", "error setting userid to an invalid value is possible");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // ---------------- testing id == +0 ----------------------------------

        outcome = abstractEntityHome.setEntityId("+0");
        Assert.assertEquals(outcome, "invalid", "error setting userid to an invalid value is possible");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // ---------------- testing id > Long.MAX_VALUE -----------------------

        BigInteger bigNumber = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger evenBigger = bigNumber.multiply(BigInteger.valueOf(500L));

        outcome = abstractEntityHome.setEntityId(evenBigger.toString());
        Assert.assertEquals(outcome, "invalid", "setting entity id to Long.MAX_VALUE + x is possible");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // ---------------- testing id > Long.MAX_VALUE and returning
        // entity-----------------------

        bigNumber = BigInteger.valueOf(Long.MAX_VALUE);
        evenBigger = bigNumber.multiply(BigInteger.valueOf(500L));

        outcome = abstractEntityHome.setEntityId(evenBigger.toString());
        Assert.assertEquals(outcome, "invalid", "setting entity id to Long.MAX_VALUE + x is possible");

        // calling the factory method after we got "invalid" for the id
        // should give us a new unsaved entity
        final Object entity1 = abstractEntityHome.getInstance();
        Assert.assertNotNull(entity1, "entity must not be null");
        Assert.assertNull(abstractEntityHome.getId(), "id for new home object should be null but is " + abstractEntityHome.getId());

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // ---------------- testing id > Long.MAX_VALUE and returning
        // entity-----------------------

        // this is a call from pages.xml to create an entity
        outcome = abstractEntityHome.setEntityId("");
        Assert.assertEquals(outcome, "valid", "setting entity id to Long.MAX_VALUE + x is possible");

        final Object entity2 = abstractEntityHome.getInstance();
        Assert.assertNotNull(entity2, "entity must not be null");
        Assert.assertNull(abstractEntityHome.getId(), "id for new home object should be null but is " + abstractEntityHome.getId());

        outcome = abstractEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "setting entity id to Long.MAX_VALUE + x is possible");

        final Object entity3 = abstractEntityHome.getInstance();
        Assert.assertNotNull(entity3, "entity must not be null");
        Assert.assertNull(abstractEntityHome.getId(), "id for new home object should be null but is " + abstractEntityHome.getId());

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

    }

    @Test
    public void testCreateUserAction() {
        String outcome;
        // convert to specific class
        final CharmsUserActionBean home = (CharmsUserActionBean) abstractEntityHome;

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting userid");
        // calling the factory method
        CharmsUser charmsUser = home.getCharmsUser();

        // simulate some user entries:

        charmsUser.setName("test");
        charmsUser.setFirstname("test");
        charmsUser.setLastname("test");
        home.setPasswd("password");
        home.setPasswdConfirm("password");

        home.persist();

        final Long id = home.getCharmsUser().getId();
        assert id != null : "persisted user has null id ";

        hibernateSession.clear();
        abstractEntityHome.clearInstance();

        // -- we need to delete the user

        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting userid");
        // calling the factory method
        charmsUser = home.getCharmsUser();
        Assert.assertEquals(charmsUser.getLastname(), "test", "users lastname doesn't match");

        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing user");
    }

    @Test
    public void testCreateAndLookupAction() {
        CharmsUser charmsUser;
        String outcome;

        // convert to specific class
        final CharmsUserActionBean home = (CharmsUserActionBean) abstractEntityHome;

        // ---------- create user 1

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting userid");
        // calling the factory method
        charmsUser = home.getCharmsUser();

        // simulate some user entries:

        charmsUser.setName("test1");
        charmsUser.setFirstname("test1");
        charmsUser.setLastname("test1");
        home.setPasswd("password1");
        home.setPasswdConfirm("password1");

        home.persist();

        // this is the id to lookup...
        final Long id1 = home.getCharmsUser().getId();
        assert id1 != null : "persisted user has null id ";

        // ---------- create user 2

        outcome = home.setEntityId(null);
        assert "valid".equals(outcome) : "error setting userid";
        // calling the factory method
        charmsUser = home.getCharmsUser();

        // simulate some user entries:

        charmsUser.setName("test222");
        charmsUser.setFirstname("test222");
        charmsUser.setLastname("test222");
        home.setPasswd("password222");
        home.setPasswdConfirm("password222");

        Assert.assertEquals(home.persist(), "persisted");

        // this is the id to lookup...
        final Long id2 = home.getCharmsUser().getId();
        assert id2 != null : "persisted user has null id ";

        // ---------- create user 3

        outcome = home.setEntityId(null);
        assert "valid".equals(outcome) : "error setting userid";
        // calling the factory method
        charmsUser = home.getCharmsUser();

        // simulate some user entries:

        charmsUser.setName("test3");
        charmsUser.setFirstname("test3");
        charmsUser.setLastname("test3");
        home.setPasswd("password3");
        home.setPasswdConfirm("password3");

        home.persist();

        // this is the id to lookup...
        final Long id3 = home.getCharmsUser().getId();
        assert id3 != null : "persisted user has null id ";

        // --- lookup user 2
        outcome = home.setEntityId(id2.toString());
        assert "valid".equals(outcome) : "error setting userid for an existing user, id is " + id1 + " outcome is " + outcome + " instead of 'valid'";
        // calling the factory method
        charmsUser = home.getCharmsUser();

        assert charmsUser.getName().equals("test222");
        outcome = home.remove();
        assert "removed".equals(outcome) : "error removing user, id is " + id2 + " outcome is " + outcome + " instead of 'removed'";

        // --- lookup user 1
        outcome = home.setEntityId(id1.toString());
        assert "valid".equals(outcome) : "error setting userid for an existing user, id is " + id1 + " outcome is " + outcome + " instead of 'valid'";
        // calling the factory method
        charmsUser = home.getCharmsUser();

        assert charmsUser.getName().equals("test1");
        outcome = home.remove();
        assert "removed".equals(outcome) : "error removing user, id is " + id1 + " outcome is " + outcome + " instead of 'removed'";

        // --- lookup user 3
        outcome = home.setEntityId(id3.toString());
        assert "valid".equals(outcome) : "error setting userid for an existing user, id is " + id3 + " outcome is " + outcome + " instead of 'valid'";
        // calling the factory method
        charmsUser = home.getCharmsUser();

        assert charmsUser.getName().equals("test3");
        outcome = home.remove();
        assert "removed".equals(outcome) : "error removing user, id is " + id3 + " outcome is " + outcome + " instead of 'removed'";

        hibernateSession.clear();
        abstractEntityHome.clearInstance();
    }

    @Test
    public void testRemoveGroup() {
        String outcome;
        final CharmsUserActionBean home = (CharmsUserActionBean) abstractEntityHome;

        // create user renametest1
        home.setEntityId(null);
        // calling the factory method
        CharmsUser charmsUser1 = home.getCharmsUser();
        charmsUser1.setName("removetest1");
        charmsUser1.setFirstname("test1");
        charmsUser1.setLastname("test1");
        home.setPasswd("password1");
        home.setPasswdConfirm("password1");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        charmsUser1 = home.getCharmsUser();
        hibernateSession.flush();


        // create two groups:
        CharmsRole charmsRole1 = new CharmsRole();
        charmsRole1.setName("removeRole1");
        hibernateSession.persist(charmsRole1);
        hibernateSession.flush();
        charmsRole1 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", "removeRole1").uniqueResult();
        Assert.assertNotNull(charmsRole1);

        CharmsRole charmsRole2 = new CharmsRole();
        charmsRole2.setName("removeRole2");
        hibernateSession.persist(charmsRole2);
        hibernateSession.flush();
        charmsRole2 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", "charmsRole2").uniqueResult();
        Assert.assertNull(charmsRole2);
        charmsRole2 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", "removeRole2").uniqueResult();
        Assert.assertNotNull(charmsRole2);

        charmsUser1 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", "removetest1").uniqueResult();
        Assert.assertNotNull(charmsUser1);


        Assert.assertEquals(charmsUser1.getMemberships().size(), 0, "membership size should be zero");

        home.setEntityId(charmsUser1.getId().toString());
        List<CharmsRoleItem> roleItems = home.getAvailableRoleItems();

        List<CharmsRoleItem> selectedItems = new ArrayList<CharmsRoleItem>();
        for (final CharmsRoleItem item : roleItems) {
            selectedItems.add(item);
        }

        // add them all:
        home.setSelectedRoleItems(selectedItems);
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        charmsUser1 = home.getCharmsUser();
        Assert.assertEquals(charmsUser1.getMemberships().size(), roleItems.size(), "wrong membership count");


        outcome = home.setEntityId(charmsUser1.getId().toString());
        Assert.assertEquals(outcome, "valid");
        roleItems = home.getSelectedRoleItems();

        selectedItems = new ArrayList<CharmsRoleItem>();
        for (final CharmsRoleItem item : roleItems) {
            selectedItems.add(item);
        }
        selectedItems.remove(0); // remove one item
        // set the minus one list
        home.setSelectedRoleItems(selectedItems);
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        // now delete the user:
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");

        hibernateSession.flush();

        // delete the groups:

        charmsRole1 = (CharmsRole) hibernateSession.get(CharmsRole.class, charmsRole1.getId());
        hibernateSession.delete(charmsRole1);

        charmsRole2 = (CharmsRole) hibernateSession.get(CharmsRole.class, charmsRole2.getId());
        hibernateSession.delete(charmsRole2);

        hibernateSession.flush();
    }

    @Test
    public void testRenameUserAction() {
        String outcome;
        final CharmsUserActionBean home = (CharmsUserActionBean) abstractEntityHome;

        // create user renametest1
        home.setEntityId(null);
        // calling the factory method
        CharmsUser charmsUser1 = home.getCharmsUser();
        charmsUser1.setName("renametest1");
        charmsUser1.setFirstname("test1");
        charmsUser1.setLastname("test1");
        home.setPasswd("password1");
        home.setPasswdConfirm("password1");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        charmsUser1 = home.getCharmsUser();

        // create user renametest2
        home.setEntityId(null);
        // calling the factory method
        CharmsUser charmsUser2 = home.getCharmsUser();
        charmsUser2.setName("renametest2");
        charmsUser2.setFirstname("test2");
        charmsUser2.setLastname("test2");
        home.setPasswd("password2");
        home.setPasswdConfirm("password2");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        charmsUser2 = home.getCharmsUser();

        // try to rename to a name that already exists
        home.setEntityId(Long.toString(charmsUser2.getId()));
        charmsUser2 = home.getCharmsUser();
        Assert.assertEquals(charmsUser2.getName(), "renametest2");
        charmsUser2.setName("renametest1");
        outcome = home.update();
        Assert.assertEquals("invalid", outcome);

        // try to rename to a new name
        home.setEntityId(Long.toString(charmsUser2.getId()));
        charmsUser2 = home.getCharmsUser();
        home.cancel(); // evict charmsUser2 from hibernate session

        // this should be the original name from the database
        Assert.assertEquals(charmsUser2.getName(), "renametest2");
        home.setEntityId(null);
        final CharmsUser dummy = home.getCharmsUser();
        Assert.assertNotNull(dummy);
        home.cancel(); // evict from hibernate session

        home.setEntityId(Long.toString(charmsUser2.getId()));
        charmsUser2 = home.getCharmsUser();
        // now we should have the old name from DB
        Assert.assertEquals(charmsUser2.getName(), "renametest2");
        charmsUser2.setName("renametest3");
        outcome = home.update();
        Assert.assertEquals("updated", outcome);

        home.setEntityId(Long.toString(charmsUser1.getId()));
        charmsUser1 = home.getCharmsUser();
        Assert.assertEquals(charmsUser1.getName(), "renametest1");

        home.setEntityId(Long.toString(charmsUser2.getId()));
        charmsUser2 = home.getCharmsUser();
        Assert.assertEquals(charmsUser2.getName(), "renametest3");

        // delete the users:
        home.setEntityId(Long.toString(charmsUser1.getId()));
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");

        home.setEntityId(Long.toString(charmsUser2.getId()));
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");


    }

}
