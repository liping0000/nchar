package net.wohlfart.framework.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * this class is not used, remove it...
 * 
 * @author michael
 *
 */
public class BlobBridge implements FieldBridge {

    private final static Logger LOGGER = LoggerFactory.getLogger(BlobBridge.class);

    @Override
    public void set(final String name, // the field name
            final Object value, // value of the field
            final Document document, // lucene document
            final LuceneOptions luceneOptions) {

        // value is usually of type
        // org.hibernate.lob.SerializableBlob
        // implements java.sql.Blob and Serializable

        // http://geeklondon.com/blog/view/dummy-object-dummy-error

        LOGGER.debug("property name is {} value class is {}", name, value.getClass().getName());
        try {
            if (value instanceof Blob) {
                final Blob blob = (Blob) value;
                final InputStream stream = blob.getBinaryStream();
                System.out.println(convertStreamToString(stream));
            }
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String convertStreamToString(final InputStream is) {
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
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    /*
     * 
     * 
     * 
     * Date date = (Date) value; Calendar cal =
     * GregorianCalendar.getInstance(GMT); cal.setTime(date); int year =
     * cal.get(Calendar.YEAR); int month = cal.get(Calendar.MONTH) + 1; int day
     * = cal.get(Calendar.DAY_OF_MONTH); // set year Field field = new
     * Field(name + ".year", String.valueOf(year), luceneOptions.getStore(),
     * luceneOptions.getIndex(), luceneOptions.getTermVector());
     * field.setBoost(luceneOptions.getBoost()); document.add(field); // set
     * month and pad it if needed field = new Field(name + ".month", month < 10
     * ? "0" : "" + String.valueOf(month), luceneOptions.getStore(),
     * luceneOptions.getIndex(), luceneOptions.getTermVector());
     * field.setBoost(luceneOptions.getBoost()); document.add(field); // set day
     * and pad it if needed field = new Field(name + ".day", day < 10 ? "0" : ""
     * + String.valueOf(day), luceneOptions.getStore(),
     * luceneOptions.getIndex(), luceneOptions.getTermVector());
     * field.setBoost(luceneOptions.getBoost()); document.add(field);
     */
}
