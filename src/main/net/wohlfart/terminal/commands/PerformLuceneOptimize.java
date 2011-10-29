package net.wohlfart.terminal.commands;

import net.wohlfart.framework.search.FullTextSessionImpl;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformLuceneOptimize implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformLuceneOptimize.class);

    private static final String COMMAND_STRING = "lucene optimize";

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

        // see:
        // http://docs.jboss.org/hibernate/stable/search/reference/en/html_single/#search-mapping-bridge
        // about (re)initialization
        // FullTextSession fullTextSession =
        // Search.getFullTextSession(hibernateSession);
        final FullTextSession fullTextSession = new FullTextSessionImpl(hibernateSession);

        final Transaction tx = fullTextSession.getTransaction();
        tx.begin();

        fullTextSession.getSearchFactory().optimize();

        tx.commit();
        return "optimize done";
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": optimize lucene's search index database";
    }

}
