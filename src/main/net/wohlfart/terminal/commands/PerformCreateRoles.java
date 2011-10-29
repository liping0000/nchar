package net.wohlfart.terminal.commands;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformCreateRoles implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformCreateRoles.class);

    private static final String COMMAND_STRING = "create roles";

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
        final Transaction tx = hibernateSession.getTransaction();
        LOGGER.debug("tx.isActive(): {}", tx.isActive());

        String param = StringUtils.substringAfter(commandLine, COMMAND_STRING);
        if (StringUtils.isEmpty(param)) {
            LOGGER.warn("param string is empty");
            return "param string is empty";
        }

        param = param.trim();
        final int count = Integer.parseInt(param);

        final Long max = (Long) hibernateSession.createQuery("select count(*) from CharmsRole").uniqueResult();

        for (int i = 0; i < count; i++) {
            final CharmsRole role = new CharmsRole();
            role.setName("role-" + max + "-" + i);
            hibernateSession.persist(role);
        }

        hibernateSession.flush();
        hibernateSession.clear();
        return "done";
    }

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": create testgroups";
    }

}
