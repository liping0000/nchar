package net.wohlfart.charms.test.jbpm4;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.ProcessInstance;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Michael Wohlfart
 */
public class CustomTasksSpawn1Test extends AbstractJbpm4TestBase {

    @Test
    public void testSerialExecution() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testSpawn0");
        charmsUser.setFirstname("test");
        charmsUser.setLastname("test");
        hibernateSession.persist(charmsUser);
        hibernateSession.flush();
        Contexts.getSessionContext().set("authenticatedUser", charmsUser);

        transitionData = new TransitionData();
        // transitionData.setReceiverUserId(charmsUser.getId());
        transitionData.setReceiverUser(charmsUser);
        transitionData.setReceiverLabel(charmsUser.getLabel());
        hibernateSession.persist(transitionData);

        final String deploymentPid = deployJpdlXmlString(
                
                "<process name='CustomTasksSpawn1'>"

                + "  <start name='CustomTasksSpawn.start'" 
                + "         form='CustomTasksSpawn/start.html'>"
                + "      <transition name='toInitialize' to='CustomTasksSpawn.initialize'/>" 
                + "  </start>"

                + "  <automaticTaskActivity name='CustomTasksSpawn.initialize'>" 
                + "    <transition name='toTask1' to='CustomTasksSpawn.task1'/>"
                + "  </automaticTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task1'" 
                + "                      form='changerequest/task1.html'>"
                + "    <transition name='toTask2' to='CustomTasksSpawn.task2'/>" 
                + "  </customTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task2'" 
                + "                      form='changerequest/task2.html'>"
                + "    <transition name='toTask3' to='CustomTasksSpawn.task3'/>" 
                + "  </customTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task3'" 
                + "                      form='changerequest/task3.html'>"
                + "    <transition name='toTask4' to='CustomTasksSpawn.task4'/>" 
                + "  </customTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task4'" 
                + "                      form='changerequest/task4.html'>"
                + "    <transition name='toEnd' to='CustomTasksSpawn.end'/>" 
                + "  </customTaskActivity>"

                + "<end name='CustomTasksSpawn.end'/>"

                + "</process>");

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); 
        hibernateSession.flush();

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, 
                " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        // skip the business key:
        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksSpawn1");
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");
        hibernateSession.flush();

        TaskImpl taskImpl;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.initialize");
        signalTaskWithTransData(taskImpl, "toTask1");
        hibernateSession.flush();

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task1");
        signalTaskWithTransData(taskImpl, "toTask2");

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task2");
        signalTaskWithTransData(taskImpl, "toTask3" /* , map */);

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task3");
        signalTaskWithTransData(taskImpl, "toTask4" /* , map */);

        hibernateSession.flush(); // timing issue here
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task4");
        signalTaskWithTransData(taskImpl, "toEnd" /* , map */);

        assertExecutionEnded(processInstance1.getId());

        deleteRegisteredDeployments();
        hibernateSession.flush();
        deleteUserAndTransitions(charmsUser);
        hibernateSession.flush();
        hibernateSession.clear();

    }

    @Test
    public void testSimpleSpawn1() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testSpawn1");
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
                "<process name='CustomTasksSpawn1'>"

                + "  <start name='CustomTasksSpawn.start'" 
                + "         form='CustomTasksSpawn/start.html'>"
                + "      <transition name='toInitialize' to='CustomTasksSpawn.initialize'/>" 
                + "  </start>"

                + "  <automaticTaskActivity name='CustomTasksSpawn.initialize'>" 
                + "    <transition name='toTask1' to='CustomTasksSpawn.task1'/>"
                + "  </automaticTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task1'" 
                + "                      form='changerequest/task1.html'>"
                + "    <transition name='toTask2' to='CustomTasksSpawn.task2'/>"
                + "  </customTaskActivity>"

                + "  <customTaskActivity name='CustomTasksSpawn.task2'"
                + "                      form='changerequest/task2.html'>"
                + "    <transition name='toTask3' to='CustomTasksSpawn.task3'/>"
                + "  </customTaskActivity>"

                // the spawning starts here

                + "  <customTaskActivity name='CustomTasksSpawn.task3'" 
                + "                      form='changerequest/task3.html' "
                + "                      spawnSignals='toTask4, toNowhere' >" 
                + "    <transition name='toSelf' to='CustomTasksSpawn.task3'/>"
                + "    <transition name='toTask4' to='CustomTasksSpawn.task4'/>"
                + "    <transition name='toEnd' to='CustomTasksSpawn.end'/>"
                + "  </customTaskActivity>"

                // this task is spawned:
                + "  <customTaskActivity name='CustomTasksSpawn.task4'" 
                + "                      form='changerequest/task4.html'>"
                + "    <transition name='toSelf' to='CustomTasksSpawn.task4'/>" 
                + "    <transition name='toEnd' to='CustomTasksSpawn.end'/>"
                + "  </customTaskActivity>"

                + "<end name='CustomTasksSpawn.end'/>"

                + "</process>");

        Assert.assertNotNull(deploymentPid, "depoyment pid is null"); // used to be "2"
        hibernateSession.flush();

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        // skip the business key:
        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksSpawn1");
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");

        hibernateSession.flush();

        TaskImpl taskImpl;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.initialize");
        signalTaskWithTransData(taskImpl, "toTask1" /* , map */);

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task1");
        signalTaskWithTransData(taskImpl, "toTask2" /* , map */);

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task2");
        signalTaskWithTransData(taskImpl, "toTask3" /* , map */);

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task3");

        // at this point we are at task3

        // sending some spawn signals:
        final int spawns = 10;
        for (int i = 0; i < spawns; i++) {
            taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(taskImpl.getExecution().getId()).uniqueResult();
            signalTaskWithTransData(taskImpl, "toTask4" /* , map */);
        }

        final long count = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).count();
        Assert.assertEquals(count, spawns + 1, "spawn didn't work");

        // we are still at task4
        Assert.assertNotNull(taskImpl, "autoTask is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksSpawn.task3");

//        try {
//            Thread.sleep(5000);
//        } catch (final Exception ex) {
//            ex.printStackTrace();
//        }
//        ;
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(taskImpl.getExecution().getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toEnd" /* , map */);
        hibernateSession.flush();
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
