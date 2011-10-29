package net.wohlfart.framework.mime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.medsea.mimeutil.MimeException;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.detector.MimeDetector;

class CustomExtensionMimeDetector extends MimeDetector {

    private final static Logger LOGGER                   = LoggerFactory.getLogger(CustomExtensionMimeDetector.class);

    // this file must be in the same package as this class
    private final static String MIME_PROPERTIES_FILENAME = "mime-types.properties";

    // Extension MimeTypes
    @SuppressWarnings("rawtypes")
    private static Map          extMimeTypes;

    public CustomExtensionMimeDetector() {
        CustomExtensionMimeDetector.initMimeTypes();
    }

    @Override
    public String getDescription() {
        return "Get the mime types of file extensions";
    }

    /**
     * Get the mime type of a file using extension mappings. The file path can
     * be a relative or absolute path or can refer to a completely non-existent
     * file as only the extension is important here.
     * 
     * @param file
     *            points to a file or directory. May not actually exist
     * @return collection of the matched mime types.
     * @throws MimeException
     *             if errors occur.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection getMimeTypesFile(final File file) throws MimeException {
        return getMimeTypesFileName(file.getName());
    }

    /**
     * Get the mime type of a URL using extension mappings. Only the extension
     * is important here.
     * 
     * @param url
     *            is a valid URL
     * @return collection of the matched mime types.
     * @throws MimeException
     *             if errors occur.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection getMimeTypesURL(final URL url) throws MimeException {
        return getMimeTypesFileName(url.getPath());
    }

    /**
     * Get the mime type of a file name using file name extension mappings. The
     * file name path can be a relative or absolute path or can refer to a
     * completely non-existent file as only the extension is important here.
     * 
     * @param fileName
     *            points to a file or directory. May not actually exist
     * @return collection of the matched mime types.
     * @throws MimeException
     *             if errors occur.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Collection getMimeTypesFileName(final String fileName) throws MimeException {
        final Collection mimeTypes = new HashSet();

        String fileExtension = MimeUtil.getExtension(fileName);
        while (fileExtension.length() != 0) {
            String types = null;
            // First try case sensitive
            types = (String) extMimeTypes.get(fileExtension);
            if (types != null) {
                final String[] mimeTypeArray = types.split(",");
                for (final String element : mimeTypeArray) {
                    mimeTypes.add(new MimeType(element));
                }
                return mimeTypes;
            }
            if (mimeTypes.isEmpty()) {
                // Failed to find case insensitive extension so lets try again
                // with
                // lowercase
                types = (String) extMimeTypes.get(fileExtension.toLowerCase());
                if (types != null) {
                    final String[] mimeTypeArray = types.split(",");
                    for (final String element : mimeTypeArray) {
                        mimeTypes.add(new MimeType(element));
                    }
                    return mimeTypes;
                }
            }
            fileExtension = MimeUtil.getExtension(fileExtension);
        }
        return mimeTypes;
    }

    /*
     * This loads the mime-types.properties files that define mime types based
     * on file extensions using the following load sequence 1. Loads the
     * property file from the mime utility jar named
     * eu.medsea.mime.mime-types.properties. 2. Locates and loads a file named
     * .mime-types.properties from the users home directory if one exists. 3.
     * Locates and loads a file named mime-types.properties from the classpath
     * if one exists 4. locates and loads a file named by the JVM property
     * mime-mappings i.e. -Dmime-mappings=../my-mime-types.properties
     */
    @SuppressWarnings("rawtypes")
    private static synchronized void initMimeTypes() {
        extMimeTypes = new Properties();
        // Load the file extension mappings from the internal property file and
        // then
        // from the custom property files if they can be found
        InputStream inputStream = null;
        try {
            // Load the default supplied mime types
            // FileInputStream fis = new
            // FileInputStream("mime-types.properties");
            inputStream = CustomExtensionMimeDetector.class.getResourceAsStream(MIME_PROPERTIES_FILENAME);
            if (inputStream == null) {
                LOGGER.error("can't find internal file mime-types.properties");
            } else {
                ((Properties) extMimeTypes).load(inputStream);
            }
        } catch (final Exception e) {
            // log the error but don't throw the exception up the stack
            LOGGER.error("Error loading internal mime-types.properties", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        // Load the mime types into the known mime types map of MimeUtil
        final Iterator it = extMimeTypes.values().iterator();
        while (it.hasNext()) {
            final String[] types = ((String) it.next()).split(",");
            for (final String type : types) {
                LOGGER.trace("adding mime type {}", type);
                MimeUtil.addKnownMimeType(type);
            }
        }
    }

    /**
     * This method is required by the abstract MimeDetector class. As we do not
     * support extension mapping of streams we just throw an
     * {@link UnsupportedOperationException}. This ensures that the
     * getMimeTypes(...) methods ignore this method. We could also have just
     * returned an empty collection.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection getMimeTypesInputStream(final InputStream in) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This MimeDetector does not support detection from streams.");
    }

    /**
     * This method is required by the abstract MimeDetector class. As we do not
     * support extension mapping of byte arrays we just throw an
     * {@link UnsupportedOperationException}. This ensures that the
     * getMimeTypes(...) methods ignore this method. We could also have just
     * returned an empty collection.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection getMimeTypesByteArray(final byte[] data) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This MimeDetector does not support detection from byte arrays.");
    }

}
