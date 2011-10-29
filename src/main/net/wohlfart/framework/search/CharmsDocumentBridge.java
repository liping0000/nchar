package net.wohlfart.framework.search;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Map;

import net.wohlfart.framework.entities.CharmsDocument;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.hibernate.search.bridge.LuceneOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * we use solr and tika to create an index...
 * 
 * * Tika will automatically attempt to determine the input document type (word,
 * pdf, etc.) and extract the content appropriately. If you want, you can
 * explicitly specify a MIME type for Tika wth the stream.type parameter
 * 
 * Tika does everything by producing an XHTML stream that it feeds to a SAX
 * ContentHandler.
 * 
 * Solr then reacts to Tika's SAX events and creates the fields to index. Tika
 * produces Metadata information such as Title, Subject, and Author, according
 * to specifications like DublinCore.
 * 
 * See http://lucene.apache.org/tika/formats.html for the file types supported.
 * 
 * * All of the extracted text is added to the "content" field * We can map
 * Tika's metadata fields to Solr fields. We can boost these fields * We can
 * also pass in literals for field values. * We can apply an XPath expression to
 * the Tika XHTML to restrict the content that is produced.
 * 
 * 
 */
public class CharmsDocumentBridge extends AbstractWorkflowBridgeAdaptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsDocumentBridge.class);

    // see:
    // http://blog.pfa-labs.com/2009/03/building-custom-entity-bridge-with.html
    // for indexing with a Class Bridge...

    @Override
    public void set(final String entityName, final Object value, final Document document, final LuceneOptions luceneOptions) {

        LOGGER.debug("set called, entityName is: {}", entityName);
        LOGGER.debug("  value is: {}", value);
        LOGGER.debug("  document is: {}", document);
        LOGGER.debug("  luceneOptions is: {}", luceneOptions);

        // value is the entity to be stored, make sure we have the right type
        if ((value == null) || (!(value instanceof CharmsDocument))) {
            LOGGER.warn("wrong document type for indexing, is {}, should be CharmsDocument", value != null ? value.getClass() : "null",
                    CharmsDocument.class.getName());
            return;
        }

        final CharmsDocument charmsDocument = (CharmsDocument) value;

        InputStream inputStream = null;
        try {
            LOGGER.debug("getting file");
            final File file = charmsDocument.getFile();
            if (file != null) {
                LOGGER.debug("getting input stream for file");
                // first try to get the file
                inputStream = new FileInputStream(file);
            } else {
                LOGGER.debug("getting input stream for content");
                // try to get the content from DB
                inputStream = charmsDocument.getContentStream();
                // System.out.println(convertStreamToString(charmsDocument.getContentStream()));
            }
            // InputStream stream =
            // charmsDocument.get.getContent().getBinaryStream();

            LOGGER.debug("running indexer");
            String content = runIndexer(inputStream);
            LOGGER.debug("finished indexer");
            content = StringUtils.defaultIfEmpty(content, "");
            // LOGGER.debug("creating field");
            // Field nameField = new Field(
            // "content", content,
            // luceneOptions.getStore(),
            // luceneOptions.getIndex(),
            // luceneOptions.getTermVector() );
            //
            // LOGGER.debug("adding to document");
            // document.add( nameField );
            // LOGGER.debug("added to document");

            // new in lucene 3:
            luceneOptions.addFieldToDocument("content", content, document);

        } catch (final FileNotFoundException ex) {
            LOGGER.warn("file not found for indexing: {}", ex);
            return;
        } catch (final IOException ex) {
            LOGGER.warn("IO error indexing: {}", ex);
            return;
        } catch (final SQLException ex) {
            LOGGER.warn("database error indexing: {}", ex);
        } catch (final SAXException ex) {
            LOGGER.warn("SAXException while indexing: {}", ex);
        } catch (final TikaException ex) {
            LOGGER.warn("TikaException while indexing: {}", ex);
        } catch (final Exception ex) {
            ex.printStackTrace();
        } catch (final Throwable ex) {
            ex.printStackTrace();
        } finally {
            if (inputStream != null) {
                LOGGER.info("closing stream");
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    // ignored
                }
            }
        }

        // In this particular class the name of the new field was passed
        // from the name field of the ClassBridge Annotation. This is not
        // a requirement. It just works that way in this instance. The
        // actual name could be supplied by hard coding it below.

        // add custom mappings...
        // http://docs.jboss.org/hibernate/stable/search/api/org/hibernate/search/bridge/LuceneOptions.html#addFieldToDocument%28java.lang.String,%20java.lang.String,%20org.apache.lucene.document.Document%29

        final String name = StringUtils.defaultIfEmpty(charmsDocument.getName(), "");
        // Field nameField = new Field(
        // "name", name,
        // luceneOptions.getStore(),
        // luceneOptions.getIndex(),
        // luceneOptions.getTermVector() );
        // document.add( nameField );
        // new in lucene 3:
        luceneOptions.addFieldToDocument("name", name, document);

        final String editorName = StringUtils.defaultIfEmpty(charmsDocument.getEditorName(), "");
        // Field editorNameField = new Field(
        // "editorName", editorName,
        // luceneOptions.getStore(),
        // luceneOptions.getIndex(),
        // luceneOptions.getTermVector() );
        // document.add( editorNameField );
        // new in lucene 3:
        luceneOptions.addFieldToDocument("editorName", editorName, document);

        final String comment = StringUtils.defaultIfEmpty(charmsDocument.getComment(), "");
        // Field commentField = new Field(
        // "comment", comment,
        // luceneOptions.getStore(),
        // luceneOptions.getIndex(),
        // luceneOptions.getTermVector() );
        // document.add( commentField );
        // new in lucene 3:
        luceneOptions.addFieldToDocument("comment", comment, document);

        /*
         * Department dep = (Department) value; String fieldValue1 =
         * dep.getBranch(); if ( fieldValue1 == null ) { fieldValue1 = ""; }
         * String fieldValue2 = dep.getNetwork(); if ( fieldValue2 == null ) {
         * fieldValue2 = ""; } String fieldValue = fieldValue1 + sepChar +
         * fieldValue2;
         * 
         * field.setBoost( luceneOptions.getBoost() );
         */

    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setParameterValues(final Map parameters) {
        LOGGER.info("setParameterValues called, parameters is: {}", parameters);
    }

    private String runIndexer(final InputStream inputStream) throws IOException, SAXException, TikaException {
        /*
         * Metadata metadata = new Metadata(); Parser parser = new
         * AutoDetectParser(); //Automatically determines file type
         * ContentHandler handler = new BodyContentHandler(); content =
         * parser.parse(inputStream,handler,metadata);
         * 
         * 
         * see:
         * http://www.slideshare.net/jukka/mime-magic-with-apache-tika-2440559
         */

        final Metadata metadata = new Metadata();
        final AutoDetectParser parser = new AutoDetectParser(); // Automatically
                                                                // determines
                                                                // file type
        final ContentHandler handler = new BodyContentHandler();

        // System.err.println("newline is: >"
        // + System.getProperty("line.separator")
        // + "<");

        BufferedInputStream bufferedInputStream;
        bufferedInputStream = new BufferedInputStream(inputStream);
        parser.parse(bufferedInputStream, handler, metadata);
        // MediaType type = parser.getDetector().detect(bufferedInputStream,
        // metadata);

        bufferedInputStream.close();
        // System.err.println("newline is: >"
        // + System.getProperty("line.separator")
        // + "<");

        // System.err.println("MediaType: " + type);

        // System.err.println("handler: " + handler.toString());
        // parser.getDetector().
        //
        // parser.parse(inputStream,handler,metadata);

        return null; // metadata.get(Metadata.);
    }

    @SuppressWarnings("unused")
    private String convertStreamToString(final InputStream is) {
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

}
