package net.wohlfart.charms.test.jbpm4;

import org.hibernate.FlushMode;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.task.Task;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Michael Wohlfart
 */
public class StandardSwimlaneTest extends AbstractJbpm4TestBase {

    @Test
    public void testSimpleDeployment() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final String deploymentPid = deployJpdlXmlString(
                "<process name='Swimlane'>" 
                
                + "<swimlane name='user'/>" 
                + "<start g='168,19,48,48' name='start1'>"
                + "  <transition g='-61,-18' name='to FirstTask' to='FirstTask'/>" 
                + "</start>" 
                + "<task g='155,86,92,52' name='FirstTask' swimlane='user'>"
                + "  <transition g='-64,-18' name='to timerTask' to='timerTask'/>" 
                + "</task>" 
                + "<task g='157,228,92,52' name='timerTask' swimlane='user'>"
                + "  <transition g='-42,-18' name='to end1' to='end1'>" 
                + "    <timer duedate='30 seconds' />" 
                + "  </transition>" 
                + "</task>"
                + "<end g='176,330,48,48' name='end1'/>" 
                + "</process>");

        Assert.assertNotNull(deploymentPid, "depoyment pid is null"); // used to
        hibernateSession.flush();
                                                                     
        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        final ProcessInstance processInstance = executionService.startProcessInstanceByKey("Swimlane");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance, "processInstance is null");

        Task firstTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).uniqueResult();
        Assert.assertNotNull(firstTask, "firstTask is null");

        taskService.assignTask(firstTask.getId(), "alex");
        hibernateSession.flush();

        firstTask = taskService.getTask(firstTask.getId());
        Assert.assertEquals("alex", firstTask.getAssignee());
        Assert.assertEquals("FirstTask", firstTask.getName());
        taskService.completeTask(firstTask.getId());
        hibernateSession.flush();

        final Task timerTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).uniqueResult();
        Assert.assertEquals("timerTask", timerTask.getName());
        Assert.assertEquals("alex", timerTask.getAssignee());

        // delete our stuff from the db so we can rerun this test
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        //deleteUserAndTransitions(charmsUser);
        //hibernateSession.flush();
        hibernateSession.clear();
    }
}
