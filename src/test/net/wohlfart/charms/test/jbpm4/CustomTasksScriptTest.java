package net.wohlfart.charms.test.jbpm4;

import java.util.ArrayList;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.ProcessInstance;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * this test creates a long running script on task creation, the JobImpl for the
 * taskStart event is removed from the job Executor, the activity tried to
 * remove the same JobImpl and since it started earlier it has the JobImpl still
 * in the session.
 * 
 * this is fixed by a reload(JobImpl) in the DbSessionImpl, but still creates an
 * exception during the reload ...
 * 
 * 
 * @author Michael Wohlfart
 */
public class CustomTasksScriptTest extends AbstractJbpm4TestBase {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomTasksScriptTest.class);

    public static final class CallCollector {

        public static final ArrayList<String> callStrings = new ArrayList<String>();

        public static void longCall(final String string) throws InterruptedException {
            callStrings.add(string);
            LOGGER.debug("test called, sleeping");
            // System.out.println("test calledsleeping for 10 s...");
            Thread.sleep(10 * 1000);
            // System.err.println("stacktrace follows:");
            // Thread.dumpStack();
        }

        public static void call(final String string) throws InterruptedException {
            callStrings.add(string);
            LOGGER.warn("test called");

        }
    }

    @Test
    public void testCustomTasksScript() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testScript");
        charmsUser.setFirstname("test");
        charmsUser.setLastname("test");
        hibernateSession.persist(charmsUser);
        Contexts.getSessionContext().set("authenticatedUser", charmsUser);

        // a variable we need for assigning the next user/task
        transitionData = new TransitionData();
        // transitionData.setReceiverUserId(charmsUser.getId());
        transitionData.setReceiverUser(charmsUser);
        transitionData.setReceiverLabel(charmsUser.getLabel());
        hibernateSession.persist(transitionData);
        hibernateSession.flush();

        final String deploymentPid = deployJpdlXmlString("<process name='CustomTasksScript'>"

        + "  <start name='CustomTasksScript.start'" + "         form='CustomTasksScript/start.html'>"
                + "      <transition name='auto' to='CustomTasksScript.task1'/>" + "  </start>"
                // start task to check: -----------------------
                + "  <customTaskActivity name='CustomTasksScript.task1'" + "                      form='changerequest/task1.html'>"

                // this script is executed syncronous when the token reaches
                // this task
                // and the execute method is called
                + "  <script lang='groovy'><text><![CDATA["
                // + "    System.err.println('--- before long call ---');"
                + "    " + CustomTasksScriptTest.class.getName() + ".CallCollector.longCall('test');"
                // + "    System.err.println('--- after long call ---');"
                + " ]]></text></script>"

                + "    <transition name='toEnd' to='CustomTasksScript.end'/>"

                + "  </customTaskActivity>"
                // end task to check: -----------------------
                + "<end name='CustomTasksScript.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); // used
                                                                       // to be
                                                                       // "2"

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

//        final Object object = processEngine.get("jobExecutor");
//        Assert.assertNotNull(object);
//        final JobExecutor jobExecutor = (JobExecutor) object;
//        if (!jobExecutor.isActive()) {
//            jobExecutor.start();
//        }

        // start
        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksScript");
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");
        jobExecutor.jobWasAdded(); // the problem is we don't do commits here so
                                   // we have to trigger the job executor
                                   // manually
        hibernateSession.flush();

        TaskImpl taskImpl;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "first is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksScript.task1");
        hibernateSession.flush();
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
