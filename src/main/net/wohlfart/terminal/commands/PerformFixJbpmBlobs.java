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
public class PerformFixJbpmBlobs implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixJbpmBlobs.class);

    private static final String COMMAND_STRING = "fix jbpmblobs";

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

        final String a = fixJbpmBlobs(hibernateSession);

        LOGGER.debug("flushing...");
        hibernateSession.flush();

        LOGGER.debug("committing...");
        hibernateSession.getTransaction().commit();

        return COMMAND_STRING + " done [" + a + "]";
    }

    private String fixJbpmBlobs(final Session hibernateSession) {

        // just remove any blob that is not refered to be a document,
        // not sure if subqueries are supported in all databases
        final int a = hibernateSession.createSQLQuery("delete from JBPM4_LOB where DEPLOYMENT_ is null and NAME_ is null").executeUpdate();

        return a + "";

    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": fix jbpm blob anomalies, remove all blobs without name and deployment";
    }

}
