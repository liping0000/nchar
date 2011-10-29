package net.wohlfart.admin;

import org.jboss.seam.ScopeType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.framework.excel.WorkbookDeployer;
import net.wohlfart.jbpm4.Jbpm4Utils;
import net.wohlfart.jbpm4.JbpmSetup;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.dom4j.DocumentException;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jbpm.api.ProcessEngine;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * component to upload and install process definitions
 * 
 * @author Michael Wohlfart
 */
@Scope(ScopeType.CONVERSATION)
@Name("charmsProcessDefActionBean")
public class CharmsProcessDefActionBean extends AbstractActionBean implements Serializable {

    static final long serialVersionUID = -1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsProcessDefActionBean.class);

    // the upload event
    private transient UploadEvent uploadEvent;


    @Transactional
    public void refresh() {
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
        //final Session session = (Session) Component.getInstance("hibernateSession");

        try {
            final List<UploadItem> items = uploadEvent.getUploadItems();
            for (final UploadItem uploadItem : items) {
                LOGGER.debug("found item to upload");

                // check if we can use the getFile method, in order for this to work
                if (uploadItem.isTempFile()) {
                    final File file = uploadItem.getFile();
                    final String filename = Jbpm4Utils.extractFileNameWithSuffix(uploadItem.getFileName());
                    deployProcessData(filename, file);                   
                    facesMessages.add(Severity.INFO, "process definition data deployed");
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


    private void deployProcessData(String filename, File file) throws FileNotFoundException, IOException, DocumentException {
        // TODO Auto-generated method stub
        final FileInputStream input1 = new FileInputStream(file);
        final String processName = Jbpm4Utils.findProcessName(input1);
        input1.close();
        
        final JbpmSetup jbpmSetup = (JbpmSetup) Component.getInstance("jbpmSetup", true);
        
        final FileInputStream input2 = new FileInputStream(file);
        jbpmSetup.deployInputStreams(input2, null, processName, " manual deployment " + new Date());
        input2.close();     
    }

}
