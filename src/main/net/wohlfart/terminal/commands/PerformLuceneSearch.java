package net.wohlfart.terminal.commands;

import java.util.List;

import net.wohlfart.framework.search.FullTextSessionImpl;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.util.Version;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformLuceneSearch implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformLuceneSearch.class);

    private static final String COMMAND_STRING = "lucene search";

    @Override
    public boolean canHandle(final String commandLine) {
        return StringUtils.startsWith(StringUtils.trim(commandLine), COMMAND_STRING);
    }

    @SuppressWarnings("rawtypes")
    @Override
    @Transactional
    public String doHandle(final String commandLine) {
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        LOGGER.debug("hibernateSession is: {}", hibernateSession);

        if (hibernateSession == null) {
            LOGGER.warn("can't find EntityManager");
            return "can't find EntityManager for searching";
        }

        String searchExpression = StringUtils.substringAfter(commandLine, COMMAND_STRING);
        if (StringUtils.isEmpty(searchExpression)) {
            LOGGER.warn("search string is empty");
            return "search string is empty";
        }
        searchExpression = searchExpression.trim();

        // FullTextSession fullTextSession =
        // Search.getFullTextSession(hibernateSession);
        final FullTextSession fullTextSession = new FullTextSessionImpl(hibernateSession);

        try {
            // create native Lucene query
            final String[] fields = new String[] { "authorFullname", "content", "problemDescription", "receiverFullname", "title" };
            final MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_29, fields, new StandardAnalyzer(Version.LUCENE_29));
            parser.setAllowLeadingWildcard(true);
            final org.apache.lucene.search.Query query = parser.parse(searchExpression);

            // wrap Lucene query in a javax.persistence.Query
            final Query persistenceQuery = fullTextSession.createFullTextQuery(query /*
                                                                                      * ,
                                                                                      * Object
                                                                                      * .
                                                                                      * class
                                                                                      */);

            // execute search
            final List result = persistenceQuery.list();
            return "found " + result.size() + " elements in fields " + StringUtils.join(fields, ", ");

        } catch (final org.apache.lucene.queryParser.ParseException ex) {
            LOGGER.warn("error: {}", ex);
            return "error: " + ex.toString();
        }

    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + " &lt;expression&gt;: search lucene's index and return hitcount for &lt;expression&gt;";
    }

}
