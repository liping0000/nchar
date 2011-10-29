package net.wohlfart.admin;

import org.jboss.seam.ScopeType;

import java.util.List;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;
import net.wohlfart.framework.AbstractEntityHome;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("charmsPermissionTargetActionBean")
public class CharmsPermissionTargetActionBean extends AbstractEntityHome<CharmsPermissionTarget> {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsPermissionTargetActionBean.class);

    private static final String CHARMS_PERMISSION_TARGET = "charmsPermissionTarget";

    private List<CharmsTargetAction> actions;

    private String description;

    @Override
    protected String getNameInContext() {
        return CHARMS_PERMISSION_TARGET;
    }

    @Transactional
    @Factory(value = CHARMS_PERMISSION_TARGET)
    public CharmsPermissionTarget getCharmsPermissionTarget() {
        LOGGER.debug("getCharmsPermissionTarget called");
        final CharmsPermissionTarget charmsPermissionTarget = getInstance();
        return charmsPermissionTarget;
    }

    @Override
    @Create
    // parent create method also...
    public void create() {
        super.create();
    }

    // init the entity and this action bean
    @Override
    @Transactional
    public void initInstance() {
        super.initInstance();

        final CharmsPermissionTarget charmsPermissionTarget = getInstance();
        actions = charmsPermissionTarget.getActions();
        Hibernate.initialize(actions);
        LOGGER.debug("initInstance called: {}", actions);
        
        // this might be the name of a user or the name of a role
        // String recipientName = charmsPermission.getRecipient();
        LOGGER.debug("charmsPermissionTarget: {} ", charmsPermissionTarget);
    }

    @Override
    @Transactional
    public String update() {
        LOGGER.debug("update called for action: {}", actions);
        Session session = getSession();
        // update all actions
        for (CharmsTargetAction action : actions) {
            LOGGER.debug("saveOrUpdate, id is: {}", action.getId());
            session.saveOrUpdate(action);
        }
        LOGGER.debug("session is: {}", getSession());
        String result = "invalid";
        result = super.update();
        // Remove from Conversation context and clear the home
        session.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "updated"
    }

    @Override
    @Transactional
    public String persist() {
        String result = "invalid";
        CharmsPermissionTarget permissionTarget = getInstance();     
        if (StringUtils.isEmpty(permissionTarget.getTargetString())) {
            LOGGER.info("empty target string");
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "empty target string");
            return "invalid";
        }
        // persist all action strings
        Session session = getSession();
        session.persist(permissionTarget);
        
        // save all actions, add the target
        permissionTarget = getInstance();
        for (CharmsTargetAction action : actions) {
            LOGGER.debug("saveOrUpdate, id; {}", action.getId());
            action.setTarget(permissionTarget);
            session.persist(action);
        }
             
        try {
            result = super.persist();
        } catch (Exception ex) {
            return "invalid";
        }
        // Remove from Conversation context and clear the home
        session.flush();
        // setId(permission.getId());
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "persisted"
    }

    @BypassInterceptors
    public String getDescription() {
        return description;
    }

    @Override
    @Transactional
    public String remove() {
        if (getId() == null) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "No entity to delete, id is null");
            return "error";
        }
        final String result = super.remove();
        // Remove from Conversation context and clear the home
        Session session = getSession();
        session.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "removed"
    }

    @Override
    public String cancel() {
        final String result = super.cancel();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result;
    }

    @BypassInterceptors
    public List<CharmsTargetAction> getActions() {
        LOGGER.debug("get action called, actions are: {}", actions);
        return actions;
    }

    @BypassInterceptors
    public void addAction() {
        LOGGER.debug("add action called");
        // adding a new blank action to the actions hashmap
        final CharmsTargetAction targetAction = new CharmsTargetAction();
        targetAction.setTarget(getInstance());
        actions.add(targetAction);
        // can't persist here since the name is still null
        //getSession().persist(targetAction);
    }

    @BypassInterceptors
    public void delAction() {
        LOGGER.debug("add action called");
        // remove the last action in the array
        int position = actions.size() - 1;
        if (position >= 0) {
            CharmsTargetAction delAction = actions.get(position);
            //delAction.setTarget(null);
            LOGGER.debug("removing target action with id: {}", delAction.getId());
            actions.remove(position);
            getSession().delete(delAction);
            //getSession().evict(delAction);
        }     
    }

    @BypassInterceptors
    public Boolean getMultiActions() {
        return (actions.size() > 0);
    }
}
