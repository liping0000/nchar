package net.wohlfart.framework;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.wohlfart.framework.i18n.DatabaseMessageMap;
import net.wohlfart.framework.i18n.GlobalResourceBundle;
import net.wohlfart.framework.i18n.ITranslateable;

import org.hibernate.Session;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.international.LocaleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class extends the home object with business data translation features
 * the homies need to implement Translateable and the subclass needs a method
 * getBundleName(); anything else is used liek in the standard home object
 * 
 * 
 * 
 * @author Michael Wohlfart
 * 
 * @param <E>
 */
public abstract class TranslateableHome<E extends ITranslateable> extends AbstractEntityHome<E> {


    private final static Logger LOGGER       = LoggerFactory.getLogger(TranslateableHome.class);

    // the i18n hashtable with a locale to name mapping
    private Map<String, String> translations = null;

    // subclass must implement this
    // to tell us which bundle we have for crud operations
    public abstract String getBundleName();

    // used for formatting message codes for entity message bundles
    // seems to be thread safe as long as the format is not changed...
    public static final DecimalFormat MESSAGE_BUNDLE_ID_NUMBER_FORMAT = new DecimalFormat("000");

    /**
     * the original getInstance() plus initialization of the translations map
     * this is called in the factory, the result stays in conversation scope
     */
    @Override
    @Transactional
    public E getInstance() {
        final E instance = super.getInstance();

        // this only works if we set translations to null in clearInstance
        // and call clear instance when we are done
        if (translations == null) {
            // null values for all languages
            translations = new HashMap<String, String>();

            // some tools we need
            final Session hibernateSession = getSession();
            final LocaleConfig localeConfig = LocaleConfig.instance();

            // init the Hashtable with the available keys
            for (final String localeId : localeConfig.getSupportedLocales()) {
                translations.put(localeId, "");
            }

            // if this entity is managed already, try to get as many translation
            // as possible
            if (hibernateSession.contains(instance)) {
                // we already should have a i18n code
                final String code = instance.getMessageCode();
                final Set<String> localeIds = translations.keySet();
                for (final String localeId : localeIds) {
                    final DatabaseMessageMap bundle = getBundle(getBundleName(), localeId);
                    translations.put(localeId, bundle.getMessages().get(code));
                }
            }
        }
        return instance;
    }

    /**
     * enhanced clear instance to include the translation map
     * 
     */
    @Override
    public void clearInstance() {
        super.clearInstance();
        translations = null;
    }

    /**
     * this is needed in the view for editing
     * 
     */
    public Map<String, String> getTranslations() {
        return translations;
    }

    /**
     * enhanced update to also include the translations and fix the message code
     * if none was set up so far
     * 
     */
    @Override
    @Transactional
    public String update() {
        LOGGER.debug("session usded for update is: " + getSession());

        final String result = super.update();
        // should be persisted by now
        final E instance = super.getInstance();

        // we need a refresh in case we are using blobs
        getSession().refresh(instance);

        // make sure the message code is not null
        if (instance.getMessageCode() == null) {
            // can only be called after the instance is persisted
            instance.setupMessageCode();
            // this is considered as a bug in the data, the message code should
            // be
            // initialized on persist, not on update
            LOGGER.debug("init message code during update(), this is just wrong and should be done in persist() " + " code:" + instance.getMessageCode());
        }
        LOGGER.debug("instance.getMessageCode() is: {}", instance.getMessageCode());
        saveTranslations(instance);
        return result;
    }

    /**
     * enhanced persist to create a message code and include the translations on
     * persist
     * 
     */
    @Override
    @Transactional
    public String persist() {

        final String result = super.persist();
        final E instance = super.getInstance();

        // we need a refresh in case we are using blobs
        getSession().refresh(instance);

        instance.setupMessageCode();
        saveTranslations(instance);
        return result;

    }

    /**
     * enhanced remove to also remove the translations
     * 
     */
    @Override
    @Transactional
    public String remove() {
        LOGGER.debug("remove() called");
        final String result = super.remove();
        final E instance = super.getInstance();
        // instance.setupMessageCode();
        LOGGER.debug("deleteTranslations");
        deleteTranslations(instance);
        return result;
    }

    // --------------- tools ---------------------

    /**
     * return a bundle that may or may not be persisted already
     * 
     */
    @SuppressWarnings("unchecked")
    private DatabaseMessageMap getBundle(final String bundleName, final String localeId) {
        // query the DB for the bundle
        final List<DatabaseMessageMap> list = getSession().getNamedQuery(DatabaseMessageMap.FIND_BY_NAME_AND_LOCALE_ID).setParameter("name", bundleName)
                .setParameter("localeId", localeId).list();

        LOGGER.debug("query result is: {}", list);

        // create one if nothing found in the DB
        DatabaseMessageMap bundle;
        if ((list != null) && (list.size() == 1)) {
            bundle = list.get(0);
            LOGGER.debug("found bundle {} with message size {}", bundle, bundle.getMessages().size());
        } else {
            bundle = new DatabaseMessageMap();
            bundle.setName(bundleName);
            bundle.setLocaleId(localeId);
            LOGGER.debug("creating new bundle with name {}, localeid {}", bundleName, localeId);
        }
        return bundle;
    }

    /**
     * update an already persisted bundle or insert a new bundle
     * 
     */
    private void saveTranslations(final E instance) {
        final String messageCode = instance.getMessageCode();
        LOGGER.debug("persisting translations for code: {}", messageCode);
        final Session hibernateSession = getSession();

        final Set<String> localeIds = translations.keySet();
        for (final String localeId : localeIds) {
            final DatabaseMessageMap bundle = getBundle(getBundleName(), localeId);

            final String translation = translations.get(localeId);
            if ((translation == null) || (translation.trim().length() == 0)) {
                bundle.getMessages().remove(messageCode);
            } else {
                bundle.getMessages().put(messageCode, translation);
            }

            if (hibernateSession.contains(bundle)) {
                LOGGER.debug("merging bundle for code: {} with locale: {}", messageCode, localeId);
                hibernateSession.merge(bundle);
            } else {
                LOGGER.debug("persisting bundle for code: {} with locale: {}", messageCode, localeId);
                hibernateSession.persist(bundle);
            }
        }
        hibernateSession.flush();
        // ResourceBundle.clearCache();

        refreshBundles();
        LOGGER.debug("flushing entityManager, clearing cache");
    }

    /**
     * delete an entry in a bundle from the DB defined by the bundle name and
     * the message code
     */
    private void deleteTranslations(final E instance) {
        final String messageCode = instance.getMessageCode();
        final Session hibernateSession = getSession();

        final Set<String> localeIds = translations.keySet();
        for (final String localeId : localeIds) {
            final DatabaseMessageMap bundle = getBundle(getBundleName(), localeId);

            final String translation = translations.get(localeId);
            if ((translation == null) || (translation.trim().length() == 0)) {
                bundle.getMessages().remove(messageCode);
            }

            if (hibernateSession.contains(bundle)) {
                LOGGER.debug("mergeing bundle for code: {} with locale: {}", messageCode, localeId);
                hibernateSession.merge(bundle);
            } else {
                LOGGER.debug("persisting bundle for code: {} with locale: {}", messageCode, localeId);
                hibernateSession.persist(bundle);
            }
        }

        refreshBundles();
    }

    // this method reloads the bundles in the global bundle
    private void refreshBundles() {
        // refresh the resource bundles
        final GlobalResourceBundle resourceBundle = (GlobalResourceBundle) Contexts.getApplicationContext().get("org.jboss.seam.core.resourceBundle");
        if (resourceBundle == null) {
            LOGGER.debug("can't find resourceBundle in application context, available names in application context are: ");
            final String[] names = Contexts.getApplicationContext().getNames();
            for (final String name : names) {
                LOGGER.debug("  " + name);
            }
            LOGGER.debug("not refreshing the respource bundles");
        } else {
            final Set<String> localeIds = translations.keySet();
            for (final String localeId : localeIds) {
                resourceBundle.reloadBundle(localeId, getBundleName());
            }
        }
    }

}
