package net.wohlfart.terminal.commands;

import java.util.Date;
import java.util.List;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
import net.wohlfart.changerequest.entities.MessageType;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * this class fixes the user id fields in the message entities
 * 
 * @author Michael Wohlfart
 * 
 */
public class PerformFixChangeRequestDataPes implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixChangeRequestDataPes.class);

    private static final String COMMAND_STRING = "fix changeRequestDataPes";

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

        // System.err.println("doTheFix called");

        final List<ChangeRequestData> list = hibernateSession.createQuery("from " + ChangeRequestData.class.getName()).list();

        // System.err.println("element count is:" + list.size());

        for (final ChangeRequestData data : list) {

            //System.err.println(" -- " + data.getProcessInstanceId() + " -- ChangeRequestData [" + data.getBusinessKey() + "]");

            final Long pid = data.getProcessInstanceId();

            final ChangeRequestMessageEntry rootEntry = (ChangeRequestMessageEntry) hibernateSession
                .getNamedQuery(ChangeRequestMessageEntry.FIND_ROOT_BY_PID)
                .setParameter("pid", pid)
                .uniqueResult();

            final List<ChangeRequestMessageEntry> children = rootEntry.getChildren();

            CharmsUser processUser = data.getProcessUser();
            Date processDate = data.getProcessDate();

            // get the last entry from the children
            final ChangeRequestMessageEntry entry = children.get(children.size() - 1);

            if (entry.getType() != null) {

                if (entry.getType().equals(MessageType.ASSIGN)) {
                    processUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", entry.getReceiverId()).uniqueResult();
                    processDate = entry.getTimestamp();

                    // finishing user
                } else if (entry.getType().equals(MessageType.FINISH)) {
                    processUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", entry.getAuthorId()).uniqueResult();
                    processDate = entry.getTimestamp();

                } else if (entry.getType().equals(MessageType.CANCEL)) {
                    processUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", entry.getAuthorId()).uniqueResult();
                    processDate = entry.getTimestamp();

                } else if (entry.getType().equals(MessageType.FORWARD)) {
                    processUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", entry.getReceiverId()).uniqueResult();
                    processDate = entry.getTimestamp();

                } else if (entry.getType().equals(MessageType.IMPLEMENT)) {
                    processUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", entry.getAuthorId()).uniqueResult();
                    processDate = entry.getTimestamp();

                } else if (entry.getType().equals(MessageType.HANDLE)) {
                    processUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", entry.getAuthorId()).uniqueResult();
                    processDate = entry.getTimestamp();

                } else if (entry.getType().equals(MessageType.REALIZE)) {
                    processUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", entry.getAuthorId()).uniqueResult();
                    processDate = entry.getTimestamp();

                } else {
                    System.err.println(" -- unhandled message type: " + entry.getType());
                }

            } // end if entry:type == null

            if (data.getProcessUser() == null) {
                data.setProcessUser(processUser);
                data.setProcessDate(processDate);
            } else {
                if (!data.getProcessUser().getId().equals(processUser.getId())) {
                    LOGGER.warn("inconsistent userid for process user is {}, trying to set to {}", data.getProcessUser().getId() + "("
                            + data.getProcessUser().getName() + ")", processUser.getId() + "(" + processUser.getName() + ")");
                }
            }

            hibernateSession.persist(data);
        }

    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": set PE ids in ChangeRequestData based on message entries";
    }

}
