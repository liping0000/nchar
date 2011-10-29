package net.wohlfart.jbpm4;

import org.jboss.seam.annotations.Transactional;
import org.jbpm.api.cmd.Command;
import org.jbpm.pvm.internal.cmd.CommandService;
import org.jbpm.pvm.internal.env.EnvironmentFactory;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is used in the spring config as command execution service for the job
 * executor, note that we have to take care of the flush since our seam session
 * is configured to flush manually...
 * 
 * the following Commands get executed here:
 * org.jbpm.pvm.internal.cmd.ExecuteJobCmd
 * org.jbpm.pvm.internal.jobexecutor.AcquireJobsCmd
 * org.jbpm.pvm.internal.jobexecutor.GetNextDueDateCmd
 * 
 * Problem seems to be that the hibernate session is not thread save
 * 
 * @author Michael Wohlfart
 * 
 */
public class JobExecutionCommandService implements CommandService {

    private final static Logger LOGGER = LoggerFactory.getLogger(JobExecutionCommandService.class);

    private EnvironmentFactory  environmentFactory;

    /*      */
    @Override
    @Transactional
    public <T> T execute(final Command<T> command) {
        final EnvironmentImpl environment = environmentFactory.openEnvironment();
        // session is not thread safe:
        final DbSession dbSession = environment.get(DbSession.class);
        // final org.hibernate.Session databaseSession =
        // EnvironmentImpl.getFromCurrent(org.hibernate.Session.class);
        
        LOGGER.debug("executing command: {}", command);

        // databaseSession.setFlushMode(FlushMode.COMMIT);
        // databaseSession.setFlushMode(FlushMode.COMMIT);
        // databaseSession.setFlushMode(FlushMode.AUTO); // default flush mode
        try {
            return command.execute(environment);
        } catch (final Exception ex) {
            LOGGER.warn("exception while execution job service, flushing session and closing environment", ex);
        } finally {
            // session is not thread safe, we might be in deep shit here
            dbSession.flush();
            environment.close();
        }
        return null;
    }

    
    
    /**
     * simplified version picking up the command service from the environment
    @Override
    //@Transactional
    public <T> T execute(final Command<T> command) {
        final EnvironmentImpl environment = environmentFactory.openEnvironment();
        final DbSession dbSession = environment.get(DbSession.class);
        
        //final CommandService commandService = environment.get(CommandService.class);
        //final CommandService commandService = (CommandService) environment.get(CommandService.NAME_NEW_TX_REQUIRED_COMMAND_SERVICE);
        //final CommandService commandService = (CommandService) environment.get(CommandService.NAME_TX_REQUIRED_COMMAND_SERVICE);
        final CommandService commandService = (CommandService) environment.get("jobExecutionCommandService");
        
        try {
            return commandService.execute(command);
        } catch (final Exception ex) {
            LOGGER.warn("exception while execution job service, flushing session and closing environment", ex);
        } finally {
            dbSession.flush();
            environment.close();
        }
        return null;
    }
      */
  
    

    public void setEnvironmentFactory(final EnvironmentFactory environmentFactory) {
        this.environmentFactory = environmentFactory;
    }

    public EnvironmentFactory getEnvironmentFactory() {
        return environmentFactory;
    }

}
