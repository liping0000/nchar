package net.wohlfart.charms.test.action;

import java.math.BigInteger;
import java.util.List;

import net.wohlfart.admin.CharmsPermissionTargetActionBean;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CharmsPermissionTargetActionBeanTest extends AbstractHomeActionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup the component under test
        abstractEntityHome = (CharmsPermissionTargetActionBean) Component.getInstance("charmsPermissionTargetActionBean");
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
    public void testCreatePermissionTargetAction() {
        Long id;
        String outcome;
        CharmsPermissionTarget charmsPermissionTarget;
        final CharmsPermissionTargetActionBean home = (CharmsPermissionTargetActionBean) abstractEntityHome;

        // ---------------- testing creation of a new entity ------------------
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        id = charmsPermissionTarget.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");

        // persist without adding soe propertie
        outcome = home.persist();
        Assert.assertEquals(outcome, "invalid", "error persisting invalid entity is possible");

        id = home.getCharmsPermissionTarget().getId();
        Assert.assertNull(id, "unpersisted permission has not null id ");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();    

        // ---------------- now really create a new entity ------------------
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        id = charmsPermissionTarget.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");

        // simulate some user entries:
        charmsPermissionTarget.setTargetString("some.action");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted", "error persisting valid entity is not possible");

        id = home.getCharmsPermissionTarget().getId();
        Assert.assertNotNull(id, "persisted permission has null id ");
        
        hibernateSession.flush();
        hibernateSession.clear();
        abstractEntityHome.clearInstance();    
        
        // ---------------- now delete that entity ------------------
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        id = charmsPermissionTarget.getId();
        Assert.assertNotNull(id, "persisted permission should not have null id");
        Assert.assertEquals(charmsPermissionTarget.getTargetString(), "some.action");
        
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing entity");
        
        hibernateSession.flush();
        hibernateSession.clear();
    }

    
    @Test
    public void testAddSomeActions() {
        Long id;
        String outcome;
        CharmsPermissionTarget charmsPermissionTarget;
        final CharmsPermissionTargetActionBean home = (CharmsPermissionTargetActionBean) abstractEntityHome;

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        id = charmsPermissionTarget.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");

        // simulate some user entries:
        charmsPermissionTarget.setTargetString("some.other.action");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted", "error persisting valid entity is not possible");

        id = home.getCharmsPermissionTarget().getId();
        Assert.assertNotNull(id, "persisted permission has null id ");
        
        // call the action again
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        
        List<CharmsTargetAction> actions = home.getActions();
        Assert.assertEquals(actions.size(), 0);
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 1);
        actions.get(0).setName("testaction1");
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 2);
        actions.get(0).setName("testaction1");
        actions.get(0).setName("tastaction2");
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 3);
        actions.get(0).setName("tastaction1");
        actions.get(1).setName("testaction2");
        actions.get(2).setName("testaction3");

        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 4);
        actions.get(0).setName("tastaction1");
        actions.get(1).setName("testaction2");
        actions.get(2).setName("testaction3");
        actions.get(3).setName("testaction4");

        outcome = home.update();
        Assert.assertEquals(outcome, "updated", "error persisting valid entity is not possible");
        
        hibernateSession.flush();
        hibernateSession.clear(); 
        
        // pick it up and check:
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        Long id2 = charmsPermissionTarget.getId();
        Assert.assertEquals(id2, id, "unpersisted permission should have null id");
        
        // check if we still have all actions
        Assert.assertEquals(charmsPermissionTarget.getActions().size(), 4, "actions have not been persisted");
        
        hibernateSession.flush();
        hibernateSession.clear(); 
      
        // remove 
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing");
        
        hibernateSession.flush();
        hibernateSession.clear(); 
      
    }
    
    
    @Test
    public void testAddRemoveSomeActions() {
        Long id;
        String outcome;
        CharmsPermissionTarget charmsPermissionTarget;
        final CharmsPermissionTargetActionBean home = (CharmsPermissionTargetActionBean) abstractEntityHome;

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        id = charmsPermissionTarget.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");

        // simulate some user entries:
        charmsPermissionTarget.setTargetString("some.other2.action");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted", "error persisting valid entity is not possible");

        id = home.getCharmsPermissionTarget().getId();
        Assert.assertNotNull(id, "persisted permission has null id ");
        
        // call the action again
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        
        List<CharmsTargetAction> actions = home.getActions();
        Assert.assertEquals(actions.size(), 0);
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 1);
        actions.get(0).setName("testaction1");
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 2);
        actions.get(0).setName("testaction1");
        actions.get(0).setName("tastaction2");
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 3);
        actions.get(0).setName("tastaction1");
        actions.get(1).setName("testaction2");
        actions.get(2).setName("testaction3");

        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 4);
        actions.get(0).setName("tastaction1");
        actions.get(1).setName("testaction2");
        actions.get(2).setName("testaction3");
        actions.get(3).setName("testaction4");

        outcome = home.update();
        Assert.assertEquals(outcome, "updated", "error persisting valid entity is not possible");
        
        hibernateSession.flush();
        hibernateSession.clear(); 
        
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        Long id2 = charmsPermissionTarget.getId();
        Assert.assertEquals(id2, id, "unpersisted permission should have null id");
        
        // check if we still have all actions
        Assert.assertEquals(charmsPermissionTarget.getActions().size(), 4, "actions have not been persisted");
        
        // lets remove two action strings here
        home.delAction();
        home.delAction();
        
        outcome = home.update();
        Assert.assertEquals(outcome, "updated", "error updating valid entity is not possible");
         
        hibernateSession.flush();
        hibernateSession.clear(); 
        
        
        // remove 
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing");
        
        hibernateSession.flush();
        hibernateSession.clear(); 
      
    }

    @Test
    public void testCreateAddSomeActions() {
        Long id;
        String outcome;
        CharmsPermissionTarget charmsPermissionTarget;
        final CharmsPermissionTargetActionBean home = (CharmsPermissionTargetActionBean) abstractEntityHome;

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        id = charmsPermissionTarget.getId();
        Assert.assertNull(id, "unpersisted permission should have null id");
        // simulate some user entries:
        charmsPermissionTarget.setTargetString("some.other3.action");
        
        List<CharmsTargetAction> actions = home.getActions();
        Assert.assertEquals(actions.size(), 0);
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 1);
        actions.get(0).setName("testaction1");
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 2);
        actions.get(0).setName("testaction1");
        actions.get(0).setName("tastaction2");
        
        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 3);
        actions.get(0).setName("tastaction1");
        actions.get(1).setName("testaction2");
        actions.get(2).setName("testaction3");

        home.addAction();
        actions = home.getActions();
        Assert.assertEquals(actions.size(), 4);
        actions.get(0).setName("tastaction1");
        actions.get(1).setName("testaction2");
        actions.get(2).setName("testaction3");
        actions.get(3).setName("testaction4");

        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted", "error persisting valid entity is not possible");
        id = home.getCharmsPermissionTarget().getId();
        
        // pick it up and check:
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        // calling the factory method
        charmsPermissionTarget = home.getCharmsPermissionTarget();
        Long id2 = charmsPermissionTarget.getId();
        Assert.assertEquals(id2, id, "unpersisted permission should have null id");
        
        // check if we still have all actions
        Assert.assertEquals(charmsPermissionTarget.getActions().size(), 4, "actions have not been persisted");
        
        hibernateSession.flush();
        hibernateSession.clear(); 
        
        // pick it up and remove:
        outcome = home.setEntityId(id.toString());
        Assert.assertEquals(outcome, "valid", "error setting entityid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing");
        
        hibernateSession.flush();
        hibernateSession.clear(); 
                
   
    }

}
