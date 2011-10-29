package net.wohlfart.framework.search;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * general rules for the annotations: see:
 * http://docs.jboss.org/hibernate/stable
 * /search/reference/en/html/search-mapping.html#basic-mapping
 * 
 * manual indexing: see:
 * http://lingpipe-blog.com/2008/11/05/updating-and-deleting
 * -documents-in-lucene-24-lingmed-case-study/
 * 
 * * index: - Date Fields must not be tokenized: Index.UN_TOKENIZED (no analyzer
 * pre-processing) - using an analyzer for html content: Index.TOKENIZED (use an
 * analyzer to process the property)
 * 
 * * store: - Store.YES only needed for projection, see:
 * http://docs.jboss.org/hibernate
 * /stable/search/reference/en/html/search-query.html#projections - Store.NO is
 * the default value When a property is stored, you can retrieve its original
 * value from the Lucene Document
 * 
 * @author Michael Wohlfart
 * 
 */
public abstract class AbstractWorkflowBridgeAdaptor implements FieldBridge, ParameterizedBridge {

    private final static Logger LOGGER                  = LoggerFactory.getLogger(AbstractWorkflowBridgeAdaptor.class);

    // this is a fieldname for collecting all users that ever worked on the
    // workflow to be indexed, we use this in the search implementation to deny
    // access from any non-admin users to a workflow, we use the nonchangable
    // primary key from the message entry for users in the database
    public static final String  PARTICIPATING_USER_ID   = "participatingUserId";
    // this stored the username for easy searching all workflows a certain user
    // worked on
    public static final String  PARTICIPATING_USER_NAME = "participatingUserName";

    public static final String  INITIATING_USER_ID      = "initiatingUserId";
    // this stored the username for easy searching all workflows a certain user
    // worked on
    public static final String  INITIATING_USER_NAME    = "initiatingUserName";

    public static final String  SUBMITTING_USER_ID      = "submittingUserId";
    // this stored the username for easy searching all workflows a certain user
    // worked on
    public static final String  SUBMITTING_USER_NAME    = "submittingUserName";

    public static final String  ASSIGNING_USER_ID       = "assigningUserId";
    // this stored the username for easy searching all workflows a certain user
    // worked on
    public static final String  ASSIGNING_USER_NAME     = "assigningUserName";

    public static final String  PROCESSING_USER_ID      = "processingUserId";
    // this stored the username for easy searching all workflows a certain user
    // worked on
    public static final String  PROCESSING_USER_NAME    = "processingUserName";

    // for storing message data in the index
    public static final String  MESSAGE_TEXT            = "messageText";

    // public static final String MESSAGE_PARTICIPATION_FULLNAME =
    // "messageParticipationFullname";
    // public static final String MESSAGE_PARTICIPATION_ID =
    // "messageParticipationId";

    // All implementations have to be thread-safe, but the parameters are set
    // during
    // initialization and no special care is required at this stage.
    // see:
    // http://docs.jboss.org/hibernate/stable/search/reference/en/html/search-mapping-bridge.html

    // seems like we can use fields set by setParameterValues in the set
    // method???

    /**
     * this method has to set up the lucene document Object
     */
    @Override
    abstract public void set(String entityName, Object value, Document document, LuceneOptions luceneOptions);

    /**
     * this gives us the parameters from the annotation
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void setParameterValues(final Map parameters) {
        LOGGER.info("setParameterValues called, parameters is: {}", parameters);
    }

}
