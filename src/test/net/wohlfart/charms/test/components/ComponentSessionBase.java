package net.wohlfart.charms.test.components;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.mock.SeamTest;
import org.jboss.seam.security.Identity;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class ComponentSessionBase extends SeamTest {

    protected Session hibernateSession;
    protected Transaction tx;
  
    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        Identity.setSecurityEnabled(false);
        Lifecycle.beginCall();
        Lifecycle.beginSession(session.getAttributes(), null);

        // setup a hibernate session
        hibernateSession = (Session) Component.getInstance("hibernateSession");

        tx = hibernateSession.beginTransaction();       
        Assert.assertTrue(tx.isActive());
    };

    
    @AfterMethod
    @Override
    public void end() {
        session.invalidate();
        Assert.assertTrue(tx.isActive());
        tx.rollback();
    };

}
