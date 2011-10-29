package net.wohlfart.jbpm4;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jbpm.api.IdentityService;
import org.jbpm.api.ProcessDefinition;
import org.jbpm.api.ProcessEngine;
import org.jbpm.api.RepositoryService;
import org.jbpm.pvm.internal.repository.DeploymentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.STATELESS)
@Name("jbpmSetup")
public class JbpmSetup {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmSetup.class);

    @Transactional
    public void startup() {
        final ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");
        LOGGER.info("startup called for JbpmSetup");
        final RepositoryService repositoryService = processEngine.getRepositoryService();
        LOGGER.info("RepositoryService is: " + repositoryService);
    }

    /**
     * 
     * 
     * 
     * @param processDefinition
     *            input stream of the process definition
     * @param processImage
     *            input stream of the process image
     * @param processName
     *            the name of the image/precessdef properties in the database,
     *            .png/.jpdl.xml will be appended, this is important since the
     *            ids are generated with this name
     * @param deploymentName
     *            description of the deployment, shown in the process list
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public void deployInputStreams(
            final InputStream processDefinition, 
            final InputStream processImage, 
            final String processName, 
            final String deploymentName) {

        final ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession");
        hibernateSession.flush();
        
        final RepositoryService repositoryService = processEngine.getRepositoryService();
        LOGGER.info("RepositoryService is: " + repositoryService 
                + " class is " + repositoryService.getClass());

        final IdentityService identityService = processEngine.getIdentityService();
        LOGGER.info("IdentityService is: " + identityService 
                + " class is " + identityService.getClass());

        // we have a custom Repository service
        final CustomRepositoryService customRepositoryService = (CustomRepositoryService) processEngine.getRepositoryService();

        // FIXME: use a common deployment component and share it with
        // the upload processdefinition page

        // FIXME: unify this with the upload page

        // Deploying a process
        // the backend deploys the data within the class ProcessDeployer
        // implements Deployer
        // which is scanning for .png endings in the added resources...
        final DeploymentImpl deployment = (DeploymentImpl) customRepositoryService.createDeployment();
        deployment.setName(deploymentName);
        deployment.setTimestamp(Calendar.getInstance().getTimeInMillis());

        boolean deploy = false;

        // InputStream processDefinition =
        // JbpmSetup.class.getResourceAsStream("ChangeRequest.jpdl.xml");
        // InputStream processImage =
        // JbpmSetup.class.getResourceAsStream("ChangeRequest.png");

        // upload definition
        if (processDefinition != null) {
            deployment.addResourceFromInputStream(processName + ".jpdl.xml", processDefinition);
            deploy = true;
        }

        if (processImage != null) {
            deployment.addResourceFromInputStream(processName + ".png", processImage);
            deploy = true;
        }

        // deployment.addResourceFromClasspath("net/wohlfart/jbpm4/ChangeRequest.jpdl.xml");
        // deployment.addResourceFromClasspath("net/wohlfart/jbpm4/ChangeRequest.png");

        String deploymentId = null;
        try {
            LOGGER.info("running deployment...");
            if (deploy) {
                deploymentId = deployment.deploy();
                LOGGER.debug("deployment.deploy() is: " + deploymentId);
                deploymentId = deployment.getId();
                LOGGER.debug("deployment.getId() is: " + deploymentId);

                // we need a flush here so the following query can find the
                // process definition
                hibernateSession.flush();

                // String processDefinitionId =
                // deployment.getProcessDefinitionId(processName);
                final List<ProcessDefinition> processDefinitionList = customRepositoryService.createProcessDefinitionQuery().list();
                LOGGER.debug("processDefinitionList is: " + processDefinitionList);
                String processDefinitionId = null;
                for (final ProcessDefinition definition : processDefinitionList) {
                    LOGGER.debug("definition.getDeploymentId() is: " + definition.getDeploymentId());
                    if (definition.getDeploymentId().equals(deploymentId)) {
                        processDefinitionId = definition.getId();
                        break;
                    }
                }

                // init the permissions
                final Set<String> resourcenames = deployment.getResourceNames();
                for (final String resourcename : resourcenames) {
                    // this is a bit hacky, the definition has a jpdl.xml ending
                    if (StringUtils.endsWith(resourcename, ".jpdl.xml")) {
                        LOGGER.debug("definition resourcename found in DB: " + resourcename);
                        // String definition = new
                        // String(deployment.getBytes(resourcename));

                        final List<CharmsPermissionTarget> permissionTargets = customRepositoryService
                        .getPermissionTargets(processDefinitionId);
                        // List<CharmsPermissionTarget> permissionTargets = new
                        // ProcessDefinitionWrapper(definition)
                        // .getPermissionTargets();

                        LOGGER.debug("number of permissionTargets found: {}", permissionTargets.size());
                        for (final CharmsPermissionTarget pTarget : permissionTargets) {
                            // FIXME: there is duplicate code in the setups
                            final List<CharmsPermissionTarget> permissionTargetList = hibernateSession
                            .getNamedQuery(CharmsPermissionTarget.FIND_BY_TARGET_STRING)
                            .setParameter("targetString", pTarget.getTargetString())
                            .list();

                            if (permissionTargetList.size() == 0) {
                                // persist since this permission target is not yet in the database
                                hibernateSession.persist(pTarget);
                                // we have to store all actions
                                for (final CharmsTargetAction action : pTarget.getActions()) {
                                    // save the target action, it is new... (all are new)
                                    hibernateSession.persist(action);
                                }
                            } else if (permissionTargetList.size() == 1) {
                                // the permission target is already in the database
                                final CharmsPermissionTarget dbTarget = permissionTargetList.get(0);
                                // get the names of the already stored actions
                                final List<String> dbNames = new ArrayList<String>();
                                for (final CharmsTargetAction action : dbTarget.getActions()) {
                                    dbNames.add(action.getName());
                                }

                                for (final CharmsTargetAction action : pTarget.getActions()) {
                                    if (!dbNames.contains(action.getName())) {
                                        dbTarget.addAction(action);
                                        // save the target action, it is new...
                                        hibernateSession.persist(action);
                                    }
                                }

                            } else {
                                LOGGER.error("multiple targets with same name found in database");
                            }
                        }
                        hibernateSession.flush();
                    }
                } // end for resources
                hibernateSession.flush();
            } else {
                LOGGER.info("no workflow package to deploy, you have to manually upload the process definition");
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            LOGGER.warn("error while deploying", ex);
        }
        if (deployment.hasErrors()) {
            LOGGER.warn("error deploying process definition: " + deployment.getJbpmException());
        } else if (deployment.hasProblems()) {
            LOGGER.warn("problem deploying process definition: " + deployment.getProblems());
        } else {
            LOGGER.info("deployment successful, id is: " + deploymentId);
        }
    }

    public boolean containsProcessDefinition(final String processDefinitionName) {
        final ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");        
        final RepositoryService repositoryService = processEngine.getRepositoryService();
        // RepositoryService repositoryService =
        // jbpmConfiguration.getRepositoryService();

        LOGGER.debug("check for processdefinition with name {}", processDefinitionName);

        final int count = repositoryService.createProcessDefinitionQuery()
            .processDefinitionName(processDefinitionName)
            .list()
            .size();
        LOGGER.debug("count for name is {}", count);

        // just for debugging
        final int totalCount = repositoryService.createProcessDefinitionQuery().list().size();
        LOGGER.debug("totalCount is {}", totalCount);

        return (count > 0);
    }
    
    
    

}
