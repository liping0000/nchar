package net.wohlfart.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

// public class CustomTransactionManager extends HibernateTransactionManager /*
// implements TransactionManager */ {
// public class CustomTransactionManager extends JtaTransactionManager /*
// implements TransactionManager */ {

public class CustomTransactionManager extends HibernateTransactionManager {

    /**
     *
     */
    private static final long   serialVersionUID = 1L;
    private final static Logger LOGGER           = LoggerFactory.getLogger(CustomTransactionManager.class);

    @Override
    protected void doBegin(final Object object, final TransactionDefinition transactionDefinition) {
        LOGGER.debug("--- transaction begin --->>> " + object);
        super.doBegin(object, transactionDefinition);
    }

    @Override
    protected void doCommit(final DefaultTransactionStatus defaultTransactionStatus) {
        super.doCommit(defaultTransactionStatus);
        LOGGER.debug("<<<--- transaction commit ---");
    }

    @Override
    protected void doRollback(final DefaultTransactionStatus defaultTransactionStatus) {
        super.doRollback(defaultTransactionStatus);
        LOGGER.debug("<<<--- transaction rollback ---");
    }

}
