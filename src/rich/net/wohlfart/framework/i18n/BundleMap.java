package net.wohlfart.framework.i18n;

import java.util.Collection;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * implemention of the map that is used in the jsf view layer most map methods
 * are unsupported, we basically just need the get() method to get the
 * translation for a message key
 * 
 * see:
 * http://www.jboss.org/index.html?module=bb&op=viewtopic&t=101324&postdays=
 * 0&postorder=asc&start=20
 * 
 * @author Michael Wohlfart
 * 
 */
public class BundleMap implements Map<String, String> {

    private final static Logger LOGGER         = LoggerFactory.getLogger(BundleMap.class);

    // the data holder
    private ResourceBundle      resourceBundle = null;

    public BundleMap(final ResourceBundle resourceBundle) {
        LOGGER.debug("BundleMap created for a resource bundle with locale {}", resourceBundle.getLocale());
        this.resourceBundle = resourceBundle;
    }

    public ResourceBundle getBundle() {
        return resourceBundle;
    }

    // these methods are called for each translation text,
    // so they better be fast...

    // the only implemented map method
    @Override
    public String get(final Object key) {
        return get(key, key == null ? "null" : key.toString());
    }

    // custom method to return default values instead of the keys
    public String get(final Object key, final String dfault) {
        if (key == null) {
            return dfault;
        }
        try {
            final String result = resourceBundle.getString(key.toString());
            if (result == null) {
                return dfault;
            } else {
                return result;
            }
        } catch (final MissingResourceException mre) {
            return dfault;
        }
    }

    // -------------- unsupported map methods follow --------------

    @Override
    public void clear() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean containsKey(final Object key) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean containsValue(final Object value) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Set<java.util.Map.Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String put(final String key, final String value) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void putAll(final Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String remove(final Object key) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Collection<String> values() {
        throw new UnsupportedOperationException("not implemented");
    }

}
