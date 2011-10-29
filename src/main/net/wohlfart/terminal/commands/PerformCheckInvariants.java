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
 * set the process id in ChangeRequestMessageEntrys
 * 
 * @author Michael Wohlfart
 * 
 */
public class PerformCheckInvariants implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformCheckInvariants.class);

    private static final String COMMAND_STRING = "check invariants";

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

        String result = "";

        result += checkCount(hibernateSession, "select count(*) from CHARMS_WFL_DATA", "this is the count of all running workflows (any workflow definition)");

        result += checkCount(hibernateSession, "select count(*) from CHREQ_DATA", "this is the count of all ChangeRequest workflows");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_WFL_DATA where BUSINESS_KEY_ is null",
                "this is the count of all workflows without business key (open drafts, canceled drafts)");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_WFL_DATA where BUSINESS_KEY_ is null and FINISH_DATE_ is null",
                "this is the count of all workflows without business key (open drafts)");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_WFL_DATA where BUSINESS_KEY_ is null and FINISH_DATE_ is not null",
                "this is the count of all ended workflows without business key (canceled drafts)");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_WFL_DATA where BUSINESS_KEY_ is not null",
                "this is the count of all workflows with business key (submited workflows)");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_WFL_DATA where BUSINESS_KEY_ is not null and FINISH_DATE_ is null",
                "this is the count of all workflows with business key and no finish date (submited workflows, not ended)");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_WFL_DATA where BUSINESS_KEY_ is not null and FINISH_DATE_ is not null",
                "this is the count of all workflows with business key and finish date (submited workflows, ended)");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_DOC_BLB", "this is the count of all blobs in the database");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_DOCUMENT",
                "this is the count of all documents in the database, should match the blobs");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_DOCUMENT where DOC_BLOB_ID_ not in (select ID_ from CHARMS_DOC_BLB)",
                "documents without blobs");

        result += checkCount(hibernateSession, "select count(*) from CHARMS_DOC_BLB where ID_  not in (select DOC_BLOB_ID_ from CHARMS_DOCUMENT)",
                "blobs without documents");

        LOGGER.debug("flushing...");
        hibernateSession.flush();
        LOGGER.debug("committing...");
        hibernateSession.getTransaction().commit();
        return result;
    }

    private String checkCount(final Session hibernateSession, final String countSql, final String comment) {
        final int countResult = ((Number) hibernateSession.createSQLQuery(countSql).uniqueResult()).intValue();
        return countSql + " --> " + countResult + "   " + comment + "<br />";
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": application health check";
    }

}
