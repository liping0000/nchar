package net.wohlfart.charms.test.action;

import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.ITranslateable;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractTranslateableActionBase extends AbstractHomeActionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
    };

    @Test
    public void testTranslations() {
        String outcome;
        Object entity;

        // flush to have a clean session
        hibernateSession.flush();

        outcome = abstractEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityId to null for creatig a new entity");
        entity = abstractEntityHome.getInstance(); // calling the factory method
        Assert.assertNotNull(entity, "new entity must not be null");
        hibernateSession.flush();

        outcome = abstractEntityHome.persist();
        Assert.assertEquals(outcome, "invalid", "persisting a translatable entity without default name shouldn't be possible");

        hibernateSession.flush();
        hibernateSession.clear();
        abstractEntityHome.clearInstance(); 

        @SuppressWarnings("rawtypes")
        final TranslateableHome translateableEntityHome = (TranslateableHome) abstractEntityHome;

        outcome = translateableEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid", "error setting entityId to null for creatig a new entity");
        // calling the factory method
        final ITranslateable translatableEntity = translateableEntityHome.getInstance();
        Assert.assertNotNull(translatableEntity, "new entity must not be null");

        translatableEntity.setDefaultName("test");
        outcome = translateableEntityHome.persist();
        Assert.assertEquals(outcome, "persisted", "can't persist a translateable with default name set");
        ITranslateable instance = translateableEntityHome.getInstance();
        Assert.assertNotNull(instance, "persisted instance is null");
        
        hibernateSession.flush();
        hibernateSession.clear();

        // now remove the entity to clean the database
        outcome = translateableEntityHome.setEntityId(instance.getId()+ "");
        Assert.assertEquals(outcome, "valid", "error setting entityId to " + instance.getId() + " for reading an entity");
        outcome = translateableEntityHome.remove();
        Assert.assertEquals(outcome, "removed", "can't remove a translateable with default name set");
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
