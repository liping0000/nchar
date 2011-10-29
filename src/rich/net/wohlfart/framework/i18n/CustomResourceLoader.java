package net.wohlfart.framework.i18n;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.core.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see: http://in.relation.to/2303.lace
// http://www.jboss.org/index.html?module=bb&op=viewtopic&t=101324&postdays=0&postorder=asc&start=50
// override messages:
// http://www.jboss.org/index.html?module=bb&op=viewtopic&t=101324&postdays=0&postorder=asc&start=20

/**
 * this class replaces the ordinary way to load resource bundles in seam it
 * enhances the message loading by: - adding the file along the .xhtml
 * definition as message properties - adding messages from the DB as resource
 * bundle
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.STATELESS)
@BypassInterceptors
// @Name("org.jboss.seam.core.resourceLoader")
public class CustomResourceLoader extends org.jboss.seam.faces.ResourceLoader {

    private final static Logger LOGGER                    = LoggerFactory.getLogger(CustomResourceLoader.class);

    // hardcoded resource bundle names for the business objects
    public static final String  CHREQ_PROD_BUNDLE_NAME    = "chreq.product";
    public static final String  CHREQ_UNIT_BUNDLE_NAME    = "chreq.unit";
    public static final String  CHREQ_CODE_BUNDLE_NAME    = "chreq.code";

    public static final String  CHARMS_REPORT_BUNDLE_NAME = "charms.report";

    // for debugging and testing
    public static final String  TEST_BUNDLE_NAME          = "testbundle";

    @Override
    public ResourceBundle loadBundle(final String bundleName) {
        return loadBundle(bundleName, Locale.instance());
    }

    public ResourceBundle loadBundle(final String bundleName, final String localeId) {
        return loadBundle(bundleName, new java.util.Locale(localeId));
    }

    @SuppressWarnings("unchecked")
    public ResourceBundle loadBundle(final String bundleName, final java.util.Locale locale) {
        java.util.ResourceBundle diskBundle = null;
        java.util.ResourceBundle databaseBundle = null;

        // try to load the bundle from disk:
        try {
            diskBundle = java.util.ResourceBundle.getBundle(bundleName, locale, Thread.currentThread().getContextClassLoader());
        } catch (final MissingResourceException mre) {
        }

        // create a seam context for this thread if we don't have one yet...
        // FIXME: this is a hack
        final boolean createContexts = !Contexts.isConversationContextActive();
        if (createContexts) {
            LOGGER.warn("creating a seam context to initialize the hibernateSession");
            Lifecycle.beginCall();
        }

        // try to load the bundle from the database, create on if none there
        // yet...
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        final String localeId = locale.getLanguage();

        if (hibernateSession == null) {
            LOGGER.warn("skipping database bundle load for bundleName: {} localeId: {}, no hibernateSession found", new Object[] { bundleName, localeId });
        } else {
            final List<DatabaseMessageMap> result = hibernateSession.getNamedQuery(DatabaseMessageMap.FIND_BY_NAME_AND_LOCALE_ID)
                    .setParameter("name", bundleName).setParameter("localeId", localeId).list();

            if ((result != null) && (result.size() == 1)) {
                final DatabaseMessageMap databaseMessageMap = result.get(0);
                databaseBundle = createBundle(databaseMessageMap);
            }
        }

        if (databaseBundle != null) {
            return databaseBundle;
        } else {
            return diskBundle;
        }

    }

    // create an adaptor to turn a map into a resource bundle
    @Transactional
    public ResourceBundle createBundle(final DatabaseMessageMap databaseMessageMap) {
        return new ResourceBundle() {

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(databaseMessageMap.getMessages().keySet());
            }

            @Override
            protected Object handleGetObject(final String key) {
                return databaseMessageMap.getMessages().get(key);
            }
        };
    }

}
