package net.wohlfart.framework.mime;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;

/**
 * uses the magic.mime files to detect file content and assign a mime type we
 * just override the default location for the mime config files to only use the
 * internal file
 */
class CusomtMagicMimeMimeDetector extends MagicMimeMimeDetector {

    private static Logger     LOGGER           = LoggerFactory.getLogger(CusomtMagicMimeMimeDetector.class);

    // Having the defaultLocations as protected allows you to subclass this
    // class
    // and add different paths or remove them all so that the internal file is
    // always used
    protected static String[] defaultLocations = {};

    @SuppressWarnings("rawtypes")
    @Override
    public Collection getMimeTypesFile(final File file) throws UnsupportedOperationException {
        LOGGER.debug("resolving mime type for file: {}", file);
        return super.getMimeTypesFile(file);
    }
}
