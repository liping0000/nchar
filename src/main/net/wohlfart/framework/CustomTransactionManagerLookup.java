package net.wohlfart.framework;

import java.util.Properties;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import net.wohlfart.jbpm4.CustomRepositoryService;

import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;
import org.jboss.seam.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * transaction manager lookup for ehcache, not working yet and not used yet
 * afaik
 * 
 * @author Michael Wohlfart
 * 
 */
public class CustomTransactionManagerLookup implements TransactionManagerLookup {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomTransactionManagerLookup.class);

    private TransactionManager transactionManager;

    /*
     * @Override public TransactionManager getTransactionManager() { if
     * (transactionManager == null) { TransactionManager transactionManager =
     * (TransactionManager) Component.getInstance("localTransactionManager");
     * System.out.println("getTransactionManager: " + transactionManager); }
     * System.out.println("getTransactionManager called: " +
     * transactionManager); return transactionManager; }
     * 
     * @Override public void register(EhcacheXAResource arg0) {
     * System.out.println("register called: " + arg0); }
     * 
     * @Override public void setProperties(Properties arg0) {
     * System.out.println("setProperties called: " + arg0); }
     * 
     * @Override public void unregister(EhcacheXAResource arg0) {
     * System.out.println("unregister called: " + arg0); }
     */

    @Override
    public TransactionManager getTransactionManager(final Properties props) throws HibernateException {
        if (transactionManager == null) {

            // problem is we can't cast
            // local transaction manager to javax.transaction.TransactionManager
            // and need to...

            transactionManager = (TransactionManager) Component.getInstance("localTransactionManager", true);
            LOGGER.info("getTransactionManager called, returning {}", transactionManager);
            //System.out.println("getTransactionManager: " + transactionManager);
        }
        //System.out.println("getTransactionManager called: " + transactionManager);
        return transactionManager;
    }

    @Override
    public String getUserTransactionName() {
        //System.out.println("getUserTransactionName");
        return null;
    }

    @Override
    public Object getTransactionIdentifier(final Transaction transaction) {
        //System.out.println("getTransactionIdentifier");
        return null;
    }

}
