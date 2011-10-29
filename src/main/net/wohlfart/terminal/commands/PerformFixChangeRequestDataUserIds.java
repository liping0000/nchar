package net.wohlfart.terminal.commands;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
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
public class PerformFixChangeRequestDataUserIds implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixChangeRequestDataUserIds.class);

    private static final String COMMAND_STRING = "fix changeRequestDataUserIds";

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

    @SuppressWarnings({ "unchecked", "null" })
    private void doTheFix(final Session hibernateSession) {

        // System.err.println("doTheFix called");

        final List<ChangeRequestData> list = hibernateSession.createQuery("from " + ChangeRequestData.class.getName()).list();

        // System.err.println("element count is:" + list.size());

        for (final ChangeRequestData data : list) {

            // System.err.println(" -- " + data.getProcessInstanceId() + " -- ChangeRequestData [" + data.getBusinessKey() + "]");

            Date takeDate = null;
            Long takeUserId = null;
            CharmsUser takeUser = null;
            Integer takeSort = null;
            ChangeRequestMessageEntry processEntry = null;

            final Long pid = data.getProcessInstanceId();

            final ChangeRequestMessageEntry rootEntry = (ChangeRequestMessageEntry) hibernateSession.getNamedQuery(ChangeRequestMessageEntry.FIND_ROOT_BY_PID)
                    .setParameter("pid", pid).uniqueResult();
            // System.err.println("found root: " + rootEntry);
            // System.err.println("  child count: " +
            // rootEntry.getChildren().size());

            final List<ChangeRequestMessageEntry> children = rootEntry.getChildren();

            final Iterator<ChangeRequestMessageEntry> iter = children.iterator();
            Boolean hasTake = false;
            final ChangeRequestMessageEntry first = null;
            int position = 0;
            while (iter.hasNext()) {
                position++;
                final ChangeRequestMessageEntry e = iter.next();

                System.err.println(" -- " + pid + " entry type: " + e.getType() + " entry author: " + e.getAuthorId() + " data business key: "
                        + data.getBusinessKey());

                final MessageType type = e.getType();
                if (MessageType.TAKE.equals(type)) {
                    hasTake = true;
                }

                if (MessageType.PROCESS.equals(type)) {
                    takeDate = e.getTimestamp();
                    final GregorianCalendar calendar = new GregorianCalendar();
                    calendar.setTime(takeDate);
                    calendar.add(Calendar.MINUTE, -5);
                    takeDate = calendar.getTime();

                    takeUserId = e.getAuthorId();

                    takeUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", takeUserId).uniqueResult();

                    takeSort = position;
                    if (processEntry != null) {
                        System.err.println("!!! error, already found a process entry !!!");
                    }
                    processEntry = e;
                }
            }
            if ((!hasTake) && (processEntry != null)) {
                System.err.println(" --> create take for: " + takeDate + " takeUserId: " + takeUserId);

                final ChangeRequestMessageEntry entry = new ChangeRequestMessageEntry();
                entry.setTitle("TQM is taking task"); // FIXME: still needed
                                                      // after the message
                                                      // approach?
                entry.setType(MessageType.TAKE);
                entry.setTimestamp(takeDate);
                entry.setAuthor(takeUser);
                rootEntry.getChildren().add(takeSort - 1, entry);
                entry.setParent(rootEntry);
                entry.setProcessInstanceId(rootEntry.getProcessInstanceId());
                hibernateSession.persist(rootEntry);
                hibernateSession.persist(entry);
                hasTake = true;
            } else if (hasTake) {
                // System.err.println(" --> found take for " +
                // data.getBusinessKey());
            } else {
                System.err.println(" --> check, no take, no process, (must be discarded) " + data.getBusinessKey());
            }

            // we try to fix the following data:
            final CharmsUser initiateUser = data.getInitiateUser();
            final CharmsUser submitUser = data.getSubmitUser();
            final CharmsUser assignUser = data.getAssignUser();
            final CharmsUser processUser = data.getProcessUser();
            final CharmsUser implementUser = data.getImplementUser();
            final CharmsUser finishUser = data.getFinishUser();

            // the search is going after:
            // * submittingUserName (Submitter)
            // * assigningUserName (TQM)
            // * processingUserName (PE)
            //

            final List<ChangeRequestMessageEntry> entries = hibernateSession.getNamedQuery(ChangeRequestMessageEntry.FIND_CURRENT_BY_EID).setParameter("eid", pid)
                    .list();

            for (final ChangeRequestMessageEntry entry : entries) {

                if (entry.getType() != null) {

                    // initial user safe
                    if (entry.getType().equals(MessageType.INITIAL_SAVE)) {
                        final CharmsUser charmsUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID)
                                .setParameter("id", entry.getAuthorId()).uniqueResult();
                        if (initiateUser == null) {
                            data.setInitiateUser(charmsUser);
                        } else {
                            if (!initiateUser.getId().equals(charmsUser.getId())) {
                                LOGGER.warn("inconsistent userid for initiate user is {}, trying to set to {}",
                                        initiateUser.getId() + "(" + initiateUser.getName() + ")", charmsUser.getId() + "(" + charmsUser.getName() + ")");
                            }
                        }

                        // user submitted
                    } else if (entry.getType().equals(MessageType.SUBMIT)) {
                        final CharmsUser charmsUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID)
                                .setParameter("id", entry.getAuthorId()).uniqueResult();
                        if (submitUser == null) {
                            data.setSubmitUser(charmsUser);
                        } else {
                            if (!submitUser.getId().equals(charmsUser.getId())) {
                                LOGGER.warn("inconsistent userid for submit user is {}, trying to set to {}", submitUser.getId() + "(" + submitUser.getName()
                                        + ")", charmsUser.getId() + "(" + charmsUser.getName() + ")");
                            }
                        }

                        // TQM takes the job, we assume noone used a forward yet
                    } else if (entry.getType().equals(MessageType.TAKE)) {
                        final CharmsUser charmsUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID)
                                .setParameter("id", entry.getAuthorId()).uniqueResult();
                        if (assignUser == null) {
                            data.setAssignUser(charmsUser);
                        } else {
                            if (!assignUser.getId().equals(charmsUser.getId())) {
                                LOGGER.warn("inconsistent userid for assign user is {}, trying to set to {}", assignUser.getId() + "(" + assignUser.getName()
                                        + ")", charmsUser.getId() + "(" + charmsUser.getName() + ")");
                            }
                        }

                    } else if (entry.getType().equals(MessageType.ASSIGN)) {
                        final CharmsUser charmsUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID)
                                .setParameter("id", entry.getAuthorId()).uniqueResult();
                        if (assignUser == null) {
                            data.setAssignUser(charmsUser);
                        } else {
                            if (!assignUser.getId().equals(charmsUser.getId())) {
                                LOGGER.warn("inconsistent userid for assign user is {}, trying to set to {}", assignUser.getId() + "(" + assignUser.getName()
                                        + ")", charmsUser.getId() + "(" + charmsUser.getName() + ")");
                            }
                        }

                        // finishing user
                    } else if (entry.getType().equals(MessageType.FINISH)) {
                        final CharmsUser charmsUser = (CharmsUser) hibernateSession.getNamedQuery(CharmsUser.FIND_BY_ID)
                                .setParameter("id", entry.getAuthorId()).uniqueResult();
                        if (data.getFinishDate() != null) {
                            if (finishUser == null) {
                                data.setFinishUser(charmsUser);
                            } else {
                                if (!finishUser.getId().equals(charmsUser.getId())) {
                                    LOGGER.warn("inconsistent userid for finish user is {}, trying to set to {}",
                                            finishUser.getId() + "(" + finishUser.getName() + ")", charmsUser.getId() + "(" + charmsUser.getName() + ")");
                                }
                            }
                        } else {
                            System.err.println(" --> check, finish task, no finish date " + data.getBusinessKey());
                        }
                    } else if (entry.getType().equals(MessageType.PROCESS)) {
                        // change process to assign

                        /*
                         * CharmsUser charmsUser = (CharmsUser) hibernateSession
                         * .getNamedQuery(CharmsUser.FIND_BY_ID)
                         * .setParameter("id", entry.getAuthorId())
                         * .uniqueResult(); if (processUser == null) {
                         * data.setProcessUser(charmsUser); } else { if
                         * (!processUser.getId().equals(charmsUser.getId())) {
                         * LOGGER.warn(
                         * "inconsistent userid for process user is {}, trying to set to {}"
                         * , processUser.getId() + "(" + processUser.getName() +
                         * ")", charmsUser.getId() + "(" + charmsUser.getName()
                         * + ")"); } }
                         */

                        // rename process to assign:
                        entry.setType(MessageType.ASSIGN);
                        hibernateSession.persist(entry);
                    }

                } // end if entry:type == null
            } // end for

            hibernateSession.persist(data);
        }

    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": set user ids in ChangeRequestData based on message entries";
    }

}
