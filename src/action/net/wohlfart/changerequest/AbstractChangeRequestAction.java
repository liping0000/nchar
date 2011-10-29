package net.wohlfart.changerequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestCostSheet;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestFolder;
import net.wohlfart.changerequest.entities.ChangeRequestImpactSheet;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
import net.wohlfart.changerequest.entities.MessageType;
import net.wohlfart.framework.IllegalParameterException;
import net.wohlfart.framework.entities.CharmsDocument;
import net.wohlfart.framework.entities.CharmsFolder;
import net.wohlfart.framework.queries.ProcessTaskTable;
import net.wohlfart.framework.search.FullTextSessionImpl;
import net.wohlfart.jbpm4.CustomRepositoryService;
import net.wohlfart.jbpm4.entities.TransitionChoice;
import net.wohlfart.jbpm4.entities.TransitionData;
import net.wohlfart.jbpm4.node.TransitionConfig;
import net.wohlfart.terminal.commands.PerformFixExecutionVariables;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jbpm.api.TaskService;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.processengine.ProcessEngineImpl;
import org.jbpm.pvm.internal.svc.ExecutionServiceImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.StringUtils;

/**
 * this component has to be initialized by a page action, there are two ways to
 * do this
 * 
 * - when starting a process/workflow we don't have a task in the database and
 * don't want eagerly create one since the user might decide not to start the
 * process anyways so we have to put all data into the conversation and be ready
 * to lazyly create the task/process and assign all data to it as soon as the
 * user decides to start the workflow this is done like this * call init():
 * creates all the data we need and initialized the conversation with the data,
 * this is done by a page action, * this method calls setupConversation() to set
 * the variables in the conversation * on submit/start call
 * startProcessInstance(pdKey): this assigns the data with the process instance
 * setting process instance ids in the data container so we can find them later
 * and putting the data in the process context as variables * todo: we migh need
 * to add some facet data into the execution before the first task is created
 * this way we can have a "assign-on-start" feature
 * 
 * - when a process or execution is running already and we have a human task
 * that is picked by a user we have to setup a conversation with the
 * task/execution/process/UI data * call setTaskDbid(string) to set the id of
 * the task to work on this method resolves the variables from withing the
 * task/executon scope * this method calls setupConversation() to set the
 * variables in the conversation
 * 
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class AbstractChangeRequestAction extends AbstractActionBean implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractChangeRequestAction.class);

    @In(value = "processEngine") // spring bean
    protected ProcessEngineImpl processEngine;

    @In(value = "hibernateSession")
    protected FullTextSession hibernateSession;

    @In(value = "authenticatedUser")
    protected CharmsUser authenticatedUser;

    // the taskid the user is working on right now
    protected String taskDbid;

    // needed in case we want to query the next task in an execution
    protected String executionId;

    // this is the data that is stored in the process
    protected ChangeRequestData         changeRequestData;
    protected ChangeRequestFolder       changeRequestFolder;
    protected ChangeRequestCostSheet    changeRequestCostSheet;
    protected ChangeRequestImpactSheet  changeRequestImpactSheet;
    // the first entry is the root of the tree
    protected ChangeRequestMessageEntry changeRequestMessageTree;                                           
    // this is the data that is stored in the execution
    protected ChangeRequestMessageEntry currentMessageEntry;

    // this is the data that is stored in the task context
    protected TransitionChoice          transitionChoice;

    // move this out to a seperate class:
    protected ProcessTaskTable          processTaskTable;

    /**
     * this is called by a page action from the startProcess page for
     * initializing a process instance, the data is persisted when the task is
     * completed
     */
    @Transactional
    public void init() {
        LOGGER.debug("running init method");
        // charmsWorkflowData = new CharmsWorkflowData();
        changeRequestData = new ChangeRequestData();
        changeRequestFolder = new ChangeRequestFolder();
        // there is no subprocess yet :-/
        processTaskTable = new ProcessTaskTable();
        changeRequestMessageTree = new ChangeRequestMessageEntry();
        // we start with the root entry...
        currentMessageEntry = changeRequestMessageTree; // = new
        // ChangeRequestMessageEntry();
        // changeRequestFacetState = new ChangeRequestFacetState();
        // initFacetStateDates();
        transitionChoice = new TransitionChoice();

        changeRequestCostSheet = new ChangeRequestCostSheet();
        // load the default content
        changeRequestCostSheet.initContent();

        changeRequestImpactSheet = new ChangeRequestImpactSheet();
        // load the default content
        changeRequestImpactSheet.initContent();

        // store the data in the conversation
        setupConversation();
    }

    /**
     * we change the taskDbid on the fly on task signaling in order to jump to
     * the next page right away...
     * 
     * @return
     */
    public String getTaskDbid() {
        LOGGER.debug("getting taskDbid is: {}", taskDbid);
        return taskDbid;
    }

    /**
     * this method is called before page rendering for any workflow task by a
     * page action
     * 
     * @param taskDbid
     * @throws IllegalParameterException
     */
    @Transactional
    public void setTaskDbid(final String taskDbid) throws IllegalParameterException {
        if (StringUtils.isEmptyOrWhitespaceOnly(taskDbid)) {
            LOGGER.error("invalid taskDbid, is empty or whitespaceonly: >" + taskDbid + "<");
            throw new IllegalParameterException("Sorry, can't find the task for you");
        }
        LOGGER.debug("setting taskDbid to: >" + taskDbid + "<");

        final TaskService taskService = processEngine.getTaskService();
        final TaskImpl task = (TaskImpl) taskService.getTask(taskDbid);

        // might be a fake taskid
        if (task == null) {
            throw new IllegalParameterException("Sorry, can't find the task for you, task is null");
        }

        if (task.isCompleted()) {
            LOGGER.warn("task is already completed: {}", taskDbid);
            throw new IllegalParameterException("Sorry, task is already completed");
        }

        if (task.isSuspended()) {
            LOGGER.warn("task is suspended: {}", taskDbid);
            throw new IllegalParameterException("Sorry, task is suspended");
        }

        // just reload the task table since we might have send a review or handle request...
        // the tasktable for the current execution instance
        processTaskTable = new ProcessTaskTable(); // creating a task table
        // without a fab doesn't give us the seam interceptors!!
        processTaskTable.setTaskInstanceId(task.getDbid());
        Contexts.getConversationContext().set(ProcessTaskTable.PROCESS_TASK_TABLE, processTaskTable);

        // check if we are already set up (this might be a redirect to the same
        // page after a review request or something like this),
        // no need to bother the DB or to loose any nonsaved facet states in
        // this case
        if (!taskDbid.equals(this.taskDbid)) {
            setNewTaskDbid(taskDbid);
        }
    }

    protected void setNewTaskDbid(final String taskDbid) {
        this.taskDbid = taskDbid;
        // load the data part for this process instance...

        final TaskService taskService = processEngine.getTaskService();
        final TaskImpl task = (TaskImpl) taskService.getTask(taskDbid);
        final Long processInstanceId = task.getProcessInstance().getDbid();
        LOGGER.info("processInstanceId: {} taskDbid: {}", processInstanceId, taskDbid);

        // find and create the stuff for the conversation context
        changeRequestData = (ChangeRequestData) hibernateSession.getNamedQuery(ChangeRequestData.FIND_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();
        changeRequestFolder = (ChangeRequestFolder) hibernateSession.getNamedQuery(ChangeRequestFolder.FIND_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();
        changeRequestCostSheet = (ChangeRequestCostSheet) hibernateSession.getNamedQuery(ChangeRequestCostSheet.FIND_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();
        changeRequestImpactSheet = (ChangeRequestImpactSheet) hibernateSession.getNamedQuery(ChangeRequestImpactSheet.FIND_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();
        changeRequestMessageTree = (ChangeRequestMessageEntry) hibernateSession.getNamedQuery(ChangeRequestMessageEntry.FIND_ROOT_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();

        // the variables get deleted as soon as the execution is finished!
        final ExecutionImpl execution = task.getExecution();
        currentMessageEntry = (ChangeRequestMessageEntry) execution.getVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE);
        if (currentMessageEntry == null) {
            // this is a fallback in case we can't find the variable we are looking for
            try {
            currentMessageEntry = (ChangeRequestMessageEntry) hibernateSession
                .getNamedQuery(ChangeRequestMessageEntry.FIND_CURRENT_BY_EID)
                .setParameter("eid", execution.getDbid())
                .uniqueResult(); // its not always unique here!!   
            } catch (Exception ex) {
                LOGGER.warn("nonunique message entry found");
            }
        }
        if (currentMessageEntry == null) {
            throw new IllegalArgumentException("can't find the message entry, the execution data are most likely corrupted,"
                    + " run '" + PerformFixExecutionVariables.COMMAND_STRING + "' ");
        }

        // let's see if this works:
        transitionChoice = (TransitionChoice) task.getVariable(TransitionChoice.TRANSITION_CHOICE);
        if (transitionChoice == null) {
            LOGGER.debug("creating a new transition choice since we can't find one attached to the current task");
            transitionChoice = new TransitionChoice();
            transitionChoice.setTid(task.getDbid());
            // there will be a variable referring to this transitionchoice
            // object assigned to the current task
        }

        // get the data for the choices
        final String activityName = task.getActivityName();
        final CustomRepositoryService customRepositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        final String processDefinitionId = task.getProcessInstance().getProcessDefinitionId();
        final Set<String> facets = customRepositoryService.getFacetStrings(processDefinitionId, activityName);

        // add any missing transition data
        for (final String facet : facets) {
            if (!transitionChoice.hasData(facet)) {
                LOGGER.debug("name for new transition data is: >" + facet + "<");
                final TransitionData transitionData = new TransitionData();
                transitionData.setTransitionName(facet);
                transitionData.setTransitionChoice(transitionChoice);
                transitionChoice.getTransitions().put(transitionData.getTransitionName(), transitionData);
            }
        }

        // add all configuration data, configuration data is not persisted in
        // the database
        // so this needs to be done for each transition
        for (final String facet : facets) {
            final TransitionData transitionData = transitionChoice.getData(facet);
            // facet is the name of the transition unless its a term
            // transition which terminates the execution instance...
            final TransitionConfig transitionConfig = customRepositoryService.getTransitionConfig(processDefinitionId, activityName, facet);
            if (transitionConfig == null) {
                LOGGER.info("config for transition with name {} is null" + " we don't add any configuration data", facet);
            } else {
                transitionData.setConfig(transitionConfig);
            }
        }


        // check if we own the task
        if ((task.getAssignee() != null) && (task.getAssignee().equalsIgnoreCase(authenticatedUser.getActorId()))) {
            LOGGER.debug("owning the task already");
        } else {
            if (task.getAssignee() == null) {
                LOGGER.debug("task owner is null, taking the task now");
                takeTask();
            } else if (!task.getAssignee().equalsIgnoreCase(authenticatedUser.getActorId())) {
                LOGGER.warn("task is owned by another user, assignee is {}", task.getAssignee());
                takeTask();
            } else {
                LOGGER.debug("taking the task now");
                takeTask();
            }
        }

        // store the data in the conversation
        setupConversation();
    }

    protected void setupConversation() {
        // from the process context 
        Contexts.getConversationContext().set(ChangeRequestData.CHANGE_REQUEST_DATA, changeRequestData);
        Contexts.getConversationContext().set(ChangeRequestFolder.CHANGE_REQUEST_FOLDER, changeRequestFolder);
        Contexts.getConversationContext().set(ChangeRequestCostSheet.CHANGE_REQUEST_COSTSHEET, changeRequestCostSheet);
        Contexts.getConversationContext().set(ChangeRequestImpactSheet.CHANGE_REQUEST_IMPACTSHEET, changeRequestImpactSheet);
        Contexts.getConversationContext().set(ChangeRequestMessageEntry.CHANGE_REQUEST_MESSAGE_TREE, changeRequestMessageTree);
        // from the execution context
        Contexts.getConversationContext().set(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE, currentMessageEntry);
        // from the task context
        Contexts.getConversationContext().set(TransitionChoice.TRANSITION_CHOICE, transitionChoice);
    }

    /**
     * this pushes the data for the process instance from variable status to
     * process scoped seam component, note we still have data for the task
     * 
     * this method is only called from initializeProcess at startup of a new
     * process instance...
     */
    protected ExecutionImpl startProcessInstance(final String processDefinitionKey) {

        // the user is serious about the changerequest, persist the data now
        hibernateSession.persist(changeRequestData);
        hibernateSession.persist(changeRequestFolder);
        hibernateSession.persist(changeRequestCostSheet);
        hibernateSession.persist(changeRequestImpactSheet);
        hibernateSession.persist(changeRequestMessageTree);
        // storeDocuments(changeRequestFolder);

        // push the data into the process context
        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(ChangeRequestData.CHANGE_REQUEST_DATA, changeRequestData);
        map.put(ChangeRequestFolder.CHANGE_REQUEST_FOLDER, changeRequestFolder);
        map.put(ChangeRequestCostSheet.CHANGE_REQUEST_COSTSHEET, changeRequestCostSheet);
        map.put(ChangeRequestImpactSheet.CHANGE_REQUEST_IMPACTSHEET, changeRequestImpactSheet);
        map.put(ChangeRequestMessageEntry.CHANGE_REQUEST_MESSAGE_TREE, changeRequestMessageTree);
        // in the process instance (root execution) the message entry actually
        // equals the root node of the message tree
        // in any subexecution we have a different messageEntry and no
        // changeRequestMessageTree
        map.put(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE, changeRequestMessageTree);

        final ExecutionServiceImpl executionService = (ExecutionServiceImpl) processEngine.getExecutionService();
        final ExecutionImpl processInstance = (ExecutionImpl) executionService.startProcessInstanceByKey(processDefinitionKey, map);

        // link the process instance to the data container
        changeRequestData.setProcessInstanceId(processInstance.getDbid());
        changeRequestFolder.setProcessInstanceId(processInstance.getDbid());
        changeRequestCostSheet.setProcessInstanceId(processInstance.getDbid());
        changeRequestImpactSheet.setProcessInstanceId(processInstance.getDbid());
        changeRequestMessageTree.setProcessInstanceId(processInstance.getDbid());

        return processInstance;
    }

    protected void flushAll() {
        storeDocuments(changeRequestFolder);       
        hibernateSession.index(changeRequestData);     
        hibernateSession.flush();
    }

    /**
     * recursive method to store the document tree
     * 
     * FIXME: in order to avoid an error when two people upload documents for
     * the same workflow within two different conversations we have to remove
     * the document form the hibernate session and manually attach all changes
     * on new checked out folder from the DB...
     * 
     * @param folder
     */
    @Transactional
    private void storeDocuments(final CharmsFolder folder) {
        final FacesMessages facesMessages = FacesMessages.instance();
        LOGGER.debug("storeDocuments called, documentCount: {}", changeRequestFolder.getFileCount());

        // first iterate through the children
        final List<CharmsFolder> subfolders = folder.getChildren();
        if ((subfolders != null) && (subfolders.size() > 0)) {
            for (final CharmsFolder subfolder : subfolders) {
                storeDocuments(subfolder);
            }
        }

        // then store the documents
        final Iterator<CharmsDocument> docs = folder.getDocuments().iterator();
        // refresh the folder
        // hibernateSession.refresh(folder);

        while (docs.hasNext()) {
            final CharmsDocument document = docs.next();
            // check if we have a file that needs to be uploaded
            final File file = document.getFile();
            if (file != null) {
                LOGGER.debug("uploading a file, name is: {} MimeType is: {}", document.getName(), document.getMimeType());
                try {
                    // we have to upload that file
                    document.setContentStream(new FileInputStream(file), file.length(), hibernateSession);
                    hibernateSession.persist(document);
                    //
                    // FIXME: we can't delete the file here since the indexer
                    // needs access to it during the transaction commit
                    // this might be resolved by using byte array instead of
                    // streams here like described at
                    // https://forum.hibernate.org/viewtopic.php?p=2318509&sid=90b3ad474d908a4c0a5584a5d9970925
                    //
                    // for now we delete the file in the finalizer of the
                    // CharmsDocument object
                    //
                    // if (file.exists()) {
                    // file.delete();
                    // }
                    // document.setFile(null);
                } catch (final FileNotFoundException ex) {
                    // this may happen when the viruschecker deletes the file
                    // let's show a nice error dialog
                    facesMessages.addFromResourceBundle(Severity.ERROR, "changeRequestFolder.fileNotFound");
                    LOGGER.warn("can't upload file (file not found) " + "removing file from folder", ex);
                    docs.remove();
                } catch (final IOException ex) {
                    // this may happen when the viruschecker deletes the file
                    // let's show a nice error dialog
                    facesMessages.addFromResourceBundle(Severity.ERROR, "changeRequestFolder.ioException");
                    LOGGER.warn("can't upload file (ioexception) " + "removing file from folder", ex);
                    docs.remove();
                }
            }
        }
    }

    private void takeTask() {
        processEngine.getTaskService().assignTask(taskDbid.toString(), authenticatedUser.getActorId());
        // store a message in the db
        hibernateSession.persist(currentMessageEntry);
        final ChangeRequestMessageEntry entry = new ChangeRequestMessageEntry();
        entry.setTitle("user is taking task"); // FIXME: still needed after the message approach?
        entry.setType(MessageType.TAKE);
        entry.setTimestamp(Calendar.getInstance().getTime());
        entry.setAuthor(authenticatedUser);
        currentMessageEntry.addChild(entry); 

        hibernateSession.persist(entry);
        // we need to flush here so the task is gone for the next user
        hibernateSession.flush();
    }


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

    // checking if the task is still available...
    protected boolean isTaskAvailable() {
        if (taskDbid == null) {
            return false;
        }
        final TaskImpl task = (TaskImpl) hibernateSession.get(TaskImpl.class, Long.parseLong(taskDbid));
        if ((task == null) || (task.isCompleted())) {
            return false;
        }
        return true;
    }

}
