package net.wohlfart.framework.search;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ChangeRequestMessageBridge extends AbstractWorkflowBridgeAdaptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChangeRequestMessageBridge.class);

    // see:
    // http://blog.pfa-labs.com/2009/03/building-custom-entity-bridge-with.html
    // for indexing with a Class Bridge...

    @Override
    public void set(final String entityName, final Object value, final Document document, final LuceneOptions luceneOptions) {

        /*
         * we get the following data here: 2010-01-08 09:13:08,685
         * [http-8080-exec-2] WARN
         * net.wohlfart.framework.search.ChangeRequestMessageBridge - set
         * called, entityName is: charmsDocument 2010-01-08 09:13:08,686
         * [http-8080-exec-2] WARN
         * net.wohlfart.framework.search.ChangeRequestMessageBridge - value is:
         * net.wohlfart.changerequest.entities.ChangeRequestMessageEntry [1]
         * hashCode: 1403671073 title: null 2010-01-08 09:13:08,686
         * [http-8080-exec-2] WARN
         * net.wohlfart.framework.search.ChangeRequestMessageBridge - document
         * is:
         * Document<stored/uncompressed,indexed<_hibernate_class:net.wohlfart
         * .changerequest.entities.ChangeRequestMessageEntry>
         * stored/uncompressed,indexed<id:1>> 2010-01-08 09:13:08,686
         * [http-8080-exec-2] WARN
         * net.wohlfart.framework.search.ChangeRequestMessageBridge -
         * luceneOptions is:
         * org.hibernate.search.engine.LuceneOptionsImpl@3617a35c
         */

        LOGGER.debug("set called, entityName is: {}", entityName);
        LOGGER.debug("  value is: {}", value);
        LOGGER.debug("  document is: {}", document);
        LOGGER.debug("  luceneOptions is: {}", luceneOptions);

        // value is the entity that is being indexed
        // document is the lucene entity to be stored in the search index
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setParameterValues(final Map parameters) {
        LOGGER.info("setParameterValues called, parameters is: {}", parameters);
    }

}
