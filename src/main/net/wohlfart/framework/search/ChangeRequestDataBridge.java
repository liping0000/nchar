package net.wohlfart.framework.search;

import java.io.StringReader;
import java.util.Map;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
import net.wohlfart.changerequest.entities.Priority;
import net.wohlfart.framework.entities.CharmsDocument;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.hibernate.search.bridge.LuceneOptions;
import org.jboss.seam.core.Expressions;
import org.jboss.seam.core.Expressions.ValueExpression;
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
public class ChangeRequestDataBridge extends AbstractWorkflowBridgeAdaptor {

    private final static Logger      LOGGER = LoggerFactory.getLogger(ChangeRequestDataBridge.class);

    // for dynamically resolving the hibernate session without bijection
    private ValueExpression<Session> hibernateSession;

    @Override
    @SuppressWarnings("rawtypes")
    public void setParameterValues(final Map parameters) {
        super.setParameterValues(parameters);
        LOGGER.info("setParameterValues called, parameters is: {}", parameters);
        initHibernateSession();
    }

    protected void initHibernateSession() {
        // EL expression for runtime evaluation
        hibernateSession = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class);
    }

    protected Session lookupHibernateSession() {
        return hibernateSession.getValue();
    }

    // we index all fields manually

    // see:
    // http://blog.pfa-labs.com/2009/03/building-custom-entity-bridge-with.html
    // for indexing with a Class Bridge...

    @Override
    public void set(final String entityName, final Object value, final Document document, final LuceneOptions luceneOptions) {

        // FIXME: add the default ITranslatable strings for the referenced
        // product/unit/code ...

        LOGGER.debug("set called, indexing a workflow, entityName is: {}", entityName);
        LOGGER.debug("  value is: {}", value);
        LOGGER.debug("  document is: {}", document);
        LOGGER.debug("  luceneOptions is: {}", luceneOptions);

        // value is the entity to be stored, make sure we have the right type
        if ((value == null) || (!(value instanceof ChangeRequestData))) {
            LOGGER.warn("wrong document type for indexing, is {}, should be ChangeRequestData", value != null ? value.getClass() : "null",
                    CharmsDocument.class.getName());
            return;
        }

        final ChangeRequestData changeRequestData = (ChangeRequestData) value;
        // should be this objects class...
        // Field field = document.getField(ProjectionConstants.OBJECT_CLASS);

        indexMessageTree(changeRequestData.getProcessInstanceId(), document);

        final Priority priority = changeRequestData.getPriority();
        if (priority != null) {
            final Field field = new Field("priority", // fieldname
                    priority.ordinal() + "", // value
                    Store.YES, // store in the lucene index and can be retreaved
                    Index.ANALYZED, // tokenizes the value (firstname/lastname)
                    TermVector.NO); // doesn't store the position of the term in
            // the document
            document.add(field);
        }

        final CharmsUser initiateUser = changeRequestData.getInitiateUser();
        addInitiatingUser(initiateUser, document);

        final CharmsUser assignUser = changeRequestData.getAssignUser();
        addAssigningUser(assignUser, document);

        final CharmsUser submitUser = changeRequestData.getSubmitUser();
        addSubmittingUser(submitUser, document);

        final CharmsUser processUser = changeRequestData.getProcessUser();
        addProcessingUser(processUser, document);

    }

    // ---- private helpers ----------

    private void addInitiatingUser(final CharmsUser user, final Document document) {

        addUserToFields(user, document, INITIATING_USER_NAME, INITIATING_USER_ID);
    }

    private void addSubmittingUser(final CharmsUser user, final Document document) {

        addUserToFields(user, document, SUBMITTING_USER_NAME, SUBMITTING_USER_ID);
    }

    private void addAssigningUser(final CharmsUser user, final Document document) {

        addUserToFields(user, document, ASSIGNING_USER_NAME, ASSIGNING_USER_ID);
    }

    private void addProcessingUser(final CharmsUser user, final Document document) {

        addUserToFields(user, document, PROCESSING_USER_NAME, PROCESSING_USER_ID);
    }

    /*
     * private void addRealizingUser( CharmsUser user, Document document) {
     * 
     * addUserToFields(user, document, REALIZING_USER_NAME, REALIZING_USER_ID);
     * }
     */
    private void addUserToFields(final CharmsUser user, final Document document, final String userNameField, final String userIdField) {

        if (user != null) {

            final String userFullname = user.getLabel();
            if (!StringUtils.isEmpty(userFullname)) {
                final Field field = new Field(userNameField, // fieldname
                        userFullname, // value
                        Store.YES, // store in the lucene index and can be
                        // retreaved
                        Index.ANALYZED, // tokenizes the value
                        // (firstname/lastname)
                        TermVector.NO); // doesn't store the position of the
                // term in the document
                document.add(field);
            }

            final Long userId = user.getId();
            if (!(userId == null)) {
                final Field field = new Field(userIdField, // fieldname
                        userId.toString(), Store.YES, Index.NOT_ANALYZED, // no
                        // reason
                        // to
                        // tokenize
                        // this
                        // id
                        TermVector.NO);
                document.add(field);
            }
        }
    }

    /**
     * this method add data from the messages to the index
     * 
     * @param pid
     * @param document
     */
    private void indexMessageTree(final Long pid, final Document document) {
        if (pid == null) {
            LOGGER.warn("pid is null");
            return;
        }

        try {
            final ChangeRequestMessageEntry entry = (ChangeRequestMessageEntry) lookupHibernateSession()
            .getNamedQuery(ChangeRequestMessageEntry.FIND_ROOT_BY_PID)
            .setParameter("pid", pid)
            .uniqueResult();
            
            indexEntry(entry, document);
        } catch (NonUniqueResultException ex) {
            // our message tree is corrupt
            LOGGER.warn("non unique message root found for pid {} this means the message tree is corrupted", pid);
        }

        
    }

    /**
     * recursive method to add data from the message tree, note that the message
     * consists of html data so we need a
     * HTMLStripStandardTokenizerFactory.class
     * 
     * @param entry
     * @param document
     */
    private void indexEntry(final ChangeRequestMessageEntry entry, final Document document) {
        if (entry == null) {
            return;
        }

        for (final ChangeRequestMessageEntry subEntry : entry.getChildren()) {
            indexEntry(subEntry, document);
        }

        final HTMLStripCharFilterFactory fab = new HTMLStripCharFilterFactory();

        // indexing the content of the message
        final String content = entry.getContent();
        if (!StringUtils.isEmpty(content)) {
            // this is the content of a message / entry note that the entry is
            // of content html
            // and the content itself is not stored, it is just used for
            // indexing...
            final Field field = new Field(MESSAGE_TEXT, fab.create(CharReader.get(new StringReader(content))));
            document.add(field);
        }

        // indexing the authors name of the message
        final String authorFullname = entry.getAuthorFullname();
        if (!StringUtils.isEmpty(authorFullname)) {
            final Field field = new Field(PARTICIPATING_USER_NAME, // fieldname
                    authorFullname, // value
                    Store.YES, // stored in the lucene index and can be
                    // retrieved
                    Index.ANALYZED, // tokenizes the value (e.g. split
                    // firstname/lastname)
                    TermVector.NO); // doesn't store the position of the term in
            // the document
            document.add(field);
        }

        // indexing the receivers name of the message
        final String receiverFullname = entry.getReceiverFullname();
        if (!StringUtils.isEmpty(receiverFullname)) {
            final Field field = new Field(PARTICIPATING_USER_NAME, // fieldname
                    receiverFullname, // value
                    Store.YES, // stored in the lucene index and can be
                    // retrieved
                    Index.ANALYZED, // tokenizes the value (e.g. split
                    // firstname/lastname)
                    TermVector.NO); // doesn't store the position of the term in
            // the document
            document.add(field);
        }

        // indexing the authors id of the message
        final Long authorId = entry.getAuthorId();
        if (!(authorId == null)) {
            final Field field = new Field(PARTICIPATING_USER_ID, // fieldname
                    authorId.toString(), // value
                    Store.YES, // stored in the lucene index and can be
                    // retrieved
                    Index.NOT_ANALYZED, // no analyzer for id values
                    TermVector.NO); // doesn't store the position of the term in
            // the document
            document.add(field);
        } else {
            if (!StringUtils.isEmpty(authorFullname)) {
                LOGGER.warn("author fullname is not null but author id is in message for: {}" + ", message type is {} business key is {}", new Object[] {
                        authorFullname, entry.getType(), entry.getBusinessKey() });
            }
        }

        // indexing the receivers id of the message
        final Long receiverId = entry.getReceiverId();
        if (!(receiverId == null)) {
            final Field field = new Field(PARTICIPATING_USER_ID, // fieldname
                    receiverId.toString(), // value
                    Store.YES, // stored in the lucene index and can be
                    // retrieved
                    Index.NOT_ANALYZED, // no analyzer for id values
                    TermVector.NO); // doesn't store the position of the term in
            // the document
            document.add(field);
        } else {
            if (!StringUtils.isEmpty(receiverFullname)) {
                LOGGER.warn("receiver fullname is not null but receiver id is null!,  in message for: {}" + ", message type is {} business key is {}",
                        new Object[] { receiverFullname, entry.getType(), entry.getBusinessKey() });
            }
        }

    }

}
