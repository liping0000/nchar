package net.wohlfart.workflow;

import java.io.Serializable;
import java.util.List;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.CustomRepositoryService;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jbpm.api.ProcessDefinition;
import org.jbpm.api.ProcessDefinitionQuery;
import org.jbpm.api.ProcessEngine;
import org.jbpm.api.RepositoryService;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.EVENT)
@Name("jbpmDispatcher")
public class Dispatcher extends AbstractActionBean implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(Dispatcher.class);

    // private static final String DEFAULT_PROCESS_DEFINITION_NAME =
    // "ChangeRequest";

    private static final String INVALID_VIEW_URL = "home";

    // the name of the directory where all the workflow subdirectories reside
    // this is relative to the servlet context
    private final StringBuffer WORKFLOW_DIR = new StringBuffer("/pages/wfl/");

    private final String HOME_URL = "/pages/user/home.html";

    // this send the user to the configured start or activity page depending on
    //
    // workflow id: pdid
    // task: ??
    //
    // there might be multiple start activities in future jbpm
    // implementations...


    @In(value = "authenticatedUser")
    private CharmsUser authenticatedUser;

    @In(value = "processEngine")
    // spring bean
    protected ProcessEngine processEngine;

    /**
     * this method is called to find the start task of a process instance, the
     * pdName must be set in order to find the process definition the url of the
     * start task for this process is returned, the process is not yet created
     * it is up to the page actions to start/create a process in the database
     * when the user entered valid data...
     * 
     * @return
     */
    @Transactional
    public String startProcess(final String processDefinitionName) {
        LOGGER.debug("calling dispatcher, processDefinitionName is: {}", processDefinitionName);

        // fallback to the default process definition name
        // if (pdName == null) {
        // pdName = DEFAULT_PROCESS_DEFINITION_NAME;
        // }

        String processDefinitionId = null;
        
        CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        processDefinitionId = repositoryService.getLatestProcessDefinitionId(processDefinitionName);
        
        
        
        /*
        // find the latest process definition id for the name
        final List<ProcessDefinition> list = processEngine.getRepositoryService()
            .createProcessDefinitionQuery()
            .processDefinitionName(pdName)
            .orderDesc(ProcessDefinitionQuery.PROPERTY_ID)
            .list();

        LOGGER.debug("found list of process definitions for {}, list is {} ", pdName, list);

        if ((list != null) && (list.size() > 0)) {
            processDefinitionId = list.get(0).getId();
            LOGGER.debug("trying to get processDefinitionId: {}", processDefinitionId);
        } else {
            LOGGER.error("result list for processdefinitions is empty!  pdName is: {}", pdName);
            return WORKFLOW_DIR.append(INVALID_VIEW_URL).toString();
        }
        */
        

        String form = null;
        if (processDefinitionId != null) {
            
            LOGGER.debug("processDefinitionId is: {}", processDefinitionId);

            final String activityName = getStartActivityName(processDefinitionId);
            LOGGER.debug("start activity name is: " + activityName);

            if (activityName != null) {
                form = processEngine.getRepositoryService().getStartFormResourceName(processDefinitionId, activityName);
                LOGGER.debug("form url is: {}", form);
                // the view id is the url sans the servlet base path
                return WORKFLOW_DIR.append(form).toString();
            } else {
                LOGGER.error("can't activityName for start activity,  processDefinitionName is: {}", processDefinitionName);
                return WORKFLOW_DIR.append(INVALID_VIEW_URL).toString();
            }
        } else {
            LOGGER.error("can't find id for process definition,  processDefinitionName is: {}", processDefinitionName);
            return WORKFLOW_DIR.append(INVALID_VIEW_URL).toString();
        }

    }

    /*
     * @Transactional public String doTask(Long taskDbid) {
     * LOGGER.debug("doTask called with taskDbid: " + taskDbid);
     * 
     * //TaskImpl task = (TaskImpl) hibernateSession.load(TaskImpl.class,
     * taskDbid); TaskImpl task =
     * (TaskImpl)processEngine.getTaskService().getTask(taskDbid.toString());
     * 
     * // FIXME: check if task is already assigned to the current actor
     * 
     * String form = task.getFormResourceName(); if (StringUtils.isEmpty(form))
     * { LOGGER.warn(
     * "form is empty for taskDbid in doTask: {} this is likely a programmers error"
     * , taskDbid); return HOME_URL; }
     * 
     * // the view id is the url sans the servlet base path return
     * WORKFLOW_DIR.append(form).toString(); }
     */

    // FIXME: merge with the doTask method and use only one of them...
    @Transactional
    public String doTask(final Long taskDbid) {
        LOGGER.debug("doTask called with taskDbid: {}", taskDbid);
        final FacesMessages facesMessages = FacesMessages.instance();

        // check if the task is available
        final TaskImpl task = (TaskImpl) processEngine.getTaskService().getTask(taskDbid.toString());
        if (task == null) {
            LOGGER.warn("can't figure out what task to do, the task is either not yet "
                    + "persisted or already performed, the taskDbid is {}, sending user to the home page", taskDbid);
            facesMessages.add(Severity.ERROR, "Die Aufgabe kann nicht gefunden werden, die Kennung der Aufgabe ist " + taskDbid);
            return HOME_URL;
        }

        // check if the task is asigned to a different user
        final String assigned = task.getAssignee();
        final String current = authenticatedUser.getActorId();
        if ((assigned != null) && (!assigned.equals(current))) {
            LOGGER.warn("the task is assigned to a different user, the current user is {} "
                    + " the user to which the task is assigned to is {}, the taskDbid is {}", new Object[] { current, assigned, taskDbid });
            facesMessages.add(Severity.ERROR, "Die Aufgabe ist einem Anderen Benutzer " + "zugewiesen (" + assigned + " != " + current + ")");
            return HOME_URL;
        }

        // FIXME: check if the current actor is a candidate for the task
        // and the task is not already taken

        final String form = task.getFormResourceName();
        if (StringUtils.isEmpty(form)) {
            LOGGER.warn("form is empty for taskDbid {}, sending the user to the home page ", taskDbid);
            facesMessages.add(Severity.ERROR, "es konnte keine Startseite für die Aufgabe gefunden werden");
            return HOME_URL;
        }

        // the view id is the url sans the servlet base path, the taskid param
        // comes from the pages.xml
        return WORKFLOW_DIR.append(form).toString();
    }

    @Transactional
    public String linked(final Long taskDbid) {
        LOGGER.debug("takeAndDoTask called with taskDbid: {}", taskDbid);

        // TaskImpl task = (TaskImpl) hibernateSession.load(TaskImpl.class,
        // taskDbid);
        final TaskImpl task = (TaskImpl) processEngine.getTaskService().getTask(taskDbid.toString());

        final FacesMessages facesMessages = FacesMessages.instance();

        if (task == null) {
            facesMessages.add(Severity.ERROR, "Die Aufgabe konnte nicht gefunden werden ( {} )", taskDbid);
            return HOME_URL;
        }

        final String assigned = task.getAssignee();
        final String current = authenticatedUser.getActorId();

        if ((assigned != null) && (!assigned.equals(current))) {
            facesMessages.add(Severity.ERROR, 
                    "Die Aufgabe ist einem Anderen Benutzer zugewiesen ({} <> {})", assigned, current);
            return HOME_URL;
        }

        // this has been moved to the action class in order to support adding a
        // message to the
        // entry tree when the user takes the task
        // if (assigned == null) {
        // processEngine.getTaskService().assignTask(taskDbid.toString(),
        // authenticatedUser.getActorId());
        // // facesMessages.add(Severity.ERROR,
        // "Die Aufgabe ist einem Anderen Benutzer zugewiesen");
        // // return HOME_URL;
        // }

        // jbpmConfiguration.getTaskService().assignTask(taskDbid.toString(),
        // authenticatedUser.getActorId());
        // task.setAssignee(authenticatedUser.getActorId());
        // // FIXME: check if the current actor is a candidate for the task
        // // and the task is not already taken
        // entityManager.flush();

        final String form = task.getFormResourceName();
        if (StringUtils.isEmpty(form)) {
            LOGGER.warn("form is empty for taskDbid in linked: [}", taskDbid);
            return HOME_URL;
        }

        // the view id is the url sans the servlet base path
        return WORKFLOW_DIR.append(form).toString();
    }

    @Transactional
    public String viewTask(final Long taskDbid) {
        LOGGER.debug("viewTask called with taskDbid: [}", taskDbid);

        // TaskImpl task = (TaskImpl) hibernateSession.load(TaskImpl.class,
        // taskDbid);
        final TaskImpl task = (TaskImpl) processEngine.getTaskService().getTask(taskDbid.toString());

        // FIXME: some kind of permission check for access to the task

        final String form = task.getFormResourceName();
        if (StringUtils.isEmpty(form)) {
            LOGGER.warn("form is empty for taskDbid in viewTask: [}", taskDbid);
            return HOME_URL;
        }

        // the view id is the url sans the servlet base path
        return WORKFLOW_DIR.append(form).toString();
    }

    @Transactional
    public String viewProcess(final Long procDbid) {
        // fixme: lookup workflow home dirs according to the start task form
        // maybe...
        return WORKFLOW_DIR.append("changerequest/home.html").toString();
    }

    // ------------ helpers -------------

    private String getStartActivityName(final String processDefinitionId) {
        LOGGER.debug("looking for start activity for process definition with id: {}", processDefinitionId);
        final List<String> startActivities = processEngine.getRepositoryService().getStartActivityNames(processDefinitionId);

        if (startActivities == null) {
            LOGGER.warn("no start activity found for processDefinitionId: {}" 
                    + " make sure there is a process definition with id: {}", 
                    processDefinitionId, processDefinitionId);
            return null;
        } else if (startActivities.size() == 0) {
            LOGGER.warn("no start activity found for pdId: {}" 
                    + " make sure there is a start activity in the process definition", 
                    processDefinitionId);
            return null;
        } else if (startActivities.size() > 1) {
            LOGGER.warn("we have multiple start activities for: {}" 
                    + " not sure which one to use for start: {}", 
                    processDefinitionId, startActivities);
            return null;
        } else {
            LOGGER.debug("we have exactly one start activity");
            final String start = startActivities.get(0);
            if (start == null) {
                LOGGER.warn("actName is null please name your start activity in the process definition");
                return null;
            } else {
                LOGGER.info("found start activity name: {}", 
                        start);
                return start;
            }
        }
    }

    //
    //
    //
    //
    // private String getFullTaskUrl(String relativePath) {
    // LOGGER.debug("FacesContext.getCurrentInstance().getExternalContext().getRequestServletPath() is: "
    // +
    // FacesContext.getCurrentInstance().getExternalContext().getRequestServletPath());
    // LOGGER.debug("FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() is: "
    // +
    // FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath());
    // LOGGER.debug("FacesContext.getCurrentInstance().getExternalContext().getRequestPathInfo() is: "
    // +
    // FacesContext.getCurrentInstance().getExternalContext().getRequestPathInfo());
    //
    // // we don't need context path if the redirect uses view-id instead of url
    // StringBuilder basePath = new
    // StringBuilder(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath());
    //
    // basePath.append(WORKFLOW_DIR);
    // if (!StringUtils.startsWith(relativePath, "/")) {
    // basePath.append("/");
    // }
    // basePath.append(relativePath);
    //
    // LOGGER.debug("base path is: " + basePath);
    // return basePath.toString();
    // }

    /**
     * the faces trace API uses the toString Method to display some information
     * about the components in the UI we need to make sure Seam's Bijection
     * doesn't kick in and gives us an exception
     */
    @Override
    @BypassInterceptors
    public String toString() {
        return super.toString();
    }

}
