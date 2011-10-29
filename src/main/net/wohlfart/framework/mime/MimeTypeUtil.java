package net.wohlfart.framework.mime;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;

public class MimeTypeUtil {

    private final static Logger                      LOGGER                            = LoggerFactory.getLogger(MimeTypeUtil.class);

    public static final String                       MIMETYPE_APPLICATION_OCTET_STREAM = MimeUtil2.UNKNOWN_MIME_TYPE.toString();
    public static final String                       MIMETYPE_APPLICATION_ZIP          = "application/zip";
    public static final String                       MIMETYPE_TEXT_PLAIN               = "text/plain";
    public static final String                       MIMETYPE_TEXT_XML                 = "text/xml";
    public static final String                       MIMETYPE_APPLICATION_EXCEL        = "application/excel";
    public static final String                       MIMETYPE_APPLICATION_WORD         = "application/word";
    public static final String                       MIMETYPE_APPLICATION_POWERPT      = "application/powerpoint";

    private final static CustomMimeDetector          customMimeDetector                = new CustomMimeDetector();
    // this reads the mime-types.properties file
    private final static CustomExtensionMimeDetector extensionMimeDetector             = new CustomExtensionMimeDetector();
    // this reads the magix.mime file
    private final static CusomtMagicMimeMimeDetector magicMimeMimeDetector             = new CusomtMagicMimeMimeDetector();

    public static String findMimeType(final File file) {
        try {
            return findMimeType(file.getCanonicalPath(), file);
        } catch (final IOException ex) {
            LOGGER.warn("Exception finding mime type", ex);
            return MIMETYPE_APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * result must not be null
     * 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String findMimeType(final String fileName, final File fileContent) {
        // problem is the name of file is different than the real filename since
        // the upload gives us
        // a random filename

        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(fileContent));
        } catch (final FileNotFoundException ex) {
            ex.printStackTrace();
            return MIMETYPE_APPLICATION_OCTET_STREAM;
        }

        // need a list here becaus we might have the same mime type multiple
        // times from different matchers
        final Collection<MimeType> collection = new ArrayList<MimeType>();

        // custom mime detector implementation
        final Collection set01 = customMimeDetector.getMimeTypes(fileName);
        LOGGER.debug("CustomMimeDetector analysis by file name: {}", set01);
        collection.addAll(set01);

        // try to detect the mime type by extension, the extensionMimeDetector
        // only
        // analyzes the filename
        final Collection<MimeType> set03 = extensionMimeDetector.getMimeTypes(fileName);
        collection.addAll(set03);
        LOGGER.debug("ExtensionMimeDetector analysis by file name: {}", set03);

        // try to detect the mime type by content, the magicMimeMimeDetector
        // only
        // analyzes the file content
        Collection<MimeType> set05 = null;
        try {
            set05 = magicMimeMimeDetector.getMimeTypes(bufferedInputStream);
            collection.addAll(set05);
        } catch (final UnsupportedOperationException e) {
            e.printStackTrace();
        }
        LOGGER.debug("MagicMimeMimeDetector analysis by file content: {}", set05);

        LOGGER.debug("collection is: {}, analyzing collection now...", collection);
        final HashMap<String, Integer> hitCount = new HashMap<String, Integer>();
        Integer bestCount = 0;
        String bestMatch = MIMETYPE_APPLICATION_OCTET_STREAM;
        for (final Object type : collection) {
            LOGGER.debug("  collection element: {} is class: {}", type, type.getClass());
            final String key = type.toString();
            Integer newCount = 1;
            if (hitCount.containsKey(key)) {
                newCount = hitCount.get(key) + 1;
            }
            hitCount.put(key, newCount);
            if (newCount > bestCount) {
                bestCount = newCount;
                bestMatch = key;
            }
        }
        LOGGER.debug("dumping hitCount map: {} ...", hitCount);
        final Set<Entry<String, Integer>> set = hitCount.entrySet();
        for (final Entry<String, Integer> entry : set) {
            LOGGER.debug("  key: {}, value: {}", entry.getKey(), entry.getValue());
        }

        LOGGER.debug("best match is: {} with {} hits", bestMatch, bestCount);
        return bestMatch;
    }

}
