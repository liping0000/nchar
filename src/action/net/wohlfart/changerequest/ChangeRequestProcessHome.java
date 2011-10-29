package net.wohlfart.changerequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.changerequest.entities.ChangeRequestCostSheet;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestFacetState;
import net.wohlfart.changerequest.entities.ChangeRequestFolder;
import net.wohlfart.changerequest.entities.ChangeRequestImpactSheet;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
import net.wohlfart.framework.IllegalParameterException;
import net.wohlfart.framework.entities.CharmsDocument;
import net.wohlfart.framework.entities.CharmsFolder;
import net.wohlfart.framework.interceptor.TransitionStrategy;
import net.wohlfart.framework.queries.ProcessTaskTable;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.StringUtils;

// @TransitionException
@Scope(ScopeType.CONVERSATION)
@Name("changeRequestProcessHome")
@TransitionStrategy
// custom interceptor
public class ChangeRequestProcessHome extends AbstractActionBean implements Serializable {


    private final static Logger       LOGGER = LoggerFactory.getLogger(ChangeRequestProcessHome.class);

    // process definition id is "key" plus "version"
    // private static final String PROCESS_DEFINITION_KEY = "ChangeRequest";

    @In(value = "hibernateSession")
    private Session                   hibernateSession;

    // @In(value="authenticatedUser")
    // private CharmsUser authenticatedUser;

    // don't set this directly, use the setter for this since it switched
    // depending components
    // private String processDbid;

    // the view data:

    private ChangeRequestData         changeRequestData;
    private ChangeRequestFolder       changeRequestFolder;
    private ChangeRequestCostSheet    changeRequestCostSheet;
    private ChangeRequestImpactSheet  changeRequestImpactSheet;
    private ChangeRequestMessageEntry changeRequestMessageTree;                                        // the
                                                                                                        // root

    // private Long productId;
    // private Long errorId;
    // private Long unitId;

    // this depends on the task instance
    private ChangeRequestFacetState   changeRequestFacetState;

    // this depends on the execution instance
    private ChangeRequestMessageEntry currentMessageEntry;                                             // the
                                                                                                        // current
                                                                                                        // entry

    // table view backing bean
    private ProcessTaskTable          processTaskTable;

    /**
     * this method is called before page rendering the process is the root
     * instance of all executions
     * 
     * @param taskDbid
     * @throws IllegalParameterException
     */
    @Transactional
    public void setProcessDbid(final String processDbid) throws IllegalParameterException {
        if (StringUtils.isEmptyOrWhitespaceOnly(processDbid)) {
            LOGGER.error("invalid processDbid, is empty or whitespaceonly: >{}<", processDbid);
            throw new IllegalParameterException("Sorry, can't find the process for you");
        }
        LOGGER.debug("setting processDbid to: >{}<", processDbid);

        final Long processInstanceId = new Long(processDbid);

        // find and create the stuff for the conversation context
        changeRequestData = (ChangeRequestData) hibernateSession.getNamedQuery(ChangeRequestData.FIND_BY_PID).setParameter("pid", processInstanceId)
                .uniqueResult();
        changeRequestFolder = (ChangeRequestFolder) hibernateSession.getNamedQuery(ChangeRequestFolder.FIND_BY_PID).setParameter("pid", processInstanceId)
                .uniqueResult();
        changeRequestCostSheet = (ChangeRequestCostSheet) hibernateSession.getNamedQuery(ChangeRequestCostSheet.FIND_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();
        changeRequestImpactSheet = (ChangeRequestImpactSheet) hibernateSession.getNamedQuery(ChangeRequestImpactSheet.FIND_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();
        changeRequestMessageTree = (ChangeRequestMessageEntry) hibernateSession.getNamedQuery(ChangeRequestMessageEntry.FIND_ROOT_BY_PID)
                .setParameter("pid", processInstanceId).uniqueResult();

        // the tasktable for the current execution instance
        processTaskTable = new ProcessTaskTable();
        processTaskTable.setProcessInstanceId(new Long(processDbid));

        setupConversation();
    }

    @Transactional
    public String save() {
        LOGGER.info("storing folder data");
        storeDocuments(changeRequestFolder);
        hibernateSession.flush();
        // /wfl/changerequest/home.html
        return "/home";
    }

    /**
     * recursiv method to store the document tree
     * 
     * FIXME: in order to avoid an error when two people upload documents for
     * the same workflow within two different conversations we have to remove
     * the document form the hibernate session and manually attach all changes
     * on new checked out folder from the DB...
     * 
     * this is dublicate code!! check the abstract action..
     * 
     * @param folder
     */
    @Transactional
    private void storeDocuments(final CharmsFolder folder) {
        final FacesMessages facesMessages = FacesMessages.instance();
        LOGGER.debug("storeDocuments called, documentCount: {}", changeRequestFolder.getFileCount());

        // first iterate through the children
        final List<CharmsFolder> subfolders = folder.getChildren();
        if ((subfolders != null) && (subfolders.size() > 0)) {
            for (final CharmsFolder subfolder : subfolders) {
                storeDocuments(subfolder);
            }
        }

        // then store the documents
        final Iterator<CharmsDocument> docs = folder.getDocuments().iterator();
        // refresh the folder
        // hibernateSession.refresh(folder);

        while (docs.hasNext()) {
            final CharmsDocument document = docs.next();
            // check if we have a file that needs to be uploaded
            final File file = document.getFile();
            if (file != null) {
                LOGGER.debug("uploading a file, name is: {} MimeType is: {}", document.getName(), document.getMimeType());
                try {
                    // we have to upload that file
                    document.setContentStream(new FileInputStream(file), file.length(), hibernateSession);
                    hibernateSession.persist(document);
                    //
                    // FIXME: we can't delete the file here since the indexer
                    // needs access to it during the transaction commit
                    // this might be resolved by using byte array instead of
                    // streams here like described at
                    // https://forum.hibernate.org/viewtopic.php?p=2318509&sid=90b3ad474d908a4c0a5584a5d9970925
                    //
                    // for now we delete the file in the finalizer of the
                    // CharmsDomcument object
                    //
                    // if (file.exists()) {
                    // file.delete();
                    // }
                    // document.setFile(null);
                } catch (final FileNotFoundException ex) {
                    // this may happen when the viruschecker deletes the file
                    // let's show a nice error dialog
                    facesMessages.addFromResourceBundle(Severity.ERROR, "changeRequestFolder.fileNotFound");
                    LOGGER.warn("can't upload file (file not found) " + "removing file from folder", ex);
                    docs.remove();
                } catch (final IOException ex) {
                    // this may happen when the viruschecker deletes the file
                    // let's show a nice error dialog
                    facesMessages.addFromResourceBundle(Severity.ERROR, "changeRequestFolder.ioException");
                    LOGGER.warn("can't upload file (ioexception) " + "removing file from folder", ex);
                    docs.remove();
                }
            }
        }
    }

    private void setupConversation() {
        Contexts.getConversationContext().set(ChangeRequestData.CHANGE_REQUEST_DATA, changeRequestData);
        Contexts.getConversationContext().set(ChangeRequestFolder.CHANGE_REQUEST_FOLDER, changeRequestFolder);

        Contexts.getConversationContext().set(ChangeRequestCostSheet.CHANGE_REQUEST_COSTSHEET, changeRequestCostSheet);
        Contexts.getConversationContext().set(ChangeRequestImpactSheet.CHANGE_REQUEST_IMPACTSHEET, changeRequestImpactSheet);

        Contexts.getConversationContext().set(ProcessTaskTable.PROCESS_TASK_TABLE, processTaskTable);
        Contexts.getConversationContext().set(ChangeRequestMessageEntry.CHANGE_REQUEST_MESSAGE_TREE, changeRequestMessageTree);

        Contexts.getConversationContext().set(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE, currentMessageEntry);
        Contexts.getConversationContext().set(ChangeRequestFacetState.CHANGE_REQUEST_FACET_STATE, changeRequestFacetState);
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
