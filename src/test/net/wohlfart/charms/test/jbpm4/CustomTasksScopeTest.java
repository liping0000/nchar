package net.wohlfart.charms.test.jbpm4;

import java.util.Collection;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.activity.CustomTaskActivity;
import net.wohlfart.jbpm4.entities.TransitionData;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.cmd.Command;
import org.jbpm.api.cmd.ParamCommand;
import org.jbpm.pvm.internal.cmd.CommandService;
import org.jbpm.pvm.internal.jobexecutor.AcquireJobsCmd;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.jobexecutor.JobParcel;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.jbpm.pvm.internal.jobexecutor.*;


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
public class CustomTasksScopeTest extends AbstractJbpm4TestBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CustomTasksScopeTest.class);

    public static volatile int callCounter = 0;
    public static volatile int taskCounter = 0;

    public static final class CallCollector {

        public static void callTaskCheck(final Object task) throws InterruptedException {
            if (task != null) {
                CustomTasksScopeTest.taskCounter++;
            }
            CustomTasksScopeTest.callCounter++;
        }

        public static void callDumpCheck() throws InterruptedException {
            Thread.dumpStack();
        }
    }


    // random test failures here: org.hibernate.AssertionFailure: possible nonthreadsafe access to session
    //
    @Test
    public void testCustomTasksScope() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testCustomTasksScope");
        charmsUser.setFirstname("testcts");
        charmsUser.setLastname("testcts");
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
        hibernateSession.clear();

        final String deploymentPid = deployJpdlXmlString("<process name='CustomTasksScope'>"

                + "  <start name='CustomTasksScript.start'" 
                + "         form='CustomTasksScript/start.html'>"
                + "      <transition name='auto' to='CustomTasksScope.task1'/>" 
                + "  </start>"

                // // start task to check: -----------------------
                + "  <customTaskActivity name='CustomTasksScope.task1'" 
                + "                      form='changerequest/task1.html'"
                + "                      spawnSignals='toSpawn'" 
                + "                      termSignals='toTerm'" 
                + ">"

                // this script is executed syncronous when the token reaches
                // this task and the execute() method is called in the activity
                + "  <script lang='groovy'><text><![CDATA[" + "    "
                + CustomTasksScopeTest.class.getName() + ".CallCollector.callTaskCheck(task);"
                + "    System.err.println(\" ****  called the synced script **** \");"
                + " ]]></text></script>"

                // this script is executed syncronous when the token reaches
                // this task and the execute() method is called in the activity
                + " <on event=\"" + CustomTaskActivity.TASK_NOTIFY_EVENT  + "\">"
                + "  <script lang='groovy'><text><![CDATA[" + "    "
                + CustomTasksScopeTest.class.getName() + ".CallCollector.callTaskCheck(task);"
                + "    System.err.println(\" ****  called the async notify script(1) **** \");"
                + " ]]></text></script>"
                + " </on>"

                // this script is executed synchronous when the token reaches
                // this task and the execute() method is called in the activity
                + " <on event=\""  + CustomTaskActivity.TASK_NOTIFY_EVENT   + "\">"
                + "  <script lang='groovy'><text><![CDATA[" + "    "
                + CustomTasksScopeTest.class.getName() + ".CallCollector.callTaskCheck(task);"
                + "    System.err.println(\" ****  called the async notify script(2) **** \");"
                //+ "    " + CustomTasksScopeTest.class.getName() + ".CallCollector.callDumpCheck();"
                + " ]]></text></script>"
                + " </on>"

                // this should be fired by the jBPM API when the "toNext"
                // transition is taken
                + " <on event=\"toNext\">"
                + "  <script lang='groovy'><text><![CDATA["
                //+ "    " + CustomTasksScopeTest.class.getName() + ".CallCollector.callDumpCheck();"
                + "    " + CustomTasksScopeTest.class.getName() + ".CallCollector.callTaskCheck(task);"
                + "    System.err.println(\" ****  called the toNext event **** \");"
                + " ]]></text></script>"
                + " </on>"

                // this script is executed syncronous when the token reaches
                // this task and the execute() method is called in the activity
                + " <on event=\""
                + CustomTaskActivity.TASK_END_EVENT
                + "\">"
                + "  <script lang='groovy'><text><![CDATA["
                // + "    " + CustomTasksScopeTest.class.getName() +
                // ".CallCollector.callDumpCheck();"
                + "    "
                + CustomTasksScopeTest.class.getName()
                + ".CallCollector.callTaskCheck(task);"
                + "    System.err.println(\" ****  called the sync task term script(2) **** \");" + " ]]></text></script>" + " </on>"

                + "    <transition name='toNext' to='CustomTasksScope.task2'/>" // to
                // the next step
                + "    <transition name='toSpawn' to='CustomTasksScope.task1'/>" // a
                // loop

                + "  </customTaskActivity>"
                // end task to check: -----------------------

                + "  <customTaskActivity name='CustomTasksScope.task2'" + "                      form='changerequest/task2.html'>"

                + "    <transition name='toEnd' to='CustomTasksScope.end'/>"

                + "  </customTaskActivity>"

                + "<end name='CustomTasksScope.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); // used
        // to be
        // "2"

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, 
                " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);
        //hibernateSession.flush();
        
        runJobs();


        //        final Object object = processEngine.get("jobExecutor");
        //        Assert.assertNotNull(object);
        //        final JobExecutor jobExecutor = (JobExecutor) object;
        //        if (!jobExecutor.isActive()) {
        //            jobExecutor.start();
        //        }

        // start
        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksScope");
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");
        //hibernateSession.flush();
        //jobExecutor.jobWasAdded(); // the problem is we don't do commits here so
        // we have to trigger the job executor
        // manually

        TaskImpl taskImpl;

        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "first is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksScope.task1");
        //jobExecutor.jobWasAdded();
        try {
            Thread.sleep(4000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run
        //hibernateSession.flush();
        runJobs();              
        hibernateSession.flush();

        // failure here!!!
        Assert.assertEquals(callCounter, 3);
        Assert.assertEquals(taskCounter, 3); // one sync script call on task
        // execute and two notify events
        // triggered by a timer here

        // wait for the notify timer to kick in
        // try { Thread.sleep(1000); } catch (final InterruptedException ex) {
        // ex.printStackTrace(); }
        //System.err.println("hibernateSession before: " +  hibernateSession);
        signalTaskWithTransData(taskImpl, "toSpawn" /* , map */);
        //hibernateSession.refresh(processInstance1); // get the processInstance1 back into the session
        
        runJobs();
        //jobExecutor.jobWasAdded();
        hibernateSession.flush();
        //System.err.println("hibernateSession after: " +  hibernateSession);

        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        } // need time for the groovy scripts to run
        Assert.assertEquals(callCounter, 6);
        Assert.assertEquals(taskCounter, 6); // a spawn loop: plus 3 again, we
        // should have 2 tasks right now
        // one process instance and one execution instance

        Assert.assertEquals(processInstance1.getExecutions().size(), 1); // why 0 ??

        // this is the spawned execution
        final ExecutionImpl execution = (ExecutionImpl) processInstance1.getExecutions().toArray()[0];
        Assert.assertNotNull(execution);

        // wait for the notify timer to kick in
        hibernateSession.flush();
        // try { Thread.sleep(1000); } catch (final InterruptedException ex) {
        // ex.printStackTrace(); }
        // simulate the user selecting task from the spawned execution
        taskImpl = (TaskImpl) taskService.createTaskQuery().executionId(execution.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toTerm" /* , map */); // terminate the execution
        jobExecutor.jobWasAdded();
        hibernateSession.flush();

        // should be sync, but anyways...
        // try { Thread.sleep(1000); } catch (final InterruptedException ex) {
        // ex.printStackTrace(); }
        Assert.assertEquals(callCounter, 7);
        Assert.assertEquals(taskCounter, 7); // plus task end event here

        // select the remaining task
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        // wait for the notify timer to kick in
        // try { Thread.sleep(1000); } catch (final InterruptedException ex) {
        // ex.printStackTrace(); }
        signalTaskWithTransData(taskImpl, "toNext" /* , map */);
        runJobs();

        Assert.assertEquals(callCounter, 8);
        Assert.assertEquals(taskCounter, 8); // plus task end event here

        // select the remaining task again
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        // wait for the notify timer to kick in
        // try { Thread.sleep(1000); } catch (final InterruptedException ex) {
        // ex.printStackTrace(); }
        signalTaskWithTransData(taskImpl, "toEnd" /* , map */);
        //jobExecutor.jobWasAdded();
        //hibernateSession.flush();
        runJobs();

        Assert.assertEquals(callCounter, 8);
        Assert.assertEquals(taskCounter, 8); // plus task end event here

        assertExecutionEnded(processInstance1.getId());
        //jobExecutor.jobWasAdded();
        //hibernateSession.flush();

        // job executor should still be running here:
        Assert.assertFalse(jobExecutor.isActive(), "job executor not running");

        // delete our stuff from the db so we can rerun this test
        hibernateSession.flush();
        deleteRegisteredDeployments();
        hibernateSession.flush();
        Long id = charmsUser.getId();
        charmsUser = (CharmsUser) hibernateSession.get(CharmsUser.class, id);
        deleteUserAndTransitions(charmsUser);
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
