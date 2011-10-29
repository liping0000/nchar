package net.wohlfart.charms.test.changerequest;

import java.util.List;

import org.jbpm.api.ProcessInstance;
import org.jbpm.api.task.Task;
import org.testng.Assert;
import org.testng.annotations.Test;

public class WorkflowCreateTest extends AbstractWorkflowTestBase {

    @Test
    public void testUnknownProcessKeyAction() throws Exception {
        hibernateSession.flush();
        hibernateSession.clear();

        try {
            changeRequestAction.init(); // simulates a click on the "New Request" link
            changeRequestAction.initializeProcess("some unknown key here", "toComplete");
            Assert.fail("no exception for unknown process key");
        } catch (final Exception ex) {
            // expected exception
        }
        hibernateSession.flush();
        hibernateSession.clear();
    }

    @Test
    public void testToCompleteAction() throws Exception {
        hibernateSession.flush();
        hibernateSession.clear();
        
        List<ProcessInstance> pInstances;
        int baseCount;

        pInstances = processEngine.getExecutionService().createProcessInstanceQuery().list();
        baseCount = pInstances.size();

        // init the task action component
        changeRequestAction.init();

        // start a process instance
        changeRequestAction.initializeProcess(PROCESS_KEY, "toComplete");
        hibernateSession.flush();

        // lookup the instance
        pInstances = processEngine.getExecutionService().createProcessInstanceQuery().orderDesc("dbid").list();
        Assert.assertEquals(pInstances.size(), 1 + baseCount);
        final ProcessInstance instance = pInstances.get(0);
        final String key = instance.getKey();
        Assert.assertNull(key); // key should be null since the process is in
                                // the complete node now

        // lookup the next task
        final List<Task> tasksList = processEngine.getTaskService().createTaskQuery().list();
        Assert.assertEquals(pInstances.size(), 1 + baseCount);
        final Task task = tasksList.get(0);
        Assert.assertNotNull(task);
        final String taskId = task.getId();
        Assert.assertNotNull(taskId);

        changeRequestAction.setTaskDbid(taskId);
        
        // ... to be continued
        
        hibernateSession.flush();
        hibernateSession.clear();
    }

}
