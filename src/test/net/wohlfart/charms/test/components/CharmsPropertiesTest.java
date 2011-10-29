package net.wohlfart.charms.test.components;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.framework.properties.CharmsMementoState;
import net.wohlfart.framework.properties.CharmsProperty;
import net.wohlfart.framework.properties.CharmsPropertySet;
import net.wohlfart.framework.properties.CharmsPropertySetType;
import net.wohlfart.framework.properties.PropertiesManager;

import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CharmsPropertiesTest extends ComponentSessionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup a hibernate session
        hibernateSession = (Session) Component.getInstance("hibernateSession");
    }

    @Test
    public void testConstraints() {
        CharmsProperty prop;
        CharmsPropertySet set;

        prop = new CharmsProperty();
        prop.setName(null);
        prop.setValue(null);
        try {
            hibernateSession.persist(prop);
            Assert.fail("persisting a property without a name and value shouldn't be possible");
        } catch (final PropertyValueException ex) {
            // expected exception
        }

        prop = new CharmsProperty();
        prop.setName(null);
        prop.setValue("testvalue");
        try {
            hibernateSession.persist(prop);
            Assert.fail("persisting a property without a name shouldn't be possible");
        } catch (final PropertyValueException ex) {
            // expected exception
        }

        prop = new CharmsProperty();
        prop.setName("testprop");
        prop.setValue(null);
        try {
            hibernateSession.persist(prop);
            Assert.fail("persisting a property without a value shouldn't be possible");
        } catch (final PropertyValueException ex) {
            // expected exception
        }

        prop = new CharmsProperty();
        prop.setName("testprop");
        prop.setValue("testvalue");
        try {
            hibernateSession.persist(prop);
            Assert.fail("persisting a property without a set shouldn't be possible");
        } catch (final PropertyValueException ex) {
            // expected exception
        }

        hibernateSession.clear();

        set = new CharmsPropertySet();
        set.setName(null);
        set.setType(null);
        try {
            hibernateSession.persist(set);
            Assert.fail("persisting a property set without a name or type shouldn't be possible");
        } catch (final PropertyValueException ex) {
            // expected exception
        }

        set = new CharmsPropertySet();
        set.setName("name");
        set.setType(null);
        try {
            hibernateSession.persist(set);
            Assert.fail("persisting a property set without a type shouldn't be possible");
        } catch (final PropertyValueException ex) {
            // expected exception
        }

        set = new CharmsPropertySet();
        set.setName(null);
        set.setType(CharmsPropertySetType.USER);
        try {
            hibernateSession.persist(set);
            Assert.fail("persisting a property set without a name shouldn't be possible");
        } catch (final PropertyValueException ex) {
            // expected exception
        }

        hibernateSession.clear();

    }

    @Test
    public void testManager() {
        final String SIMPLE_NAME = "simplename";
        final String SIMPLE_VALUE = "simplevalue";

        CharmsProperty prop;
        CharmsPropertySet set;
        final PropertiesManager manager = new PropertiesManager();
        manager.init();

        set = manager.getApplicationProperties();
        Assert.assertNotNull(set, "the application properties set returned from the manager must not be null");
        prop = new CharmsProperty();
        prop.setName(SIMPLE_NAME);
        prop.setValue(SIMPLE_VALUE);
        prop.setPropertySet(set);
        manager.persistProperty(prop);

        hibernateSession.flush();

        set = manager.getApplicationProperties();
        final String value = set.getPropertyAsString(SIMPLE_NAME, null);
        Assert.assertEquals(value, SIMPLE_VALUE, "got wrong value from properties manager");

        hibernateSession.clear();
    }

    @Test
    public void testTableProperties() {
        final String SIMPLE_NAME = "anothername";

        final String TABLE_STATE_NAME = "tableStateName";

        CharmsPropertySet set;
        final PropertiesManager manager = new PropertiesManager();
        manager.init();

        set = manager.getApplicationProperties();
        Assert.assertNotNull(set, "the application properties set returned from the manager must not be null");

        CharmsMementoState state = new CharmsMementoState();
        state.put("fragment", "some fragment");
        state.put("sortColumn", "sorting");
        state.put("pageSize", 10);

        // check if the manager returns the default value:
        CharmsMementoState defaultState = set.getPropertyAsMementoState(TABLE_STATE_NAME, state);
        Assert.assertEquals(defaultState.getString("fragment"), state.getString("fragment"), "not returning default value");
        Assert.assertEquals(defaultState.getString("sortColumn"), state.getString("sortColumn"), "not returning default value");
        Assert.assertEquals(defaultState.getInt("pageSize"), state.getInt("pageSize"), "not returning default value");

        // check with the null value
        defaultState = set.getPropertyAsMementoState(TABLE_STATE_NAME, null);
        Assert.assertNull(defaultState);

        // persist the state
        manager.persistProperty(set, SIMPLE_NAME, state);
        hibernateSession.flush();

        set = manager.getApplicationProperties();
        Assert.assertTrue(set.hasKey(SIMPLE_NAME));
        state = set.getPropertyAsMementoState(SIMPLE_NAME, null);
        Assert.assertEquals(state.getString("fragment"), "some fragment", "not returning persisted value");
        Assert.assertEquals(state.getString("sortColumn"), "sorting", "not returning persisted value");
        Assert.assertEquals(state.getInt("pageSize"), Integer.valueOf(10), "not returning persisted value");
    }

    // test persist two peroperties with the same name

    @Test
    public void testSameNameProperty() {
        CharmsPropertySet set1;
        CharmsPropertySet set2;

        final CharmsUser charmsUser1 = new CharmsUser();
        charmsUser1.setName("login1");
        hibernateSession.persist(charmsUser1);

        final CharmsUser charmsUser2 = new CharmsUser();
        charmsUser2.setName("login2");
        hibernateSession.persist(charmsUser2);

        // flush to get ids
        hibernateSession.flush();

        final PropertiesManager manager = new PropertiesManager();
        manager.init();

        set1 = manager.getUserProperties(charmsUser1);
        set2 = manager.getUserProperties(charmsUser2);
        Assert.assertNotNull(set1, "user properties set returned from the manager must not be null");
        Assert.assertNotNull(set2, "user properties set returned from the manager must not be null");

        manager.persistProperty(set1, "commonName", "value1");
        manager.persistProperty(set2, "commonName", "value2");
        hibernateSession.flush();

    }
}
