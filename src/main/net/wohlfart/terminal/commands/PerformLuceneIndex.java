package net.wohlfart.terminal.commands;

import java.util.List;

import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.framework.search.FullTextSessionImpl;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformLuceneIndex implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformLuceneIndex.class);

    private static final String COMMAND_STRING = "lucene index";

    @Override
    public boolean canHandle(final String commandLine) {
        return StringUtils.startsWith(StringUtils.trim(commandLine), COMMAND_STRING);
    }

    // @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public String doHandle(final String commandLine) {
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        LOGGER.debug("hibernateSession is: {}", hibernateSession);

        if (hibernateSession == null) {
            LOGGER.warn("can't find EntityManager");
            return "can't find EntityManager for indexing";
        }

        // always true:
        // if (hibernateSession instanceof org.hibernate.Session) {
        // LOGGER.warn("instanceof org.hibernate.Session");
        // }

        if (hibernateSession instanceof org.hibernate.classic.Session) {
            LOGGER.warn("instanceof org.hibernate.classic.Session");
        }

        // hibernateSession.getSession(arg0)
        // see:
        // http://docs.jboss.org/hibernate/stable/search/reference/en/html_single/#search-mapping-bridge
        // about (re)initialization
        // there is a seam Problem: https://jira.jboss.org/browse/JBSEAM-2243
        // FullTextSession fullTextSession =
        // Search.getFullTextSession(hibernateSession);
        // workaround for:
        // https://forum.hibernate.org/viewtopic.php?f=1&t=983729&start=0
        // we need our own FullTextSession since the
        // org.hibernate.search.impl.FullTextSessionImpl constructor casts
        // to Session which doesn't work with injected seam component
        final FullTextSession fullTextSession = new FullTextSessionImpl(hibernateSession);
        //final FullTextSession fullTextSession = Search.getFullTextSession(hibernateSession);
        
        final Transaction tx = fullTextSession.getTransaction();
        tx.begin();
        fullTextSession.setFlushMode(org.hibernate.FlushMode.COMMIT);

        /*
         * 
         * this is for the next hibernate search release:
         * 
         * MassIndexer massIndexer = fullTextSession.createIndexer(); try {
         * massIndexer.startAndWait(); } catch (InterruptedException ex) {
         * return ex.toString(); }
         */

        fullTextSession.purgeAll(ChangeRequestData.class);
        // fullTextSession.purgeAll(ChangeRequestMessageEntry.class);
        // fullTextSession.purgeAll(CharmsDocument.class);
        // fullTextEntityManager.flushToIndexes();

        String status = "";
        status += indexDocuments(ChangeRequestData.class, fullTextSession);
        // status += indexDocuments(ChangeRequestMessageEntry.class,
        // fullTextSession);
        // status += indexDocuments(CharmsDocument.class, fullTextSession);

        fullTextSession.flushToIndexes();
        tx.commit();
        return status;
    }

    @SuppressWarnings("rawtypes")
    private String indexDocuments(final Class clazz, final FullTextSession fullTextSession) {

        // entityManager.getTransaction().begin();
        final List data = fullTextSession.createQuery("select data from " + clazz.getName() + " as data").list();

        if ((data == null) || (data.size() == 0)) {
            LOGGER.warn("can't find data, query result is: {}", data);
            return clazz.getName() + " - nothing found - ";
        }

        for (final Object d : data) {
            fullTextSession.index(d);
        }
        return clazz.getName() + " - indexed " + data.size() + " elements - ";
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": (re)index lucene's search index database";
    }

}
