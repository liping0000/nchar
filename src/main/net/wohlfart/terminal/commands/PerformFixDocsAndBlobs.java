package net.wohlfart.terminal.commands;

import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * this class fixes the html encoded umlauts in the ChangeRequestMessageEntrys
 * and ChangeRequestData
 * 
 * @author Michael Wohlfart
 * 
 */
public class PerformFixDocsAndBlobs implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixDocsAndBlobs.class);

    private static final String COMMAND_STRING = "fix docs";

    @Override
    public boolean canHandle(final String commandLine) {
        return StringUtils.startsWith(StringUtils.trim(commandLine), COMMAND_STRING);
    }

    @Override
    @Transactional
    public String doHandle(final String commandLine) {
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        LOGGER.debug("hibernateSession is: {}", hibernateSession);

        hibernateSession.getTransaction().begin();

        final String result = fixDocs(hibernateSession);

        LOGGER.debug("flushing...");
        hibernateSession.flush();

        LOGGER.debug("committing...");
        hibernateSession.getTransaction().commit();

        return COMMAND_STRING + " done [" + result + "]";
    }

    // @SuppressWarnings("unchecked")
    private String fixDocs(final Session hibernateSession) {

        // just remove any blob that is not refered to be a document,
        // not sure if subqueries are supported in all databases
        final int a = hibernateSession.createSQLQuery("delete from CHARMS_DOC_BLB where ID_ not in (select DOC_BLOB_ID_ from CHARMS_DOCUMENT)").executeUpdate();

        return a + "";

    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": change umlauts from html entity to UTF-8 in ChangeRequest Data (descriptions, messages)";
    }

}
