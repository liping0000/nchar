package net.wohlfart.changerequest.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * helper class to handle default sheets within the classpath, doesn't affect
 * persistence
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public abstract class AbstractSheet {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractSheet.class);

    abstract void setContent(String content);

    abstract void setSize(Long l);

    protected void initContent(final InputStream stream) {
        final String content = convertStreamToString(stream);
        setContent(content);
        setSize(Long.valueOf(content.length()));
    }

    protected String convertStreamToString(final InputStream is) {
        if (is == null) {
            LOGGER.warn("input stream is null for abstract sheet");
            return "";
        }
        /*
         * To convert the InputStream to String we use the
         * BufferedReader.readLine() method. We iterate until the BufferedReader
         * return null which means there's no more data to read. Each line will
         * appended to a StringBuilder and returned as String.
         */
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (final IOException ex) {
            LOGGER.warn("IO error reading input stream");
            ex.printStackTrace();
        }
        return sb.toString();
    }

}
