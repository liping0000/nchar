package net.wohlfart.framework.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * properties file to match filetypes to icons the properties have to be loaded
 * on startup and are stored in a static instance variable
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class MimeTypeIcons {

    private final static Logger LOGGER              = LoggerFactory.getLogger(MimeTypeIcons.class);

    private final static String DEFAULT_ICON        = "misc_doc.png";

    private final static String DEFAULT_CONFIG_FILE = "MimeTypeIcons.xml";

    private static Properties   properties          = null;

    public static void setupProperties() {
        InputStream inputStream = null;
        try {
            inputStream = MimeTypeIcons.class.getResourceAsStream(DEFAULT_CONFIG_FILE);
            if (inputStream == null) {
                LOGGER.warn("can't setup icons since the input stream is empty for xml resource {} " + "in class {}", DEFAULT_CONFIG_FILE,
                        MimeTypeIcons.class.getName());
            }
            properties = new Properties();
            properties.loadFromXML(inputStream);
        } catch (final InvalidPropertiesFormatException ex) {
            ex.printStackTrace();
        } catch (final IOException ex) {
            ex.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static String getIconNameForMimetype(final String mimeType) {

        if (properties == null) {
            LOGGER.error("properties for MimeTypeIcons are not set up," + " please call MimeTypeIcons.setupProperties() on startup");
            return DEFAULT_ICON;
        }

        if (StringUtils.isEmpty(mimeType)) {
            return DEFAULT_ICON;
        }

        return properties.getProperty(mimeType, DEFAULT_ICON);
    }
}
