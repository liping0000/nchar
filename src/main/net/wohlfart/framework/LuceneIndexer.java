package net.wohlfart.framework;

import java.util.List;

import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.framework.search.FullTextSessionImpl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class LuceneIndexer {

    private final static Logger LOGGER = LoggerFactory.getLogger(LuceneIndexer.class);

    /**
     * this bean uses the spring transactions!
     * 
     * @param sessionFactory
     */
    @Transactional
    public void initLuceneIndex(final SessionFactory sessionFactory) {
        LOGGER.info("init lucene index ");

        // open session gives us no transaction:
        // Session hibernateSession = sessionFactory.openSession();

        final Session hibernateSession = sessionFactory.getCurrentSession();
        LOGGER.debug("hibernateSession is {}", hibernateSession);
        
        
         // "object is not an instance of declaring class" here:
         //FullTextSession fullTextSession = Search.getFullTextSession(hibernateSession);       
         // we need to implement our own fulltext session as workarround:
        final FullTextSession fullTextSession = new FullTextSessionImpl(hibernateSession);
        // Transaction tx = fullTextSession.getTransaction();
        try {
            // tx.begin();
            fullTextSession.setFlushMode(org.hibernate.FlushMode.COMMIT);
            //fullTextSession.
            //fullTextSession.purgeAll(ChangeRequestData.class);
            indexDocuments(ChangeRequestData.class, fullTextSession);
            fullTextSession.flushToIndexes();
            // tx.commit();
        } catch (final Exception ex) {
            LOGGER.warn("failed to init lucene index ", ex);
            fullTextSession.clear();
            // tx.rollback();
        }

    }

    @SuppressWarnings("rawtypes")
    private void indexDocuments(final Class clazz, final FullTextSession fullTextSession) {

        // entityManager.getTransaction().begin();
        final List data = fullTextSession.createQuery("select data from " + clazz.getName() + " as data").list();

        if ((data == null) || (data.size() == 0)) {
            LOGGER.info("can't find data, query result is: {} for class {}, this is normal for an empty database", data, clazz);
            return;
        }

        LOGGER.debug("about to index {} elements", data.size());
        for (final Object d : data) {
            fullTextSession.index(d);
        }
        LOGGER.debug("finished lucene indexing");
    }

}
