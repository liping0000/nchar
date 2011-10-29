package net.wohlfart.admin;

import org.jboss.seam.ScopeType;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;

import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.CustomResourceLoader;
import net.wohlfart.framework.sort.SortableMoves;
import net.wohlfart.report.entities.CharmsReport;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.framework.EntityNotFoundException;
import org.jboss.seam.international.StatusMessage.Severity;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("charmsReportActionBean")
public class CharmsReportActionBean extends TranslateableHome<CharmsReport> {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsReportActionBean.class);

    private static final String CHARMS_REPORT = "charmsReport";

    private UploadEvent uploadEvent;

    @Override
    protected String getNameInContext() {
        return CHARMS_REPORT;
    }

    @Override
    public String getBundleName() {
        return CustomResourceLoader.CHARMS_REPORT_BUNDLE_NAME;
    }

    public String setReportId(final String s) {
        LOGGER.debug("setting id called: >{}< old id is: >{}<", s, getId());
        try {
            final Long id = new Long(StringUtils.defaultIfEmpty(s, "0"));
            if (!id.equals(getId() == null ? 0L : getId())) {
                Contexts.getConversationContext().remove(CHARMS_REPORT);
                clearInstance();
                if (id > 0) {
                    setId(id);
                }
                initInstance();
            }
            return "valid";
        } catch (final EntityNotFoundException e) {
            LOGGER.info("EntityNotFoundException");
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "entity not found");
            return "invalid";
        } catch (final NumberFormatException e) {
            LOGGER.info("NumberFormatException");
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "entity not found");
            return "invalid";
        }
    }

    // (Long id) {
    // setId(id);
    // }
    //
    // public Long getReportId() {
    // return (Long) getId();
    // }

    @Transactional
    @Factory(value = CHARMS_REPORT)
    public CharmsReport getCharmsReport() {
        // the getInstance method is tuned and also inits the translation map...
        return super.getInstance();
    }

    @Override
    @Transactional
    public String update() {
        // the update method is tuned and also stores the translation map...
        // CharmsReport charmsReport = super.getInstance();
        // entityManager.refresh(charmsReport);
        final CharmsReport report = getInstance();
        report.setLastModified(Calendar.getInstance().getTime());
        final String result = super.update();
        final Session hibernateSession = getSession();
        hibernateSession.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "updated"  FIXME: check for nested conversation here!
    }

    @Override
    @Transactional
    public String persist() {
        final CharmsReport report = getInstance();

        final FacesMessages facesMessages = FacesMessages.instance();
        final Session hibernateSession = getSession();

        // check if the name is already used
        final int found = hibernateSession.getNamedQuery(CharmsReport.FIND_BY_DEFAULT_NAME).setParameter("defaultName", getInstance().getDefaultName()).list()
                .size();

        if (found != 0) {
            facesMessages.add(Severity.WARN, "Name is already taken");
            return "invalid";
        }

        // check if we have a file attached
        if ((report.getSize() == null) || (report.getSize() <= 0)) {
            facesMessages.add(Severity.FATAL, "File missing");
            return "invalid";
        }

        report.setLastModified(Calendar.getInstance().getTime());
        report.setupSortIndex(hibernateSession);
        final String result = super.persist();
        hibernateSession.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "persisted" FIXME: check for nested conversation here!
    }

    @Override
    @Transactional
    public String remove() {
        final FacesMessages facesMessages = FacesMessages.instance();
        if (getId() == null) {
            facesMessages.add(Severity.FATAL, "No entity to delete, id is null");
            return "error";
        }
        final String result = super.remove();
        final Session hibernateSession = getSession();
        hibernateSession.flush();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result; // returns "removed"
    }

    @Override
    public String cancel() {
        final String result = super.cancel();
        final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
        if ((conversation != null) && (conversation.isNested())) {
            LOGGER.debug("endAndRedirect for nested conversation");
            conversation.endAndRedirect();
        }
        return result;
    }

    @Transactional
    public String sortUp() {
        final FacesMessages facesMessages = FacesMessages.instance();
        final Session hibernateSession = getSession();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortUp");
        SortableMoves.moveUp(CharmsReport.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

    @Transactional
    public String sortDown() {
        final FacesMessages facesMessages = FacesMessages.instance();
        final Session hibernateSession = getSession();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortDown");
        SortableMoves.moveDown(CharmsReport.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

    // ---------------- for uploading -------------------

    /**
     * this method is called when a user uploads a file, for whatever reason
     * this method can not update the facesMessages, probably related to the JSF
     * lifecycle so we just store the event in this component and wait for the
     * upload completed callback from the client to set the facesMessage and
     * install the file into the database
     */
    @BypassInterceptors
    public void uploadListener(final UploadEvent uploadEvent) {
        this.uploadEvent = uploadEvent;
    }

    // this is the callback from the browser after the upload finished on the
    // client side
    // we use this to set a status message
    @Transactional
    public void uploadComplete() {

        final FacesMessages facesMessages = FacesMessages.instance();
        LOGGER.debug("uploading report definition ...");

        try {
            if (uploadEvent == null) {
                LOGGER.warn("uploadEvent is null");
                facesMessages.addFromResourceBundle(Severity.WARN, "charmsReportActionBean.nullUploadEvent");
                return;
            }
            final List<UploadItem> items = uploadEvent.getUploadItems();
            // FIXME: there should just be one file here
            for (final UploadItem uploadItem : items) {
                LOGGER.debug("found item to upload");

                // FIXME: blob issues
                // https://forum.hibernate.org/viewtopic.php?f=1&t=929348&view=previous

                // check if we can use the getFile method, in order for this to
                // work we need
                if (uploadItem.isTempFile()) {
                    LOGGER.debug("uploading temp file");
                    final File file = uploadItem.getFile();
                    LOGGER.debug("uploaded file: " + file);

                    final long size = file.length();
                    if (size > 0) {
                        final CharmsReport report = getInstance();
                        // initial safe, everything is fine
                        LOGGER.error("setup a session here t call the blob creator");
                        report.setContentStream(new FileInputStream(file), file.length(), null);
                        report.setSize(size);
                        facesMessages.add(Severity.INFO, "report deployed");
                        final boolean deleted = file.delete();
                        if (!deleted) {
                            LOGGER.warn("unable to delete file {}", file);
                        }
                    } else {
                        facesMessages.add(Severity.WARN, "file is empty");
                        final boolean deleted = file.delete();
                        if (!deleted) {
                            LOGGER.warn("error deleting file {}", deleted);
                        }
                    }
                } else {
                    LOGGER.warn("no temp file, can't upload file");
                    // FIXME: i18n !
                    facesMessages.add(Severity.FATAL, "there is no tempfile, check web.xml for the tempfile setting for uploads");
                }
            }

        } catch (final Throwable th) {
            facesMessages.add(Severity.FATAL, "error uploading file");
            LOGGER.error("problem while uploading file: {}", th);
        } finally {
            uploadEvent = null;
        }
        LOGGER.debug("...finished uploading report");
    }

}
