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

public class PerformResetPasswords implements IRemoteCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(PerformResetPasswords.class);

    private static final String COMMAND_STRING = "reset passwords";

    @Override
    public boolean canHandle(final String commandLine) {
        return StringUtils.startsWith(StringUtils.trim(commandLine), COMMAND_STRING);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public String doHandle(final String commandLine) {
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        LOGGER.debug("hibernateSession is: {}", hibernateSession);

        String passwd = StringUtils.substringAfter(commandLine, COMMAND_STRING);
        if (StringUtils.isEmpty(passwd)) {
            LOGGER.warn("passwd string is empty");
            return "passwd string is empty";
        }

        passwd = passwd.trim();

        if (passwd.length() < 6) {
            LOGGER.warn("passwd string is too small");
            return "passwd string is empty";
        }

        LOGGER.warn("passwd string is >{}<", passwd);

        hibernateSession.getTransaction().begin();

        final List<CharmsUser> list = hibernateSession.createQuery("from " + CharmsUser.class.getName()).list();

        for (final CharmsUser user : list) {
            user.setPasswd(CustomHash.instance().generateSaltedHash(passwd, user.getName()));
            hibernateSession.persist(user);
        }

        LOGGER.debug("flushing...");
        hibernateSession.flush();

        LOGGER.debug("committing...");
        hibernateSession.getTransaction().commit();

        return COMMAND_STRING + " done";
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + " &lt;new password&gt; : reset all passwords (DON'T DO THIS IN PRODUCTION!!!)";
    }

}
