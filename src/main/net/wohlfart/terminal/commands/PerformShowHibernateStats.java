package net.wohlfart.terminal.commands;

import java.util.List;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.CustomHash;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformShowHibernateStats implements IRemoteCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(PerformShowHibernateStats.class);

    private static final String COMMAND_STRING = "show stats";

    @Override
    public boolean canHandle(final String commandLine) {
        return StringUtils.startsWith(StringUtils.trim(commandLine), COMMAND_STRING);
    }

    @Override
    @Transactional
    public String doHandle(final String commandLine) {
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        LOGGER.debug("hibernateSession is: {}", hibernateSession);

        String stats = hibernateSession.getStatistics().toString();

        return stats;
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + " : display the hibernate stats";
    }

}
