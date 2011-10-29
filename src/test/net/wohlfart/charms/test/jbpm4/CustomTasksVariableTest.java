package net.wohlfart.charms.test.jbpm4;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.ProcessInstance;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.jbpm.pvm.internal.type.Variable;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * testing the scopes for the script calls
 * 
 * 
 * the original jbpm4 is patched at
 * org.jbpm.jpdl.internal.activity.ScriptActivity the task context is added to
 * wrap a task context around the call
 * 
 * use public void perform(OpenExecution execution) {} to get this kind of
 * service
 * 
 * - AutomaticTaskActivity and CreateBusinessKeyActivity use this in the
 * execute() method of the Activity after creating a new task
 * 
 * 
 * @author Michael Wohlfart
 */
public class CustomTasksVariableTest extends AbstractJbpm4TestBase {

    public static volatile int      callCounter = 0;
    public static volatile int      taskCounter = 0;

    public static volatile TaskImpl currentTask;

    public static final class CallCollector {

        public static void callTaskCount(final Object task) throws InterruptedException {
            if (task != null) {
                CustomTasksVariableTest.taskCounter++;
            }
            CustomTasksVariableTest.callCounter++;
        }

        public static void callDumpCheck() throws InterruptedException {
            Thread.dumpStack();
        }

        public static void checkCurrentTask(final Object task) throws InterruptedException {
            Assert.assertEquals(((TaskImpl) task).getDbid(), currentTask.getDbid());
        }
    }

    @Test
    public void testCustomTasksVariables() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final Variable<?> transitionChoiceVariable;

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testVariableeee");
        charmsUser.setFirstname("test");
        charmsUser.setLastname("test");
        hibernateSession.persist(charmsUser);
        Contexts.getSessionContext().set("authenticatedUser", charmsUser);
        hibernateSession.flush();

        // a variable we need for assigning the next user/task
        transitionData = new TransitionData();
        // transitionData.setReceiverUserId(charmsUser.getId());
        transitionData.setReceiverUser(charmsUser);
        transitionData.setReceiverLabel(charmsUser.getLabel());
        hibernateSession.persist(transitionData);

        final String deploymentPid = deployJpdlXmlString("<process name='CustomTasksVar'>"

                + "  <start name='CustomTasksVar.start'"
                + "         form='CustomTasksVar/start.html'>"
                + "      <transition name='auto' to='CustomTasksVar.task1'/>"
                + "  </start>"

                // // start task to check: -----------------------
                + "  <customTaskActivity name='CustomTasksVar.task1'"
                + "                      form='changerequest/task1.html'"
                + "                      spawnSignals='toSpawn'"
                + "                      termSignals='toTerm'"
                + ">"

                // this script is executed syncronous when the token reaches
                // this task and the execute() method is called in the activity
                + " <on event=\"toLoop\">" + "  <script lang='groovy'><text><![CDATA[" 
                + "    System.err.println(\" ****  called the toLoop script **** \");"
                + "    System.err.println(\" ****  task is \" + task + \" **** \");"
                + "    System.err.println(\" ****  hashvalue of task is \" + task.hashCode() + \" **** \");" + "    "
                + CustomTasksVariableTest.class.getName()
                + ".CallCollector.checkCurrentTask(task);"
                + " ]]></text></script>"
                + " </on>"

                // this script is executed syncronous when the token reaches
                // this task and the execute() method is called in the activity
                + " <on event=\"toSpawn\">"
                + "  <script lang='groovy'><text><![CDATA["
                + "    System.err.println(\" ****  called the toSpawn script **** \");"
                + "    System.err.println(\" ****  task is \" + task + \" **** \");"
                + "    System.err.println(\" ****  hashvalue of task is \" + task.hashCode() + \" **** \");"
                + "    "
                + CustomTasksVariableTest.class.getName()
                + ".CallCollector.checkCurrentTask(task);"
                + " ]]></text></script>"
                + " </on>"

                + " <on event=\"toNext\">"
                + "  <script lang='groovy'><text><![CDATA["
                + "    System.err.println(\" ****  called the toNext script **** \");"
                + "    System.err.println(\" ****  task is \" + task + \" **** \");"
                + "    System.err.println(\" ****  hashvalue of task is \" + task.hashCode() + \" **** \");"
                // +
                // "    System.err.println(\" ****  myVar is \" + myVar1 + \" **** \");"
                + " ]]></text></script>"
                + " </on>"

                + " <on event=\"toTerm\">"
                + "  <script lang='groovy'><text><![CDATA["
                + "    System.err.println(\" ****  called the toTerm script **** \");"
                + "    System.err.println(\" ****  task is \" + task + \" **** \");"
                + "    System.err.println(\" ****  hashvalue of task is \" + task.hashCode() + \" **** \");"
                // +
                // "    System.err.println(\" ****  myVar is \" + myVar1 + \" **** \");"
                + " ]]></text></script>" + " </on>"

                + "    <transition name='toNext' to='CustomTasksVar.task2'/>" // to
                                                                              // the
                                                                              // next
                                                                              // step
                + "    <transition name='toLoop' to='CustomTasksVar.task1'/>" // a
                                                                              // simple
                                                                              // loop
                                                                              // /
                                                                              // forward
                + "    <transition name='toSpawn' to='CustomTasksVar.task1'/>" // a
                                                                               // looped
                                                                               // spawn

                + "  </customTaskActivity>"
                // end task to check: -----------------------

                + "  <customTaskActivity name='CustomTasksVar.task2'" + "                      form='changerequest/task2.html'>"

                + "    <transition name='toEnd' to='CustomTasksVar.end'/>"

                + "  </customTaskActivity>"

                + "<end name='CustomTasksVar.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); // used
                                                                       // to be
                                                                       // "2"
        hibernateSession.flush();

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        final Object object = processEngine.get("jobExecutor");
        Assert.assertNotNull(object);
        final JobExecutor jobExecutor = (JobExecutor) object;
        if (!jobExecutor.isActive()) {
            jobExecutor.start();
        }

        // start
        ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksVar");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");

        TaskImpl taskImpl;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "first is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksVar.task1");
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run

        environment = processEngine.openEnvironment();
        try {
            // we simulate creating a variable in the signal method
            Assert.assertFalse(taskImpl.hasVariable("myVar1"));
            Assert.assertFalse(taskImpl.hasVariable("myVar2"));
            taskImpl.createVariable("myVar1", "some value");
            //taskImpl.createVariable("myVar2", "value".toCharArray(), "char[]", false);
            taskImpl.createVariable("myVar2", "value");
            //taskImpl.createVariable("myVar2", "value"/*.toCharArray()*/, "char[]", false);
            final Variable<?> varObject1 = taskImpl.getVariableObject("myVar1");
            varObject1.setExecution(null);
            final Variable<?> varObject2 = taskImpl.getVariableObject("myVar2");
            varObject2.setExecution(null);
            Assert.assertTrue(taskImpl.hasVariable("myVar1"));
            Assert.assertTrue(taskImpl.hasVariable("myVar2"));
        } finally {
            environment.close();
        }

        hibernateSession.flush();

        // just do a loop
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toLoop" /* , map */);
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run

        // get the new task
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertFalse(taskImpl.hasVariable("myVar1"));
        Assert.assertFalse(taskImpl.hasVariable("myVar2"));

        hibernateSession.flush();
        // hibernateSession.clear();
        // try { Thread.sleep(1000 * 60 * 60); } catch (InterruptedException ex)
        // { ex.printStackTrace(); }

        // another loop
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toLoop" /* , map */);
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run

        // get the new task
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertFalse(taskImpl.hasVariable("myVar1"));
        Assert.assertFalse(taskImpl.hasVariable("myVar2"));

        // another loop
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toLoop" /* , map */);
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run

        // another loop
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toLoop" /* , map */);
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run

        // another loop
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toLoop" /* , map */);
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run

        // wait for the notify timer to kick in
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toSpawn" /* , map */);
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run

        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();

        Assert.assertFalse(taskImpl.hasVariable("myVar1"));
        Assert.assertFalse(taskImpl.hasVariable("noVar1"));

        // reload the process instance:
        processInstance1 = executionService.findProcessInstanceById(processInstance1.getId());
        Assert.assertEquals(processInstance1.getExecutions().size(), 1);

        final ExecutionImpl execution = (ExecutionImpl) processInstance1.getExecutions().toArray()[0];
        Assert.assertNotNull(execution);

        // wait for the notify timer to kick in
        hibernateSession.flush();
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(execution.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toTerm" /* , map */);
        // should be sync, but anyways...
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }

        // wait for the notify timer to kick in
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toNext" /* , map */);

        // wait for the notify timer to kick in
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(processInstance1.getId()).uniqueResult();
        CustomTasksVariableTest.currentTask = taskImpl;
        signalTaskWithTransData(taskImpl, "toEnd" /* , map */);

        assertExecutionEnded(processInstance1.getId());
        // job executor should still be running here:
        Assert.assertTrue(jobExecutor.isActive(), "job executor not running");

        // delete our stuff from the db so we can rerun this test
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        deleteUserAndTransitions(charmsUser);
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
