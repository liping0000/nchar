package net.wohlfart.terminal.commands;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformCreateUsers implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformCreateUsers.class);

    private static final String COMMAND_STRING = "create users";

    @Override
    public boolean canHandle(final String commandLine) {
        return StringUtils.startsWith(StringUtils.trim(commandLine), COMMAND_STRING);
    }

    // @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public String doHandle(final String commandLine) {
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        LOGGER.debug("hibernateSession is: {}", hibernateSession);

        String param = StringUtils.substringAfter(commandLine, COMMAND_STRING);
        if (StringUtils.isEmpty(param)) {
            LOGGER.warn("param string is empty");
            return "param string is empty";
        }

        param = param.trim();
        final int count = Integer.parseInt(param);

        final Long max = (Long) hibernateSession.createQuery("select count(*) from CharmsUser").uniqueResult();

        for (int i = 0; i < count; i++) {
            final CharmsUser user = new CharmsUser();
            user.setLastname("last-" + max + "-" + i);
            user.setFirstname("first-" + max + "-" + i);
            user.setPasswd("xxxxxxx");
            user.setEmail("xxxxxxx@" + max + "." + i + ".xx");
            user.setName("" + max + "xxx" + i); // no special chars in name
                                                // allowed
            user.setEnabled(true);
            hibernateSession.persist(user);
        }

        hibernateSession.flush();
        hibernateSession.clear();
        return "done";
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": create testusers";
    }

}
