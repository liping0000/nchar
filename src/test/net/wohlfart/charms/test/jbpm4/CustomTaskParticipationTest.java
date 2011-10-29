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
 * testing the participationRole parameter
 * 
 * 
 * @author Michael Wohlfart
 */
public class CustomTaskParticipationTest extends AbstractJbpm4TestBase {


    @Test
    // random test failure here: java.lang.AssertionError: Error: an active process instance with id CustomTaskParticipationTest1.41 was found expected:<true> but was:<false>

    public void testTaskParticipationCandidate1() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final Variable<?> transitionChoiceVariable;

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testVariablexxx");
        charmsUser.setFirstname("testxxx");
        charmsUser.setLastname("testxxx");
        hibernateSession.persist(charmsUser);
        Contexts.getSessionContext().set("authenticatedUser", charmsUser);
        hibernateSession.flush();

        // a variable we need for assigning the next user/task
        transitionData = new TransitionData();
        // transitionData.setReceiverUserId(charmsUser.getId());
        transitionData.setReceiverUser(charmsUser);
        transitionData.setReceiverLabel(charmsUser.getLabel());
        hibernateSession.persist(transitionData);

        final String deploymentPid = deployJpdlXmlString("<process name='CustomTaskParticipationTest1'>"

                + "  <start name='CustomTaskParticipationTest1.start'"
                + "         form='CustomTaskParticipationTest1/start.html'>"
                + "      <transition name='auto' to='CustomTaskParticipationTest1.task1'/>"
                + "  </start>"

                // task1: -----------------------
                + "  <customTaskActivity name='CustomTaskParticipationTest1.task1'"
                + "                      form='CustomTaskParticipationTest1/task1.html'"
                + ">"

                + "    <transition name='toNext' to='CustomTaskParticipationTest1.task2'/>" 

                + "  </customTaskActivity>"

                
                // task1: -----------------------
                + "  <customTaskActivity name='CustomTaskParticipationTest1.task2'" 
                + "                      form='customTaskParticipationTest1/task2.html'"
                + ">"

                + "    <transition name='toEnd' to='CustomTasksVar.end'/>"

                + "  </customTaskActivity>"

                + "<end name='CustomTasksVar.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); 
        hibernateSession.flush();

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, 
                " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

//        final Object object = processEngine.get("jobExecutor");
//        Assert.assertNotNull(object);
//        final JobExecutor jobExecutor = (JobExecutor) object;
//        if (!jobExecutor.isActive()) {
//            jobExecutor.start();
//        }

        // start
        ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTaskParticipationTest1");
        hibernateSession.flush();
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");

        TaskImpl taskImpl;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "first is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTaskParticipationTest1.task1");
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run
        
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toNext");
        //executionService.signalExecutionById(processInstance1.getId(), "toNext");

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toEnd");
        //executionService.signalExecutionById(processInstance1.getId(), "toEnd");
               
        assertExecutionEnded(processInstance1.getId());
        // job executor should still be running here:
        Assert.assertFalse(jobExecutor.isActive(), "job executor not running");

        // delete our stuff from the db so we can rerun this test
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        deleteUserAndTransitions(charmsUser);
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
