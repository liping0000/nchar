package net.wohlfart.framework.mime;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.detector.MimeDetector;

class CustomMimeDetector extends MimeDetector {

    private static final HashMap<String, MimeType> map = new HashMap<String, MimeType>();

    { // setup a static list of ending to mimetype mappings, this are only the
      // preferred mimetypes
        map.put(".txt", new MimeType(MimeTypeUtil.MIMETYPE_TEXT_PLAIN));
        map.put(".xls", new MimeType(MimeTypeUtil.MIMETYPE_APPLICATION_EXCEL));
        map.put(".doc", new MimeType(MimeTypeUtil.MIMETYPE_APPLICATION_WORD));
        map.put(".ppt", new MimeType(MimeTypeUtil.MIMETYPE_APPLICATION_POWERPT));
        map.put(".zip", new MimeType(MimeTypeUtil.MIMETYPE_APPLICATION_ZIP));
    }

    public CustomMimeDetector() {
        initMimeTypes();
    }

    @Override
    public String getDescription() {
        return "a simple mime detector to boost detection of prefered mimetypes";
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Collection getMimeTypesByteArray(final byte[] data) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not supported");
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Collection getMimeTypesFile(final File file) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not supported");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Collection getMimeTypesFileName(final String fileName) throws UnsupportedOperationException {
        if ((fileName == null) || (fileName.length() < 4)) {
            return new ArrayList();
        }

        final ArrayList result = new ArrayList();
        final String suffix = StringUtils.right(fileName, 4);
        if (map.containsKey(suffix)) {
            result.add(map.get(suffix));
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Collection getMimeTypesInputStream(final InputStream in) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not supported");
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Collection getMimeTypesURL(final URL url) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not supported");
    }

    // The Alias list should contain just about all the mime types used by
    // this MimeDetector so we will be content with these entries
    private void initMimeTypes() {
        for (final MimeType mimeType : map.values()) {
            MimeUtil.addKnownMimeType(mimeType);
        }
    }
}
