package net.wohlfart.charms.test.jbpm4;

import java.util.Collection;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.Execution;
import org.jbpm.api.JbpmException;
import org.jbpm.api.ProcessInstance;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Michael Wohlfart
 */
public class CustomTasksSpawn3Test extends AbstractJbpm4TestBase {

    @Test
    public void testComplexSpawn3() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testSpawn3");
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

        final String deploymentPid = deployJpdlXmlString(
                "<process name='CustomTasksSpawn3'>"

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

        hibernateSession.flush();
        Assert.assertNotNull(deploymentPid, "deployment pid is null"); 

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, 
                " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        // /////////////////////////////////////////////////////////////////////////////
        // first instance

        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksSpawn3");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");

        // this checks the custom table id generator
        // BigInteger for MySQL
        // BigDecimal for Oracle
        //
        final Number number = (Number) hibernateSession.createSQLQuery("select NEXT_VAL from HIBERNATE_SEQUENCES where SEQUENCE_NAME = 'JBPM4_EXECUTION'")
                .uniqueResult();
        final int id = number.intValue() - 1;
        Assert.assertEquals(processInstance1.getId(), "CustomTasksSpawn3." + id, "definitionId doesn't match");
        Assert.assertEquals(processInstance1.getProcessInstance().getId(), "CustomTasksSpawn3." + id, "definitionId doesn't match");

        TaskImpl taskImpl;
        long taskCount;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toTask1" /* , map */);

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();

        // we should get an error here:
        /*
        try {
            signalTaskWithTransData(taskImpl, "toTask1");
            Assert.fail("unknown transition check failed");
        } catch (final JbpmException ex) {
            // expected
        }
        */

        //
        // execution is sitting at task1 now, we have the following options:
        // spawnSignals='toSelfSpawn, toTask2Spawn'
        // termSignals='finish'
        //

        signalTaskWithTransData(taskImpl, "toSelfSpawn" /* , map */);
        taskCount = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        // at this point we should have two tasks/executions
        Assert.assertEquals(taskCount, 2, "after the spawn exactly two tasks should be available here");

        // process definition Id is "CustomTasksSpawn3-1"
        final String definitionId = processInstance1.getProcessDefinitionId();
        Assert.assertEquals(definitionId, "CustomTasksSpawn3-1", "definitionId doesn't match");

        ProcessInstance process = executionService.createProcessInstanceQuery().processDefinitionId(definitionId).uniqueResult();
        Assert.assertNotNull(process, "process instance should not be null");
        Assert.assertNotNull(process.getProcessInstance(), "process.getProcessInstance() is not null:" + process.getProcessInstance());
        Assert.assertTrue(process.getProcessInstance().getIsProcessInstance(), "missing a process instance");
        Assert.assertEquals(process.getProcessInstance().getId(), process.getId(), "getProcessInstance not returning the process instance");

        Collection<? extends Execution> executions = process.getExecutions();
        Assert.assertEquals(executions.size(), 1, "execution count should be 1 after a spawn");

        Execution execution = (Execution) executions.toArray()[0];

        // process and spawned off execution
        Assert.assertNotNull(process, "process instance should not be null");
        Assert.assertNotNull(execution, "execution instance should not be null");
        Assert.assertNotSame(process, execution);

        // signal the finish to the process, this should cause an exception
        /*
        try {
            taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(process.getId()).uniqueResult();
            signalTaskWithTransData(taskImpl, "finish" );
            Assert.fail("terminating process instance shouldn't be possible");
        } catch (final JbpmException ex) {
            // expected
        }
        */

        // signal the finish to the execution, this should work:
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(execution.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "finish" /* , map */);

        process = executionService.createProcessInstanceQuery().processDefinitionId(definitionId).uniqueResult();
        Assert.assertNotNull(process, "process instance should not be null");

        executions = process.getExecutions();
        Assert.assertEquals(executions.size(), 1, "execution count should still be 1 after the term");

        execution = (Execution) executions.toArray()[0];
        Assert.assertEquals(execution.getState(), Execution.STATE_ENDED, "execution should have state ended here");

        // delete our stuff from the db so we can rerun this test
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        deleteUserAndTransitions(charmsUser);
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
