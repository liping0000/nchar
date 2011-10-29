package net.wohlfart.charms.test.jbpm4;

import java.util.List;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.ProcessInstance;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 
 * 
 * 
 * @author Michael Wohlfart
 */
public class CustomExceptionHandlerTest extends AbstractJbpm4TestBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CustomTasksScopeTest.class);

    public static volatile int callCounter = 0;
    public static volatile int taskCounter = 0;

    public static final class CallCollector {

        public static void callTaskCheck(final Object task) throws InterruptedException {
            if (task != null) {
                CustomExceptionHandlerTest.taskCounter++;
            }
            CustomExceptionHandlerTest.callCounter++;
        }

        public static void callDumpCheck() throws InterruptedException {
            Thread.dumpStack();
        }
    }

    @Test
    public void testExceptionHandler() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testExceptionHandler");  // max 20 chars here !
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
        hibernateSession.flush();

        final String deploymentPid = deployJpdlXmlString("<process name='CustomExceptionHandler'>"

        + "  <start name='CustomExceptionHandler.start'" + "         form='CustomExceptionHandler/start.html'>"
                + "      <transition name='auto' to='CustomExceptionHandler.task1'/>" + "  </start>"

                // // task1: -----------------------
                + "  <customTaskActivity name='CustomExceptionHandler.task1'" + "                      form='changerequest/task1.html'"
                + "                      spawnSignals='toSpawn'" + "                      termSignals='toTerm'" + ">"

                // to the next step with a script
                + "<transition name='toNext' to='CustomExceptionHandler.task2'>" // to
                                                                                 // the
                                                                                 // next
                                                                                 // step
                + "  <script lang='groovy'><text><![CDATA["
                // + "    " + CustomTasksTransitionTest.class.getName() +
                // ".CallCollector.callDumpCheck();"
                + "    " + CustomExceptionHandlerTest.class.getName() + ".CallCollector.callTaskCheck(task);"
                // +
                // "    System.err.println(\" ****  called the toNext event **** \");"
                + " ]]></text></script>" + "</transition>"

                + "    <transition name='toSpawn' to='CustomExceptionHandler.task1'/>" // a
                                                                                       // loop

                + "  </customTaskActivity>"

                // // task2: -----------------------
                + "  <customTaskActivity name='CustomExceptionHandler.task2'" + "                      form='changerequest/task2.html'>"

                + "    <transition name='toEnd' to='CustomExceptionHandler.end'/>" + "    <transition name='toLoop' to='CustomExceptionHandler.task1'/>"

                + "  </customTaskActivity>"

                + "<end name='CustomExceptionHandler.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); // used
                                                                       // to be
                                                                       // "2"

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);



        // /////////////////////////////////
        // start
        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomExceptionHandler");
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");
        hibernateSession.flush();
        jobExecutor.jobWasAdded(); // the problem is we don't do commits here so
                                   // we have to trigger the job executor
                                   // manually

        TaskImpl taskImpl;

        // first task
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomExceptionHandler.task1");
        signalTaskWithTransData(taskImpl, "toNext"); // we need this to set the transition
                                        // data

        Assert.assertEquals(callCounter, 1);
        Assert.assertEquals(taskCounter, 1);

        // to end
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomExceptionHandler.task2");
        signalTaskWithTransData(taskImpl, "toEnd"); // we need this to set the transition
                                       // data

        // end
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNull(taskImpl, "taskImpl is null");
        //hibernateSession.flush();
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
