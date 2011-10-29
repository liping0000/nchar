package net.wohlfart.admin;

import org.jboss.seam.ScopeType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.framework.excel.WorkbookDeployer;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class to import/export the reference/business data, and provide some
 * statistics, note that there are some problems with the file upload component
 * one of them seems to be not having the proper view id in the html component,
 * check the com.sun.faces.enableViewStateIdRendering property in web.xml
 * 
 * the export feature still has to be implemented
 * 
 * @author Michael Wohlfart
 */
@Scope(ScopeType.CONVERSATION)
@Name("charmsDataStoreActionBean")
public class CharmsDataStoreActionBean extends AbstractActionBean implements Serializable {

    static final long serialVersionUID = -1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsDataStoreActionBean.class);

    // the upload event
    private transient UploadEvent uploadEvent;

    // scalar data for the UI
    private Long unitCount;
    private Long codeCount;
    private Long productCount;
    private Long userCount;

    // refresh to get some statistics about the reference data, this method is called
    // as page action or after an upload, make sure it is not called on postback
    @Transactional
    public void refresh() {
        final Session session = (Session) Component.getInstance("hibernateSession");
        productCount = (Long) session.getNamedQuery(ChangeRequestProduct.COUNT_QUERY).uniqueResult();
        unitCount = (Long) session.getNamedQuery(ChangeRequestUnit.COUNT_QUERY).uniqueResult();
        codeCount = (Long) session.getNamedQuery(ChangeRequestCode.COUNT_QUERY).uniqueResult();
        userCount = (Long) session.getNamedQuery(CharmsUser.COUNT_QUERY).uniqueResult();
    }

    @BypassInterceptors
    public Long getProductCount() {
        return productCount;
    }

    @BypassInterceptors
    public Long getUnitCount() {
        return unitCount;
    }

    @BypassInterceptors
    public Long getCodeCount() {
        return codeCount;
    }

    @BypassInterceptors
    public Long getUserCount() {
        return userCount;
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
        LOGGER.debug("uploadEvent is : {}", uploadEvent);
        this.uploadEvent = uploadEvent;
    }

    /**
     * this is the callback from the browser after the upload finished on the
     * client side, we use this to set a status message
     */
    @Transactional
    public void uploadComplete() {

        if (uploadEvent == null) {
            LOGGER.debug("uploadEvent is null");
            return;
        }

        LOGGER.info("uploading data ...");
        final FacesMessages facesMessages = FacesMessages.instance();
        final Session session = (Session) Component.getInstance("hibernateSession");

        try {
            final List<UploadItem> items = uploadEvent.getUploadItems();
            for (final UploadItem uploadItem : items) {
                LOGGER.debug("found item to upload");

                // check if we can use the getFile method, in order for this to work
                if (uploadItem.isTempFile()) {
                    final File file = uploadItem.getFile();
                    new WorkbookDeployer().deployData(new HSSFWorkbook(new FileInputStream(file)), session);
                    facesMessages.add(Severity.INFO, "reference data deployed");
                    final boolean deleted = file.delete();
                    if (!deleted) {
                        LOGGER.warn("can't delete file {}", file);
                    }
                } else {
                    LOGGER.warn("no temp file, can't upload file");
                    facesMessages.add(Severity.FATAL, "there is no tempfile, check web.xml for the tempfile setting for uploads");
                }
            }
        } catch (final FileNotFoundException ex) {
            facesMessages.add(Severity.FATAL, "File hasn't been found on the server");
            LOGGER.error("problem while uploading file: {}", ex);
        } catch (final IOException ex) {
            facesMessages.add(Severity.FATAL, "IOException while uploading the file");
            LOGGER.error("problem while uploading file: {}", ex);
        } catch (final OfficeXmlFileException ex) {
            facesMessages.add(Severity.FATAL, "Format Error uploading file");
            LOGGER.error("problem while uploading file: {}", ex);
        } catch (final Throwable th) {
            facesMessages.add(Severity.FATAL, "error uploading file");
            LOGGER.error("problem while uploading file: {}", th);
        } finally {
            uploadEvent = null;
        }
        refresh();
        LOGGER.info("...finished uploading data");
    }

}
