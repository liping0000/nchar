package net.wohlfart.jbpm4.command;

import java.util.HashMap;

import net.wohlfart.jbpm4.CustomRepositoryService;

import org.jbpm.api.JbpmException;
import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.cmd.AbstractCommand;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.repository.DeploymentImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.session.RepositorySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class GetProcessDefinitionPropertiesCmd extends AbstractCommand<HashMap<String, Object>> {


    private final static Logger LOGGER = LoggerFactory.getLogger(GetProcessDefinitionPropertiesCmd.class);

    String                      processDefinitionId;

    public GetProcessDefinitionPropertiesCmd(final String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public HashMap<String, Object> execute(final Environment environment) throws Exception {
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

        final HashMap<String, Object> properties = new HashMap<String, Object>();

        final String id = deployment.getProcessDefinitionId(processDefinition.getName());
        properties.put(DeploymentImpl.KEY_PROCESS_DEFINITION_ID, id);
        final String key = deployment.getProcessDefinitionKey(processDefinition.getName());
        properties.put(DeploymentImpl.KEY_PROCESS_DEFINITION_KEY, key);
        final Long version = deployment.getProcessDefinitionVersion(processDefinition.getName());
        properties.put(DeploymentImpl.KEY_PROCESS_DEFINITION_VERSION, version);
        final String language = deployment.getProcessLanguageId(processDefinition.getName());
        properties.put(DeploymentImpl.KEY_PROCESS_LANGUAGE_ID, language);

        properties.put(CustomRepositoryService.PROCESS_DEFINITION_NAME, processDefinition.getName());

        // FIXME: check what this is doing:
        // processDefinition.getProperty(key)

        return properties;
    }

}
