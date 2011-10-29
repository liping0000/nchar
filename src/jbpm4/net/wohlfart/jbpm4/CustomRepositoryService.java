package net.wohlfart.jbpm4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;
import net.wohlfart.jbpm4.command.GetActivitiesCmd;
import net.wohlfart.jbpm4.command.GetActivityCmd;
import net.wohlfart.jbpm4.command.GetActivityNamesCmd;
import net.wohlfart.jbpm4.command.GetFacetStringsCmd;
import net.wohlfart.jbpm4.command.GetLatestProcessDefinitionIdCmd;
import net.wohlfart.jbpm4.command.GetMailProducerCmd;
import net.wohlfart.jbpm4.command.GetProcessDefinitionCmd;
import net.wohlfart.jbpm4.command.GetProcessDefinitionCodeCmd;
import net.wohlfart.jbpm4.command.GetProcessDefinitionPropertiesCmd;
import net.wohlfart.jbpm4.command.GetTransitionDataCmd;
import net.wohlfart.jbpm4.command.SetProcessDefinitionCodeCmd;
import net.wohlfart.jbpm4.command.SetProcessDefinitionPropertiesCmd;
import net.wohlfart.jbpm4.mail.CustomMailProducer;
import net.wohlfart.jbpm4.node.TransitionConfig;

import org.jbpm.api.listener.EventListener;
import org.jbpm.api.model.Event;
import org.jbpm.api.model.Transition;
import org.jbpm.jpdl.internal.activity.MailActivity;
import org.jbpm.pvm.internal.email.spi.MailProducer;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.EventImpl;
import org.jbpm.pvm.internal.model.EventListenerReference;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.repository.DeploymentImpl;
import org.jbpm.pvm.internal.repository.RepositoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * we extend the jbpm4 repository service with some extra methods
 * 
 * Deployments contain a set of named resources. Those resources can represent
 * process definitions, forms, images and so on. The repository contains and
 * manages the process definitions.
 * 
 * @author Michael Wohlfart
 * 
 */
public class CustomRepositoryService extends RepositoryServiceImpl {

    private final static Logger LOGGER                  = LoggerFactory.getLogger(CustomRepositoryService.class);

    public final static String  PROCESS_DEFINITION_NAME = "name";

    // returning a list of all activity names for a process definition
    public List<String> getActivityNames(final String processDefinitionId) {
        LOGGER.debug("getActivityNames called for processDefinitionId {}", processDefinitionId);
        return commandService.execute(new GetActivityNamesCmd(processDefinitionId));
    }

    // returning the activities itself of a process definition, this is
    // jbpm internal stuff
    public List<ActivityImpl> getActivities(final String processDefinitionId) {
        LOGGER.debug("getActivities called for processDefinitionId {}", processDefinitionId);
        return commandService.execute(new GetActivitiesCmd(processDefinitionId));
    }

    // returning the activities itself of a process definition, this is
    // jbpm internal stuff
    public ActivityImpl getActivity(final String processDefinitionId, final String activityName) {
        LOGGER.debug("getActivity called for processDefinitionId {}, activityName {}", processDefinitionId, activityName);
        return commandService.execute(new GetActivityCmd(processDefinitionId, activityName));
    }

    // return a list of CharmsPermissionTarget for a processDefinition
    // a permission target is a workflow note plus all possible transition on
    // that node
    // we might add the possible signals later...
    public List<CharmsPermissionTarget> getPermissionTargets(final String processDefinitionId) {
        LOGGER.debug("getPermissionTargets called for processDefinitionId {}", processDefinitionId);
        final List<ActivityImpl> activities = getActivities(processDefinitionId);

        final List<CharmsPermissionTarget> permissionTargets = new ArrayList<CharmsPermissionTarget>();
        for (final ActivityImpl activity : activities) {
            // each activity is a target for a permission
            final CharmsPermissionTarget permissionTarget = new CharmsPermissionTarget();
            permissionTarget.setTargetString(activity.getName());
            // each transition is an action for a target
            final List<? extends Transition> transitions = activity.getOutgoingTransitions();
            for (final Transition transition : transitions) {
                final CharmsTargetAction targetAction = new CharmsTargetAction();
                targetAction.setTarget(permissionTarget);
                targetAction.setName(transition.getName());
                permissionTarget.addAction(targetAction);
            }
            // plus one action which is performing the activity itself
            final CharmsTargetAction targetAction = new CharmsTargetAction();
            targetAction.setTarget(permissionTarget);
            targetAction.setName("do"); // FIXME: constant here is ugly
            permissionTarget.addAction(targetAction);
            permissionTargets.add(permissionTarget);
        }
        return permissionTargets;
    }

    // return a list of TargetActions for a node in the processDefinition
    // a TargetActions is a string describing an action plus a string describing
    // the object
    // on which the action is performed, in this case the object is the node
    // and the actions are the transitions
    public List<CharmsTargetAction> getTargetActions(final String processDefinitionId, final String activityName) {
        LOGGER.debug("getTargetActions called for processDefinitionId {}, activityName {}", processDefinitionId, activityName);
        final List<CharmsTargetAction> result = new ArrayList<CharmsTargetAction>();
        final ActivityImpl activity = getActivity(processDefinitionId, activityName);

        // the activity is a target for a permission
        final CharmsPermissionTarget permissionTarget = new CharmsPermissionTarget();
        permissionTarget.setTargetString(activity.getName());
        // each transition is an action for a target
        final List<? extends Transition> transitions = activity.getOutgoingTransitions();
        for (final Transition transition : transitions) {
            final CharmsTargetAction targetAction = new CharmsTargetAction();
            targetAction.setTarget(permissionTarget);
            targetAction.setName(transition.getName());
            result.add(targetAction);
        }
        // plus one action which is performing the activity itself
        final CharmsTargetAction targetAction = new CharmsTargetAction();
        targetAction.setTarget(permissionTarget);
        targetAction.setName("do"); // FIXME: constant here is ugly
        result.add(targetAction);

        return result;
    }

    // returns all mail template names from a process definition
    public List<String> getMailTemplateNames(final String processDefinitionId, final String activityName) {
        LOGGER.debug("getMailTemplateNames called for processDefinitionId {}, activityName {}", processDefinitionId, activityName);
        final List<String> result = new ArrayList<String>();
        final ActivityImpl activity = getActivity(processDefinitionId, activityName);

        final ProcessDefinitionImpl processDefinition = (ProcessDefinitionImpl) commandService.execute(new GetProcessDefinitionCmd(processDefinitionId));

        // FIXME: we should move this to the custom repository service
        final Map<String, ? extends Event> events = activity.getEvents();

        if ((events == null) || (events.size() == 0)) {
            return result;
        }

        final Set<String> keys = events.keySet();
        for (final String key : keys) {
            LOGGER.debug("key: " + key);
            final EventImpl event = (EventImpl) events.get(key);
            LOGGER.debug("  value: " + event);
            final List<EventListenerReference> listenerRefs = event.getListenerReferences();
            for (final EventListenerReference listenerRef : listenerRefs) {
                LOGGER.debug("  listener: " + listenerRef);
                final EventListener listener = listenerRef.getEventListener();
                if (listener instanceof MailActivity) {
                    final MailActivity mailActivity = (MailActivity) listener;
                    // old with customized getter:
                    // MailProducer producer = mailActivity.getMailProducer();
                    // UserCodeReference mailProducerReference =
                    // mailActivity.getMailProducerReference();
                    // MailProducer mailProducer = (MailProducer)
                    // mailProducerReference.getObject(processDefinition);
                    final MailProducer mailProducer = commandService.execute(new GetMailProducerCmd(processDefinition, mailActivity));
                    if (mailProducer instanceof CustomMailProducer) {
                        final CustomMailProducer customProducer = (CustomMailProducer) mailProducer;
                        final String templateName = customProducer.getTemplateName();
                        LOGGER.debug("   templateName: " + templateName);
                        result.add(templateName);
                    }
                }
            }
        }
        return result;
    }

    public byte[] getProcessDefinitionGraph(final String processDefinitionId) {
        LOGGER.debug("getProcessDefinitionGraph called for processDefinitionId {}", processDefinitionId);
        final ProcessDefinitionImpl processDefinition = (ProcessDefinitionImpl) createProcessDefinitionQuery().processDefinitionId(processDefinitionId)
                .uniqueResult();
        final String imageResourceName = processDefinition.getImageResourceName();

        final DeploymentImpl deployment = (DeploymentImpl) createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).uniqueResult();

        final byte[] image = deployment.getBytes(imageResourceName);
        return image;
    }
    
    public String getLatestProcessDefinitionId(final String definitionName) {
        LOGGER.debug("getLatestProcessDefinitionId called for definitionName {}", definitionName);
        return commandService.execute(new GetLatestProcessDefinitionIdCmd(definitionName));        
    }

    public String getProcessDefinitionCode(final String processDefinitionId) {
        LOGGER.debug("getProcessDefinitionCode called for processDefinitionId {}", processDefinitionId);
        return commandService.execute(new GetProcessDefinitionCodeCmd(processDefinitionId));
    }

    public void setProcessDefinitionCode(final String processDefinitionId, final String processDefinitionCode) {
        LOGGER.debug("setProcessDefinitionCode called for processDefinitionId {}", processDefinitionId);
        commandService.execute(new SetProcessDefinitionCodeCmd(processDefinitionId, processDefinitionCode));
    }

    public HashMap<String, Object> getProcessDefinitionProperties(final String processDefinitionId) {
        LOGGER.debug("getProcessDefinitionProperties called for processDefinitionId {}", processDefinitionId);
        return commandService.execute(new GetProcessDefinitionPropertiesCmd(processDefinitionId));
    }

    public void setProcessDefinitionProperties(final String processDefinitionId, final HashMap<String, Object> processDefinitionProperties) {
        LOGGER.debug("setProcessDefinitionProperties called for processDefinitionId {}", processDefinitionId);
        commandService.execute(new SetProcessDefinitionPropertiesCmd(processDefinitionId, processDefinitionProperties));
    }

    public Set<String> getFacetStrings(final String processDefinitionId, final String activityName) {
        LOGGER.debug("getFacetStrings called for processDefinitionId {}, activityName {}", processDefinitionId, activityName);
        return commandService.execute(new GetFacetStringsCmd(processDefinitionId, activityName));
    }

    public TransitionConfig getTransitionConfig(final String processDefinitionId, final String activityName, final String transitionName) {
        LOGGER.debug("getTransitionData called for processDefinitionId {}, activityName {}, transitionName {}", new Object[] { processDefinitionId,
                activityName, transitionName });
        return commandService.execute(new GetTransitionDataCmd(processDefinitionId, activityName, transitionName));
    }

}
