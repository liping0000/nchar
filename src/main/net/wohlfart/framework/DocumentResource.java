package net.wohlfart.framework;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import net.wohlfart.changerequest.entities.ChangeRequestFolder;
import net.wohlfart.framework.entities.CharmsDocument;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// see:
// http://seamframework.org/Community/DirectPDFServing
// http://relation.to/Bloggers/FileDownloadSupport

/**
 * front end to serving a resource to the user, this is backed up by the
 * AttachmentFile class, the only propose of this class is to lookup the
 * AttachmentFile in the Folder array and delete tempfiles created by the
 * AttachmentFile's getData() method
 * 
 * 
 * TODO: extend this component to server all kind of documents also by id
 * 
 */
@Name("documentResource")
@Scope(ScopeType.EVENT)
// seems like logger doesn't work in event context
public class DocumentResource {

    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentResource.class);

    @In(create = false)
    // get this only if it is available in the conversation anyways...
    private ChangeRequestFolder changeRequestFolder;

    @In(value = "hibernateSession")
    private Session             hibernateSession;

    @RequestParameter
    private Integer             rowKey;

    @RequestParameter
    private Long                documentId;                                              // alternative
                                                                                          // to
                                                                                          // rowKey
                                                                                          // which
                                                                                          // is
                                                                                          // relative
                                                                                          // to
                                                                                          // a
                                                                                          // folder

    @RequestParameter
    private String              name;                                                    // not
                                                                                          // yet
                                                                                          // used

    // the backing data, filename etc. are fetched on demand
    private CharmsDocument      charmsDocument;

    // this method is called in the attachmentResource.page.xml definition
    // during the first rendering
    @Transactional
    public void findResource() {
        LOGGER.debug("changeRequestFolder: " + changeRequestFolder);
        LOGGER.debug("rowKey: " + rowKey);
        LOGGER.debug("name: " + name);
        LOGGER.debug("documentId: " + documentId);

        // are we called from the changeRequest workflow with a open folder
        // active
        if (rowKey != null) {
            // handle changerequest with a rowKey
            final List<CharmsDocument> charmsDocuments = changeRequestFolder.getDocuments();
            if ((rowKey >= 0) && (rowKey < changeRequestFolder.getFileCount())) {
                charmsDocument = charmsDocuments.get(rowKey);
            } else {
                LOGGER.warn("rowKey: " + rowKey + " out of array range: " + changeRequestFolder.getFileCount() + " returning empty document");
                charmsDocument = new CharmsDocument();
            }

            // otherwise this may be a call with the document id only from the
            // repository page
        } else if (documentId != null) {
            charmsDocument = (CharmsDocument) hibernateSession.get(CharmsDocument.class, documentId);
        }

        // FIXME: check permissions, etc

    }

    @Create
    public void createComponent() {
        LOGGER.debug("createComponent called");
    }

    @Destroy
    public void destroyComponent() {
        LOGGER.debug("destroyComponent called");
        charmsDocument = null;
    }

    @BypassInterceptors
    public String getFileName() {
        return charmsDocument.getName();
    }

    @BypassInterceptors
    public String getContentType() {
        return charmsDocument.getMimeType();
    }

    @Transactional
    public byte[] getData() {
        // we have to convert the blob to a byte stream since the
        // backend database doesn't allow too much fiddling around
        // with an already opened blob (esp. derby)
        // so just to make sure the blob is opened exactly once
        // we buffer the data in a byte array here

        LOGGER.debug("getData called on DocumentResource");
        // check if this document is still in the filesystem
        // and not yet uploaded to the DB
        final File file = charmsDocument.getFile();
        if (file != null) {
            FileInputStream stream = null;
            try {
                LOGGER.debug("reading file stream");
                stream = new FileInputStream(file);
                final long length = file.length();
                final byte[] bytes = new byte[(int) length];
                int offset = 0;
                int numRead = 0;
                while ((offset < bytes.length) && ((numRead = stream.read(bytes, offset, bytes.length - offset)) >= 0)) {
                    offset += numRead;
                }
                return bytes;
            } catch (final FileNotFoundException ex) {
                ex.printStackTrace();
                return null;
            } catch (final IOException ex) {
                ex.printStackTrace();
                return null;
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (final IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            try {
                LOGGER.debug("reading blob stream");
                // need to refresh because there are some issues with derby and
                // filestreams
                // for blob objects
                hibernateSession.refresh(charmsDocument);

                final BufferedInputStream stream = new BufferedInputStream(charmsDocument.getContentStream());
                final long length = charmsDocument.getSize();
                final byte[] bytes = new byte[(int) length];
                int offset = 0;
                int numRead = 0;
                while ((offset < bytes.length) && ((numRead = stream.read(bytes, offset, bytes.length - offset)) >= 0)) {
                    offset += numRead;
                }
                stream.close();
                return bytes;
            } catch (final SQLException ex) {
                ex.printStackTrace();
                return null;
            } catch (final IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

}
