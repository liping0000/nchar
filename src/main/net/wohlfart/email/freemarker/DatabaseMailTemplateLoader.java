package net.wohlfart.email.freemarker;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import net.wohlfart.email.entities.CharmsEmailTemplate;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.international.LocaleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.TemplateLoader;

/**
 * this class loads the email templates from the database
 * 
 * @author Michael Wohlfart
 */
public class DatabaseMailTemplateLoader implements TemplateLoader {

    private final static Logger LOGGER = LoggerFactory.getLogger(DatabaseMailTemplateLoader.class);

    /**
     * this is the main method that looks up a template in the database the
     * template name is encoded with the locale string since we can use multiple
     * tmepleates with the same name in DB we use the id of the template as name
     */
    @SuppressWarnings({ "unchecked", "unused" })
    @Override
    @Transactional(TransactionPropagationType.MANDATORY)
    public Object findTemplateSource(final String name) throws IOException {

        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        Object result = null;

        LOGGER.debug("lookup for: " + name);

        // name contains the locale like:
        // "implement_reminder_mail_de"
        // if such a call returns null the next lookup is done by:
        // "implement_reminder_mail"
        if (name == null) {
            return null;
        }

        String lookupName = name;
        String lookupLocale = null;
        // loop through the locales and try to chop off the locale extension
        final LocaleConfig localeConfig = LocaleConfig.instance();
        for (final String localeId : localeConfig.getSupportedLocales()) {
            if (name.endsWith("_" + localeId)) {
                lookupName = name.substring(0, name.length() - (1 + localeId.length()));
                lookupLocale = name.substring(name.length() - 2);
                LOGGER.debug("lookup for: " + lookupName + " with locale: " + lookupLocale);
            }
        }

        final List<CharmsEmailTemplate> tmpltList = hibernateSession.getNamedQuery(CharmsEmailTemplate.FIND_BY_ID).setParameter("id", new Long(lookupName))
                .list();

        if (tmpltList.size() == 0) {
            // nothing found
            LOGGER.warn("no template found returning null, name was: {} with locale: {} ", lookupName, lookupLocale);
            return null;
        }

        final CharmsEmailTemplate template = tmpltList.get(0);

        // TODO:
        // we don't support localized templates so far even if they are
        // persisted and managed by the database already, just return a template
        // if it isn't for any specific locale:
        //
        // FIXME: before removing this check make sure that all localized
        // versions are actually set up
        // for all emails, or even better add a boolean to the email
        // translations
        // for activating/deactivating them...
        if (lookupLocale != null) {
            return null; // the caller will try to get the non localized version
                         // next
        }

        // do we need a locale for this lookup
        if (lookupLocale == null) {
            result = new MailTemplateSource(template);
            LOGGER.debug("locale less template " + result);
            return result;
            // org.hibernate.HibernateException: null index column for
            // collection:
            // net.wohlfart.charms.entity.CharmsEmailTemplate.translations
        } else if ((lookupLocale != null) && (template.getTranslations().size() > 0) && (template.getTranslations().containsKey(lookupLocale))) {
            result = new MailTemplateSource(template, lookupLocale); // template.getTranslations().get(lookupLocale);
            LOGGER.debug("localized template: " + result);
            return result;
        } else {
            LOGGER.warn("template not found, lookupLocale is {}, name is {} ", lookupLocale, lookupName);
            return null;
        }

    }

    @Override
    public long getLastModified(final Object templateSource) {
        if ((templateSource == null) || (!(templateSource instanceof MailTemplateSource)) || (((MailTemplateSource) templateSource).getLastModified() == null)) {
            return -1; // -1 means I don't know
        } else {
            return ((MailTemplateSource) templateSource).getLastModified().getTime();
        }
    }

    @Override
    public Reader getReader(final Object templateSource, final String encoding) throws IOException {
        return ((MailTemplateSource) templateSource).getReader();
    }

    @Override
    public void closeTemplateSource(final Object templateSource) throws IOException {
        // do nothing
    }

}
