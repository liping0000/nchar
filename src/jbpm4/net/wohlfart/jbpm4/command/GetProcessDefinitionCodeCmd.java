package net.wohlfart.jbpm4.command;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
public class GetProcessDefinitionCodeCmd extends AbstractCommand<String> {


    private final static Logger LOGGER = LoggerFactory.getLogger(GetProcessDefinitionCodeCmd.class);

    String                      processDefinitionId;

    public GetProcessDefinitionCodeCmd(final String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String execute(final Environment environment) throws Exception {
        LOGGER.debug("running execute method");
        // we have the processDefinitionID and want the process definition
        // code from the deployment resouces

        // get the deployment id from the process definition
        final RepositorySession repositorySession = environment.get(RepositorySession.class);
        final ProcessDefinitionImpl processDefinition = repositorySession.findProcessDefinitionById(processDefinitionId);
        // FIXME: we get a nullpointer here on the ajax pull call
        final String deploymentId = processDefinition.getDeploymentId();

        // get the deployment by id from hibernate
        final DbSession dbSession = environment.get(DbSession.class);
        final DeploymentImpl deployment = dbSession.get(DeploymentImpl.class, Long.parseLong(deploymentId));
        if (deployment == null) {
            LOGGER.warn("can't find deployment with deploymentid {}", deploymentId);
            throw new JbpmException("deployment " + deploymentId + " doesn't exist");
        }

        String definitionResourceName = null;
        final Set<String> resourcenames = deployment.getResourceNames();
        for (final String resourcename : resourcenames) {
            // this is a bit hacky, the code needs a ".jpdl.xml" ending
            if (StringUtils.endsWith(resourcename, ".jpdl.xml")) {
                LOGGER.debug("definition resourcename found in DB: " + resourcename);
                definitionResourceName = resourcename;
                break;
            }
        }

        // read the process definition from the resources of the deployment
        String result = null;
        if (definitionResourceName != null) {
            LOGGER.info("getting the process definition for {}", definitionResourceName);
            result = new String(deployment.getBytes(definitionResourceName));
        } else {
            LOGGER.warn("can't find resource name for process definition");
            throw new JbpmException("no process definition resource name found (*.jpdl.xml)");
        }

        return result;

    }

}
