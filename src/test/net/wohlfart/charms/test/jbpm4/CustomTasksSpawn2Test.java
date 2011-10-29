package net.wohlfart.charms.test.jbpm4;

import java.util.Collection;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.Execution;
import org.jbpm.api.JbpmException;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.task.Task;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Michael Wohlfart
 */
public class CustomTasksSpawn2Test extends AbstractJbpm4TestBase {

    @Test
    public void testComplexSpawn2() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testSpawn2");
        charmsUser.setFirstname("test");
        charmsUser.setLastname("test");
        hibernateSession.persist(charmsUser);
        hibernateSession.flush();
        Contexts.getSessionContext().set("authenticatedUser", charmsUser);

        // a variable we need for assigning the next user/task
        transitionData = new TransitionData();
        // transitionData.setReceiverUserId(charmsUser.getId());
        transitionData.setReceiverUser(charmsUser);
        transitionData.setReceiverLabel(charmsUser.getLabel());
        hibernateSession.persist(transitionData);
        hibernateSession.flush();

        final String deploymentPid = deployJpdlXmlString(
                "<process name='CustomTasksSpawn2'>"
                + "  <start name='CustomTasksSpawn.start'" 
                + "         form='CustomTasksSpawn/start.html'>"
                + "      <transition name='toInitialize' to='CustomTasksSpawn.initialize'/>" 
                + "  </start>"

                + "  <automaticTaskActivity name='CustomTasksSpawn.initialize'>" 
                + "    <transition name='toTask1' to='CustomTasksSpawn.task1'/>"
                + "  </automaticTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task1'" 
                + "                      form='changerequest/task1.html' "
                + "                      spawnSignals='toSelfSpawn, toTask2Spawn' " 
                + "                      termSignals='finish' >"
                + "    <transition name='toSelf' to='CustomTasksSpawn.task1'/>" 
                + "    <transition name='toSelfSpawn' to='CustomTasksSpawn.task1'/>"
                + "    <transition name='toTask2Spawn' to='CustomTasksSpawn.task2'/>" 
                + "    <transition name='toEnd' to='CustomTasksSpawn.end'/>"
                + "    <transition name='toNext' to='CustomTasksSpawn.task2'/>" 
                + "  </customTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task2'" 
                + "                      form='changerequest/task2.html'>"
                + "    <transition name='toTask3' to='CustomTasksSpawn.task3'/>" 
                + "  </customTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task3'" 
                + "                      form='changerequest/task3.html' "
                + "                      spawnSignals='toTask4, toNowhere' >" 
                + "    <transition name='toSelf' to='CustomTasksSpawn.task3'/>"
                + "    <transition name='toEnd' to='CustomTasksSpawn.end'/>" 
                + "  </customTaskActivity>"

                + "<end name='CustomTasksSpawn.end'/>"

                + "</process>");

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); // used to be "2"
        hibernateSession.flush();

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, 
                " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        // /////////////////////////////////////////////////////////////////////////////
        // first instance

        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksSpawn2");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");

        // this checks the custom table id generator
        // BigInteger for MySQL
        // BigDecimal for Oracle
        //
        final Number number = (Number) hibernateSession
           .createSQLQuery("select NEXT_VAL from HIBERNATE_SEQUENCES where SEQUENCE_NAME = 'JBPM4_EXECUTION'")
           .uniqueResult();
        final int id = number.intValue() - 1;
        Assert.assertEquals(processInstance1.getId(), 
                "CustomTasksSpawn2." + id, "definitionId doesn't match");
        Assert.assertEquals(processInstance1.getProcessInstance().getId(), 
                "CustomTasksSpawn2." + id, "definitionId doesn't match");

        TaskImpl taskImpl;
        long taskCount;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.initialize");

        signalTaskWithTransData(taskImpl, "toTask1");

        // we should get an error here:
        /*
        try {
            signalTaskWithTransData(taskImpl, "toTask1");
            Assert.fail("unknown transition check failed");
        } catch (final JbpmException ex) {
            // expected
        }
        */
        hibernateSession.flush();

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toSelf");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        Assert.assertEquals(taskCount, 1, "only one task should be available");

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toSelf");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        Assert.assertEquals(taskCount, 1, "only one task should be available");

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toSelf");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        Assert.assertEquals(taskCount, 1, "only one task should be available");

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toSelfSpawn");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        // at this point we should have two tasks/executions
        Assert.assertEquals(taskCount, 2, "after the spawn exactly two tasks should be available here");

        // and another spawn with the same task
        signalTaskWithTransData(taskImpl, "toSelfSpawn");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        // at this point we should have two tasks/executions
        Assert.assertEquals(taskCount, 3, "after the spawn exactly three tasks should be available here");

        // process instance Id is "CustomTasksSpawn2.1"
        final String processInstanceId = processInstance1.getProcessInstance().getId();
        Assert.assertEquals(processInstanceId, "CustomTasksSpawn2." + id, "definitionId doesn't match");

        // process definition Id is "CustomTasksSpawn2-1"
        final String definitionId = processInstance1.getProcessDefinitionId();
        Assert.assertEquals(definitionId, "CustomTasksSpawn2-1", "definitionId doesn't match");

        ProcessInstance process = executionService.createProcessInstanceQuery().processDefinitionId(definitionId).uniqueResult();
        Assert.assertNotNull(process, "process instance should not be null");
        Assert.assertTrue(process.getIsProcessInstance(), "should be process instances");
        Collection<? extends Execution> executions = process.getExecutions();
        Assert.assertEquals(executions.size(), 2, "after spawning there should be 2 executions (and one process instance)");

        // some general checks
        for (final Execution execution : executions) {
            Assert.assertFalse(execution.getIsProcessInstance(), "subexecutions shouldn't be process instances");
            Assert.assertEquals(execution.getState(), "active-concurrent");

            // process instance Id is for example: "CustomTasksSpawn2.1.2"
            final String executionId = execution.getId();
            Assert.assertNotNull(executionId);

            // can't find tasks for the execution, only process instance works:
            Task stask = taskService.createTaskQuery().processInstanceId(executionId).uniqueResult();
            Assert.assertNull(stask);

            // custom extension for execution ids
            stask = taskService.createTaskQuery().executionId(executionId).uniqueResult();
            Assert.assertNotNull(stask);
        }

        process = executionService.createProcessInstanceQuery().processDefinitionId(definitionId).uniqueResult();
        Assert.assertNotNull(process, "process instance should not be null");
        Assert.assertTrue(process.getIsProcessInstance(), "should be process instances");
        executions = process.getExecutions();
        Assert.assertEquals(executions.size(), 2, "after spawning there should be 2 executions (and one process instance)");

        // some forwards
        for (final Execution execution : executions) {
            // process instance Id is for example: "CustomTasksSpawn2.1.2"
            final String executionId = execution.getId();
            TaskImpl stask = (TaskImpl) taskService.createTaskQuery().executionId(executionId).uniqueResult();
            Assert.assertNotNull(stask);
            Assert.assertEquals(stask.getName(), "CustomTasksSpawn.task1");

            // advance the task in a loop
            signalTaskWithTransData(stask, "toSelf");
            stask = (TaskImpl) taskService.createTaskQuery().executionId(executionId).uniqueResult();
            Assert.assertNotNull(stask);
            Assert.assertEquals(stask.getName(), "CustomTasksSpawn.task1");
        }

        process = executionService.createProcessInstanceQuery().processDefinitionId(definitionId).uniqueResult();
        executions = process.getExecutions();

        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        // at this point we should have two tasks/executions
        Assert.assertEquals(taskCount, 3, "after the spawn exactly 3 tasks should be available here");

        // 3 more spawns
        signalTaskWithTransData(taskImpl, "toTask2Spawn");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        // at this point we should have two tasks/executions
        Assert.assertEquals(taskCount, 4, "after the spawn exactly 4 tasks should be available here");
        //
        signalTaskWithTransData(taskImpl, "toTask2Spawn");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        // at this point we should have two tasks/executions
        Assert.assertEquals(taskCount, 5, "after the spawn exactly 5 tasks should be available here");
        //
        signalTaskWithTransData(taskImpl, "toTask2Spawn");
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        // at this point we should have two tasks/executions
        Assert.assertEquals(taskCount, 6, "after the spawn exactly 6 tasks should be available here");

        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(taskImpl.getExecution().getId()).uniqueResult();

        // we should get an error here:
        /*
        try {
            signalTaskWithTransData(taskImpl, "toInvalidTransitionHere");
            Assert.fail("unknown transition check failed");
        } catch (final JbpmException ex) {
            // Assert.fail("unknown transition check failed " + ex);
        }
        */

        // check if no accidential transition happend here
        process = executionService.createProcessInstanceQuery().processDefinitionId(definitionId).uniqueResult();
        executions = process.getExecutions();
        taskCount = taskService.createTaskQuery().processInstanceId(process.getId()).count();
        Assert.assertEquals(taskCount, 6, "after the spawn exactly 6 tasks should be available here");

        taskCount = taskService.createTaskQuery().activityName("CustomTasksSpawn.task1").count();
        Assert.assertEquals(taskCount, 3, "wrong number of tasks in activity");

        taskCount = taskService.createTaskQuery().activityName("CustomTasksSpawn.task2").count();
        Assert.assertEquals(taskCount, 3, "wrong number of tasks in activity");

        process = executionService.createProcessInstanceQuery().processDefinitionId(definitionId).uniqueResult();
        Assert.assertNotNull(process, "process instance should not be null");
        Assert.assertTrue(process.getIsProcessInstance(), "should be process instances");
        executions = process.getExecutions();
        Assert.assertEquals(executions.size(), 5, "after spawning there should be 5 executions (and one process instance)");

        // terminate the whole process
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(taskImpl.getExecution().getId()).uniqueResult();
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task1");
        signalTaskWithTransData(taskImpl, "toNext");

        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(taskImpl.getExecution().getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toTask3");

        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(taskImpl.getExecution().getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toEnd");

        assertExecutionEnded(processInstance1.getId());

        // delete our stuff from the db so we can rerun this test
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        deleteUserAndTransitions(charmsUser);
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
