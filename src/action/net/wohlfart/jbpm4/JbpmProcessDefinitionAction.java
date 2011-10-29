package net.wohlfart.jbpm4;

import org.jboss.seam.ScopeType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import net.wohlfart.AbstractActionBean;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jbpm.api.JbpmException;
import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.repository.DeploymentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class implements the methods to interact with a process definition
 * 
 * @author Michael Wohlfart
 */
@Scope(ScopeType.CONVERSATION)
@Name("jbpmProcessDefinitionAction")
public class JbpmProcessDefinitionAction extends AbstractActionBean implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmProcessDefinitionAction.class);

    // injecting a spring bean:
    @In(value = "processEngine")
    private ProcessEngine processEngine;

    private String processDefinitionId;

    // we have to buffer this since it might get changed, we don't need the image
    // since the image can not be changed by the user in any other way than uploading
    // a new process definition
    private String processDefinitionCode;

    private String pdId;

    private String pdKey;

    private Long pdVersion;

    private String name;

    private String languageId;

    public String setProcessDefinitionId(final String s) {
        LOGGER.debug("setting id called: >{}< old id is: >{}<", s, processDefinitionId);
        processDefinitionId = s;

        final CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();

        try {
            // we might get an exception here if the process definition is invalid
            processDefinitionCode = repositoryService.getProcessDefinitionCode(processDefinitionId);

            // FIXME: error checks before returning "valid"

            final HashMap<String, Object> properties = repositoryService.getProcessDefinitionProperties(processDefinitionId);

            setPdId((String) properties.get(DeploymentImpl.KEY_PROCESS_DEFINITION_ID));
            pdKey = (String) properties.get(DeploymentImpl.KEY_PROCESS_DEFINITION_KEY);
            pdVersion = (Long) properties.get(DeploymentImpl.KEY_PROCESS_DEFINITION_VERSION);
            name = (String) properties.get(CustomRepositoryService.PROCESS_DEFINITION_NAME);
            setLanguageId((String) properties.get(DeploymentImpl.KEY_PROCESS_LANGUAGE_ID));
        } catch( JbpmException ex) {
            LOGGER.warn("exception for process processDefinitionId {}, exception {}", processDefinitionId, ex);
        }

        return "valid";
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public byte[] getImage() {
        // get the repository service
        final CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        // get the image from the definition
        final byte[] graph = repositoryService.getProcessDefinitionGraph(processDefinitionId);
        return graph;
    }

    public List<ActivityImpl> getActivities() {
        // get the repository service
        final CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        final List<ActivityImpl> activities = repositoryService.getActivities(processDefinitionId);

        // get the data in the UI like this:
        // activities.get(0).getDbid(); <-- doesn't work for activities
        // activities.get(0).getName();
        // activities.get(0).getCoordinates().getHeight();
        // activities.get(0).getCoordinates().getWidth();
        // activities.get(0).getCoordinates().getX();
        // activities.get(0).getCoordinates().getY();
        return activities;
    }

    public String updateCode() {
        final CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        repositoryService.setProcessDefinitionCode(processDefinitionId, processDefinitionCode);
        return "updated";
    }


    public String remove() {
        LOGGER.warn("implement this method");
        return "invalid";
    }    


    // ------------ getters and setters ---------

    // he process definition id
    public void setPdId(final String pdId) {
        this.pdId = pdId;
    }

    public String getPdId() {
        return pdId;
    }

    // the process definition code
    public String getCode() {
        return processDefinitionCode;
    }

    public void setCode(final String definition) {
        processDefinitionCode = definition;
    }

    // the process definition key
    public void setPdKey(final String pdKey) {
        this.pdKey = pdKey;
    }

    public String getPdKey() {
        return pdKey;
    }

    // the process definition version
    public void setPdVersion(final Long pdVersion) {
        this.pdVersion = pdVersion;
    }

    public Long getPdVersion() {
        return pdVersion;
    }

    // the process definition name
    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // the language id
    public void setLanguageId(final String languageId) {
        this.languageId = languageId;
    }

    public String getLanguageId() {
        return languageId;
    }

}
