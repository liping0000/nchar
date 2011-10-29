package net.wohlfart.terminal.commands;

import java.util.List;

import net.wohlfart.authentication.entities.CharmsUser;
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
 * this class fixes the user id fields in the message entities
 * 
 * @author Michael Wohlfart
 * 
 */
public class PerformFixMessageUserIds implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixMessageUserIds.class);

    private static final String COMMAND_STRING = "fix messageUserIds";

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

        final List<ChangeRequestMessageEntry> list = hibernateSession.createQuery("from " + ChangeRequestMessageEntry.class.getName()).list();

        LOGGER.debug("element count is {}", list.size());

        for (final ChangeRequestMessageEntry data : list) {

            if (data.getAuthorId() == null) {
                final String authorFullname = data.getAuthorFullname();
                final Long authorId = (Long) hibernateSession.getNamedQuery(CharmsUser.FIND_ID_BY_FULLNAME).setParameter("fullname", authorFullname)
                        .uniqueResult();
                final CharmsUser author = (CharmsUser) hibernateSession.get(CharmsUser.class, authorId);
                data.setAuthor(author);
            }

            if (data.getReceiverId() == null) {
                final String receiverFullname = data.getReceiverFullname();
                final Long receiverId = (Long) hibernateSession.getNamedQuery(CharmsUser.FIND_ID_BY_FULLNAME).setParameter("fullname", receiverFullname)
                        .uniqueResult();
                final CharmsUser receiver = (CharmsUser) hibernateSession.get(CharmsUser.class, receiverId);
                data.setReceiver(receiver);
            }

            hibernateSession.persist(data);
        }

    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": set user ids in message entries";
    }

}
