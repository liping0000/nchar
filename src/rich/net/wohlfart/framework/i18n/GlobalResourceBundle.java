package net.wohlfart.framework.i18n;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Interpolator;
import org.jboss.seam.core.ResourceLoader;
import org.jboss.seam.core.SeamResourceBundle;
import org.jboss.seam.util.EnumerationEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is the one and only seam session wide resource bundle application wide
 * probably doesn't work since the locale is defined for a session and so is the
 * resource bundle probably too...
 * 
 * see:
 * http://www.jboss.org/index.html?module=bb&op=viewtopic&t=101324&postdays=
 * 0&postorder=asc&start=20
 * 
 * this object does the resource bundle caching!
 * 
 * 
 * the original resource bundle is tied to the session, we use application to
 * conserve some memory drawback is we have to query the sessions's locale and
 * always keep the bundles for all locales
 * 
 * 
 * 
 * @author Michael Wohlfart
 * 
 */

@Scope(ScopeType.APPLICATION)
@BypassInterceptors
@Name("org.jboss.seam.core.resourceBundle")
public class GlobalResourceBundle extends SeamResourceBundle implements Serializable {


    private final static Logger                                          LOGGER             = LoggerFactory.getLogger(GlobalResourceBundle.class);

    // localeId, bundle name, (message key, massage text)
    // this is the central datastore for all translations resource bundles
    ConcurrentHashMap<String, ConcurrentHashMap<String, ResourceBundle>> allBundles         = new ConcurrentHashMap<String, ConcurrentHashMap<String, ResourceBundle>>();

    /** constant indicating that no resource bundle exists */
    private static final ResourceBundle                                  NONEXISTENT_BUNDLE = new ResourceBundle() {

                                                                                                @Override
                                                                                                public Enumeration<String> getKeys() {
                                                                                                    return null;
                                                                                                }

                                                                                                @Override
                                                                                                protected Object handleGetObject(final String key) {
                                                                                                    return null;
                                                                                                }

                                                                                                @Override
                                                                                                public String toString() {
                                                                                                    return "NONEXISTENT_BUNDLE";
                                                                                                }
                                                                                            };

    // the Resource Bundle Abstract method overrides

    /**
     * use the key to search for the message
     */
    @Override
    protected Object handleGetObject(final String key) {
        // the original method tries to resolve for page resources first,
        // we just skip this for performance and don't use page resources
        // in charms

        // try all the resource bundles for this locale one by one...

        // getBundlesForCurrentLocale never returns null and loads the bundles
        // on
        // demand
        final Enumeration<ResourceBundle> enumeration = getBundlesForCurrentLocale().elements();

        // go through the bundles and try to resolve the key
        while (enumeration.hasMoreElements()) {
            final ResourceBundle bundle = enumeration.nextElement();
            try {
                final Object resolved = bundle.getObject(key);
                if (resolved != null) {
                    return interpolate(resolved);
                }
            } catch (final MissingResourceException mre) {
                // nothing to do
            }
        }
        return null;
    }

    /**
     * 
     * the Resource Bundle Abstract method override
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getKeys() {
        final ConcurrentHashMap<String, ResourceBundle> bundles = getBundlesForCurrentLocale();
        final Enumeration<String>[] enumerations = new Enumeration[bundles.size()];

        int i = 0;
        final Enumeration<ResourceBundle> elements = bundles.elements();
        while (elements.hasMoreElements()) {
            enumerations[i++] = elements.nextElement().getKeys();
        }

        return new EnumerationEnumeration<String>(enumerations);
    }

    /**
     * this is needed for changing messages on the fly, we just reload the whole
     * bundle with all the containing messages
     * 
     * this method does NOT persis the bundles to the database this method is
     * called in TranslateableHome after setting the translated names
     * 
     * @param bundleName
     * @param localeId
     */
    public void reloadBundle(final String localeId, final String bundleName /*
                                                                             * ,
                                                                             * String
                                                                             * messageCode
                                                                             * ,
                                                                             * String
                                                                             * message
                                                                             */) {

        // load all bundles the
        if (!allBundles.containsKey(localeId)) {
            loadBundlesForLocale(localeId);
        }
        final ConcurrentHashMap<String, ResourceBundle> localeBundles = allBundles.get(localeId);
        if (localeBundles == null) {
            LOGGER.warn("no bundle found for localeId: {}, bundleName: {}", localeId, bundleName);
            return;
        }

        // try to load the new bundle from the store
        final CustomResourceLoader resourceLoader = (CustomResourceLoader) ResourceLoader.instance();
        ResourceBundle newBundle = resourceLoader.loadBundle(bundleName, localeId);
        if (newBundle == null) {
            newBundle = NONEXISTENT_BUNDLE; // needed since the hashmap doesn't
                                            // accept null values
        }
        LOGGER.debug("found newBundle name: {} locale: {}  object: {} ", new Object[] { newBundle, bundleName, localeId, newBundle });

        // the bundles for the required locale and name, this locale might not
        // have been loaded so far
        // if the locale wasn't used by any user yet
        final ResourceBundle oldBundle = localeBundles.get(bundleName);
        if (oldBundle == null) {
            LOGGER.debug("oldBundle with name {} for localeId {} not found", bundleName, localeId);
            LOGGER.debug("available bundleName are: ");
            final Set<String> keys = localeBundles.keySet();
            for (final String key : keys) {
                LOGGER.debug("   " + key);
            }
            LOGGER.debug("try to set bundle anyways");
            localeBundles.put(bundleName, newBundle);
        } else {
            LOGGER.debug("found oldBundle name: {} locale: {} oldBundle: {} newBundle: {} ", new Object[] { bundleName, localeId, oldBundle, newBundle });
            localeBundles.replace(bundleName, newBundle);
        }

    }

    // seam magic to resolve scoped placeholders
    private Object interpolate(final Object message) {
        return (message != null) && (message instanceof String) ? Interpolator.instance().interpolate((String) message) : message;
    }

    /**
     * return a mapping from bundle name to resource bundle for the current
     * locale in the session try to resolve by lookup in the allBundles first,
     * if the named resoucre bundle can't be found there then call
     * loadBundlesForLocale() in order to get the resource bundle from the
     * store/file/DB...
     * 
     * @return
     */
    //
    private ConcurrentHashMap<String, ResourceBundle> getBundlesForCurrentLocale() {
        final String currentLocale = org.jboss.seam.core.Locale.instance().getLanguage();

        // check if the locale is loaded already
        if (!allBundles.containsKey(currentLocale)) {
            // load the bundle
            loadBundlesForLocale(currentLocale);
            if (allBundles.get(currentLocale) == null) {
                LOGGER.warn("bundles for locale {} wasn't loaded", currentLocale);
            }
        }

        // bundle name, messageKey to text string
        LOGGER.debug("returning bundles for localeId: {}", currentLocale);
        final ConcurrentHashMap<String, ResourceBundle> result = allBundles.get(currentLocale);
        return result;
    }

    /**
     * load all bundles for a locale, the loading itself is delegated to the
     * CustomResourceLoader the loaded bundle is set in the allBundles delegatee
     * field, we can only load complete bundles at once with this method
     * 
     * @param currentLocale
     * @return
     */
    private void loadBundlesForLocale(final String localeId) {
        LOGGER.debug("loading bundle for localeId: {}", localeId);

        // this field contains all bundles for one locale
        // each bundle can be resolved by its name so we can reload single
        // bundles
        final ConcurrentHashMap<String, ResourceBundle> localeBundles = new ConcurrentHashMap<String, ResourceBundle>();

        final CustomResourceLoader resourceLoader = (CustomResourceLoader) ResourceLoader.instance();
        for (final String bundleName : resourceLoader.getBundleNames()) {
            LOGGER.debug("loading bundle with name {} for localeId: {} ", bundleName, localeId);
            final ResourceBundle bundle = resourceLoader.loadBundle(bundleName, localeId);
            if (bundle != null) {
                localeBundles.put(bundleName, bundle);
            } else {
                localeBundles.put(bundleName, NONEXISTENT_BUNDLE);
            }
        }

        // add the new locale bundle set to the global set of delegatees
        // or replace an existing value
        LOGGER.debug("replacing/setting bundles for locale {} ", localeId);
        allBundles.put(localeId, localeBundles);

        // the original implementation adds
        // loadBundle("ValidatorMessages");
        // loadBundle("org/hibernate/validator/resources/DefaultValidatorMessages");
        // loadBundle("javax.faces.Messages");
        // for loading, we specify them in components.xml
    }

}
