package net.wohlfart.changerequest;

import net.wohlfart.framework.IllegalParameterException;
import net.wohlfart.framework.interceptor.MeasureCalls;
import net.wohlfart.jbpm4.TransitionNotFoundException;
import net.wohlfart.jbpm4.entities.TransitionChoice;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.criterion.Restrictions;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jbpm.api.ExecutionService;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.TaskService;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.jbpm.pvm.internal.type.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * for a discussion about save()/persist():
 * https://forum.hibernate.org/viewtopic.php?p=2325758&sid=a69cf62ab0451699a62552ac473356a7
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.CONVERSATION)
@Name("changeRequestAction")
// custom interceptor that converts exception into returning an "invalid" string
// @TransitionStrategy
@MeasureCalls
public class ChangeRequestAction extends AbstractChangeRequestAction {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChangeRequestAction.class);

    // these variables are needed in the UserStatistics
    public static final String  INITIALIZE_TASK = "changerequest.initialize"; // task is auto performed on submit
    public static final String  COMPLETE_TASK   = "changerequest.complete"; // this task is skipped in some overviews
    public static final String  IMPLEMENT_TASK  = "changerequest.implement";
    public static final String  REALIZE_TASK    = "changerequest.realize"; // task is automatically opened
    public static final String  DISCARD_TASK    = "changerequest.discard"; // the last task for a discarded process instance, needed as filter

    /**
     * generic method to replace the start methods
     * toCreateBusinessKey()/toComplete() this is the method that starts a
     * process and completes the initialize activity
     * 
     * @param pdKey
     *            the process definition key
     * @param transitionAfterInit
     *            the workflow transition
     * @throws IllegalParameterException
     */
    @Transactional
    public String initializeProcess(final String pdKey, final String signalAfterInit) throws IllegalParameterException {
        // this starts the process and generates the first task
        final ExecutionImpl processInstance = startProcessInstance(pdKey);

        flushAll();
        executionId = processInstance.getId();

        final TaskService taskService = processEngine.getTaskService();
        final TaskImpl task = (TaskImpl) taskService.createTaskQuery().executionId(executionId).uniqueResult();

        setTaskDbid(task.getDbid() + "");

        return signal(signalAfterInit);
    }

    /**
     * signal is called for an already existing task but never for starting a
     * process, and always from the user interface, usually there is a facet in
     * the user interface that holds a form for the further workflow processing
     * -> assigning follow up tasks to multiple users/groups -> due/remind dates
     * and intervals -> comments/notes/orders for the follow up tasks
     * 
     * @param signal
     * @return
     */
    @Transactional
    public String signal(final String signal) {
        // FIXME: implement something like errorIfNoTaskAvailable(); the hibernate ways to check
        LOGGER.debug("getting a signal: {}", signal);

        // check if the human task is still available
        LOGGER.info("signal called, taskDbid: {}", taskDbid);
        final FacesMessages facesMessages = FacesMessages.instance();
        // the task might no longer be valid if the user used a back button for
        // example or someone else did it already
        if (!isTaskAvailable()) {
            LOGGER.warn("task no longer available: {}, returning 'unavailableTask'", taskDbid);
            facesMessages.addFromResourceBundle(Severity.ERROR, "page.workflow.changerequest.taskUnavailable");
            return "unavailableTask"; // result if the task is not availavle
        }
        LOGGER.debug("taskDbid is: {}", taskDbid);
        
        if (!isValid(facesMessages, signal)) {
            LOGGER.warn("invalid form data, returning 'invalidForm'", taskDbid);
            return "invalidForm";
        }

        // load the task/execution/process
        final TaskImpl task = (TaskImpl) hibernateSession.load(TaskImpl.class, new Long(taskDbid));
        final String taskName = task.getName();
        LOGGER.debug("task is: {}", task);

        // at this point we know for sure we have a task
        final EnvironmentImpl environment = processEngine.openEnvironment();
        // TaskContext taskContext = new TaskContext(task);
        // environment.setContext(taskContext);
        try {
            final ExecutionService executionService = processEngine.getExecutionService();

            // setting transition choice variable in the task context
            Variable<?> transitionChoiceVariable = task.getVariableObject(TransitionChoice.TRANSITION_CHOICE);
            if (transitionChoiceVariable != null) {
                LOGGER.debug("found already stored {}, variable is {}", 
                        TransitionChoice.TRANSITION_CHOICE, transitionChoiceVariable.getValue(task));
            } else {
                LOGGER.debug("storing {} value is {}", TransitionChoice.TRANSITION_CHOICE, transitionChoice);
                hibernateSession.persist(transitionChoice);
                // this can only be done in an open environment, otherwise we get an exeception like
                // Unable to resolve entity name from Class [org.jbpm.pvm.internal.type.variable.UnpersistableVariable]
                task.createVariable(TransitionChoice.TRANSITION_CHOICE, transitionChoice, /* "hibernate-long-id" */null, false);
                transitionChoiceVariable = task.getVariableObject(TransitionChoice.TRANSITION_CHOICE);
                // set execution to null, a link to task is enough otherwise we have trouble deleting the task
                transitionChoiceVariable.setExecution(null);  
            }

            // the data is needed in the scripts
            final TransitionData transitionData = transitionChoice.getData(signal);
            task.createVariable(TransitionData.TRANSITION_DATA, transitionData);
            transitionChoiceVariable = task.getVariableObject(TransitionData.TRANSITION_DATA);
            transitionChoiceVariable.setExecution(null); // set execution to
            // null, a link to task is enough otherwise we have trouble deleting the task

            // executionService.signalExecutionById(task.getProcessInstance().getId(), signal);
            // HashMap<String, Object> map = new HashMap<String, Object>();
            // map.put(TransitionData.TRANSITION_DATA, transitionData);
            final ProcessInstance processInstance = executionService.signalExecutionById(task.getExecution().getId(), signal);          
            LOGGER.debug("task.getExecution().getId(): {}, processInstance.getId(): {}", task.getExecution().getId(), processInstance.getId());

            // set selected to null so it doesn't pop up on the next render and
            // the user has some kind of indication that the action was
            // performed
            transitionChoice.setSelected(null);
            // need to remember the execution id in case we want to follow up on the next task
            executionId = task.getExecution().getId();
            taskDbid  = task.getId();
            // debug the session content
            LOGGER.debug("session content is: {}, executionId is: {}", hibernateSession, executionId);
            // flush session and run lucene indexer
            flushAll();
            // add a message to the UI for the user facesMessages.addFromResourceBundle(Severity.INFO,
            // "page.workflow.changerequest." + signal + ".ok" );
            facesMessages.addFromResourceBundle(Severity.INFO, 
                    "page.workflow." + taskName + "." + signal + ".ok");

            // the returning string must be dealt with in the pageflow configuration
            return signal + "Done";
        } catch (final TransitionNotFoundException ex) {
            LOGGER.warn("transition not available: {}, returning 'unavailableTransition'", signal);
            return "unavailableTransition"; // result if the transition is not
            // available
        } finally {
            // environment.removeContext(taskContext);
            environment.close();
        }
    }
    
    
    /**
     * this is the central mehtod to validate form inputs
     * 
     * 
     * @param signal
     * @return true if valid
     */
    private boolean isValid(FacesMessages facesMessages, String signal) {
        boolean isValid = true;
        
        // FIXME: this needs to be configurable in the process definition:
        //        required user/group selection by the user in order to perform the transition
        
        if ("forward".equals(signal)) {
            LOGGER.info("validate for forward here");
            isValid  = hasValidReceiver(facesMessages, transitionChoice.getData(signal));

        } else if ("review".equals(signal)) {
            LOGGER.info("validate for review here");
            isValid  = hasValidReceiver(facesMessages, transitionChoice.getData(signal));  
            
        } else if ("handle".equals(signal)) {
            LOGGER.info("validate for handle here");
            isValid  = hasValidReceiver(facesMessages, transitionChoice.getData(signal));        
           
        } else if ("implement".equals(signal)) {
            LOGGER.info("validate for implement here");
            isValid  = hasValidReceiver(facesMessages, transitionChoice.getData(signal));        
           
        } else if ("toProcess".equals(signal)) {
            LOGGER.warn("validate for toProcess here");
            
        } else if ("toRealize".equals(signal)) {
            LOGGER.info("validate for toRealize here");
            
            // this is for unit tests
            if ((changeRequestData.getCostA() == null) || (changeRequestData.getCostB() == null)) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.workflow.changerequest.implement.noCost");
                isValid = false;
            }

            // check if we selected a cost scheme
            if (!changeRequestData.getCostA() && !changeRequestData.getCostB()) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.workflow.changerequest.implement.noCost");
                isValid = false;
            }

            // check if we selected a priority
            if (changeRequestData.getPriority() == null) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.workflow.changerequest.implement.noPriority");
                isValid = false;
            }

        } else {
            
        }
        
        return isValid;
        
    }
    
    
    private boolean hasValidReceiver(FacesMessages facesMessages, TransitionData data) {
         
        // we need at least one receiver for the next task
        // should be easier to check with getNextSwimlane() or getNextParticipations()
        if ((data.getReceiverUser() == null) && (data.getReceiverGroup() == null)) {
            facesMessages.addFromResourceBundle(Severity.ERROR, 
                    "page.workflow.changerequest." + data.getTransitionName() + ".noReceiver");
            return false;                
        }

        return true;
    }
    


    /**
     * sometimes we need the taskid of a just created task (e.g. user does a
     * workflow transition in the UI and we want to send him right to the next
     * task) this method should be called right after calling signal(), or at
     * least in the same conversation, this way we still have the execution and 
     * the old dbid or whatever else we need to figure out what the next task is
     * 
     */
    public String getTaskDbid(final String activityName) {
        
        TaskImpl taskImpl = (TaskImpl) hibernateSession
        .createCriteria(TaskImpl.class)
        .createAlias("execution", "e")
        .add(Restrictions.eq("e.id", executionId))
        .add(Restrictions.eq("activityName", activityName))
        .add(Restrictions.eq("assignee", authenticatedUser.getActorId()))
        .setFirstResult(0)
        //.setMaxResults(1)
        .uniqueResult();
        
        if (taskImpl == null) {
            // we have a problem here a task is missing
            LOGGER.warn("task not found, lookig for activityName {} with executionId {} ", activityName, executionId);
            throw new IllegalArgumentException("can't find activity with name " + activityName);
        } else {
            return taskImpl.getId();
        }
        
        /*
              
        return taskDbid;
        
      
        
        final TaskService taskService = processEngine.getTaskService();        
        
//        // the old query used the activity name:
//        final List<Task> tasks = taskService.createTaskQuery()
//            .executionId(executionId)
//            .activityName(activityName) 
//            .orderAsc(TaskQuery.PROPERTY_CREATEDATE) // the last one created
//            .list();
         
        // now we just try to figure out the next task with the assigned actor:       
        final List<Task> tasks = taskService.createTaskQuery()
            .executionId(executionId)
            .assignee(authenticatedUser.getActorId())
            .orderAsc(TaskQuery.PROPERTY_CREATEDATE) // the last one created
            .list();

        String result;
        switch (tasks.size()) {
            case 0:
                // no tasks in this execution, might be a problem if we expect one
                LOGGER.warn("no task in this execution, execution id is {}, return 0 as taskid", executionId);
                result = "0";
                break;
            case 1:
                LOGGER.info("exactly one task found");
                result = tasks.get(0).getId();
                break;
            default:
                LOGGER.info("more than one task found in execution, we get the first one (ordered by creation date)");
                result = tasks.get(0).getId();
        }
        
        
        
        
        System.out.println("executionId: " + executionId);
        System.out.println("authenticatedUser: " + authenticatedUser.getActorId());
        System.out.println("taskDbid: " + taskDbid);
        
        
        if (taskImpl == null) {
            result = taskDbid;
            LOGGER.warn("result: for taskId from taskDbid: {}", result);
        } else {
            result = taskImpl.getId();
            LOGGER.warn("result: for taskId from criteria query: {}", result);
        }            
//                 (DeploymentProperty) session.createCriteria(DeploymentProperty.class)
//            .add(Restrictions.eq("objectName", processDefinitionName))
//            .add(Restrictions.eq("key", "pdid"))
//            .createAlias("deployment", "d")
//            .addOrder(Order.desc("d.timestamp"))
//            .setFirstResult(0)
//            .setMaxResults(1)
//            .uniqueResult();       
        return result;
  */
    }

    /**
     * save changes, no transition
     * 
     * @return
     */
    @Transactional
    public String save() {
        // we need an environment in order to have access to the hibernate type
        // definition for the persist...
        final EnvironmentImpl environment = processEngine.openEnvironment();
        try {
            LOGGER.info("save called, taskDbid: {}", taskDbid);
            final FacesMessages facesMessages = FacesMessages.instance();
            // the task might no longer be valid if the user used a back button
            // for example
            if (!isTaskAvailable()) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.workflow.changerequest.taskUnavailable");
                return "unavailable";
            }
            LOGGER.debug("taskDbid is: {}", taskDbid);

            final TaskImpl task = (TaskImpl) hibernateSession.load(TaskImpl.class, new Long(taskDbid));
            //final String taskName = task.getName();
            Variable<?> transitionChoiceVariable = task.getVariableObject(TransitionChoice.TRANSITION_CHOICE);
            if (transitionChoiceVariable == null) {
                LOGGER.info("storing {} value is {}", TransitionChoice.TRANSITION_CHOICE, transitionChoice);
                hibernateSession.persist(transitionChoice);
                hibernateSession.flush(); // we need to flush it before we can
                // attach it to a task
                task.createVariable(TransitionChoice.TRANSITION_CHOICE, transitionChoice, null, false);
                transitionChoiceVariable = task.getVariableObject(TransitionChoice.TRANSITION_CHOICE);
                // set execution to null, a link to task is enough otherwise we have trouble deleting the task
                transitionChoiceVariable.setExecution(null); 
            } else {
                LOGGER.warn("  found already stored {}, variable is {}", transitionChoiceVariable);
            }

            // store the view
            // hibernateSession.persist(changeRequestFacetState);
            // hibernateSession.persist(transitionChoice);
            // FIXME: persist the changes in the form
            // flush the data to the DB
            flushAll();
            facesMessages.addFromResourceBundle(Severity.INFO, "page.workflow.save.ok");
            return "saved";
        } finally {
            environment.close();
        }
    }

    /**
     * undo changes, no transition
     * 
     * @return
     */
    @Transactional
    public String cancel() {
        LOGGER.info("cancel called, taskDbid: {}", taskDbid);
        final FacesMessages facesMessages = FacesMessages.instance();
        facesMessages.addFromResourceBundle(Severity.INFO, "page.workflow.cancel.ok");
        hibernateSession.clear();
        return "canceled";
    }

}
