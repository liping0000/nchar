package net.wohlfart.admin;

import org.jboss.seam.ScopeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.PermissionTargetCollection;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.targets.ProductInstanceTargetSetup;
import net.wohlfart.authorization.targets.SeamRoleInstanceTargetSetup;
import net.wohlfart.authorization.targets.SeamUserInstanceTargetSetup;
import net.wohlfart.framework.AbstractEntityHome;
import net.wohlfart.refdata.entities.ChangeRequestProduct;

import org.apache.commons.lang.StringUtils;
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
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: use the seam permission store to manage permission, or check at least
// the permissions for security related actions here!!
@Scope(ScopeType.CONVERSATION)
@Name("charmsPermissionActionBean")
public class CharmsPermissionActionBean extends AbstractEntityHome<CharmsPermission> {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsPermissionActionBean.class);

    private static final String CHARMS_PERMISSION = "charmsPermission";

    @RequestParameter
    private String predefinedTarget;

    @RequestParameter
    private String predefinedAction;

    private PermissionTargetCollection permissionTargetCollection;

    // contains the list of keys for the targets
    private List<String> targetStrings = new ArrayList<String>();

    private String recipientActorId;
    private String recipientLabel;  
    
    private String targetString;
    private Long targetId;
    private String targetLabel;

    // the available actions for a specific target
    // and their status, we don't select actions we just enable or disable them
    private HashMap<String, Boolean> availableActions = new HashMap<String, Boolean>();

    private String description;

    @Override
    protected String getNameInContext() {
        return CHARMS_PERMISSION;
    }

    @Transactional
    @Factory(value = CHARMS_PERMISSION)
    public CharmsPermission getCharmsPermission() {
        LOGGER.debug("getCharmsPermission called");
        final CharmsPermission charmsPermission = getInstance();
        return charmsPermission;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Create
    // parent create method also...
    @Transactional
    public void create() {
        super.create();
        final Session hibernateSession = getSession();
        permissionTargetCollection = new PermissionTargetCollection(hibernateSession);
        // the string keys for the targets of all possible targets for this app
        targetStrings = new ArrayList(permissionTargetCollection.keySet());
        LOGGER.debug("targetStrings are: {}, size is {}", targetStrings, targetStrings.size());
        Collections.sort(targetStrings);
    }

    // init the entity and this action bean
    @Override
    @Transactional
    public void initInstance() {
        super.initInstance();
        final Session hibernateSession = getSession();

        final CharmsPermission charmsPermission = getInstance();

        // this might be the name of a user or the name of a role
        // String recipientName = charmsPermission.getRecipient();
        LOGGER.debug("charmsPermission: {} ", charmsPermission);

        if (!isManaged()) {
            // new creation, no discriminator yet
            recipientLabel = " - ";
            recipientActorId = null;
        } else if (charmsPermission.isRolePermission()) {
            final String recipientName = charmsPermission.getRecipient();
            // lookup the role table
            CharmsRole charmsRole = (CharmsRole) hibernateSession
                .getNamedQuery(CharmsRole.FIND_BY_NAME)
                .setParameter("name", recipientName)
                .uniqueResult();
            recipientLabel = charmsRole.getLabel();
            recipientActorId = charmsRole.getActorId();
        } else if (charmsPermission.isUserPermission()) {
            final String recipientName = charmsPermission.getRecipient();
            // lookup the user table
            CharmsUser charmsUser = (CharmsUser) hibernateSession
                .getNamedQuery(CharmsUser.FIND_BY_NAME)
                .setParameter("name", recipientName)
                .uniqueResult();
            recipientLabel = charmsUser.getLabel();
            recipientActorId = charmsUser.getActorId();
        } else {
            LOGGER.warn("problem initializing permission, unknown discriminator: {}", charmsPermission);
            recipientLabel = " - ";
            recipientActorId = null;
        }

        // target
        LOGGER.debug("predefined target: >{}<", predefinedTarget);
        // FIXME: guess we don't need the if check since we can set null
        if (predefinedTarget != null) { 
            charmsPermission.setTarget(predefinedTarget);
        }
        targetString = charmsPermission.getTarget();
        targetId = charmsPermission.getTargetId();
        if (SeamUserInstanceTargetSetup.TARGET_STRING.equals(targetString)) {
            CharmsUser charmsUser = (CharmsUser) hibernateSession
            .getNamedQuery(CharmsUser.FIND_BY_ID)
            .setParameter("id", targetId)
            .uniqueResult();    
            targetLabel = charmsUser.getLabel();
        } else if (SeamRoleInstanceTargetSetup.TARGET_STRING.equals(targetString)) {
            CharmsRole charmsRole = (CharmsRole) hibernateSession
            .getNamedQuery(CharmsRole.FIND_BY_ID)
            .setParameter("id", targetId)
            .uniqueResult();            
            targetLabel = charmsRole.getLabel();
        } else if (ProductInstanceTargetSetup.TARGET_STRING.equals(targetString)) {
            ChangeRequestProduct changeRequestProduct = (ChangeRequestProduct) hibernateSession
            .getNamedQuery(ChangeRequestProduct.FIND_BY_ID)
            .setParameter("id", targetId)
            .uniqueResult();   
            targetLabel = changeRequestProduct.getDefaultName();
        } else {
            targetLabel = null;
        }
        

        // actions
        LOGGER.debug("predefined action: >{}<", predefinedAction);
        // FIXME: guess we don't need the if check since we can set null
        if (predefinedAction != null) { 
            charmsPermission.setAction(predefinedAction);
        }
        final String actions = charmsPermission.getAction();
        LOGGER.debug("got actions: {} ", actions);

        setupAvailableActionForTarget(actions);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public String update() {
        final String actionString = getActionString();
        if (StringUtils.isEmpty(actionString)) {
            LOGGER.info("no action selected");
            final FacesMessages facesMessages = FacesMessages.instance();            
            facesMessages.addFromResourceBundle(Severity.WARN, "charmsPermissionActionBean.noActionSelected");                                                                     
            return "invalid";
        }
        final Session hibernateSession = getSession();

        // the problem here might be that we store a permission for a
        // recipient/target combination that is already in the database
        // but is not the permission that was originaly loaded or may be
        // completely new
        // because the user is allowed to change any attribute of the permission
        // so we have to first check if the combination is already existent

        final CharmsPermission permission = getInstance();
        setupRecipientAndDiscriminator(permission);
        permission.setTarget(targetString);
        permission.setAction(actionString);

        List<CharmsPermission> permissions = new ArrayList<CharmsPermission>();
        if (permission.isRolePermission()) {
            permissions = hibernateSession
                .getNamedQuery(CharmsPermission.FIND_BY_RECIPIENT_ROLE_AND_TARGET)
                .setParameter("recipient", permission.getRecipient())
                .setParameter("target", permission.getTarget())
                .list();
        } else if (permission.isUserPermission()) {
            permissions = hibernateSession
                .getNamedQuery(CharmsPermission.FIND_BY_RECIPIENT_USER_AND_TARGET)
                .setParameter("recipient", permission.getRecipient())
                .setParameter("target", permission.getTarget())
                .list();
            // } else if (permission.isRoleOnlyPermission()) {
            // permissions = hibernateSession
            // .getNamedQuery(
            // CharmsPermission.FIND_BY_RECIPIENT_ROLEONLY_AND_TARGET)
            // .setParameter("recipient", permission.getRecipient())
            // .setParameter("target", permission.getTarget()).list();
        } else {
            LOGGER.warn("unknown discriminator for permission: >{}<", permission.getDiscriminator());
        }

        String result = "invalid";

        if ((permissions == null) || (permissions.size() == 0)) {
            LOGGER.debug("changing permission in db");
            // the permission was changed completely, that's fine
            // the old one will be replaced
            result = super.persist();
            // entityManager.flush();
        } else if (permissions.size() == 1) {
            // there is already a permission in db like the current one,
            // we either have to modify if the one in db is the current one
            // or remove the current and modify the one in db

            LOGGER.debug("replacing db permission");
            // we have one permission which may or may not be the one
            // we are editing
            final CharmsPermission dbPermission = permissions.get(0);

            if (dbPermission.getId().equals(permission.getId())) {
                LOGGER.debug("replacing same db permission");
                // same permission, only targets were changed
                result = super.persist();
                // flush();
                result = "updated";
            } else {
                // different permission, we have to delete the current and
                // modify the one in DB
                LOGGER.debug("removing current, replacing in db permission");
                super.remove();
                dbPermission.setAction(permission.getAction());
                hibernateSession.persist(dbPermission);
                // flush();
                result = "updated";
            }
        } else {
            LOGGER.warn("multiple permissions found for: " + " recipient: {} " + " discriminator: {} " + " target: {} ",
                    new Object[] { permission.getRecipient(), permission.getDiscriminator(), permission.getTarget() });
        }

        // Remove from Conversation context and clear the home
        hibernateSession.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "updated"
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public String persist() {
        final String actionString = getActionString();
        if (StringUtils.isEmpty(actionString)) {
            LOGGER.info("no action selected");
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.addFromResourceBundle(Severity.WARN, "charmsPermissionActionBean.noActionSelected");                                                                     
            return "invalid";
        }
        if (StringUtils.isEmpty(recipientActorId)) {
            LOGGER.info("no recipient selected");
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.addFromResourceBundle(Severity.WARN, "charmsPermissionActionBean.noRecipientSelected");                                                                     
            return "invalid";
        }
        final Session hibernateSession = getSession();

        // another problem here might be that we store a permission for a
        // recipient/target combination that is already in the database
        // so we have to first check if the combination is already existent

        final CharmsPermission permission = getInstance();
        setupRecipientAndDiscriminator(permission);
        permission.setTarget(targetString);
        permission.setTargetId(targetId);
        permission.setAction(actionString);

        // persist means this permission should not already be in the database
        // if the same permission is already there, we override the permission
        // in the db
        // the permission in db may have different actions
        List<CharmsPermission> permissions = new ArrayList<CharmsPermission>();
        if (permission.isRolePermission()) {
            permissions = hibernateSession
                .getNamedQuery(CharmsPermission.FIND_BY_RECIPIENT_ROLE_AND_TARGET)
                .setParameter("recipient", permission.getRecipient())
                .setParameter("target", permission.getTarget())
                .list();
        } else if (permission.isUserPermission()) {
            permissions = hibernateSession
                .getNamedQuery(CharmsPermission.FIND_BY_RECIPIENT_USER_AND_TARGET)
                .setParameter("recipient", permission.getRecipient())
                .setParameter("target", permission.getTarget())
                .list();
            // } else if (permission.isRoleOnlyPermission()) {
            // permissions = hibernateSession
            // .getNamedQuery(
            // CharmsPermission.FIND_BY_RECIPIENT_ROLEONLY_AND_TARGET)
            // .setParameter("recipient", permission.getRecipient())
            // .setParameter("target", permission.getTarget()).list();
        } else {
            LOGGER.warn("unknown discriminator for permission: >{}<", permission.getDiscriminator());
        }

        String result = "invalid";

        if ((permissions == null) || (permissions.size() == 0)) {
            // LOGGER.debug("inserting a new permission");
            // everything fine, nothing in the db
            result = super.persist();
            // entityManager.flush();
        } else if (permissions.size() == 1) {
            // LOGGER.debug("updating an existing permission");
            // oh-ooh already in the db
            final CharmsPermission dbPermission = permissions.get(0);
            // update the permission in the db instead of persisting a new one:
            dbPermission.setAction(getActionString());
            // the other propeties are unchanged
            // flush();
            result = "persisted";
        } else {
            LOGGER.warn("multiple permissions found for: " + " recipient: {} " + " discriminator: {} " + " target: {} ",
                    new Object[] { permission.getRecipient(), permission.getDiscriminator(), permission.getTarget() });
        }

        // Remove from Conversation context and clear the home
        hibernateSession.flush();
        // setId(permission.getId());
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "persisted"
    }

    private void setupAvailableActionForTarget(final String allowedActions) {
        // LOGGER.debug("setTargetString: " + targetString);
        // LOGGER.debug("permissionTargetCollection: " +
        // permissionTargetCollection);

        String[] selected = new String[] {};
        if ((allowedActions != null) && (allowedActions.trim().length() > 0)) {
            selected = StringUtils.split(allowedActions, ",");
        }
        final Set<String> map = new HashSet<String>();
        for (final String string : selected) {
            map.add(string.trim());
        }

        availableActions = new HashMap<String, Boolean>();
        description = "";
        if (permissionTargetCollection.containsKey(targetString)) {
            // LOGGER.debug("targetString found in permissionTargetCollection");

            final String[] actions = permissionTargetCollection.get(targetString).getAllActions();
            for (final String action : actions) {
                availableActions.put(action, map.contains(action));
            }

            description = permissionTargetCollection.get(targetString).getDescription();
        }
    }

    @BypassInterceptors
    public String getDescription() {
        return description;
    }

    @BypassInterceptors
    public String getRecipientLabel() {
        return recipientLabel;
    }

    @BypassInterceptors
    public void setRecipientLabel(final String recipientLabel) {
        this.recipientLabel = recipientLabel;
    }

    // called from persist() and update() to setup the
    // discriminator and recipient fields in the permission
    private void setupRecipientAndDiscriminator(final CharmsPermission permission) {
        final Session hibernateSession = getSession();
        if (StringUtils.isEmpty(recipientActorId)) {
            LOGGER.warn("no recipientActorId found in permission");
            // none selected FIXME: show some message here
        } else if (recipientActorId.startsWith(CharmsRole.GROUP_ACTOR_PREFIX)) {
            // lookup the role table
            final String roleString = hibernateSession
                .getNamedQuery(CharmsRole.FIND_NAME_BY_ACTOR_ID)
                .setParameter("actorId", recipientActorId)
                .uniqueResult()
                .toString();
            permission.setRecipient(roleString);
            permission.setDiscriminator(CharmsPermission.ROLE);
        } else if (recipientActorId.startsWith(CharmsUser.ACTOR_PREFIX)) {
            // lookup the user table
            final String userString = hibernateSession
                .getNamedQuery(CharmsUser.FIND_NAME_BY_ACTOR_ID)
                .setParameter("actorId", recipientActorId)
                .uniqueResult()
                .toString();
            permission.setRecipient(userString);
            permission.setDiscriminator(CharmsPermission.USER);
        } else {
            LOGGER.warn("can't figure out discriminator value, recipient actor id is: >{}<, should start with >{}< or >{}<", 
                    new Object[] { recipientActorId, CharmsRole.GROUP_ACTOR_PREFIX, CharmsUser.ACTOR_PREFIX });
        }
    }

    private String getActionString() {
        final Set<String> keys = availableActions.keySet();
        final StringBuffer buf = new StringBuffer();
        // String result = "";
        for (final String key : keys) {
            if (availableActions.get(key)) {
                // a selected action
                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(key);
            }
        }
        return buf.toString();
    }

    @Override
    @Transactional
    public String remove() {
        final FacesMessages facesMessages = FacesMessages.instance();
        if (getId() == null) {
            facesMessages.addFromResourceBundle(Severity.WARN, "charmsPermissionActionBean.noIdToDelete");  
            facesMessages.add(Severity.FATAL, "No entity to delete, id is null");
            return "error";
        }
        final String result = super.remove();
        // Remove from Conversation context and clear the home
        final Session hibernateSession = getSession();
        hibernateSession.flush();
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

    // ---- for the UI

    /*
     * //@Factory(value = "permissionDiscriminatorSelects")
     * 
     * @BypassInterceptors public List<SelectItem> getDiscriminatorSelects() {
     * LOGGER.debug("getPermissionDiscriminatorSelects called: {} size is {}",
     * selectItems, selectItems.size()); return selectItems; }
     * 
     * @BypassInterceptors public List<?> getSelectableRecipients() {
     * LOGGER.debug("getSelectableRecipients called: {} size is {}",
     * recipientList, recipientList.size()); return recipientList; }
     */

    @BypassInterceptors
    public List<String> getSelectableTargets() {
        LOGGER.debug("getSelectableTargets called: {} size is {}", targetStrings, targetStrings.size());
        return targetStrings;
    }

    // ---------- getter and setter for the GUI ------------

    @BypassInterceptors
    public String getTargetString() {
        LOGGER.debug("getTargetString: {}", targetString);
        return targetString;
    }

    @BypassInterceptors
    public void setTargetString(final String targetString) {
        LOGGER.debug("setTargetString: {}", targetString);
        this.targetString = targetString;
        // actions
        final CharmsPermission charmsPermission = getInstance();
        final String actions = charmsPermission.getAction();
        setupAvailableActionForTarget(actions);
        setupTargetIdentifier(targetString);
    }
    
    @BypassInterceptors
    public Long getTargetId() {
        LOGGER.debug("getTargetString: {}", targetString);
        return targetId;
    }
    @BypassInterceptors
    public void setTargetId(Long targetId) {
        LOGGER.debug("setTargetId: {}", targetId);
        this.targetId = targetId;
    }
    
    @BypassInterceptors
    public String getTargetLabel() {
        LOGGER.debug("getTargetLabel: {}", targetLabel);
        return targetLabel;
    }
    @BypassInterceptors
    public void setTargetLabel(String targetLabel) {
        LOGGER.debug("setTargetLabel: {}", targetLabel);
        this.targetLabel = targetLabel;
    }

    
    public void setupTargetIdentifier(final String targetString) {
        LOGGER.warn("targetString: {} we need to setup a selector" , targetString);
        targetId = 0L;
        targetLabel = null;
    }
    
    @BypassInterceptors
    public HashMap<String, Boolean> getSelectableActions() {
        LOGGER.debug("selectable actions returned: {}", availableActions);
        return availableActions;
    }

    @BypassInterceptors
    public String getRecipientActorId() {
        LOGGER.debug("getRecipientActorId called: {}", recipientActorId);
        return recipientActorId;
    }

    @BypassInterceptors
    public void setRecipientActorId(final String recipientActorId) {
        LOGGER.warn("setRecipientActorId called: >{}<", recipientActorId);

        if (StringUtils.isEmpty(recipientActorId)) {
            this.recipientActorId = null;
        } else {
            // this is needed for IE support IEs JavaScript doesn't trim
            // right...
            this.recipientActorId = recipientActorId.trim();
        }
        LOGGER.info("setRecipientActorId id: >{}<", this.recipientActorId);
        LOGGER.info("trimmed value is: >{}<", (this.recipientActorId == null) ? "null" : this.recipientActorId.trim());
    }
}
