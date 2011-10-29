package net.wohlfart.charms.test.action;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.admin.CharmsRoleActionBean;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsRoleItem;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authentication.entities.CharmsUserItem;

import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CharmsRoleActionBeanTest extends AbstractHomeActionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup the component under test
        abstractEntityHome = (CharmsRoleActionBean) Component.getInstance("charmsRoleActionBean");
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
    public void testCreateRoleAction() {
        String outcome;
        final CharmsRoleActionBean home = (CharmsRoleActionBean) abstractEntityHome;

        // ---------------- testing creation of a new entity ------------------

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        final CharmsRole charmsRole = home.getCharmsRole();

        // simulate some user entries:

        charmsRole.setName("test");

        outcome = abstractEntityHome.persist();
        Assert.assertEquals(outcome, "persisted");
        

        final Long id = home.getCharmsRole().getId();
        Assert.assertNotNull(id, "persisted role has null id ");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();
        
        // remove it
        outcome = abstractEntityHome.setEntityId(id + "");
        Assert.assertEquals(outcome, "valid");
        
        outcome = abstractEntityHome.remove();
        Assert.assertEquals(outcome, "removed");
       
    }

    @Test
    public void testCreateAndLookupAction() {
        CharmsRole charmsRole;
        String outcome;
        final CharmsRoleActionBean home = (CharmsRoleActionBean) abstractEntityHome;

        // ---------- create role 1

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid"); // "error setting roleid to nulll";
        // calling the factory method
        charmsRole = home.getCharmsRole();

        // simulate some role entries:

        charmsRole.setName("test1");
        home.persist();

        // this is the id to lookup...
        final Long id1 = home.getCharmsRole().getId();
        Assert.assertNotNull(id1, "persisted role has null id ");

        // ---------- create role 2

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();

        // simulate some role entries:

        charmsRole.setName("test222");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");

        // this is the id to lookup...
        final Long id2 = home.getCharmsRole().getId();
        Assert.assertNotNull(id2, "persisted role has null id ");

        // ---------- create role 3

        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();

        // simulate some role entries:

        charmsRole.setName("test3");
        home.persist();

        // this is the id to lookup...
        final Long id3 = home.getCharmsRole().getId();
        Assert.assertNotNull(id3, "persisted user has null id ");

        // --- lookup role 2
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid", "error setting roleid for an existing role, id is " + id1 + " outcome is " + outcome + " instead of 'valid'");
        // calling the factory method
        charmsRole = home.getCharmsRole();

        Assert.assertEquals(charmsRole.getName(), "test222");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing user, id is " + id2 + " outcome is " + outcome + " instead of 'removed'");

        // --- lookup role 1
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid", "error setting userid for an existing user, id is " + id1 + " outcome is " + outcome + " instead of 'valid'");
        // calling the factory method
        charmsRole = home.getCharmsRole();

        assert charmsRole.getName().equals("test1");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing role, id is " + id1 + " outcome is " + outcome + " instead of 'removed'");

        // --- lookup role 3
        outcome = home.setEntityId(id3.toString());
        Assert.assertEquals(outcome, "valid", "error setting userid for an existing user, id is " + id3 + " outcome is " + outcome + " instead of 'valid'");
        // calling the factory method
        charmsRole = home.getCharmsRole();

        assert charmsRole.getName().equals("test3");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed", "error removing user, id is " + id3 + " outcome is " + outcome + " instead of 'removed'");

        hibernateSession.clear();
        abstractEntityHome.clearInstance();
    }

    @Test
    public void testRenameRoleAction() {
        String outcome;
        final CharmsRoleActionBean home = (CharmsRoleActionBean) abstractEntityHome;

        // create user renametest1
        home.setEntityId(null);
        // calling the factory method
        CharmsRole charmsRole1 = home.getCharmsRole();
        charmsRole1.setName("renametest1");
        outcome = home.persist();
        final Long id1 = charmsRole1.getId();
        // the id should be set by the home object on persist
        Assert.assertNotNull(id1, "id shouldn't be null after persist");

        // create user renametest2
        home.setEntityId(null);
        // calling the factory method
        CharmsRole charmsRole2 = home.getCharmsRole();
        charmsRole2.setName("renametest2");
        outcome = home.persist();
        final Long id2 = charmsRole2.getId();
        // the id should be set be the home object on persist
        Assert.assertNotNull(id2, "id shouldn't be null after persist");

        // try to rename to a name that already exists
        home.setEntityId(id2.toString());
        charmsRole2 = home.getCharmsRole();
        Assert.assertEquals(charmsRole2.getName(), "renametest2");
        charmsRole2.setName("renametest1");
        outcome = home.update();
        Assert.assertEquals(outcome, "invalid"); // hit an invalid update
        Assert.assertEquals(id2, charmsRole2.getId());

        // the changed entity should stay in the home object if we
        // query it again this simulates hitting the back button on the home
        // page and selecteing the entity again
        // in the list view
        home.setEntityId(Long.toString(charmsRole2.getId()));
        charmsRole2 = home.getCharmsRole();
        // this should be the original name fresh from the database
        Assert.assertEquals(charmsRole2.getName(), "renametest2");
        home.cancel();

        // query a different entity to reset the home object
        home.setEntityId(Long.toString(charmsRole1.getId()));
        charmsRole1 = home.getCharmsRole();
        Assert.assertEquals(charmsRole1.getName(), "renametest1");
        home.cancel();

        // now get the entity to change again
        home.setEntityId(Long.toString(charmsRole2.getId()));
        charmsRole2 = home.getCharmsRole();
        // now we should have the old name from DB again
        Assert.assertEquals(charmsRole2.getName(), "renametest2");
        charmsRole2.setName("renametest3");
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        home.setEntityId(Long.toString(charmsRole1.getId()));
        charmsRole1 = home.getCharmsRole();
        Assert.assertEquals(charmsRole1.getName(), "renametest1");

        home.setEntityId(Long.toString(charmsRole2.getId()));
        charmsRole2 = home.getCharmsRole();
        Assert.assertEquals(charmsRole2.getName(), "renametest3");
        
        // delete the roles again
        
        home.setEntityId(Long.toString(charmsRole1.getId()));
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");

        home.setEntityId(Long.toString(charmsRole2.getId()));
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");

    }


    @Test
    public void testRemoveUser() {
        String outcome;
        final CharmsRoleActionBean home = (CharmsRoleActionBean) abstractEntityHome;
        
        hibernateSession.clear();
        hibernateSession.flush();

        home.clearInstance();
        // create user removetest1
        home.setEntityId(null);
        // calling the factory method
        CharmsRole charmsRole1 = home.getCharmsRole();
        charmsRole1.setName("removetest1");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        hibernateSession.flush();
        charmsRole1 = home.getCharmsRole();
        final Long id1 = charmsRole1.getId();
        Assert.assertNotNull(id1);

        // create two groups:
        CharmsUser charmsUser1 = new CharmsUser();
        charmsUser1.setName("removeUser1");
        charmsUser1.setFirstname("test1");
        charmsUser1.setLastname("test1");
        hibernateSession.persist(charmsUser1);
        hibernateSession.flush();
        charmsUser1 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", "removeUser1").uniqueResult();
        Assert.assertNotNull(charmsUser1);
        Assert.assertNotNull(charmsUser1.getId());
        
        CharmsUser charmsUser2 = new CharmsUser();
        charmsUser2.setName("removeUser2");
        charmsUser2.setFirstname("test2");
        charmsUser2.setLastname("test2");
        hibernateSession.persist(charmsUser2);
        hibernateSession.flush();
        charmsUser2 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", "removeUser2").uniqueResult();
        Assert.assertNotNull(charmsUser2);
        
        charmsRole1 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", "removetest1").uniqueResult();
        Assert.assertNotNull(charmsRole1);

        Assert.assertEquals(charmsRole1.getMemberships().size(), 0, "membership size should be zero");

        home.setEntityId(charmsRole1.getId().toString());
        List<CharmsUserItem> userItems = home.getAvailableUserItems();

        List<CharmsUserItem> selectedItems = new ArrayList<CharmsUserItem>();
        for (final CharmsUserItem item : userItems) {
            selectedItems.add(item);
        }

        // add them all:
        home.setSelectedUserItems(selectedItems);
        outcome  = home.update();
        Assert.assertEquals(outcome, "updated");

        charmsRole1 = home.getCharmsRole();
        Assert.assertEquals(charmsRole1.getMemberships().size(), userItems.size(), "wrong membership count");


        outcome  = home.setEntityId(charmsRole1.getId().toString());
        Assert.assertEquals(outcome, "valid");
        userItems = home.getSelectedUserItems();

        selectedItems = new ArrayList<CharmsUserItem>();
        for (final CharmsUserItem item : userItems) {
            selectedItems.add(item);
        }
        selectedItems.remove(0); // remove one item
        // set the minus one list
        home.setSelectedUserItems(selectedItems);
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");
        
        hibernateSession.flush();
        
        // remove what we created:
        
        charmsUser1 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", "removeUser1").uniqueResult();
        Assert.assertNotNull(charmsUser1);
        hibernateSession.delete(charmsUser1);
        hibernateSession.flush();
     
        charmsUser2 = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", "removeUser2").uniqueResult();
        Assert.assertNotNull(charmsUser2);
        hibernateSession.delete(charmsUser2);
        hibernateSession.flush();
       
        home.clearInstance();
        //charmsRole1 = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", "removetest1").uniqueResult();
        //Assert.assertNotNull(charmsRole1);
        outcome  = home.setEntityId(id1 + "");
        Assert.assertEquals(outcome, "valid");
        outcome  = home.remove();
        Assert.assertEquals(outcome, "removed");
        
        hibernateSession.flush();
        
    }

    
    @Test
    public void testContainingRoles2() {
        CharmsRole charmsRole;
        String outcome;
        final CharmsRoleActionBean home = (CharmsRoleActionBean) abstractEntityHome;
        
        // there is a bug when we add and remove all roles to/from a group, this 
        // test should trigger this bug if it still exists...

        // ---------- create testCont1
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid"); // "error setting roleid to nulll";
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont1");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        final Long id1 = home.getCharmsRole().getId();
        Assert.assertNotNull(id1, "persisted role has null id ");

        // ---------- create testCont2
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont2");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        final Long id2 = home.getCharmsRole().getId();
        Assert.assertNotNull(id2, "persisted role has null id ");

        // ---------- create testCont3
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont3");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        final Long id3 = home.getCharmsRole().getId();
        Assert.assertNotNull(id3, "persisted user has null id ");
        
        // pick up role1 and add all we got
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        CharmsRole charmsRole1 = home.getCharmsRole(); 
        // add all roles to this group
        home.getSelectedRoleItems().add(new CharmsRoleItem(id1, "some name"));
        home.getSelectedRoleItems().add(new CharmsRoleItem(id2, "some name"));
        home.getSelectedRoleItems().add(new CharmsRoleItem(id3, "some name"));
        home.update();
        
        hibernateSession.flush();
        hibernateSession.clear();
        
        
        
        // note we have circular refefrences here
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole1 = home.getCharmsRole(); 
        Assert.assertEquals(charmsRole1.getUpstream().size(), 3);     // 1-3
        // own role is downstream, but only if we refresh the session
        Assert.assertEquals(charmsRole1.getDownstream().size(), 1);   // own role as downstream
        Assert.assertEquals(charmsRole1.getContained().size(), 3);    // all

        
        // pick up role2 and add all we got
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        CharmsRole charmsRole2 = home.getCharmsRole(); 
        home.getSelectedRoleItems().add(new CharmsRoleItem(id1, "some name"));
        home.getSelectedRoleItems().add(new CharmsRoleItem(id2, "some name"));
        home.getSelectedRoleItems().add(new CharmsRoleItem(id3, "some name"));
        home.update();

        // note: we have circular references here
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole1 = home.getCharmsRole(); 
        Assert.assertEquals(charmsRole1.getUpstream().size(), 3);   // all
        Assert.assertEquals(charmsRole1.getDownstream().size(), 2); // 1 and 2 
        Assert.assertEquals(charmsRole1.getContained().size(), 3);  // all

        // note we have circular references here
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole2 = home.getCharmsRole(); 
        Assert.assertEquals(charmsRole2.getUpstream().size(), 3);   // all
        Assert.assertEquals(charmsRole2.getDownstream().size(), 2); // 1 and 2
        Assert.assertEquals(charmsRole2.getContained().size(), 3);  // all
        
        
        // removing might get us into trouble:
        
        // pick up role1 and remove the stuff
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole1 = home.getCharmsRole(); 
        // empty selection
        home.setSelectedRoleItems(new ArrayList<CharmsRoleItem>());
        home.update();
  
        
        // pick up role2 and remove the stuff
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole2 = home.getCharmsRole(); 
        // empty selection
        home.setSelectedRoleItems(new ArrayList<CharmsRoleItem>());
        home.update();
        
        //hibernateSession.flush();
        //hibernateSession.clear();
        
        // note: we have circular references here
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole1 = home.getCharmsRole(); 
        Assert.assertEquals(charmsRole1.getUpstream().size(), 0);   
        Assert.assertEquals(charmsRole1.getDownstream().size(), 0); 
        Assert.assertEquals(charmsRole1.getContained().size(), 0);  

        // note we have circular references here
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole2 = home.getCharmsRole(); 
        Assert.assertEquals(charmsRole2.getUpstream().size(), 0);  
        Assert.assertEquals(charmsRole2.getDownstream().size(), 0);
        Assert.assertEquals(charmsRole2.getContained().size(), 0);  
        
        
        // remove the roles
        outcome = home.setEntityId(id1 + "");
        Assert.assertEquals(outcome, "valid");  
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");  
        
        outcome = home.setEntityId(id2 + "");
        Assert.assertEquals(outcome, "valid");  
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");  

        outcome = home.setEntityId(id3 + "");
        Assert.assertEquals(outcome, "valid");  
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");  
        
        hibernateSession.flush();

    }


    @Test
    public void testContainingRoles1() {
        CharmsRole charmsRole;
        String outcome;
        final CharmsRoleActionBean home = (CharmsRoleActionBean) abstractEntityHome;
        
        // ids to delete:
        Long delId1;

        // ---------- create testCont1
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid"); // "error setting roleid to nulll";
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont1");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted"); 
        final Long id1 = home.getCharmsRole().getId();
        Assert.assertNotNull(id1, "persisted role has null id ");

        // ---------- create testCont2
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont2");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        final Long id2 = home.getCharmsRole().getId();
        Assert.assertNotNull(id2, "persisted role has null id ");

        // ---------- create testCont3
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont3");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        final Long id3 = home.getCharmsRole().getId();
        Assert.assertNotNull(id3, "persisted role has null id ");

        // ---------- create testCont4
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont4");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        final Long id4 = home.getCharmsRole().getId();
        Assert.assertNotNull(id4, "persisted role has null id ");

        // ---------- create testCont5
        outcome = home.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting roleid");
        // calling the factory method
        charmsRole = home.getCharmsRole();
        charmsRole.setName("testCont5");
        outcome = home.persist();
        Assert.assertEquals(outcome, "persisted");
        final Long id5 = home.getCharmsRole().getId();
        Assert.assertNotNull(id5, "persisted role has null id ");

        hibernateSession.flush();
        hibernateSession.clear();



        // --- adding role2 to role1
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        CharmsRole charmsRole1 = home.getCharmsRole(); 
        home.getSelectedRoleItems().add(new CharmsRoleItem(id2, "some name"));
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        hibernateSession.flush();
        hibernateSession.clear();

        charmsRole1 = (CharmsRole) hibernateSession.get(CharmsRole.class, id1);

        Assert.assertEquals(charmsRole1.getUpstream().size(), 1);
        Assert.assertEquals(charmsRole1.getDownstream().size(), 0);
        Assert.assertEquals(charmsRole1.getContained().size(), 1);


        // --- adding role3 to role2
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        CharmsRole charmsRole2 = home.getCharmsRole(); 
        home.getSelectedRoleItems().add(new CharmsRoleItem(id3, "some name"));
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        hibernateSession.flush();
        hibernateSession.clear();

        charmsRole2 = (CharmsRole) hibernateSession.get(CharmsRole.class, id2);

        Assert.assertEquals(charmsRole2.getUpstream().size(), 1);  // role3
        Assert.assertEquals(charmsRole2.getDownstream().size(), 1);  // role1
        Assert.assertEquals(charmsRole2.getContained().size(), 1);  // role3

        hibernateSession.flush();
        hibernateSession.clear();


        // -- check role 1 again, upstream should be role2, contained should be role2 and role3
        charmsRole1 = (CharmsRole) hibernateSession.get(CharmsRole.class, id1);

        Assert.assertEquals(charmsRole1.getUpstream().size(), 1);
        Assert.assertEquals(charmsRole1.getDownstream().size(), 0);
        Assert.assertEquals(charmsRole1.getContained().size(), 2);

        hibernateSession.flush();
        hibernateSession.clear();


        // --- remove role3 from role2
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole1 = home.getCharmsRole(); 
        home.getSelectedRoleItems().remove(0);
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        // -- check role 1 again, upstream should be role2, contained should be role2 
        charmsRole1 = (CharmsRole) hibernateSession.get(CharmsRole.class, id1);

        Assert.assertEquals(charmsRole1.getUpstream().size(), 1);
        Assert.assertEquals(charmsRole1.getDownstream().size(), 0);
        Assert.assertEquals(charmsRole1.getContained().size(), 1);


        hibernateSession.flush();
        hibernateSession.clear();

        // adding role 4,5 to role2
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole2 = home.getCharmsRole(); 
        home.getSelectedRoleItems().add(new CharmsRoleItem(id4, "some name"));
        home.getSelectedRoleItems().add(new CharmsRoleItem(id5, "some name"));
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");


        // -- check role 1 again, upstream should be role2, contained should be role2 , 4, 5
        charmsRole1 = (CharmsRole) hibernateSession.get(CharmsRole.class, id1);
        Assert.assertEquals(charmsRole1.getUpstream().size(), 1);  // role2
        Assert.assertEquals(charmsRole1.getDownstream().size(), 0);  // nothing
        Assert.assertEquals(charmsRole1.getContained().size(), 3);  // role2,4,5


        hibernateSession.flush();
        hibernateSession.clear();


        // adding role 4,5 to role3
        outcome = home.setEntityId(id3.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        CharmsRole charmsRole3 = home.getCharmsRole(); 
        home.getSelectedRoleItems().add(new CharmsRoleItem(id4, "some name"));
        home.getSelectedRoleItems().add(new CharmsRoleItem(id5, "some name"));
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        charmsRole3 = (CharmsRole) hibernateSession.get(CharmsRole.class, id3);    
        Assert.assertEquals(charmsRole3.getUpstream().size(), 2);    // role4, 5
        Assert.assertEquals(charmsRole3.getDownstream().size(), 0);  // nothing
        Assert.assertEquals(charmsRole3.getContained().size(), 2);   // role4, 5

        charmsRole2 = (CharmsRole) hibernateSession.get(CharmsRole.class, id2);    
        Assert.assertEquals(charmsRole2.getUpstream().size(), 2);    // role4, 5
        Assert.assertEquals(charmsRole2.getDownstream().size(), 1);  // role1
        Assert.assertEquals(charmsRole2.getContained().size(), 2);   // role4, 5

        hibernateSession.flush();
        hibernateSession.clear();

        // --- adding role3 to role2
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole2 = home.getCharmsRole(); 
        home.getSelectedRoleItems().add(new CharmsRoleItem(id3, "some name"));
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        hibernateSession.flush();
        hibernateSession.clear();


        charmsRole3 = (CharmsRole) hibernateSession.get(CharmsRole.class, id3);    
        Assert.assertEquals(charmsRole3.getUpstream().size(), 2);    // role4, 5
        Assert.assertEquals(charmsRole3.getDownstream().size(), 1);  // role2
        Assert.assertEquals(charmsRole3.getContained().size(), 2);   // role4, 5

        charmsRole2 = (CharmsRole) hibernateSession.get(CharmsRole.class, id2);    
        Assert.assertEquals(charmsRole2.getUpstream().size(), 3);    // role3, 4, 5
        Assert.assertEquals(charmsRole2.getDownstream().size(), 1);  // role1
        Assert.assertEquals(charmsRole2.getContained().size(), 3);   // role3, 4, 5


        hibernateSession.flush();
        hibernateSession.clear();

        charmsRole1 = (CharmsRole) hibernateSession.get(CharmsRole.class, id1);    
        Assert.assertEquals(charmsRole1.getUpstream().size(), 1); // role2
        Assert.assertEquals(charmsRole1.getDownstream().size(), 0);  // nothing
        Assert.assertEquals(charmsRole1.getContained().size(), 4); // role2, 3, 4, 5

        // remove role 5 from role 2
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        // calling the factory method
        charmsRole1 = home.getCharmsRole(); 
        CharmsRoleItem role5Item = null;
        List<CharmsRoleItem> roleItems = home.getSelectedRoleItems();
        for (CharmsRoleItem roleItem : roleItems) {
            if (roleItem.getValue().toString().equals(id5.toString())) {
                role5Item = roleItem;
            }
        }
        Assert.assertNotNull(role5Item);
        home.getSelectedRoleItems().remove(role5Item);
        outcome = home.update();
        Assert.assertEquals(outcome, "updated");

        hibernateSession.flush();
        hibernateSession.clear();

        charmsRole2 = (CharmsRole) hibernateSession.get(CharmsRole.class, id2);    
        Assert.assertEquals(charmsRole2.getUpstream().size(), 2);    // role3, 4
        Assert.assertEquals(charmsRole2.getDownstream().size(), 1);  // role1
        Assert.assertEquals(charmsRole2.getContained().size(), 3);   // role3, 4, 5 

        charmsRole3 = (CharmsRole) hibernateSession.get(CharmsRole.class, id3);    
        Assert.assertEquals(charmsRole3.getUpstream().size(), 2);    // role4, 5
        Assert.assertEquals(charmsRole3.getDownstream().size(), 1);  // role2
        Assert.assertEquals(charmsRole3.getContained().size(), 2);   // role4, 5

        charmsRole1 = (CharmsRole) hibernateSession.get(CharmsRole.class, id1);    
        Assert.assertEquals(charmsRole1.getUpstream().size(), 1); // role2
        Assert.assertEquals(charmsRole1.getDownstream().size(), 0);  // nothing
        Assert.assertEquals(charmsRole1.getContained().size(), 4); // role2, 3, 4, 5


        hibernateSession.clear();
        abstractEntityHome.clearInstance();
        
        // remove the roles:
        outcome = home.setEntityId(id1.toString());
        Assert.assertEquals(outcome, "valid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");
        
        outcome = home.setEntityId(id2.toString());
        Assert.assertEquals(outcome, "valid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");
        
        outcome = home.setEntityId(id3.toString());
        Assert.assertEquals(outcome, "valid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");
        
        outcome = home.setEntityId(id4.toString());
        Assert.assertEquals(outcome, "valid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");
        
        outcome = home.setEntityId(id5.toString());
        Assert.assertEquals(outcome, "valid");
        outcome = home.remove();
        Assert.assertEquals(outcome, "removed");
            
        hibernateSession.flush();
        hibernateSession.clear();
        abstractEntityHome.clearInstance();     
        
        /*
        Object role1 = hibernateSession.get(CharmsRole.class, id1);
        hibernateSession.delete(role1);
        Object role2 = hibernateSession.get(CharmsRole.class, id2);
        hibernateSession.delete(role2);
        Object role3 = hibernateSession.get(CharmsRole.class, id3);
        hibernateSession.delete(role3);
        Object role4 = hibernateSession.get(CharmsRole.class, id4);
        hibernateSession.delete(role4);
        Object role5 = hibernateSession.get(CharmsRole.class, id5);
        hibernateSession.delete(role5);
        */
        
    }

}
