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
public class CustomTasksTest extends AbstractJbpm4TestBase {

    @Test
    public void testSimpleDeployment() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testSD");
        charmsUser.setFirstname("test");
        charmsUser.setLastname("test");
        hibernateSession.persist(charmsUser);
        hibernateSession.flush();
        Contexts.getSessionContext().set("authenticatedUser", charmsUser);

        /*
        // a variable we need for assigning the next user/task
        transitionData = new TransitionData();
        // transitionData.setReceiverUserId(charmsUser.getId());
        transitionData.setReceiverUser(charmsUser);
        transitionData.setReceiverLabel(charmsUser.getLabel());
        hibernateSession.persist(transitionData);
        */

        final String deploymentPid = deployJpdlXmlString("<process name='CustomTasks1'>"

                + "  <start name='CustomTasks1.start'" 
                + "         form='CustomTasks1/start.html'>"
                + "      <transition name='toInitialize' to='CustomTasks1.initialize'/>" 
                + "  </start>"

                + "  <automaticTaskActivity name='CustomTasks1.initialize'>"
                + "    <transition name='toCreateBusinessKey' to='CustomTasks1.createBusinessKey'/>"
                + "    <transition name='toComplete' to='CustomTasks1.complete'/>" 
                + "  </automaticTaskActivity>"

                + "  <createBusinessKey name='CustomTasks1.createBusinessKey'" 
                + "        prefix='CM'" 
                + "        location='Ditzingen'>"
                + "    <transition name='toComplete' to='CustomTasks1.complete'/>" 
                + "  </createBusinessKey>"

                + "  <customTaskActivity name='CustomTasks1.complete'" 
                + "                      form='changerequest/complete.html'>"
                + "    <transition name='toEnd' to='CustomTasks1.end'/>" 
                + "  </customTaskActivity>"

                + "<end name='CustomTasks1.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "depoyment pid is null"); // used to be "2"

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        // skip the business key:
        ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasks1");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");

        final TaskImpl autoTask1 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(autoTask1, "autoTask is null");
        Assert.assertEquals(autoTask1.getName(), "CustomTasks1.initialize");

        signalTaskWithTransData(autoTask1, "toComplete" /* , map */);
        TaskImpl completeTask1 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(completeTask1, "completeTask is null");
        Assert.assertEquals(completeTask1.getName(), "CustomTasks1.complete", "unknown task found");

        hibernateSession.flush(); // need some flush here since the jobs need to
        // be in the database before we end the
        // process...
        // some more delay
        completeTask1 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        processInstance1 = completeTask1.getProcessInstance();
        Assert.assertNotNull(processInstance1, "processInstance is null");
        signalTaskWithTransData(completeTask1, "toEnd" /* , map */);

        //
        ProcessInstance processInstance2 = executionService.startProcessInstanceByKey("CustomTasks1");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance2, "processInstance is null");
        Assert.assertEquals(processInstance2.getState(), "active-root");

        final TaskImpl autoTask2 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance2.getId()).uniqueResult();
        Assert.assertNotNull(autoTask2, "autoTask is null");
        Assert.assertEquals(autoTask2.getName(), "CustomTasks1.initialize");

        signalTaskWithTransData(autoTask2, "toCreateBusinessKey" /* , map */);
        final TaskImpl completeTask2 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance2.getId()).uniqueResult();
        Assert.assertEquals(completeTask2.getName(), "CustomTasks1.complete", "unknown task found");
        Assert.assertNotNull(completeTask2, "completeTask is null");

        processInstance2 = completeTask2.getProcessInstance();
        Assert.assertNotNull(processInstance2, "processInstance2 is null");
        try {
            Thread.sleep(10000);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        signalTaskWithTransData(completeTask2, "toEnd" /* , map */);

        assertExecutionEnded(processInstance2.getId());
        
        
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        deleteUserAndTransitions(charmsUser);

        hibernateSession.flush();
        
    }

    @Test
    public void testLoopInDeployment() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testLID");
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

        final String deploymentPid = deployJpdlXmlString("<process name='CustomTasks2'>"

                + "  <start name='CustomTasks1.start'" + "         form='CustomTasks1/start.html'>"
                + "      <transition name='toInitialize' to='CustomTasks1.initialize'/>" + "  </start>"

                + "  <automaticTaskActivity name='CustomTasks1.initialize'>" + "    <transition name='toComplete' to='CustomTasks1.complete'/>"
                + "  </automaticTaskActivity>"

                + "  <customTaskActivity name='CustomTasks1.complete'" + "                      form='changerequest/complete.html'>"
                + "    <transition name='toEnd' to='CustomTasks1.end'/>" + "    <transition name='toSelf' to='CustomTasks1.complete'/>"
                + "  </customTaskActivity>"

                + "<end name='CustomTasks1.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "depoyment pid is null"); // used to
        // be "2"

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        // skip the business key:
        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasks2");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");

        final TaskImpl autoTask1 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(autoTask1, "autoTask is null");
        Assert.assertEquals(autoTask1.getName(), "CustomTasks1.initialize");

        signalTaskWithTransData(autoTask1, "toComplete" /* , map */);
        TaskImpl completeTask1 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(completeTask1, "completeTask is null");
        Assert.assertEquals(completeTask1.getName(), "CustomTasks1.complete", "unknown task found");

        for (int i = 0; i < 100; i++) {
            signalTaskWithTransData(completeTask1, "toSelf" /* , map */);
            completeTask1 = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
            Assert.assertNotNull(completeTask1, "completeTask is null");
            Assert.assertEquals(completeTask1.getName(), "CustomTasks1.complete", "unknown task found");
        }

        signalTaskWithTransData(completeTask1, "toEnd" /* , map */);
        // deleteRegisteredDeployments();

        // delete our stuff from the db so we can rerun this test
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        deleteUserAndTransitions(charmsUser);
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
