package net.wohlfart.jbpm4.command;

import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.repository.RepositoryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class SetDeploymentPropertyCmd extends AbstractCommand<Void> {


    private final static Logger LOGGER = LoggerFactory.getLogger(SetDeploymentPropertyCmd.class);

    Long                        deploymentId;
    String                      propertyName;
    String                      propertyStringValue;
    Long                        propertyLongValue;

    public SetDeploymentPropertyCmd(final Long deploymentId, final String propertyName, final String propertyStringValue) {
        this.deploymentId = deploymentId;
        this.propertyName = propertyName;
        this.propertyStringValue = propertyStringValue;
    }

    public SetDeploymentPropertyCmd(final Long deploymentId, final String propertyName, final Long propertyLongValue) {
        this.deploymentId = deploymentId;
        this.propertyName = propertyName;
        this.propertyLongValue = propertyLongValue;
    }

    @Override
    public Void execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        // we have the processDefinitionID and want the process definition
        // code from the deployment resouces

        // get the deployment id from the process definition
        // RepositorySession repositorySession =
        // environment.get(RepositorySession.class);
        // DeploymentImpl deployment =
        // repositorySession.getDeployment(Long.toString(deploymentId));

        System.err.println("fixme");

        // removing deployment from the cache
        // next time it's used, it will be reloaded
        final RepositoryCache repositoryCache = environment.get(RepositoryCache.class);
        repositoryCache.remove(Long.toString(deploymentId));

        return null;
    }

}
