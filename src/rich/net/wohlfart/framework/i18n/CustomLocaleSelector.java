package net.wohlfart.framework.i18n;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.cache.CacheProvider;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * customized local selector,
 * the cache is cleared when the locale is changed
 * 
 * @author Michael Wohlfart
 */
@Scope(ScopeType.SESSION)
@Name(value = "org.jboss.seam.international.localeSelector")
@BypassInterceptors
@Install(precedence = Install.APPLICATION, classDependencies = "javax.faces.context.FacesContext")
public class CustomLocaleSelector extends LocaleSelector implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomLocaleSelector.class);

    /**
     * use to create a select pulldown menu to choose the language, the human
     * readable display names of the available locales in the current local are
     * returned
     */
    @Override
    public List<SelectItem> getSupportedLocales() {
        final List<SelectItem> selectItems = new ArrayList<SelectItem>();
        // all supported locals 
        final Iterator<Locale> locales = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
        LOGGER.debug("returning list of available locale, current locale is {}, locale iterator is: {}", getLocale().toString(), locales);
        while (locales.hasNext()) {
            final Locale locale = locales.next();
            if (!Strings.isEmpty(locale.getLanguage())) {
                // we get the names in the current locale,
                // the current locale should be stored in the parent object
                selectItems.add(new SelectItem(locale.toString(), locale.getDisplayName(getLocale())));
            }
        }
        LOGGER.debug("result size is: {}", selectItems.size());
        return selectItems;
    }

    /**
     * use to create a list of links for the language selection, the local ids
     * consisting of language and country are returned used to create a list of
     * image links...
     */
    public List<String> getSupportedLocaleIds() {
        final List<String> result = new ArrayList<String>();
        // all supported locals 
        final Iterator<Locale> locales = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
        LOGGER.debug("returning list of available locale ids, current locale is {}, locale iterator is: {}", getLocale().toString(), locales);
        while (locales.hasNext()) {
            final Locale locale = locales.next();
            if (!Strings.isEmpty(locale.getLanguage())) {
                result.add(locale.getLanguage() + (Strings.isEmpty(locale.getCountry()) ? "" : "_" + locale.getCountry())
                        + (Strings.isEmpty(locale.getVariant()) ? "" : "_" + locale.getVariant()));
            }
        }
        LOGGER.debug("result size is: {}", result.size());
        return result;
    }

    @SuppressWarnings("rawtypes")
    public void selectLocaleId(final String localeId) {
        LOGGER.debug("selectLocaleId is called with {}", localeId);
        if ((localeId != null) && (getSupportedLocaleIds().contains(localeId))) {           
            // there is no alias for this component so we need the whole path to get the instance
            CacheProvider cacheProvider = (CacheProvider) Component.getInstance("org.jboss.seam.cache.cacheProvider");            
            // FIXME: we don't need to clear the whole cache here, just the user portion of it
            if (cacheProvider == null) {
                LOGGER.info("cacheProvider is null");
            } else {
                LOGGER.debug("locale switch triggered by the user, clearing cache");
                cacheProvider.clear();
            }        
            super.setLocaleString(localeId);
        }
    }

}
