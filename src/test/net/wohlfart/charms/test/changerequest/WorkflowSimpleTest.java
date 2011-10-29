package net.wohlfart.charms.test.changerequest;

import java.util.List;

import net.wohlfart.changerequest.ChangeRequestAction;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.task.Task;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * simple and straight forward changerequest test
 * 
 * @author Michael Wohlfart
 * 
 */
public class WorkflowSimpleTest extends AbstractWorkflowTestBase {

    @Test
    public void testSimpleToCompleteAction1() throws Exception {
       
        hibernateSession.flush();
        hibernateSession.clear();     
        changeRequestAction = (ChangeRequestAction) Component.getInstance("changeRequestAction");

        String result;
        // get the number of running processes
        int count = 0;
        List<ProcessInstance> ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        count += ps.size();
        ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        Assert.assertEquals(ps.size(), count);

        // ///////////// start process1 ///////////
    
        // init the task action component
        changeRequestAction.init();

        // start process instance1
        result = changeRequestAction.initializeProcess(PROCESS_KEY, "toComplete");
        Assert.assertEquals(result, "toCompleteDone");
//        hibernateSession.flush();

        // lookup the instance the one with the last id should be the new one...
        final List<ProcessInstance> pInstances1 = processEngine  // order by id doesn't work here since it contains an unpadded number
                .getExecutionService()
                .createProcessInstanceQuery()
                .orderAsc("dbid")
                .list();
        Assert.assertEquals(pInstances1.size(), count + 1);
         ProcessInstance instance1 = pInstances1.get(pInstances1.size() - 1);
        final String key1 = instance1.getKey();
        Assert.assertNull(key1);
        
        runJobs();
        runJobs();
        runJobs();
        removeExecution(instance1.getId());

        hibernateSession.flush();
        hibernateSession.clear();
    }
       

    @Test
    public void testSimpleToCompleteAction2() throws Exception {
       
        hibernateSession.flush();
        hibernateSession.clear();     
        changeRequestAction = (ChangeRequestAction) Component.getInstance("changeRequestAction");

        String result;
        // get the number of running processes
        int count = 0;
        List<ProcessInstance> ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        count += ps.size();
        ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        Assert.assertEquals(ps.size(), count);

        // ///////////// start process2 ///////////

        // init the task action component
        changeRequestAction.init();

        // start process instance1
        result = changeRequestAction.initializeProcess(PROCESS_KEY, "toComplete");       
        Assert.assertEquals(result, "toCompleteDone");
        hibernateSession.flush();
        
        // lookup the instance the one with the last id should be the new one...
        final List<ProcessInstance> pInstances2 = processEngine
            .getExecutionService()
            .createProcessInstanceQuery()
            .orderAsc("dbid")
            .list();
        Assert.assertEquals(pInstances2.size(), count + 1);
        final ProcessInstance instance2 = pInstances2.get(pInstances2.size() - 1);
        final String key2 = instance2.getKey();
        Assert.assertNull(key2);

        // / lookup for tasks within the processes
        //final Task task1 = processEngine.getTaskService().createTaskQuery().processInstanceId(instance1.getId()).uniqueResult();
        //Assert.assertEquals(task1.getActivityName(), "changerequest.complete");
        final Task task2 = processEngine.getTaskService().createTaskQuery().processInstanceId(instance2.getId()).uniqueResult();
        Assert.assertEquals(task2.getActivityName(), "changerequest.complete");
        
        removeExecution(instance2.getId());

        hibernateSession.flush();
        hibernateSession.clear();
    }
       

    @Test
    public void testSimpleToCompleteAction3() throws Exception {
       
        hibernateSession.flush();
        hibernateSession.clear();     
        changeRequestAction = (ChangeRequestAction) Component.getInstance("changeRequestAction");

        String result;
        // get the number of running processes
        int count = 0;
        List<ProcessInstance> ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        count += ps.size();
        ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        Assert.assertEquals(ps.size(), count);
      
        // ///////////// start process3 ///////////

        // init the task action component
        changeRequestAction.init();

        // start process instance1
        result = changeRequestAction.initializeProcess(PROCESS_KEY, "toComplete");
        Assert.assertEquals(result, "toCompleteDone");
        hibernateSession.flush();

        // lookup the instance the one with the last id should be the new one...
        final List<ProcessInstance> pInstances3 = processEngine.getExecutionService().createProcessInstanceQuery().orderAsc("dbid").list();
        Assert.assertEquals(pInstances3.size(), count + 1);
        final ProcessInstance instance3 = pInstances3.get(pInstances3.size() - 1);
        final String key3 = instance3.getKey();
        Assert.assertNull(key3);

        // / lookup for tasks within the processes
        //final Task task3 = processEngine.getTaskService().createTaskQuery().processInstanceId(instance1.getId()).uniqueResult();
        //Assert.assertEquals(task3.getActivityName(), "changerequest.complete");
        //final Task task4 = processEngine.getTaskService().createTaskQuery().processInstanceId(instance2.getId()).uniqueResult();
        //Assert.assertEquals(task4.getActivityName(), "changerequest.complete");
        final Task task5 = processEngine.getTaskService().createTaskQuery().processInstanceId(instance3.getId()).uniqueResult();
        Assert.assertEquals(task5.getActivityName(), "changerequest.complete");

        removeExecution(instance3.getId());

        hibernateSession.flush();
        hibernateSession.clear();

        Contexts.getConversationContext().flush();
    }

    
    
    @Test
    public void testToCompleteMultiAction() throws Exception {
        String result;
        // get the number of running processes
        int count = 0;
        List<ProcessInstance> ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        count += ps.size();
        ps = processEngine.getExecutionService().createProcessInstanceQuery().list();
        Assert.assertEquals(ps.size(), count);

        // ///////////// start process1 ///////////

        // init the task action component
        changeRequestAction.init();

        // start process instance1
        result = changeRequestAction.initializeProcess(PROCESS_KEY, "toComplete");
        Assert.assertEquals(result, "toCompleteDone");
        hibernateSession.flush();

        // lookup the instance the one with the last id should be the new one...
        final List<ProcessInstance> pInstances1 = processEngine
        // order by id doesn't work here since it contains an unpadded
        // number
                .getExecutionService().createProcessInstanceQuery().orderAsc("dbid").list();
        Assert.assertEquals(pInstances1.size(), count + 1);
        final ProcessInstance instance1 = pInstances1.get(pInstances1.size() - 1);
        final String key1 = instance1.getKey();
        Assert.assertNull(key1);

        // ///////////// start process2 ///////////

        // init the task action component
        changeRequestAction.init();

        // start process instance1
        changeRequestAction.initializeProcess(PROCESS_KEY, "toComplete");
        Assert.assertEquals(result, "toCompleteDone");
        hibernateSession.flush();

        // lookup the instance the one with the last id should be the new one...
        final List<ProcessInstance> pInstances2 = processEngine.getExecutionService().createProcessInstanceQuery().orderAsc("dbid").list();
        Assert.assertEquals(pInstances2.size(), count + 2);
        final ProcessInstance instance2 = pInstances2.get(pInstances2.size() - 1);
        final String key2 = instance2.getKey();
        Assert.assertNull(key2);

        hibernateSession.flush();

        // / lookup for tasks within the processes
        final Task task1 = processEngine.getTaskService().createTaskQuery().processInstanceId(instance1.getId()).uniqueResult();
        Assert.assertEquals(task1.getActivityName(), "changerequest.complete");
        final Task task2 = processEngine.getTaskService().createTaskQuery().processInstanceId(instance2.getId()).uniqueResult();
        Assert.assertEquals(task2.getActivityName(), "changerequest.complete");
               
        hibernateSession.flush();
        
        Assert.assertNotSame(instance1.getId(), instance2.getId());

        removeExecution(instance1.getId());
        removeExecution(instance2.getId());

        hibernateSession.flush();
        hibernateSession.clear();

    }
}
