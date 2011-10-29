package net.wohlfart.admin;

//import static org.jboss.seam.security.management.JpaIdentityStore.EVENT_PRE_PERSIST_USER;
//import static org.jboss.seam.security.management.JpaIdentityStore.EVENT_USER_CREATED;

import static net.wohlfart.authentication.CharmsUserIdentityStore.EVENT_PRE_PERSIST_USER;
import static net.wohlfart.authentication.CharmsUserIdentityStore.EVENT_USER_CREATED;

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
import net.wohlfart.authentication.entities.Gender;
import net.wohlfart.authentication.entities.RoleClassification;
import net.wohlfart.authorization.CustomHash;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.framework.AbstractEntityHome;
import net.wohlfart.framework.properties.CharmsProperty;
import net.wohlfart.framework.properties.CharmsPropertyItem;
import net.wohlfart.framework.properties.CharmsPropertySet;
import net.wohlfart.framework.properties.CharmsPropertySetType;
import net.wohlfart.framework.properties.PropertiesManager;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.security.management.IdentityManager;
import org.jbpm.api.task.Task;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.mysql.jdbc.StringUtils;

// see:
// http://www.seamframework.org/Community/SeamIdentityManagementUserProperties
// see:
// http://www.oracle.com/technology/tech/java/oc4j/ejb3/how_to/howtoejb30entitymanager/doc/how-to-ejb30-entitymanager.html#find
// for entity manager docs

@Scope(ScopeType.CONVERSATION)
@Name("charmsUserActionBean")
public class CharmsUserActionBean extends AbstractEntityHome<CharmsUser> {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsUserActionBean.class);

    private static final String CHARMS_USER = "charmsUser";

    // some form data
    private List<CharmsRoleItem> selectedRoleItems;
    private List<CharmsRoleItem> availableRoleItems;

    // new organizational role name
    private String newGroupName;
    private List<String> allCurrentGroupNames;

    // for passwd validation
    private String passwd;
    private String passwdConfirm;

    // we need the original name to move the permissions over to the new name
    private String originalName;

    // the item converter for the listshuttle
    private CharmsRoleItemConverter charmsRoleItemConverter;
    
    // the property list
    private ArrayList<CharmsPropertyItem> charmsPropertyList;

    @Override
    protected String getNameInContext() {
        return CHARMS_USER;
    }

    // ------------ factories

    // @SuppressWarnings("unchecked")
    // the real work happens here, create a new user and
    // setup the form data
    @Transactional
    // (TransactionPropagationType.REQUIRED)
    @Factory(value = CHARMS_USER)
    public CharmsUser getCharmsUser() {
        LOGGER.debug("get instance called");
        final CharmsUser charmsUser = getInstance();
        return charmsUser;
    }

    @Factory(value = "genderSelects")
    public Gender[] getGenderSelects() {
        // return an enumeration
        return Gender.values();
    }


    @Override
    @Transactional
    public void initInstance() {
        super.initInstance();
        final Session hibernateSession = getSession();

        newGroupName = null;
        allCurrentGroupNames = new ArrayList<String>();

        final CharmsUser charmsUser = getInstance();
        // we have to track namechanges in order to move the permissions over to
        // the new name
        originalName = charmsUser.getName();

        passwd = null;
        passwdConfirm = null;
        
        // get the properties manager it is event scoped, stateless...
        PropertiesManager propertiesManager = (PropertiesManager) Component.getInstance(PropertiesManager.class);
        CharmsPropertySet charmsPropertySet;
        if (isManaged()) {
            charmsPropertySet = propertiesManager.getUserProperties(charmsUser);
            charmsPropertyList = charmsPropertySet.getList();
        } else {
            // user is not yet persisted, we need to create one...
            charmsPropertySet = new CharmsPropertySet();
            charmsPropertySet.setName(null); // need to set the userId after we got one
            charmsPropertySet.setType(CharmsPropertySetType.USER);
            charmsPropertyList = charmsPropertySet.getList();
        }
        
        final List<CharmsRoleItem> shuttleContent = CharmsRoleItem.getShuttleSelect(hibernateSession);

        // create a map for the converter to translate from id to CharmsRoleItem
        final HashMap<Long, CharmsRoleItem> roleItemMap = new HashMap<Long, CharmsRoleItem>();
        availableRoleItems = new ArrayList<CharmsRoleItem>();
        for (final CharmsRoleItem item : shuttleContent) {
            final Long id = new Long(item.getValue().toString());
            roleItemMap.put(id, item);
            availableRoleItems.add(item);
        }

        // split off the selected and the available items
        final Set<CharmsMembership> memberships = charmsUser.getMemberships();
        selectedRoleItems = new ArrayList<CharmsRoleItem>();
        if (memberships != null) { // memberships of that user
            for (final CharmsMembership memberOf : memberships) {
                final CharmsRole role = memberOf.getCharmsRole();
                // if (!role.getOrganizational()) {
                if (!role.getClassification().equals(RoleClassification.ORGANISATIONAL)) {
                    final Long id = role.getId();
                    // the role is selected and no longer available:
                    selectedRoleItems.add(roleItemMap.get(id));
                    availableRoleItems.remove(roleItemMap.get(id));
                } else {
                    allCurrentGroupNames.add(role.getName());
                }
            }
        }
        // init the converter with the map
        charmsRoleItemConverter = new CharmsRoleItemConverter(roleItemMap);
    }

    // event fired by the identityManager,
    // we need to add additional properties to the user
    @Transactional
    //@Observer(EVENT_PRE_PERSIST_USER)  this conflicts with the testability of the identityManager
    // since the observers are hit when testing the identityManager
    public void onPrePersist(final CharmsUser charmsUser) {
        LOGGER.debug("onPrePersist: {}", charmsUser);
        final CharmsUser formData = getInstance();
        // the user is about to be persisted, we have to add all the properties
        // for which the identityManager does not take care of
        charmsUser.setGender(formData.getGender());
        charmsUser.setEmail(formData.getEmail());

        charmsUser.setLocaleId(formData.getLocaleId());
        charmsUser.setTimezoneId(formData.getTimezoneId());
        charmsUser.setThemeId(formData.getThemeId());

        charmsUser.setCredentialsExpire(formData.getCredentialsExpire());
        charmsUser.setAccountExpire(formData.getAccountExpire());
        // don't enable if there is no password set:
        charmsUser.setEnabled((charmsUser.getPasswd() != null) && formData.getEnabled());
        charmsUser.setUnlocked(formData.getUnlocked());

        charmsUser.setDescription(formData.getDescription());

        charmsUser.setExternalId1(formData.getExternalId1());
        charmsUser.setExternalId2(formData.getExternalId2());

        setInstance(charmsUser);
    }

    // event fired by the identityManager,
    // we need to add the roles after the user is stored
    @Transactional
    //@Observer(EVENT_USER_CREATED) this conflicts with the testability of the identityManager
    // since the observers are hit when testing the identityManager
    public void onPostPersist(final CharmsUser charmsUser) {
        LOGGER.debug("onPostPersist: {}", charmsUser);
        // there are no roles yet since this is the initial store for this user
        // we just need to add roles, nothing to remove
        // Set<CharmsMembership> memberships = charmsUser.getMemberships();
        final Session hibernateSession = getSession();
        for (final CharmsRoleItem item : selectedRoleItems) {
            final CharmsRole role = (CharmsRole) hibernateSession.load(CharmsRole.class, new Long(item.getValue().toString()));
            final CharmsMembership m = new CharmsMembership(charmsUser, role);
            hibernateSession.persist(m);
        }
        // set the new id so the next create call doesn't hit the null id and
        // shows the old data
        setId(charmsUser.getId());

        // now add all the group names here:
        for (final String groupName : allCurrentGroupNames) {
            // check if a role with that name exists:
            CharmsRole role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", groupName).uniqueResult();
            if (role == null) {
                // ok we need to create a new role with that name
                role = new CharmsRole();
                role.setName(groupName);
                // manually added via group name its ORGANISATIONAL
                role.setClassification(RoleClassification.ORGANISATIONAL);
                hibernateSession.persist(role);
            }
            final CharmsMembership m = new CharmsMembership(charmsUser, role);
            hibernateSession.persist(m);
        }

    }

    /* -------------- main actions for the user entity -------------- */

    @Override
    // persist means we store this object for the first time into the DB
    @Transactional
    public String persist() {
        final CharmsUser charmsUser = getInstance();
        LOGGER.debug("persist user: {}", charmsUser);

        final FacesMessages facesMessages = FacesMessages.instance();

        if (!isPasswdConfirmed()) {
            LOGGER.debug("passwords don't match");
            facesMessages.addToControl("password", "Passwords do not match");
            return "invalid";
        }

        // we have to manually check the bean before persisting, since hibernate
        // throws an
        // exception if the username is not unique, we also have to verify the
        // passwdConfirm
        final ClassValidator<CharmsUser> validator = new ClassValidator<CharmsUser>(CharmsUser.class);
        final InvalidValue[] invalidValues = validator.getInvalidValues(charmsUser);
        final IdentityManager identityManager = (IdentityManager) Component.getInstance(CharmsIdentityManager.class);
        if (invalidValues.length > 0) {
            facesMessages.add(invalidValues);
            LOGGER.debug("validation failed, invalidValues are: {}", invalidValues);
            return "invalid";
        } else if (identityManager.userExists(charmsUser.getName())) {
            facesMessages.addFromResourceBundle("charmsUserActionBean.nameAlreadTaken");
            LOGGER.debug("Username is already taken");
            return "invalid";
        } else {
            LOGGER.debug("persisting...");
            // we use the identity Manager not the persistenceManager to create
            // the user...

            // set the passwd to null if it is empty we used to have a minlength
            // on the hibernate mapping
            String pass = getPasswd();
            if ((pass == null) || (pass.length() == 0)) {
                pass = null;
            }

            LOGGER.debug("persisting user: " 
                    + " login: " + charmsUser.getName() 
                    + " pass: " + pass 
                    + " firstname: " + charmsUser.getFirstname()
                    + " lastname: " + charmsUser.getLastname() 
                    + " actorId: " + charmsUser.getActorId());

            
            
            final CharmsUser user = new CharmsUser();

            // instead of event
            onPrePersist(user);
            
            user.setName(charmsUser.getName());
            user.setFirstname(charmsUser.getFirstname());
            user.setLastname(charmsUser.getLastname());

            if (pass == null) {
                // user.setPasswd(CharmsUser.LOCKED_PASSWD);
                user.setEnabled(false);
            } else {
                user.setPasswd(CustomHash.instance().generateSaltedHash(pass, charmsUser.getName()));
                user.setEnabled(true);
            }

            getSession().persist(user);
                     
            // instead of event 
            onPostPersist(user);

            getSession().flush();
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
        final CharmsUser charmsUser = getInstance();
        final FacesMessages facesMessages = FacesMessages.instance();
        final Session hibernateSession = getSession();

        // check if the name already exists, there is an
        // index on the name
        final Long collisionId = (Long) hibernateSession.getNamedQuery(CharmsUser.FIND_ID_BY_NAME).setParameter("name", charmsUser.getName()).uniqueResult();
        // since this is an update we might hit the same entity, the only way to
        // tell
        // is compare the ids, since this is an update not a persist, the
        // current entity
        // should already have an id
        if ((collisionId != null) && (!collisionId.equals(charmsUser.getId()))) {
            // an entity with the same default name already exists
            facesMessages.addFromResourceBundle("charmsUserActionBean.nameAlreadTaken"); 
            return "invalid";
        }

        if (!isPasswdConfirmed()) {
            facesMessages.addFromResourceBundle("charmsUserActionBean.passwordsDontMatch");
            return "invalid";
        }

        // the selected role ids:
        final Set<Long> selectedRoleIds = new HashSet<Long>();
        for (final CharmsRoleItem roleItem : selectedRoleItems) {
            selectedRoleIds.add(new Long(roleItem.getValue().toString()));
        }

        // remove unselected roles and remember the selected roles
        final Set<Long> roleIds = new HashSet<Long>();
        final Set<CharmsMembership> memberships = charmsUser.getMemberships();
        final Iterator<CharmsMembership> iterator1 = memberships.iterator();
        while (iterator1.hasNext()) {
            final CharmsMembership m1 = iterator1.next();
            final Long id = m1.getCharmsRole().getId();
            if (selectedRoleIds.contains(id)) {
                // this role stays
                roleIds.add(id);
            } else {
                // the role might be a orgrole, we can't delete it then
                if (!allCurrentGroupNames.contains(m1.getCharmsRole().getName())) {
                    // this role goes
                    iterator1.remove();
                    // find the role side reference and remove it
                    final CharmsRole role = m1.getCharmsRole();
                    final Iterator<CharmsMembership> iterator2 = role.getMemberships().iterator();
                    while (iterator2.hasNext()) {
                        final CharmsMembership m2 = iterator2.next();
                        if (m2.getCharmsUser().getName().equals(originalName)) {
                            iterator2.remove();
                        }
                    }
                    hibernateSession.delete(m1);
                }
            }
        }

        // add selected roles
        for (final CharmsRoleItem item : selectedRoleItems) {
            if (!roleIds.contains(new Long(item.getValue().toString()))) {
                // CharmsRole role = entityManager.find(CharmsRole.class,
                // item.getValue());
                // get the role proxy
                final CharmsRole role = (CharmsRole) hibernateSession.get(CharmsRole.class, new Long(item.getValue().toString()));
                // make sure there is a constructor for an empty set in
                // CharsmUser!
                final CharmsMembership m = new CharmsMembership(charmsUser, role);
                // only add if we don't have this role already
                memberships.add(m);
                hibernateSession.persist(m);
            }
        }


        // all rolenames that this user already has
        final Set<String> currentRoleNames = new HashSet<String>();
        final Set<CharmsMembership> currentMemberships = charmsUser.getMemberships();
        for (final CharmsMembership membership : currentMemberships) {
            final CharmsRole role = membership.getCharmsRole();
            if (role.getClassification().equals(RoleClassification.ORGANISATIONAL)) {
                currentRoleNames.add(role.getName());
            }
        }

        // add orgroles by name / create them if they don't exist
        for (final String groupName : allCurrentGroupNames) {
            // check if a role with that name exists:
            CharmsRole role = (CharmsRole) hibernateSession.getNamedQuery(CharmsRole.FIND_BY_NAME).setParameter("name", groupName).uniqueResult();
            if (role == null) {
                // ok we need to create a new role with that name
                role = new CharmsRole();
                role.setName(groupName);
                // manually added via group name, its ORGANISATIONAL
                role.setClassification(RoleClassification.ORGANISATIONAL);
                hibernateSession.persist(role);
            }

            // only persist if the membership is not set up already
            if (!currentRoleNames.contains(groupName)) {
                final CharmsMembership m = new CharmsMembership(charmsUser, role);
                hibernateSession.persist(m);
            }
        }

        // hibernateSession.update(charmsUser);
        hibernateSession.persist(charmsUser);
        hibernateSession.getNamedQuery(CharmsPermission.MOVE_TO_NEW_USER_NAME)
            .setParameter("oldName", originalName)
            .setParameter("newName", charmsUser.getName())
            .executeUpdate();
        // we need a flush here in order for the identityManager to get the
        // possibly changed username
        hibernateSession.flush();

        LOGGER.debug("passwd is: >{}<", passwd);
        // password is validated on entry
        if ((passwd != null) && (passwd.trim().length() > 0)) {
            final IdentityManager identityManager = (IdentityManager) Component.getInstance(CharmsIdentityManager.class);
            identityManager.changePassword(charmsUser.getName(), passwd);
        }

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
        LOGGER.debug("remove enter");
        if (getId() == null) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.addFromResourceBundle(Severity.FATAL, "charmsUserActionBean.noIdToDelete");
            return "error";
        }
        final CharmsUser charmsUser = getInstance();
        // restore the name in case the user changed
        // it in the UI..
        final Session hibernateSession = getSession();
        hibernateSession.refresh(charmsUser);
        // ...to make sure we delete the right user
        final String name = charmsUser.getName();
        
        // in order to delete a user we have to make sure there are no tasks assigned to the user
        int tasks = hibernateSession.createCriteria(TaskImpl.class)
            .add(Restrictions.eq("assignee", charmsUser.getActorId()))
            .list()
            .size();
        if (tasks > 0) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "Can not delete user, user has tasks assigned");
            return "error";
        }
        
        // we have foreign keys from CHARMS_DOCUMENT, _MEMBERSHIP, TRNS_DATA, WFL_DATA, CHREQ_DATA here
        // so it ispretty much impossible to delete a user...

        
        final IdentityManager identityManager = (IdentityManager) Component.getInstance(CharmsIdentityManager.class);
        boolean success = identityManager.deleteUser(name);
        if (!success) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "Can not delete user, error in IDM");
            return "error";
        }

        hibernateSession.getNamedQuery(CharmsPermission.REMOVE_FOR_USER_NAME)
            .setParameter("name", name)
            .executeUpdate();

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

    /* the passwd to store in the DB */
    @BypassInterceptors
    public String getPasswd() {
        return passwd;
    }

    @BypassInterceptors
    public void setPasswd(final String passwd) {
        this.passwd = passwd;
    }

    /* for passwd validation */
    @BypassInterceptors
    public String getPasswdConfirm() {
        return passwdConfirm;
    }

    @BypassInterceptors
    public void setPasswdConfirm(final String passwdConfirm) {
        this.passwdConfirm = passwdConfirm;
    }

    @BypassInterceptors
    private boolean isPasswdConfirmed() {
        // passwd is empty
        if (((passwd == null) || (passwd.trim().length() == 0)) && ((passwdConfirm == null) || (passwdConfirm.trim().length() == 0))) {
            passwd = null;
            return true;
        }

        if ((passwd != null) && (passwd.equals(passwdConfirm))) {
            return true;
        }
        return false;
    }

    /* the converter for the roleItems used in the form listshedule */
    @BypassInterceptors
    public CharmsRoleItemConverter getCharmsRoleItemConverter() {
        return charmsRoleItemConverter;
    }

    @BypassInterceptors
    public void setNewGroupName(final String newGroupName) {
        this.newGroupName = newGroupName;
    }

    @BypassInterceptors
    public String getNewGroupName() {
        return newGroupName;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<String> completeNewGroupName(final String query) {
        final Session hibernateSession = getSession();
        final List<String> results = hibernateSession
                .getNamedQuery(CharmsRole.FIND_NAME_BY_CLASSIFICATION_AND_NAME_LIKE)
                .setParameter("classification", RoleClassification.ORGANISATIONAL)
                .setParameter("name", query).list();
        return results;
    }
    
    @BypassInterceptors
    public void addNewGroupName() {
        if (!allCurrentGroupNames.contains(newGroupName)) {
            allCurrentGroupNames.add(newGroupName);
        }
        newGroupName = null;
    }
    
    @BypassInterceptors
    public void delNewGroupName(final String group) {
        if (allCurrentGroupNames.contains(group)) {
            allCurrentGroupNames.remove(group);
        }
    }

    @BypassInterceptors
    public void setAllCurrentGroupNames(final List<String> allCurrentGroupNames) {
        this.allCurrentGroupNames = allCurrentGroupNames;
    }
    @BypassInterceptors
    public List<String> getAllCurrentGroupNames() {
        return allCurrentGroupNames;
    }
    
    @BypassInterceptors
    public void setCharmsPropertyList(final ArrayList<CharmsPropertyItem> charmsPropertyList) {
        this.charmsPropertyList = charmsPropertyList;
    }
    @BypassInterceptors
    public ArrayList<CharmsPropertyItem> getCharmsPropertyList() {
        return charmsPropertyList;
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

    
    /*
     * FIXME: seem to happen when a assigned group was moved from org to locational
     * 
     * 
     java.lang.NullPointerException
    net.wohlfart.admin.CharmsUserActionBean$CharmsRoleItemConverter.getAsString(CharmsUserActionBean.java:616)
    org.richfaces.renderkit.ListShuttleRendererBase.encodeOneRow(ListShuttleRendererBase.java:219)
    org.richfaces.renderkit.AbstractRowsRenderer.process(AbstractRowsRenderer.java:83)
    org.richfaces.model.ListShuttleDataModel$2.process(ListShuttleDataModel.java:94)
    org.ajax4jsf.model.SequenceDataModel.walk(SequenceDataModel.java:101)
    org.richfaces.model.ListShuttleDataModel.walk(ListShuttleDataModel.java:88)
    org.ajax4jsf.component.UIDataAdaptorBase.walk(UIDataAdaptorBase.java:1156)

     */
}
