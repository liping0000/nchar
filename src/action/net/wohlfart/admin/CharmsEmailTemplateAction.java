package net.wohlfart.admin;

import org.jboss.seam.ScopeType;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.email.entities.CharmsEmailTemplate;
import net.wohlfart.email.entities.CharmsEmailTemplateReceiver;
import net.wohlfart.email.entities.CharmsEmailTemplateTranslation;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.framework.EntityNotFoundException;
import org.jboss.seam.international.LocaleConfig;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class doesn't extend the home objects since we don't need all the home
 * features and have too many custom requirements...
 * however the setEmailTemplateId() method is a copy of the setEntityId() so
 * maybe its a good idea to do some refactoring here...
 * 
 * @author Michael Wohlfart
 */
@Scope(ScopeType.CONVERSATION)
@Name("charmsEmailTemplateAction")
public class CharmsEmailTemplateAction extends AbstractActionBean implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsEmailTemplateAction.class);

    private final String patternString = "[0-9]{0,10}";

    private static final String CHARMS_EMAIL_TEMPLATE = "charmsEmailTemplate";

    @RequestParameter
    private String predefinedName;

    private Long id;

    // the instance
    private CharmsEmailTemplate instance;

    private Map<String, CharmsEmailTemplateTranslation> translations = new HashMap<String, CharmsEmailTemplateTranslation>();

    /**
     * the factory method 
     * 
     * @return
     */
    @Factory(value = CHARMS_EMAIL_TEMPLATE)
    public CharmsEmailTemplate getCharmsEmailTemplate() {
        LOGGER.debug("getCharmsEmailTemplate called");
        return getInstance();
    }

    // --- methods to imitate the AbstractEntityHome<E> base class ---
    private Long getId() {
        return id;
    }

    private void setId(final Long id) {
        this.id = id;
    }

    private void clearInstance() {
        id = null;
        instance = null;
        translations = new HashMap<String, CharmsEmailTemplateTranslation>();
    }

    private Session getSession() {
        return (Session) Component.getInstance("hibernateSession");
    }

    private String getNameInContext() {
        return CHARMS_EMAIL_TEMPLATE;
    }

    /**
     * this is a copy of the setEntityId in AbstractEntityHome
     * 
     * @param s the database key of the entity or an empty string if we
     *          create a new entity
     * @return invalid/valid
     */
    public String setEmailTemplateId(final String s) {
        final FacesMessages facesMessages = FacesMessages.instance();
        LOGGER.debug("setting id called: >{}< old id is: >{}<", s, getId());
        // check if we can parse the id string to an id number
        if ((StringUtils.isNotEmpty(s)) && (!Pattern.matches(patternString, s))) {
            getSession().evict(instance);
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance();
            facesMessages.addFromResourceBundle(Severity.FATAL, "framework.entityHome.entityNotFoundException");
            LOGGER.info("EntityNotFoundException, the id is empty or doesn't match the pattern, the string was '{}',"
                    + " returning 'invalid' as view ID, the context was cleared from the last component...", s);
            return "invalid";
        }
        try {
            // parsing the string to a long, using 0 as default
            final Long id = new Long(StringUtils.defaultIfEmpty(s, "0"));
            LOGGER.debug("cleaned parameter is: >{}<", id);
            // remove any old instance from the session so we just have
            // the current entity and nothing else to worry about
            getSession().evict(instance);
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance(); // this sets the id to null
            if (id != 0) {
                setId(id);
                initInstance();
            }
            return "valid";
        } catch (final EntityNotFoundException ex) {
            getSession().evict(instance);
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance();
            LOGGER.info("caught EntityNotFoundException, returning 'invalid' as view ID");
            facesMessages.addFromResourceBundle(Severity.FATAL, "framework.entityHome.entityNotFoundException");
            return "invalid";
        } catch (final NumberFormatException ex) {
            getSession().evict(instance);
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance();
            LOGGER.info("caught NumberFormatException, returning 'invalid' as view ID");
            facesMessages.addFromResourceBundle(Severity.FATAL, "framework.entityHome.numberFormatException");
            return "invalid";
        }
    }

    
    @Transactional
    public boolean isManaged() {
        final Session session = (Session) Component.getInstance("hibernateSession");
        return (getInstance() != null) && session.contains(getInstance());
    }

    @Transactional
    public CharmsEmailTemplate getInstance() {
        if (instance == null) {
            initInstance();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    private void initInstance() {
        LOGGER.debug("init instance");
        instance = null;

        try {

            if (id == null) {
                LOGGER.debug("creating new email template, since the provided id was null ");
                instance = new CharmsEmailTemplate();
                // default value from the request parameter
                instance.setName(predefinedName);

            } else {
                final Session session = (Session) Component.getInstance("hibernateSession");
                final List<CharmsEmailTemplate> list = session
                    .getNamedQuery(CharmsEmailTemplate.FIND_BY_ID)
                    .setParameter("id", id)
                    .list();

                if (list.size() == 0) {
                    LOGGER.warn("no template found for id: {}, creating a new template", id);
                    // no template definition yet, create one for this name
                    instance = new CharmsEmailTemplate();
                    // instance.setName(templateName);
                    // template name from the delegatee
                } else if (list.size() == 1) {
                    // found it
                    instance = list.get(0);
                    LOGGER.debug("found result for email template");
                } else {
                    // there should be no more than one instance since the id
                    // field is
                    // the primary key, however to keep findbugs happy
                    LOGGER.warn("multiple template found for id: {}, using first one found, THIS IS A BUG", id);
                    instance = list.get(0);
                }
            }

            // resolve the receiver
            final List<CharmsEmailTemplateReceiver> receiverList = instance.getReceiver();
            LOGGER.debug("queried receiver list: {}", receiverList);
            // add at least one receiver to the list if it is empty
            if (receiverList.size() == 0) {
                final CharmsEmailTemplateReceiver receiver = new CharmsEmailTemplateReceiver();
                // receiver.setAddressExpression("address@sfc.com");
                receiver.setTemplate(instance);
                receiverList.add(receiver);
            }

            // init the translation HashMap with the available i18n keys
            translations = new HashMap<String, CharmsEmailTemplateTranslation>();
            final LocaleConfig localeConfig = LocaleConfig.instance();
            for (final String localeId : localeConfig.getSupportedLocales()) {
                final CharmsEmailTemplateTranslation trans = new CharmsEmailTemplateTranslation();
                // don'T set default values, this might be used in production
                // trans.setSubject("-- subject for " + localeId + " --");
                // trans.setBody("-- content for " + localeId + " --");
                trans.setTemplate(instance);
                translations.put(localeId, trans);
            }
            // replace some of them with existing translations
            final Set<String> localeIds = translations.keySet();
            for (final String localeId : localeIds) {
                final CharmsEmailTemplateTranslation trans = instance.getTranslations().get(localeId);
                if (trans != null) {
                    LOGGER.debug("replacing translation for locale {} with already existing one", localeId);
                    translations.put(localeId, trans);
                } else {
                    LOGGER.debug("no translation found for {} keeping new created one", localeId);
                }
            }

        } catch (final Exception ex) {
            LOGGER.error("can't find mail template {}", ex);
        }

    }

    public List<CharmsEmailTemplateReceiver> getReceiverList() {
        return instance.getReceiver();
    }

    public void setReceiverList(final List<CharmsEmailTemplateReceiver> receiverList) {
        LOGGER.debug("setting receiverList: {}", receiverList);
        instance.setReceiver(receiverList);
    }

    public Boolean getMultiReceiver() {
        return ((instance.getReceiver() != null) && (instance.getReceiver().size() > 1));
    }

    public Map<String, CharmsEmailTemplateTranslation> getTranslations() {
        return translations;
    }

    public void setTranslations(final Map<String, CharmsEmailTemplateTranslation> translations) {
        this.translations = translations;
    }

    // -- actions

    public void addReceiver() {
        LOGGER.debug("add receiver called");
        final List<CharmsEmailTemplateReceiver> list = instance.getReceiver();
        final CharmsEmailTemplateReceiver receiver = new CharmsEmailTemplateReceiver();
        // receiver.setAddressExpression("address " + list.size());
        receiver.setTemplate(instance);
        list.add(receiver);
    }

    public void delReceiver() {
        LOGGER.debug("del receiver called");
        final List<CharmsEmailTemplateReceiver> list = instance.getReceiver();
        if (list.size() > 0) {
            list.remove(list.size() - 1);
        }
    }

    // -- crud actions

    @Transactional
    public String update() {
        LOGGER.debug("update called");
        final Session session = (Session) Component.getInstance("hibernateSession");

        // instance = getInstance();
        instance.setLastModified(Calendar.getInstance().getTime());
        // FIXME: check if we rather need an update here
        session.merge(instance); // generate the ids
        session.flush();

        /*
         * // cleanup the DB and delete anything no more in the list
         * hibernateSession
         * .getNamedQuery(CharmsEmailTemplateReceiver.DELETE_FOR_TEMPLATE)
         * .setParameter("template", instance) .setParameter("recieverList",
         * instance.getReceiver()) .executeUpdate();
         */

        setupTranslations();
        session.flush();

        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return "updated"; // returns "updated"
    }

    @Transactional
    public String persist() {
        LOGGER.info("persist called");
        final Session session = (Session) Component.getInstance("hibernateSession");

        // instance = getInstance();
        instance.setLastModified(Calendar.getInstance().getTime());
        session.persist(instance); // needed to generate the id
        // entityManager.flush();

        // the eceiver are all new
        for (final CharmsEmailTemplateReceiver receiver : instance.getReceiver()) {
            // done in the initIntance method: receiver.setTemplate(instance);
            session.persist(receiver);
        }

        setupTranslations();
        // there is no create action for emails yet so we don't need to
        // clean the conversation context
        session.flush();

        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return "persisted"; // returns "persisted"
    }

    @Transactional
    public String remove() {
        LOGGER.debug("remove called");
        final Session session = (Session) Component.getInstance("hibernateSession");

        session.delete(getInstance());

        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        session.flush();
        return "removed"; // returns "removed"
    }

    @Transactional
    public String disable() {
        LOGGER.debug("disable called");
        final Session session = (Session) Component.getInstance("hibernateSession");

        instance.setEnabled(false);

        session.persist(instance);
        session.flush();

        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }

        return "disabled";
    }

    @Transactional
    public String enable() {
        LOGGER.debug("enable called");
        final Session session = (Session) Component.getInstance("hibernateSession");

        instance.setEnabled(true);

        session.persist(instance);
        session.flush();

        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }

        return "enabled";
    }

    // see: http://seamframework.org/Community/EndAndRedirectInPagesxml
    public String cancel() {
        LOGGER.debug("cancel called");

        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return "canceled";
    }

    private void setupTranslations() {
        final Session session = (Session) Component.getInstance("hibernateSession");

        // walk through the translations from the GUI
        for (final String localeId : translations.keySet()) {
            // do we already have a translation
            if (instance.getTranslations().containsKey(localeId)) {
                // there is already a translation in the database
                final CharmsEmailTemplateTranslation trans = instance.getTranslations().get(localeId);
                trans.setBody(translations.get(localeId).getBody());
                trans.setSubject(translations.get(localeId).getSubject());
                trans.setLocaleId(localeId);
                session.persist(trans);
            } else {
                // there is not yet a translation in the database
                final CharmsEmailTemplateTranslation trans = translations.get(localeId);
                trans.setTemplate(instance); // link to the parent
                trans.setLocaleId(localeId); // make sure we have a hash key
                instance.getTranslations().put(localeId, trans);
                session.persist(trans);
            }
        }

    }

}
