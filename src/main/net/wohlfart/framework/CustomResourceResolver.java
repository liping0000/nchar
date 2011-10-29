package net.wohlfart.framework;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.facelets.impl.DefaultResourceResolver;
import com.sun.facelets.impl.ResourceResolver;

/**
 * in JSF2: import javax.faces.view.facelets.ResourceResolver; import
 * com.sun.faces.facelets.impl.DefaultResourceResolver;
 * 
 */
public class CustomResourceResolver extends DefaultResourceResolver implements ResourceResolver {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomResourceResolver.class);

    @Override
    public URL resolveUrl(final String resource) {
        final URL resourceUrl = super.resolveUrl(resource);
        if (resource.startsWith("/pages")) {
            LOGGER.debug("resolved resource {} to URL {}", resource, resourceUrl);
            return resourceUrl;
        }
        if (resourceUrl == null) {
            LOGGER.warn("can't resolve resource {}, return null", resource);
        }
        return resourceUrl;
    }

}
