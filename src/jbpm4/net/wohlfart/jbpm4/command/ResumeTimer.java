package net.wohlfart.jbpm4.command;

import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.job.TimerImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class ResumeTimer extends AbstractCommand<Void> {


    private final static Logger LOGGER = LoggerFactory.getLogger(ResumeTimer.class);

    private final Long          dbid;

    public ResumeTimer(final Long dbid) {
        this.dbid = dbid;
    }

    @Override
    public Void execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        final DbSession dbSession = environment.get(DbSession.class);
        final TimerImpl timer = dbSession.get(TimerImpl.class, dbid);
        timer.resume();
        dbSession.save(timer);
        return null;
    }
}
