package net.wohlfart.jbpm4.command;

import java.util.HashMap;

import org.jbpm.api.JbpmException;
import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.repository.DeploymentImpl;
import org.jbpm.pvm.internal.repository.RepositoryCache;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.session.RepositorySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class SetProcessDefinitionPropertiesCmd extends AbstractCommand<Void> {


    private final static Logger LOGGER = LoggerFactory.getLogger(SetProcessDefinitionPropertiesCmd.class);

    String                      processDefinitionId;
    HashMap<String, Object>     processDefinitionProperties;

    public SetProcessDefinitionPropertiesCmd(final String processDefinitionId, final HashMap<String, Object> processDefinitionProperties) {
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionProperties = processDefinitionProperties;
    }

    @Override
    public Void execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        // we have the processDefinitionID and want the process definition
        // code from the deployment resouces

        // get the deployment id from the process definition
        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
        final String deploymentId = processDefinition.getDeploymentId();

        // get the deployment by id from hibernate
        final DbSession dbSession = environment.get(DbSession.class);
        final DeploymentImpl deployment = dbSession.get(DeploymentImpl.class, Long.parseLong(deploymentId));
        if (deployment == null) {
            throw new JbpmException("deployment " + deploymentId + " doesn't exist");
        }

        // String definitionResourceName = null;
        // Set<String> resourcenames = deployment.getResourceNames();
        // for (String resourcename:resourcenames) {
        // // this is a bit hacky, the image needs a png ending
        // if (StringUtils.endsWith(resourcename, ".jpdl.xml")) {
        // LOGGER.debug("definition resourcename found in DB: " + resourcename);
        // definitionResourceName = resourcename;
        // break;
        // }
        // }
        //
        // if (definitionResourceName != null) {
        // LOGGER.info("updating the process definition for {}",
        // definitionResourceName);
        // // replace the process definition
        // deployment.addResourceFromString(definitionResourceName,
        // processDefinitionCode);
        // session.persist(deployment);
        // session.flush();
        // } else {
        // LOGGER.warn("can't find resource name for process definition");
        // throw new
        // JbpmException("no process definition resource name found (*.jpdl.xml)");
        // }

        // removing deployment from the cache
        // next time it's used, it will be redeployed
        final RepositoryCache repositoryCache = environment.get(RepositoryCache.class);
        repositoryCache.remove(deploymentId);

        return null;
    }

}
