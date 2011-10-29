package net.wohlfart.charms.test.jbpm4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.mock.SeamTest;
import org.jboss.seam.security.Identity;
import org.jbpm.api.Execution;
import org.jbpm.api.ExecutionService;
import org.jbpm.api.HistoryService;
import org.jbpm.api.IdentityService;
import org.jbpm.api.ManagementService;
import org.jbpm.api.ProcessDefinition;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.RepositoryService;
import org.jbpm.api.TaskService;
import org.jbpm.api.task.Task;
import org.jbpm.pvm.internal.cmd.CommandService;
import org.jbpm.pvm.internal.cmd.DeleteDeploymentCmd;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.jobexecutor.AcquireJobsCmd;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.jobexecutor.JobParcel;
import org.jbpm.pvm.internal.processengine.ProcessEngineImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.jbpm.pvm.internal.type.Variable;
import org.jbpm.test.assertion.CollectionAssertions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

// see: import org.jbpm.test.JbpmTestCase;
// for info about jbpm4 testcases

public class AbstractJbpm4TestBase extends SeamTest {

    Session                  hibernateSession;

    public TransitionData    transitionData;

    public EnvironmentImpl   environment;

    static ProcessEngineImpl processEngine;

    static JobExecutor jobExecutor;

    static RepositoryService repositoryService;
    static ExecutionService  executionService;
    static ManagementService managementService;
    static TaskService       taskService;
    static HistoryService    historyService;
    static IdentityService   identityService;

    /**
     * registered deployments. registered deployments will be deleted
     * automatically in the tearDown. This is a convenience function as each
     * test is expected to clean up the DB.
     */
    List<String>             registeredDeployments = new ArrayList<String>();

    static final String      PROCESS_KEY           = "ChangeRequest";

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        Identity.setSecurityEnabled(false);

        Lifecycle.beginCall();
        Lifecycle.beginSession(session.getAttributes(), null);

        // find the hibernateSession
        hibernateSession = (Session) Component.getInstance("hibernateSession");
        Assert.assertNotNull(hibernateSession, "hibernateSession is null");

        // find the processEngine
        initializeProcessEngine();


        // make sure the job executor is offline
        final Object object = processEngine.get("jobExecutor");        
        Assert.assertNotNull(object);
        jobExecutor = (JobExecutor) object;
        if (!jobExecutor.isActive()) {
            jobExecutor.stop(true);
        }

        // processEngine = (ProcessEngine)
        // Component.getInstance("processEngine");
        Assert.assertNotNull(processEngine, "processEngine is null");
    };

    protected synchronized void initializeProcessEngine() {
        if (processEngine == null) {
            processEngine = (ProcessEngineImpl) Component.getInstance("processEngine");

            repositoryService = processEngine.get(RepositoryService.class);
            executionService = processEngine.getExecutionService();
            historyService = processEngine.getHistoryService();
            managementService = processEngine.getManagementService();
            taskService = processEngine.getTaskService();
            identityService = processEngine.getIdentityService();
        }
    }

    @AfterMethod
    @Override
    public void end() {
        session.invalidate();
        session = null;
        //super.end();
    };

    /**
     * this does what the Action class is doing
     *  - register a TransitionData Object with the task
     *  - flush the hibernate session
     *  - DO NOT clear the hibernate session, so all entities in the session 
     *    woudl be evicted!!!
     */
    protected void signalTaskWithTransData( TaskImpl taskImpl, final String signal) {
        hibernateSession.flush();
        // clearing the session breaks the code...
        // clear() evicts all entities in the session, changes to the entites are no longer
        // persisted to the database...
        // hibernateSession.clear();
        // hibernateSession.refresh(taskImpl); // reconnect the entity with the session...

        environment = processEngine.openEnvironment();

        // this is done in the signal method when performing in the UI
        taskImpl.createVariable(TransitionData.TRANSITION_DATA, transitionData);
        final Variable<?> transitionDataVariable = taskImpl.getVariableObject(TransitionData.TRANSITION_DATA);
        // set execution to null, a link to task is enough otherwise we have trouble deleting the task
        transitionDataVariable.setExecution(null); 

        executionService.signalExecutionById(taskImpl.getExecution().getId(), signal );
        environment.close();


        // sleep to provoke 
        // org.hibernate.StaleStateException: Batch update returned unexpected row count from update [0]; actual row count: 0; expected: 1
        //
        //try {
            hibernateSession.flush();
        //} catch (Exception ex) {
        //    System.err.println(">>>>>>>>>> session: " + hibernateSession);            
        //}

    }

    @SuppressWarnings("unchecked")
    protected void deleteUserAndTransitions(CharmsUser charmsUser) {
        List<TransitionData> list1 = hibernateSession.createCriteria(TransitionData.class).list();
        for (TransitionData transitionData : list1) {
            hibernateSession.delete(transitionData);
        }       
        hibernateSession.delete(charmsUser);
    }
    
    protected void deleteRegisteredDeployments() {
        for (final String deploymentId : registeredDeployments) {
            repositoryService.deleteDeploymentCascade(deploymentId);
        }
        registeredDeployments = new ArrayList<String>();

        /*
        environment = processEngine.openEnvironment();
        // loop all deployments
        for (final String deploymentId : registeredDeployments) {
            //repositoryService.deleteDeploymentCascade(deploymentId);
            DbSession dbSession = processEngine.get(DbSession.class);

            // loop the definitions per deployment
            List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .list();

            for (ProcessDefinition processDefinition: processDefinitions) {
                String processDefinitionId = processDefinition.getId();               
                // loop instances per definiton
                List<String> processInstanceIds = dbSession.findProcessInstanceIds(processDefinitionId);             
                for (String piId : processInstanceIds) {                   
                    // loop activities per instance
                    ProcessInstance processInstance = executionService.findProcessInstanceById(piId);

                    dbSession.delete(processInstance);
                    dbSession.flush();                 
                }
                dbSession.deleteProcessDefinitionHistory(processDefinitionId);
                dbSession.flush();    
            }           
            repositoryService.deleteDeploymentCascade(deploymentId);
            dbSession.flush();    
        }
        environment.close();
         */
    }

    // deployment helper methods
    // ////////////////////////////////////////////////

    /**
     * deploys the process, keeps a reference to the deployment and
     * automatically deletes the deployment in the tearDown
     */
    public String deployJpdlXmlString(final String jpdlXmlString) {
        final String deploymentDbid = repositoryService.createDeployment().addResourceFromString("xmlstring.jpdl.xml", jpdlXmlString).deploy();

        registerDeployment(deploymentDbid);

        return deploymentDbid;
    }

    public String deployBpmn2XmlString(final String bpmn2XmlString) {
        final String deploymentDbid = repositoryService.createDeployment().addResourceFromString("xmlstring.bpmn.xml", bpmn2XmlString).deploy();

        registerDeployment(deploymentDbid);
        return deploymentDbid;
    }

    /** registered deployments will be deleted in the tearDown */
    protected void registerDeployment(final String deploymentId) {
        registeredDeployments.add(deploymentId);
    }

    // task helper methods
    // //////////////////////////////////////////////////////

    public static void assertContainsTask(final List<Task> taskList, final String taskName) {
        if (getTask(taskList, taskName) == null) {
            Assert.fail("tasklist doesn't contain task '" + taskName + "': " + taskList);
        }
    }

    public static void assertContainsTask(final List<Task> taskList, final String taskName, final String assignee) {
        if (getTask(taskList, taskName, assignee) == null) {
            Assert.fail("tasklist doesn't contain task '" + taskName + "' for assignee '" + assignee + "': " + taskList);
        }
    }

    public static Task getTask(final List<Task> taskList, final String taskName) {
        for (final Task task : taskList) {
            if (taskName.equals(task.getName())) {
                return task;
            }
        }
        return null;
    }

    public static Task getTask(final List<Task> taskList, final String taskName, final String assignee) {
        for (final Task task : taskList) {
            if (taskName.equals(task.getName())) {
                if (assignee == null) {
                    if (task.getAssignee() == null) {
                        return task;
                    }
                } else {
                    if (assignee.equals(task.getAssignee())) {
                        return task;
                    }
                }
            }
        }
        return null;
    }

    public void assertNoOpenTasks(final String processInstanceId) {
        final List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        Assert.assertTrue(tasks.isEmpty(), "There were still open tasks found for the process instance with id " + processInstanceId + ". Current tasks are: "
                + listAllOpenTasks(processInstanceId));
    }

    protected String listAllOpenTasks(final String processInstanceId) {
        final List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        final StringBuilder result = new StringBuilder();
        for (final Task task : tasks) {
            result.append("'" + task.getName() + "', ");
        }

        if (result.length() > 2) {
            result.setLength(result.length() - 2); // remove the last ', '
        }

        return result.toString();
    }

    // execution helper methods //////////////////////////////////////////

    public void assertExecutionEnded(final String processInstanceId) {
        Assert.assertNull(executionService.findProcessInstanceById(processInstanceId), "Error: an active process instance with id " + processInstanceId
                + " was found");
    }

    public void assertProcessInstanceEnded(final String processInstanceId) {
        assertExecutionEnded(processInstanceId);
    }

    public void assertProcessInstanceEnded(final ProcessInstance processInstance) {
        assertExecutionEnded(processInstance.getId());
    }

    public void assertProcessInstanceActive(final ProcessInstance processInstance) {
        assertProcessInstanceActive(processInstance.getId());
    }

    public void assertProcessInstanceActive(final String processInstanceId) {
        Assert.assertNotNull(executionService.findProcessInstanceById(processInstanceId), "Error: an active process instance with id " + processInstanceId
                + " was not found");
    }

    public void assertActivityActive(final String executionId, final String activityName) {
        Assert.assertTrue(executionService.findExecutionById(executionId).isActive(activityName), "The execution with id '" + executionId
                + "' is not active in the activity '" + activityName + "'." + "Current activitites are: " + listAllActiveActivites(executionId));
    }

    public void assertNotActivityActive(final String executionId, final String activityName) {
        final Execution execution = executionService.findExecutionById(executionId);
        Assert.assertFalse(execution.isActive(activityName));
    }

    public void assertActivitiesActive(final String executionId, final String... activityNames) {
        CollectionAssertions.assertContainsSameElements(executionService.findExecutionById(executionId).findActiveActivityNames(), activityNames);
    }

    /**
     * Checks if the given execution is active in one (or more) of the given
     * activities
     */
    public void assertExecutionInOneOrMoreActivitiesActive(final String executionId, final String... activityNames) {

        boolean inOneActivityActive = false;
        final Execution execution = executionService.findExecutionById(executionId);

        for (final String activityName : activityNames) {
            if (execution.isActive(activityName)) {
                inOneActivityActive = true;
            }
        }

        Assert.assertTrue(inOneActivityActive,
                "The execution with id '" + executionId + "' is not active in one of these activities: " + Arrays.toString(activityNames)
                + "Current activitites are: " + listAllActiveActivites(executionId));
    }

    protected String listAllActiveActivites(final String executionId) {
        final Execution execution = executionService.findExecutionById(executionId);
        final Set<String> activeActivities = execution.findActiveActivityNames();
        final StringBuilder result = new StringBuilder();
        for (final String activeActivity : activeActivities) {
            result.append("'" + activeActivity + "', ");
        }

        if (result.length() > 2) {
            result.setLength(result.length() - 2); // remove the last ', '
        }

        return result.toString();
    }


    protected void runJobs() {
        hibernateSession.flush();

        // seems to be 13 on openJDK
        // and 14 on Sun's JDK
        //Assert.assertEquals(Thread.activeCount(), 13);

        // Command<Collection<Long>> command = jobExecutor.getAcquireJobsCommand();
        AcquireJobsCmd acquireJobsCommand = new AcquireJobsCmd(jobExecutor);
        Assert.assertNotNull(acquireJobsCommand);
        CommandService executor = jobExecutor.getCommandExecutor();
        Collection<Long> result = executor.execute(acquireJobsCommand);

        if (result != null) {
            JobParcel jobs = new JobParcel(executor, result);
            jobs.run();
        }
        hibernateSession.flush();
    }

}
