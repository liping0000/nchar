package net.wohlfart.terminal.commands;

import java.util.List;

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
 * set the process id in ChangeRequestMessageEntrys
 * 
 * @author Michael Wohlfart
 * 
 */
public class PerformFixProcessId implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixProcessId.class);

    private static final String COMMAND_STRING = "fix procid";

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

        doTheFix(hibernateSession);

        LOGGER.debug("flushing...");
        hibernateSession.flush();

        LOGGER.debug("committing...");
        hibernateSession.getTransaction().commit();

        return COMMAND_STRING + " done";
    }

    @SuppressWarnings("unchecked")
    private void doTheFix(final Session hibernateSession) {

        final List<ChangeRequestMessageEntry> list = hibernateSession.createQuery(
                "from " + ChangeRequestMessageEntry.class.getName() + " where processInstanceId is null").list();

        LOGGER.debug("element count is {}", list.size());

        for (final ChangeRequestMessageEntry data : list) {
            LOGGER.debug("update: {} ..", data);

            final Long processInstanceId = getProcessInstanceId(data);
            data.setProcessInstanceId(processInstanceId);

            LOGGER.debug("... updated: {}", data);
            hibernateSession.persist(data);
        }

    }

    /**
     * recursively retrieve the process instance id
     * 
     * @param data
     * @return
     */
    private Long getProcessInstanceId(final ChangeRequestMessageEntry data) {

        if (data.getProcessInstanceId() != null) {
            return data.getProcessInstanceId();
        }
        if (data.getParent() == null) {
            LOGGER.warn("no parent and no process instance id for message with id {}", data.getId());
            return null;
        }
        return getProcessInstanceId(data.getParent());
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": set the process id in ChangeRequestMessageEntrys";
    }

}
