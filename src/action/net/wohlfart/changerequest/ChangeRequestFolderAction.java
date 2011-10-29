package net.wohlfart.changerequest;

import org.jboss.seam.ScopeType;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestFolder;
import net.wohlfart.framework.entities.CharmsDocument;
import net.wohlfart.framework.mime.MimeTypeUtil;
import net.wohlfart.jbpm4.Jbpm4Utils;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("changeRequestFolderAction")
public class ChangeRequestFolderAction extends AbstractActionBean implements Serializable {

    static final long serialVersionUID = -1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(ChangeRequestFolderAction.class);

    @In(value = "changeRequestFolder")
    private ChangeRequestFolder changeRequestFolder;

    @In(value = "authenticatedUser")
    private CharmsUser charmsUser;

    // the upload event
    private transient UploadEvent uploadEvent;

    // ---------------- for uploading with primefaces ---------------

    /*
     * use this in the poage: <p:fileUpload
     * fileUploadListener="#{changeRequestFolderAction.handleFileUpload}"
     * auto="true" />
     */
    @Transactional
    @BypassInterceptors
    public void handleFileUpload(final FileUploadEvent event) {
        //System.out.println("file uploaded: " + event);
        final UploadedFile file = event.getFile();
        LOGGER.info("file uploaded: " + file);
    }

    // ---------------- for uploading with seam -------------------

    /**
     * this method is called when a user uploads a file, for whatever reason
     * this method can not update the facesMessages, probably related to the JSF
     * lifecycle so we just store the event in this component and wait for the
     * upload completed callback from the client to set the facesMessage and
     * install the file into the database
     */
    @BypassInterceptors
    public void uploadListener(final UploadEvent uploadEvent) {
        LOGGER.info("uploadEvent: " + uploadEvent);
        this.uploadEvent = uploadEvent;
        final UploadItem item = uploadEvent.getUploadItem();
        // LOGGER.debug("data: {}" + item.getData());
        LOGGER.debug("filename: {}", item.getFileName());
        LOGGER.debug("contenttype: {}", item.getContentType());
    }

    // this is the callback from the browser after the upload finished on the  client side
    // we use this to set a status message and to do the real work...
    // FIXME: check all upload event actions we want the same semantics in all of them...
    @Transactional
    public void uploadComplete() {
        LOGGER.info("uploadComplete called: ");

        // remove any error messages from the form validation which happens on  document upload
        final Iterator<FacesMessage> messages = FacesContext.getCurrentInstance().getMessages();
        while (messages.hasNext()) {
            messages.next();
            messages.remove();
        }

        final FacesMessages facesMessages = FacesMessages.instance();
        LOGGER.debug("uploading file to the document folder...");

        try {
            if (uploadEvent == null) {
                LOGGER.warn("uploadEvent is null");
                facesMessages.addFromResourceBundle(Severity.WARN, "changeRequestFolder.noUploadEventAvailable");
                return;
            }
            final List<UploadItem> items = uploadEvent.getUploadItems();
            // FIXME: there should just be one file here
            for (final UploadItem uploadItem : items) {

                // check if we can use the getFile method
                if (uploadItem.isTempFile()) {
                    LOGGER.debug("uploading temp file into folder: " + changeRequestFolder.hashCode());
                    final File file = uploadItem.getFile();
                    LOGGER.debug("uploaded file: " + file);

                    final long size = file.length();
                    if (size > 0) {
                        final String filename = Jbpm4Utils.extractFileNameWithSuffix(uploadItem.getFileName());

                        // String contentType = uploadItem.getContentType();

                        final CharmsDocument charmsDocument = new CharmsDocument();
                        // set the mimetype
                        charmsDocument.setMimeType(MimeTypeUtil.findMimeType(filename, file));

                        String fixedFilename = filename;
                        if (filename.length() > CharmsDocument.MAX_NAME_LENGTH) {
                            facesMessages.add(Severity.WARN, "filename to long");
                            charmsDocument.setComment(filename);
                            fixedFilename = StringUtils
                            // shorten the filename
                                    .abbreviate(filename, CharmsDocument.MAX_NAME_LENGTH - 7)
                            // add the file ending
                                    + StringUtils.left(filename, 7);
                        }

                        charmsDocument.setName(fixedFilename);
                        charmsDocument.setLastModified(Calendar.getInstance().getTime());

                        // set the file for now, the upload into the database
                        // via a stream
                        // is done in the Action bean for this task
                        charmsDocument.setFile(file);
                        charmsDocument.setSize(file.length());
                        charmsDocument.setFolder(changeRequestFolder);
                        charmsDocument.setEditor(charmsUser);
                        charmsDocument.setEditorName(charmsUser.getLabel());

                        // List<AttachmentFile> files =
                        // changeRequestFolder.getAttachmentFiles();
                        // files.add(attachmentFile);
                        // changeRequestFolder.setAttachmentFiles(files);
                        changeRequestFolder.getDocuments().add(charmsDocument);
                        facesMessages.addFromResourceBundle(Severity.INFO, "changeRequestFolder.fileUploaded", charmsDocument.getName());
                    } else {
                        facesMessages.add(Severity.WARN, "file is empty");
                        final boolean deleted = file.delete();
                        if (!deleted) {
                            LOGGER.warn("error deleting file {}", deleted);
                        }
                    }

                } else {
                    LOGGER.error("can't upload data, no temp file available");
                    facesMessages.addFromResourceBundle(Severity.FATAL, "changeRequestFolder.noFileAvailable");
                }
            } // end for loop
        } catch (final Throwable th) {
            facesMessages.addFromResourceBundle(Severity.FATAL, "changeRequestFolder.uploadError");
            LOGGER.error("problem while uploading file: {}", th, th.toString());
        } finally {
            uploadEvent = null;
        }
        LOGGER.debug("...finished uploading file");
    }

    // ------------------------ helpers --------


    public void delete(final String docId) {
        LOGGER.warn("FIXME: delete called for {}, this method must be implemented....", docId);
        for (final CharmsDocument doc : changeRequestFolder.getDocuments()) {
            if (doc.getId().equals(Long.parseLong(docId))) {
                // ...
            }
        }
    }

    /**
     * the faces trace API uses the toString Method to display some information
     * about the components in the UI we need to make sure Seam's Bijection
     * doesn't kick in and gives us an exception
     */
    @Override
    @BypassInterceptors
    public String toString() {
        return super.toString();
    }

}
