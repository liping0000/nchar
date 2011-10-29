package net.wohlfart.charms.test.components;

import net.wohlfart.jbpm4.entities.TransitionChoice;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * testing TransitionChoice and TransitionData with this class
 * 
 * @author Michael Wohlfart
 * 
 */
public class TransitionDetailsTest extends ComponentSessionBase {

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup a hibernate session
        hibernateSession = (Session) Component.getInstance("hibernateSession");
    }

    @AfterMethod
    @Override
    public void end() {
        super.end();
    }

    @Test
    public void testEmptyChoiceSave() {
        long tid;
        TransitionChoice choice;
        TransitionChoice result;

        tid = 20000L; // we don't want to hit one of the test tasks
        choice = new TransitionChoice();
        choice.setTid(tid);
        hibernateSession.save(choice);
        result = (TransitionChoice) hibernateSession.getNamedQuery(TransitionChoice.FIND_BY_TID).setParameter("tid", tid).uniqueResult();
        Assert.assertNull(result);
        hibernateSession.flush();
        result = (TransitionChoice) hibernateSession.getNamedQuery(TransitionChoice.FIND_BY_TID).setParameter("tid", tid).uniqueResult();
        Assert.assertNotNull(result);

        tid = 20001L;
        choice = new TransitionChoice();
        choice.setTid(tid);
        hibernateSession.save(choice);
        hibernateSession.flush();
        result = (TransitionChoice) hibernateSession.getNamedQuery(TransitionChoice.FIND_BY_TID).setParameter("tid", tid).uniqueResult();
        Assert.assertNotNull(result);

        result.getTransitions().put("trans1", new TransitionData());
        hibernateSession.persist(result);
    }

    @Test
    public void testEmptyChoicePersist() {
        final long tid = 20004L;
        TransitionData trans1;

        final TransitionChoice choice = new TransitionChoice();
        choice.setTid(tid);

        hibernateSession.persist(choice);
        TransitionChoice result = (TransitionChoice) hibernateSession.getNamedQuery(TransitionChoice.FIND_BY_TID).setParameter("tid", tid).uniqueResult();
        Assert.assertNull(result);

        hibernateSession.flush();
        result = (TransitionChoice) hibernateSession.getNamedQuery(TransitionChoice.FIND_BY_TID).setParameter("tid", tid).uniqueResult();
        Assert.assertNotNull(result);

        result.getTransitions().put("trans1", new TransitionData());
        result.getTransitions().put("trans2", new TransitionData());
        result.getTransitions().put("trans3", new TransitionData());
        result.getTransitions().put("trans4", new TransitionData());
        hibernateSession.flush();

        result = (TransitionChoice) hibernateSession.getNamedQuery(TransitionChoice.FIND_BY_TID).setParameter("tid", tid).uniqueResult();
        Assert.assertNotNull(result);

        final String testMessage = "testmessage";
        trans1 = result.getTransitions().get("trans1");
        Assert.assertNotNull(trans1);
        trans1.setMessage(testMessage);
        hibernateSession.flush();

        result = (TransitionChoice) hibernateSession.getNamedQuery(TransitionChoice.FIND_BY_TID).setParameter("tid", tid).uniqueResult();
        Assert.assertNotNull(result);

        trans1 = result.getTransitions().get("trans1");
        Assert.assertNotNull(trans1);
        Assert.assertEquals(trans1.getMessage(), testMessage);

    }

}
