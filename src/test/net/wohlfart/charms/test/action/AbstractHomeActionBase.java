package net.wohlfart.charms.test.action;

import net.wohlfart.framework.AbstractEntityHome;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.mock.SeamTest;
import org.jboss.seam.security.Identity;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractHomeActionBase extends SeamTest {

    /**
     * this is the component under test
     */
    @SuppressWarnings("rawtypes")
    protected AbstractEntityHome abstractEntityHome;

    protected Session hibernateSession;

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        Identity.setSecurityEnabled(false);
        Lifecycle.beginCall();
        Lifecycle.beginSession(session.getAttributes(), null);

        // setup a hibernate session for all subclasses to use
        hibernateSession = (Session) Component.getInstance("hibernateSession");

        // components under test needs setup in the subclass
    };

    @AfterMethod
    @Override
    public void end() {
        session.invalidate();
        // super.end();
    };
}
