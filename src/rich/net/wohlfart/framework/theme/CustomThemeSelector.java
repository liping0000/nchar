package net.wohlfart.framework.theme;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.theme.ThemeSelector;
import org.jboss.seam.util.IteratorEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * to get a translated message for a theme: String key = "org.jboss.seam.theme."
 * + name; String localizedName = Messages.instance().get(key);
 * 
 * @author Michael Wohlfart
 * 
 */

@Scope(ScopeType.SESSION)
@Name(value = "org.jboss.seam.theme.themeSelector")
@BypassInterceptors
@Install(precedence = Install.APPLICATION, classDependencies = "javax.faces.context.FacesContext")
public class CustomThemeSelector extends ThemeSelector {


    private final static Logger LOGGER        = LoggerFactory.getLogger(CustomThemeSelector.class);

    // FIXME: use dot notation for the properties file
    // FIXME: set the cookie name

    // this is the seam nameing scheme, .skin resolves to a package when using
    // the ResourceBundle.getBundle method to load resources
    private final static String THEME_POSTFIX = ".skin.properties";

    public List<String> getSupportedThemeIds() {
        // we use the theme name as identifier
        final String[] themeNames = getAvailableThemes();
        final List<String> result = new ArrayList<String>(themeNames.length);
        for (final String name : themeNames) {
            result.add(name);
        }
        return result;
    }

    @Override
    /**
     * Get the resource bundle for the theme, we override this
     * to add a ".skin.properties" string to the filename
     *
     * some other ways to load resource bundles:
     *
     *
     */
    public ResourceBundle getThemeResourceBundle() {
        // FIXME: check if this works on windows too:
        final String filename = "/" + getTheme() + THEME_POSTFIX;
        try {

            final InputStream input = CustomThemeSelector.class.getResourceAsStream(filename);
            if (input == null) {
                LOGGER.warn("resource bundle missing: {}", filename);
                return dummyBundle();
            }
            final PropertyResourceBundle bundle = new PropertyResourceBundle(input);

            LOGGER.info("loaded resource bundle: {}", filename);
            return bundle;

        } catch (final FileNotFoundException ex) {
            LOGGER.warn("file for resource bundle missing ", ex);
            return dummyBundle();
        } catch (final IOException ex) {
            LOGGER.warn("IO exception for resource bundle in file ", ex);
            return dummyBundle();
        }
    }

    private ResourceBundle dummyBundle() {
        return new ResourceBundle() {

            @Override
            @SuppressWarnings("unchecked")
            public Enumeration<String> getKeys() {
                return new IteratorEnumeration(Collections.EMPTY_LIST.iterator());
            }

            @Override
            protected Object handleGetObject(final String key) {
                return null;
            }
        };
    }

    @Override
    public void selectTheme(final String themeName) {
        super.selectTheme(themeName);
    }

    public void selectThemeId(final String themeId) {
        if ((themeId != null) && (getSupportedThemeIds().contains(themeId))) {
            selectTheme(themeId);
        }
    }

}
