package net.wohlfart.charms.test.jbpm4;

import java.util.List;
import java.util.Set;

import net.wohlfart.jbpm4.CustomRepositoryService;

import org.jbpm.api.ProcessDefinition;
import org.jbpm.api.TaskService;
import org.jbpm.api.activity.ActivityBehaviour;
import org.jbpm.api.model.Transition;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.svc.ManagementServiceImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RepositoryServiceTest extends AbstractJbpm4TestBase {

    public static String NEWLINE = System.getProperty("line.separator");

    /*
     * @Test public void testWrapper() throws Exception { InputStream
     * inputStream = getClass().getResourceAsStream("ChangeRequest.jpdl.xml");
     * BufferedReader bufferedReader = new BufferedReader(new
     * InputStreamReader(inputStream)); StringBuilder stringBuilder = new
     * StringBuilder(); String line = null; while ((line =
     * bufferedReader.readLine()) != null) { stringBuilder.append(line);
     * stringBuilder.append(NEWLINE); } bufferedReader.close(); String docstring
     * = stringBuilder.toString(); Assert.assertTrue(docstring.length() > 0);
     * 
     * RepositoryService repositoryService;
     * 
     * ProcessDefinitionWrapper def = new ProcessDefinitionWrapper(docstring);
     * List<CharmsPermissionTarget> permissionTargets =
     * def.getPermissionTargets(); Assert.assertEquals(permissionTargets.size(),
     * 2);
     * 
     * }
     */

    @Test
    public void testDeploymentInfo() throws Exception {

        final CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        Assert.assertNotNull(repositoryService);
        //System.out.println("  repositoryService: " + repositoryService);
        final TaskService taskService = processEngine.getTaskService();
        Assert.assertNotNull(taskService);
        //System.out.println("  taskService: " + taskService);
        final ManagementServiceImpl managementService = (ManagementServiceImpl) processEngine.getManagementService();
        Assert.assertNotNull(managementService);
        //System.out.println("  managementService: " + managementService);

        final List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
        String deploymentId = null;
        String processDefinitionId = null;
        for (final ProcessDefinition definition : list) {
            deploymentId = definition.getDeploymentId();
            processDefinitionId = definition.getId();
            break;
        }
        Assert.assertNotNull(deploymentId);
        //System.out.println("  deploymentId: " + deploymentId);
        Assert.assertNotNull(processDefinitionId);
        //System.out.println("  processDefinitionId: " + processDefinitionId);
        final Set<String> resources = repositoryService.getResourceNames(deploymentId);
        for (final String resource : resources) {
            Assert.assertNotNull(resource);
            //System.out.println("  resource: " + resource);
        }

        final List<String> activityNames = repositoryService.getActivityNames(processDefinitionId);
        for (final String name : activityNames) {
            Assert.assertNotNull(name);
            // System.out.println("  name: " + name);
        }

        final List<ActivityImpl> activities = repositoryService.getActivities(processDefinitionId);
        for (final ActivityImpl activity : activities) {
            Assert.assertNotNull(activity);
            // System.out.println("  activity: " + activity);
            final ActivityBehaviour behaviour = activity.getActivityBehaviour();
            Assert.assertNotNull(behaviour);
            // System.out.println("  behaviour: " + behaviour);
            final List<? extends Transition> transitions = activity.getOutgoingTransitions();
            for (final Transition transition : transitions) {
                Assert.assertNotNull(transition.getName());
                // System.out.println("  transition: " + transition.getName());
            }
        }

        /*
         * activities = repositoryService.getActivities(processDefinitionId);
         * for (ActivityImpl activity : activities) {
         * System.out.println("activity: " + activity); Map<String, Event>
         * events = activity.getEvents(); Set<String> keys = events.keySet();
         * for(String key : keys) { System.out.println("  event key: " + key); }
         * }
         */

    }

}
