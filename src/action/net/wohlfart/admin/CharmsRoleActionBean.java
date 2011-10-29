package net.wohlfart.admin;

import org.jboss.seam.ScopeType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;

import net.wohlfart.authentication.CharmsIdentityManager;
import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsRoleItem;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authentication.entities.CharmsUserItem;
import net.wohlfart.authentication.entities.Gender;
import net.wohlfart.authentication.entities.RoleClassification;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.framework.AbstractEntityHome;

import org.hibernate.Session;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.security.management.IdentityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see:
// http://www.seamframework.org/Community/SeamIdentityManagementUserProperties
// see:
// http://www.oracle.com/technology/tech/java/oc4j/ejb3/how_to/howtoejb30entitymanager/doc/how-to-ejb30-entitymanager.html#find
// for entity manager docs

@Scope(ScopeType.CONVERSATION)
@Name("charmsRoleActionBean")
public class CharmsRoleActionBean extends AbstractEntityHome<CharmsRole> {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsRoleActionBean.class);

    private static final String CHARMS_ROLE = "charmsRole";

    // some form data
    private List<CharmsUserItem> selectedUserItems;
    private List<CharmsUserItem> availableUserItems;
    // more form data
    private List<CharmsRoleItem> selectedRoleItems;                                                                  
    private List<CharmsRoleItem> availableRoleItems;

    // we need the original name to move the permissions over to the new name
    private String originalName;

    // the item converter for the listshuttle
    CharmsUserItemConverter charmsUserItemConverter;
    CharmsRoleItemConverter charmsRoleItemConverter;

    @Override
    protected String getNameInContext() {
        return CHARMS_ROLE;
    }

    // ------------ factories

    // FIXME: move the queries to the entities as @NamedQueries
    // @SuppressWarnings("unchecked")
    // the real work happens here, create a new role and
    // setup the form data
    @Transactional
    @Factory(value = CHARMS_ROLE)
    public CharmsRole getCharmsRole() {
        LOGGER.debug("getting instance called");
        final CharmsRole charmsRole = getInstance();
        return charmsRole;
    }
    
    @Factory(value = "classificationSelects")  // FIXME: there is another fab creating the same for the tabe, unify both
    public RoleClassification[] getClassificationSelects() {
        // return an enumeration
        return RoleClassification.values();
    }

    @Override
    @Transactional
    public void initInstance() {
        super.initInstance();
        final Session hibernateSession = getSession();

        final CharmsRole charmsRole = getInstance();
        // we have to track namechanges in order to move the permissions over to
        // the new name
        originalName = charmsRole.getName();

        final List<CharmsUserItem> all1 = CharmsUserItem.getSelect(hibernateSession);
        final List<CharmsRoleItem> all2 = CharmsRoleItem.getAllSelect(hibernateSession);
        //hibernateSession.createQuery(CharmsRoleItem.getAllSelect()).list();

        // ////// users in this role
        // create a map for the converter
        final HashMap<Long, CharmsUserItem> userItemMap = new HashMap<Long, CharmsUserItem>();
        availableUserItems = new ArrayList<CharmsUserItem>();
        for (final CharmsUserItem item : all1) {
            final Long id = new Long(item.getValue().toString());
            userItemMap.put(id, item);
            availableUserItems.add(item);
        }

        // this roles userids, we have to check if this is a just created entity
        final Set<CharmsMembership> memberships1 = charmsRole.getMemberships();
        selectedUserItems = new ArrayList<CharmsUserItem>();
        if (memberships1 != null) { // members of the role
            for (final CharmsMembership member : memberships1) {
                final Long id = member.getCharmsUser().getId();
                // the role is selected and no longer available:
                selectedUserItems.add(userItemMap.get(id));
                availableUserItems.remove(userItemMap.get(id));
            }
        }

        // ///// roles contained in this role

        // create a map for the converter to translate from id to CharmsRoleItem
        final HashMap<Long, CharmsRoleItem> roleItemMap = new HashMap<Long, CharmsRoleItem>();
        availableRoleItems = new ArrayList<CharmsRoleItem>();
        for (final CharmsRoleItem item : all2) {
            final Long id = new Long(item.getValue().toString());
            roleItemMap.put(id, item);
            availableRoleItems.add(item);
        }

        // split off the selected and the available items
        final Set<CharmsRole> upstream = charmsRole.getUpstream();
        selectedRoleItems = new ArrayList<CharmsRoleItem>();
        if (upstream != null) { // memberships of that user
            for (final CharmsRole role : upstream) {
                // if (!role.getOrganizational()) {
                // if (!role.getClassification().equals(RoleClassification.ORGANISATIONAL)) {
                    final Long id = role.getId();
                    // the role is already selected and no longer available:
                    selectedRoleItems.add(roleItemMap.get(id));
                    availableRoleItems.remove(roleItemMap.get(id));
                //}
            }
        }
        // init the role converter with the map
        charmsRoleItemConverter = new CharmsRoleItemConverter(roleItemMap);

        // init the user converter with the map
        charmsUserItemConverter = new CharmsUserItemConverter(userItemMap);
    }

    /* -------------- main actions for the role entity -------------- */

    public void onPostPersist(final CharmsRole charmsRole) {
        LOGGER.debug("onPostPersist: {}", charmsRole);
        final Session hibernateSession = getSession();
        
        // there are no user as members of this role yet since 
        // this is the initial store for this role
        for (final CharmsUserItem item : selectedUserItems) {
            final CharmsUser user = (CharmsUser) hibernateSession.load(CharmsUser.class, new Long(item.getValue().toString()));
            final CharmsMembership m = new CharmsMembership(user, charmsRole);
            hibernateSession.persist(m);
        }

        // the upstream roles, initial no need to check existing
        for (final CharmsRoleItem item : selectedRoleItems) {
            final CharmsRole upstreamRole = (CharmsRole) hibernateSession.load(CharmsRole.class, new Long(item.getValue().toString()));
            charmsRole.getUpstream().add(upstreamRole);
        }

        // housekeeping the contained roles
        charmsRole.calculateContainedRoles(hibernateSession);
        
        // set the new id so the next create call doesn't hit the null id and
        // shows the old data
        setId(charmsRole.getId());
    }

    // -- some actions
    @Override
    @Transactional
    public String persist() {
        CharmsRole charmsRole = getInstance();
        LOGGER.debug("persist role: {}", charmsRole);

        final FacesMessages facesMessages = FacesMessages.instance();

        // we have to manually check the bean before persisting
        final ClassValidator<CharmsRole> validator = new ClassValidator<CharmsRole>(CharmsRole.class);
        final InvalidValue[] invalidValues = validator.getInvalidValues(charmsRole);
        // facesMessages.add(invalidValues);

        final IdentityManager identityManager = (IdentityManager) Component.getInstance(CharmsIdentityManager.class);
        if (invalidValues.length > 0) {
            facesMessages.add(invalidValues);
            LOGGER.debug("validation failed, invalidValues are: {}", invalidValues);
            return "invalid";
            // check if the rolename exists
        } else if (identityManager.roleExists(charmsRole.getName())) {
            facesMessages.add("Rolename is already taken");
            LOGGER.debug("Rolename is already taken {}", charmsRole.getName());
            return "invalid";
        } else {
            LOGGER.debug("persisting...");
            final Session hibernateSession = getSession();
            // persist to get an id:
            hibernateSession.persist(charmsRole);
            // get it back from the DB to make sure we are in sync
            charmsRole = (CharmsRole) hibernateSession.load(CharmsRole.class, charmsRole.getId());

            setInstance(charmsRole);

            onPostPersist(charmsRole);

            hibernateSession.flush();
            final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
            if ((conversation != null) && (conversation.isNested())) {
                LOGGER.debug("endAndRedirect for nested conversation");
                conversation.endAndRedirect();
            }
            return "persisted";
        }
    }

    @Override
    @Transactional
    public String update() {
        final CharmsRole charmsRole = getInstance();
        final FacesMessages facesMessages = FacesMessages.instance();
        final Session hibernateSession = getSession();

        // check if the role name already exists, there is an index on the name
        final Long collisionId = (Long) hibernateSession
            .getNamedQuery(CharmsRole.FIND_ID_BY_NAME)
            .setParameter("name", getInstance().getName())
            .uniqueResult();
        // since this is an update we might hit the same entity, the only way to tell
        // is compare the ids,  this is not a persist so the current entity should already have an id
        if ((collisionId != null) && (!collisionId.equals(charmsRole.getId()))) {
            // an entity with the same default name but different id already exists
            facesMessages.add("Name is already taken"); // FIXME: i18n
            return "invalid";
        }

        // collect the selected user ids:
        final Set<Long> selectedUserIds = new HashSet<Long>();
        for (final CharmsUserItem userItem : selectedUserItems) {
            selectedUserIds.add(new Long(userItem.getValue().toString()));
        }

        // remove unselected user and remember the selected user
        final Set<Long> userIds = new HashSet<Long>();
        final Set<CharmsMembership> memberships1 = charmsRole.getMemberships();
        final Iterator<CharmsMembership> iterator1 = memberships1.iterator();
        while (iterator1.hasNext()) {
            final CharmsMembership m1 = iterator1.next();
            final Long id = m1.getCharmsUser().getId();
            if (selectedUserIds.contains(m1.getCharmsUser().getId())) {
                // this role stays
                userIds.add(id);
            } else {
                // this membership has to be removed
                // remove from the role side:
                iterator1.remove();
                // find the user side reference and remove it, this is
                // probably no longer neccessary since we change the cascade on the user to membership link
                final CharmsUser user = m1.getCharmsUser();
                final Iterator<CharmsMembership> iterator2 = user.getMemberships().iterator();
                while (iterator2.hasNext()) {
                    final CharmsMembership m2 = iterator2.next();
                    if (m2.getCharmsRole().getName().equals(originalName)) {
                        iterator2.remove();
                    }
                }
                // m.getCharmsUser().getMemberships().r
                hibernateSession.delete(m1);
            }
        }

        // add selected user if it is not already in the set
        for (final CharmsUserItem item : selectedUserItems) {
            if (!userIds.contains(new Long(item.getValue().toString()))) {
                // user not yet a member:
                // CharmsUser user =
                // entityManager.getReference(CharmsUser.class,
                // item.getValue());
                final CharmsUser user = (CharmsUser) hibernateSession.load(CharmsUser.class, new Long(item.getValue().toString()));
                // make sure there is a constructor for an empty set in
                // CharsmUser!
                final CharmsMembership m = new CharmsMembership(user, charmsRole);
                // only add if we don't have this role already
                memberships1.add(m);
                hibernateSession.persist(m);
            }
        }
        
        
        
        // the selected role ids:
        final Set<Long> selectedRoleIds = new HashSet<Long>();
        for (final CharmsRoleItem roleItem : selectedRoleItems) {
            selectedRoleIds.add(new Long(roleItem.getValue().toString()));
        }
        
        // remove unselected roles and remember the selected roles
        final Set<Long> upstreamRoleIds = new HashSet<Long>();
        final Set<CharmsRole> upstreams = charmsRole.getUpstream();
        final Iterator<CharmsRole> iterator2 = upstreams.iterator();
        // iterate over the selected upstream roles
        while (iterator2.hasNext()) {
            final CharmsRole r1 = iterator2.next();
            final Long id = r1.getId();
            if (selectedRoleIds.contains(id)) {
                // this role stays
                upstreamRoleIds.add(id);
            } else {
                // this role goes
                iterator2.remove();
            }
        }

        // add selected upstream roles if necessary
        for (final CharmsRoleItem item : selectedRoleItems) {
            if (!upstreamRoleIds.contains(new Long(item.getValue().toString()))) {
                // CharmsRole role = entityManager.find(CharmsRole.class,
                // item.getValue());
                // get the role proxy
                final CharmsRole role = (CharmsRole) hibernateSession.get(CharmsRole.class, new Long(item.getValue().toString()));
                charmsRole.getUpstream().add(role);
            }
        }


        // persist the role
        hibernateSession.persist(charmsRole);
        // do any name changes is neccessary
        if (!originalName.equals(charmsRole.getName())) {
            hibernateSession.getNamedQuery(CharmsPermission.MOVE_TO_NEW_ROLE_NAME)
                 .setParameter("oldName", originalName)
                 .setParameter("newName", charmsRole.getName())
                 .executeUpdate();
        }
        
        // housekeeping the contained roles
        charmsRole.calculateContainedRoles(hibernateSession);

        hibernateSession.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        
        return "updated";
    }

    @Override
    @Transactional
    public String remove() {
        if (getId() == null) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "No entity to delete, id is null");
            return "error";
        }
        final CharmsRole charmsRole = getInstance();
        final Session hibernateSession = getSession();
        // restore the name in case the user changed it
        // in the UI, otherwise we might delete with the username from the UI
        hibernateSession.refresh(charmsRole);
        // ...to make sure we delete the right role
        final String name = charmsRole.getName();
        final IdentityManager identityManager = (IdentityManager) Component.getInstance(CharmsIdentityManager.class);
        // true if the role could be removed successfully
        boolean success = identityManager.deleteRole(name);
        if (!success) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "Can not delete role");
            return "error";
        }

        // remove all permissions for this role
        hibernateSession.getNamedQuery(CharmsPermission.REMOVE_FOR_ROLE_NAME).setParameter("name", name).executeUpdate();

        hibernateSession.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return "removed";
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

    /* -------------- UI helpers for the form -------------- */

    @BypassInterceptors
    public List<CharmsRoleItem> getSelectedRoleItems() {
        return selectedRoleItems;
    }

    @BypassInterceptors
    public void setSelectedRoleItems(final List<CharmsRoleItem> selectedRoleItems) {
        this.selectedRoleItems = selectedRoleItems;
    }

    @BypassInterceptors
    public List<CharmsRoleItem> getAvailableRoleItems() {
        return availableRoleItems;
    }

    @BypassInterceptors
    public void setAvailableRoleItems(final List<CharmsRoleItem> availableRoleItems) {
        this.availableRoleItems = availableRoleItems;
    }

    @BypassInterceptors
    public List<CharmsUserItem> getSelectedUserItems() {
        return selectedUserItems;
    }

    @BypassInterceptors
    public void setSelectedUserItems(final List<CharmsUserItem> selectedUserItems) {
        this.selectedUserItems = selectedUserItems;
    }

    @BypassInterceptors
    public List<CharmsUserItem> getAvailableUserItems() {
        return availableUserItems;
    }

    @BypassInterceptors
    public void setAvailableUserItems(final List<CharmsUserItem> availableUserItems) {
        this.availableUserItems = availableUserItems;
    }

    /* the converter for the roleItems used in the form list shuttle */
    @BypassInterceptors
    public CharmsUserItemConverter getCharmsUserIdItemConverter() {
        return charmsUserItemConverter;
    }

    // the converter implementation needs access to the
    // Converter must be annotated with @BypassInterceptors
    // @org.jboss.seam.annotations.faces.Converter(forClass=CharmsRoleItem.class)
    @BypassInterceptors
    public static class CharmsUserItemConverter implements Converter, Serializable {

        private final HashMap<Long, CharmsUserItem> userItemMap;

        CharmsUserItemConverter(final HashMap<Long, CharmsUserItem> roleItemMap) {
            userItemMap = roleItemMap;
        }

        @Override
        public Object getAsObject(final FacesContext context, final UIComponent component, final String string) {
            final Long id = new Long(string);
            if (userItemMap.containsKey(id)) {
                return userItemMap.get(id);
            }
            return null;
        }

        @Override
        public String getAsString(final FacesContext context, final UIComponent component, final Object item) {
            return ((SelectItem) item).getValue().toString();
        }
    }

    /* the converter for the roleItems used in the form listshedule */
    @BypassInterceptors
    public CharmsRoleItemConverter getCharmsRoleItemConverter() {
        return charmsRoleItemConverter;
    }

    // the converter implementation needs access to the
    // Converter must be annotated with @BypassInterceptors
    // not used:
    // @org.jboss.seam.annotations.faces.Converter(forClass=CharmsRoleItem.class)
    @BypassInterceptors
    public static class CharmsRoleItemConverter implements Converter, Serializable {

        private final HashMap<Long, CharmsRoleItem> roleItemMap;

        CharmsRoleItemConverter(final HashMap<Long, CharmsRoleItem> roleItemMap) {
            this.roleItemMap = roleItemMap;
        }

        @Override
        public Object getAsObject(final FacesContext context, final UIComponent component, final String string) {
            final Long id = new Long(string);
            if (roleItemMap.containsKey(id)) {
                return roleItemMap.get(id);
            }
            return null;
        }

        @Override
        public String getAsString(final FacesContext context, final UIComponent component, final Object item) {
            return ((SelectItem) item).getValue().toString();
        }
    }

}
