package net.wohlfart.charms.test.action;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import net.wohlfart.admin.CharmsPermissionActionBean;
import net.wohlfart.authorization.entities.CharmsPermission;

import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CharmsPermissionActionBeanTest extends AbstractHomeActionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup the component under test
        abstractEntityHome = (CharmsPermissionActionBean) Component.getInstance("charmsPermissionActionBean");
        Assert.assertNotNull(abstractEntityHome, "abstractEntityHome is null");
    }

    @Test
    public void testSetEntityIdAction() {
        String outcome;

        // ---------------- testing missing required field --------------------

        outcome = abstractEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");

        outcome = abstractEntityHome.persist();
        Assert.assertEquals(outcome, "invalid", "outcome is: " + outcome + " should be 'invalid' because of missing name");

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
    public void testInvalidCreatePermissionAction() {
        Long id;
        String outcome;
        final CharmsPermissionActionBean home = (CharmsPermissionActionBean) abstractEntityHome;

        // ---------------- testing creation of a new entity ------------------

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        final CharmsPermission charmsPermission = home.getCharmsPermission();
        id = charmsPermission.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");

        outcome = home.persist();
        Assert.assertEquals(outcome, "invalid", "error persisting invalid entity is possible");

        id = home.getCharmsPermission().getId();
        Assert.assertNull(id, "unpersisted permission has not null id ");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();
    }

    @Test
    public void testCreatePermissionAction() {
        Long id;
        String outcome;
        final CharmsPermissionActionBean home = (CharmsPermissionActionBean) abstractEntityHome;

        // ---------------- testing creation of a new entity ------------------

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        final CharmsPermission charmsPermission = home.getCharmsPermission();
        id = charmsPermission.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");

        List<String> targets = home.getSelectableTargets();
        Assert.assertTrue(targets.size() > 0, "no permission targets available");
        home.setTargetString(targets.get(0));
        
        
        HashMap<String, Boolean> actions = home.getSelectableActions();
        Assert.assertTrue(actions.size() > 0, "no actions available for the target " + targets.get(0));
        

        hibernateSession.clear();
        abstractEntityHome.clearInstance();
    }

    
    @Test
    public void testMissingRecipientCreatePermissionAction() {
        Long id;
        String outcome;
        final CharmsPermissionActionBean home = (CharmsPermissionActionBean) abstractEntityHome;

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        final CharmsPermission charmsPermission = home.getCharmsPermission();
        id = charmsPermission.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");


        // FIXME: check missing recipient here
        
        
        hibernateSession.clear();
        abstractEntityHome.clearInstance();
    }

    
    @Test
    public void testMissingActionCreatePermissionAction() {
        Long id;
        String outcome;
        final CharmsPermissionActionBean home = (CharmsPermissionActionBean) abstractEntityHome;

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        final CharmsPermission charmsPermission = home.getCharmsPermission();
        id = charmsPermission.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");

        
        // FIXME: check missing action here
        
        
        hibernateSession.clear();
        abstractEntityHome.clearInstance();
    }
}
