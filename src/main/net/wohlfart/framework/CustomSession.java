package net.wohlfart.framework;

import net.wohlfart.authorization.queries.CharmsPermissionTargetTable;

import org.hibernate.Session;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.persistence.ManagedHibernateSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CustomSession /* extends ManagedHibernateSession */ {

    /*
    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsPermissionTargetTable.class);

    @Unwrap
    public Session getSession() throws Exception {
        Session session = super.getSession();
        LOGGER.debug(" thread: {} sessionHash: {} session: {}",
                    new Object[] {Thread.currentThread(), session.hashCode(), session});
        return session;
    }
    */
}
