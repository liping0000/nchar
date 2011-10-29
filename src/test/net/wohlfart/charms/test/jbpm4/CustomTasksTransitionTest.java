package net.wohlfart.charms.test.jbpm4;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.entities.TransitionData;
import net.wohlfart.jbpm4.node.TransitionConfig;

import org.hibernate.FlushMode;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.activity.ActivityBehaviour;
import org.jbpm.api.listener.EventListener;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.EventListenerReference;
import org.jbpm.pvm.internal.model.ObservableElementImpl;
import org.jbpm.pvm.internal.model.TransitionImpl;
import org.jbpm.pvm.internal.model.WireProperties;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 
 * 
 * 
 * @author Michael Wohlfart
 */
public class CustomTasksTransitionTest extends AbstractJbpm4TestBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CustomTasksScopeTest.class);

    public static volatile int callCounter = 0;
    public static volatile int taskCounter = 0;

    public static final class CallCollector {

        public static void callTaskCheck(final Object task) throws InterruptedException {
            if (task != null) {
                CustomTasksTransitionTest.taskCounter++;
            }
            CustomTasksTransitionTest.callCounter++;
        }

        public static void callDumpCheck() throws InterruptedException {
            Thread.dumpStack();
        }
    }

    @Test
    public void testCustomTasksScope() {
        hibernateSession.clear(); // cleanup from whatever is left in the session from former tests
        hibernateSession.flush();
        jobExecutor.stop(true);
        Assert.assertFalse(jobExecutor.isActive());
        Assert.assertTrue(hibernateSession.isOpen());        

        final long depoymentCountBefore = repositoryService.createDeploymentQuery().count();
        hibernateSession.setFlushMode(FlushMode.MANUAL);

        final CharmsUser charmsUser = new CharmsUser();
        charmsUser.setName("testScope");
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

        final String deploymentPid = deployJpdlXmlString("<process name='CustomTasksTransition'>"

                + "  <start name='CustomTasksTransition.start'" + "         form='CustomTasksTransition/start.html'>"
                + "      <transition name='auto' to='CustomTasksTransition.task1'/>" + "  </start>"

                // // task1: -----------------------
                + "  <customTaskActivity name='CustomTasksTransition.task1'" + "                      form='changerequest/task1.html'"
                + "                      spawnSignals='toSpawn'" + "                      termSignals='toTerm'" + ">"

                // to the next step with a script
                + "<transition name='toNext' to='CustomTasksTransition.task2'>" // to the next step
                + "  <script lang='groovy'><text><![CDATA["
                // + "    " + CustomTasksTransitionTest.class.getName() +
                // ".CallCollector.callDumpCheck();"
                + "    " + CustomTasksTransitionTest.class.getName() + ".CallCollector.callTaskCheck(task);"
                // +
                // "    System.err.println(\" ****  called the toNext event **** \");"
                + " ]]></text></script>" + "</transition>"

                // to the next step plain
                + "<transition name='toNext2' to='CustomTasksTransition.task2' />"  // to the next step

                // to the next step with variable included
                + "<transition name='toNext3' to='CustomTasksTransition.task2'>"  // to the next step
                // + "  <variable name='testvar' init-expr='testvalue' />"
                + "</transition>"

                // to the next step with a new property
                + "<transition name='toNext4' to='CustomTasksTransition.task2' var='testContent' />"  // to the next step

                + "    <transition name='toNext5' to='CustomTasksTransition.task2' >"  // to the next step
                + "       <transitionConfig />" + "    </transition>"

                + "    <transition name='toNext6' to='CustomTasksTransition.task2' />"  // to the next step

                + "    <transition name='toSpawn' to='CustomTasksTransition.task1'/>" // a loop

                + "  </customTaskActivity>"

                // // task2: -----------------------
                + "  <customTaskActivity name='CustomTasksTransition.task2'" 
                + "                      form='changerequest/task2.html'>"

                + "    <transition name='toEnd' to='CustomTasksTransition.end'/>" 
                + "    <transition name='toLoop' to='CustomTasksTransition.task1'/>"

                + "  </customTaskActivity>"

                + "<end name='CustomTasksTransition.end'/>"

                + "</process>");

        hibernateSession.flush();

        Assert.assertNotNull(deploymentPid, "deployment pid is null"); // used
        // to be
        // "2"

        final long depoymentCountAfter = repositoryService.createDeploymentQuery().count();
        Assert.assertEquals(depoymentCountAfter, depoymentCountBefore + 1, " deployment count before deployment is: " + depoymentCountBefore
                + " deployment count after deployment is: " + depoymentCountAfter);

        final Object object = processEngine.get("jobExecutor");
        Assert.assertNotNull(object);
        /*
        final JobExecutor jobExecutor = (JobExecutor) object;
        if (!jobExecutor.isActive()) {
            jobExecutor.start();
        }
         */

        // /////////////////////////////////
        // start
        final ProcessInstance processInstance1 = executionService.startProcessInstanceByKey("CustomTasksTransition");
        Assert.assertNotNull(processInstance1, "processInstance is null");
        Assert.assertEquals(processInstance1.getState(), "active-root");
        hibernateSession.flush();
        jobExecutor.jobWasAdded(); // the problem is we don't do commits here so
        // we have to trigger the job executor
        // manually

        TaskImpl taskImpl;
        TransitionImpl nextTransition;

        // first task
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task1");
        signalTaskWithTransData(taskImpl, "toNext"); // we need this to set the transition
        // data

        Assert.assertEquals(callCounter, 1);
        Assert.assertEquals(taskCounter, 1);

        // /////////////////////////////////
        // toNext2
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task2");
        signalTaskWithTransData(taskImpl, "toLoop"); // we need this to set the transition
        // data

        // first task again
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task1");
        signalTaskWithTransData(taskImpl, "toNext2"); // we need this to set the transition
        // data

        Assert.assertEquals(callCounter, 1);
        Assert.assertEquals(taskCounter, 1);

        // /////////////////////////////////
        // toNext3
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task2");
        signalTaskWithTransData(taskImpl, "toLoop"); // we need this to set the transition
        // data

        // first task again
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task1");

        nextTransition = taskImpl.getExecution().getActivity().getOutgoingTransition("toNext3");
        Assert.assertNotNull(nextTransition, "nextTransition is null");
        final String descToNext3 = nextTransition.getDescription();
        Assert.assertNull(descToNext3, "description used to be null");
        final ObservableElementImpl parent = nextTransition.getParent();
        Assert.assertNotNull(parent, "parent used to be not null");
        final WireProperties properties = nextTransition.getProperties();
        Assert.assertNull(properties); // null

        final TransitionImpl nextTransition5 = taskImpl.getExecution().getActivity().getOutgoingTransition("toNext5");
        Assert.assertNotNull(nextTransition);
        Assert.assertNotNull(nextTransition.getEvents());

        // the config is in the listener reference for the take event...
        // this is what this is all about:
        final EventListenerReference listenerReference = nextTransition5.getEvent("take").getListenerReferences().get(0);
        final EventListener instance = listenerReference.getEventListener();
        Assert.assertTrue(instance instanceof TransitionConfig);
        final TransitionConfig transitionConfig = (TransitionConfig) instance;
        // System.out.println("transitionConfig is: " + transitionConfig);
        Assert.assertNotNull(transitionConfig);

        final TransitionImpl nextTransition6 = taskImpl.getExecution().getActivity().getOutgoingTransition("toNext6");
        Assert.assertNotNull(nextTransition6);
        Assert.assertNotNull(nextTransition);
        Assert.assertNotNull(nextTransition.getEvents());

        signalTaskWithTransData(taskImpl, "toNext3"); // we need this to set the transition
        // data

        Assert.assertEquals(callCounter, 1);
        Assert.assertEquals(taskCounter, 1);

        // /////////////////////////////////
        // toNext4
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task2");
        signalTaskWithTransData(taskImpl, "toLoop"); // we need this to set the transition
        // data

        // first task again
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task1");
        nextTransition = taskImpl.getExecution().getActivity().getOutgoingTransition("toNext4");
        Assert.assertNotNull(nextTransition, "nextTransition is null");
        final String descToNext4 = nextTransition.getDescription();
        Assert.assertNull(descToNext4); // null
        final Object propToNext4 = nextTransition.getProperty("var");
        Assert.assertNull(propToNext4); // null
        Assert.assertNull(nextTransition.getProperties()); // null
        Assert.assertNotNull(nextTransition.getSource());
        Assert.assertNotNull(nextTransition.getDestination());
        // nextTransition.getSourceIndex()
        signalTaskWithTransData(taskImpl, "toNext4"); // we need this to set the transition
        // data
        Assert.assertEquals(callCounter, 1);
        Assert.assertEquals(taskCounter, 1);

        // ---> it seems like we can't enhance transitions with custom
        // attributes
        // or use variables inside transitions, let's see if we can get some
        // information out of a task

        final ActivityImpl activity = taskImpl.getExecution().getActivity();
        final WireProperties aProps = activity.getProperties();
        Assert.assertNull(aProps); // null
        Assert.assertNull(activity.getActivities()); // null
        final ActivityBehaviour behaviour = activity.getActivityBehaviour();
        Assert.assertNotNull(behaviour);
        Assert.assertNotNull(behaviour.getClass()); // net.wohlfart.jbpm4.activity.CustomTaskActivity
        Assert.assertNull(activity.getActivityBehaviourDescriptor());
        Assert.assertNotNull(activity.getOutgoingTransitions());

        // /////////////////////////////////
        // toNext5
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task2");
        signalTaskWithTransData(taskImpl, "toLoop"); // we need this to set the transition
        // data
        // first task again
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toNext5"); // we need this to set the transition
        // data

        // /////////////////////////////////
        // toNext6
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task2");
        signalTaskWithTransData(taskImpl, "toLoop"); // we need this to set the transition
        // data
        // first task again
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        signalTaskWithTransData(taskImpl, "toNext6"); // we need this to set the transition
        // data

        // to end
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNotNull(taskImpl, "taskImpl is null");
        Assert.assertEquals(taskImpl.getName(), "CustomTasksTransition.task2");
        signalTaskWithTransData(taskImpl, "toEnd"); // we need this to set the transition
        // data

        // end
        taskImpl = (TaskImpl) taskService.createTaskQuery().processInstanceId(processInstance1.getId()).uniqueResult();
        Assert.assertNull(taskImpl, "taskImpl is null");
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
