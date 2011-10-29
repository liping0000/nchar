package net.wohlfart.jbpm4;

import org.jboss.seam.ScopeType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.entities.CharmsTargetAction;
import net.wohlfart.email.entities.CharmsEmailTemplate;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jbpm.api.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("jbpmActivitAction")
public class JbpmActivityAction extends AbstractActionBean implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmActivityAction.class);

    @In(value = "hibernateSession")
    private Session                 hibernateSession;                                          // needed
                                                                                                // to
                                                                                                // query
                                                                                                // for
                                                                                                // permissions
                                                                                                // and
                                                                                                // mail
                                                                                                // templates

    // injecting a spring bean:
    @In(value = "processEngine")
    private ProcessEngine           processEngine;

    // this is the data to identitfy am activity
    private String                  activityName;
    private String                  processDefinitionId;

    // data in the UI
    private List<TargetActionItem>  permissionActions;
    private List<EmailTemplateItem> emailTemplates;

    /*
     * this is not called from a page action on page load, it is called before
     * redirecting to the page since the page is reused when coming back from a
     * nested conversation
     */
    public Boolean setActivityIds(final String activityName, final String processDefinitionId) {
        LOGGER.debug("set activity name called, activityName: " + activityName + " processDefinitionId: " + processDefinitionId);
        this.activityName = activityName;
        this.processDefinitionId = processDefinitionId;
        return true; // "valid"
    }

    /* this is called on page load */
    public void refresh() {
        LOGGER.debug("refresh called");
        setupMailTemplate();
        setupTargetActions();
    }

    @BypassInterceptors
    public String getProcessDefinitionId() {
        LOGGER.debug("getProcessDefinitionId called: " + processDefinitionId);
        return processDefinitionId;
    }

    @BypassInterceptors
    public String getActivityName() {
        LOGGER.debug("getActivityName called: " + activityName);
        return activityName;
    }

    @BypassInterceptors
    public List<TargetActionItem> getTargetActions() {
        LOGGER.debug("getPermissionActions called: " + permissionActions);
        return permissionActions;
    }

    @BypassInterceptors
    public List<EmailTemplateItem> getEmailTemplates() {
        LOGGER.debug("getEmailTemplates called: " + emailTemplates);
        return emailTemplates;
    }

    // --- helpers

    private void setupMailTemplate() {
        // get the repository service
        final CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        // the empty targetActions for the current node
        final List<String> mailTemplateNames = repositoryService.getMailTemplateNames(processDefinitionId, activityName);

        // build the actual list of configured mail templates
        emailTemplates = new ArrayList<EmailTemplateItem>();
        for (final String templateName : mailTemplateNames) {
            emailTemplates.add(new EmailTemplateItem(templateName));
        }
    }

    private void setupTargetActions() {
        // get the repository service
        final CustomRepositoryService repositoryService = (CustomRepositoryService) processEngine.getRepositoryService();
        // the empty targetActions for the current node
        final List<CharmsTargetAction> targetActions = repositoryService.getTargetActions(processDefinitionId, activityName);

        // build the actual list of configured permissions for the UI
        permissionActions = new ArrayList<TargetActionItem>();
        for (final CharmsTargetAction targetAction : targetActions) {
            permissionActions.add(new TargetActionItem(targetAction.getTarget().getTargetString(), targetAction.getName()));
        }
    }

    /**
     * collect all mail templates for the UI
     * 
     * @author Michael Wohlfart
     */
    public class EmailTemplateItem {

        private final List<Long> ids;
        private final String     name;

        @SuppressWarnings("unchecked")
        EmailTemplateItem(final String name) {
            this.name = name;
            ids = hibernateSession.getNamedQuery(CharmsEmailTemplate.FIND_ID_BY_NAME).setParameter("name", name).list();
        }

        public List<Long> getIds() {
            return ids;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * collect all set permissions for a target and action for the UI
     * 
     * @author Michael Wohlfart
     */
    public class TargetActionItem {

        private final List<CharmsPermission> permissions;
        private final String                 target;
        private final String                 action;

        @SuppressWarnings("unchecked")
        TargetActionItem(final String target, final String action) {
            this.target = target;
            this.action = action;
            permissions = hibernateSession.getNamedQuery(CharmsPermission.FIND_BY_TARGET_AND_ACTION).setParameter("target", target)
                    .setParameter("action", action).list();
        }

        public String getTarget() {
            return target;
        }

        public String getAction() {
            return action;
        }

        public List<CharmsPermission> getPermissions() {
            return permissions;
        }
    }
}
