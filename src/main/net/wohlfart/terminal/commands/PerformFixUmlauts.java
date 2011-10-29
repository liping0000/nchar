package net.wohlfart.terminal.commands;

import java.util.List;

import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
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
public class PerformFixUmlauts implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixUmlauts.class);

    private static final String COMMAND_STRING = "fix umlauts";

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

        fixChangeRequestData(hibernateSession);

        fixChangeRequestMessageEntry(hibernateSession);

        LOGGER.debug("flushing...");
        hibernateSession.flush();

        LOGGER.debug("committing...");
        hibernateSession.getTransaction().commit();

        return COMMAND_STRING + " done";
    }

    @SuppressWarnings("unchecked")
    private void fixChangeRequestMessageEntry(final Session hibernateSession) {

        final List<ChangeRequestMessageEntry> list = hibernateSession.createQuery("from " + ChangeRequestMessageEntry.class.getName()).list();

        LOGGER.debug("element count is {}", list.size());

        for (final ChangeRequestMessageEntry data : list) {
            String text;
            LOGGER.debug("update: {} ..", data);

            text = replaceUmlauts(data.getContent());
            data.setContent(text);

            LOGGER.debug("... updated: {}", data);
            hibernateSession.persist(data);
        }

    }

    @SuppressWarnings("unchecked")
    private void fixChangeRequestData(final Session hibernateSession) {

        final List<ChangeRequestData> list = hibernateSession.createQuery("from " + ChangeRequestData.class.getName()).list();

        LOGGER.debug("element count is {}", list.size());

        for (final ChangeRequestData data : list) {
            String text;
            LOGGER.debug("update: {} ..", data);

            text = replaceUmlauts(data.getProblemDescription());
            data.setProblemDescription(text);

            text = replaceUmlauts(data.getConclusionDescription());
            data.setConclusionDescription(text);

            text = replaceUmlauts(data.getProposalDescription());
            data.setProposalDescription(text);

            text = replaceUmlauts(data.getHistoryDescription());
            data.setHistoryDescription(text);

            LOGGER.debug("... updated: {}", data);
            hibernateSession.persist(data);
        }

    }

    private String replaceUmlauts(final String text) {
        String result = text;
        result = StringUtils.replace(result, "&auml;", "ä");
        result = StringUtils.replace(result, "&ouml;", "ö");
        result = StringUtils.replace(result, "&uuml;", "ü");
        result = StringUtils.replace(result, "&szlig;", "ß");
        result = StringUtils.replace(result, "&Auml;", "Ä");
        result = StringUtils.replace(result, "&Ouml;", "Ö");
        result = StringUtils.replace(result, "&Uuml;", "Ü");
        return result;
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": change umlauts from html entity to UTF-8 in ChangeRequest Data (descriptions, messages)";
    }

}
