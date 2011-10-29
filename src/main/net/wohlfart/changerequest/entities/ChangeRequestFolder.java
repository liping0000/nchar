package net.wohlfart.changerequest.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import net.wohlfart.framework.entities.CharmsFolder;

import org.hibernate.annotations.AccessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this implements a folder for documents that is attached to a process instance
 * 
 * 
 * @author Michael Wohlfart
 * 
 */

/*  @formatter:off */
@NamedQueries({ 
   
    @NamedQuery(
         name = ChangeRequestFolder.FIND_BY_PID, 
         query = "from ChangeRequestFolder where processInstanceId = :pid") 
})
/*  @formatter:on */

@Entity
@DiscriminatorValue("CRQ_F")
public class ChangeRequestFolder extends CharmsFolder implements Serializable {


    private final static Logger LOGGER                = LoggerFactory.getLogger(ChangeRequestFolder.class);

    // name in the conversation context
    public static final String  CHANGE_REQUEST_FOLDER = "changeRequestFolder";

    // name of the id in the process context
    // public static final String CHANGE_REQUEST_FOLDER_ID = "folderId";

    public static final String  FIND_BY_PID           = "ChangeRequestFolder.FIND_BY_PID";

    // id, version is on the super class

    private Long                processInstanceId;
    private String              businessKey;

    /*
     * public ChangeRequestFolder() {
     * LOGGER.debug("*** constructor called for ChangeRequestFolder: " +
     * this.hashCode()); /// Thread.dumpStack(); }
     */

    @AccessType("field")
    @Column(name = "PROC_INST_ID_")
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(final Long processInstanceId) {
        if (this.processInstanceId != null) {
            throw new IllegalArgumentException("processInstanceId can not be changed," + " was: " + this.processInstanceId + " trying to set to: "
                    + processInstanceId);
        }
        this.processInstanceId = processInstanceId;
    }

    // this is redundant information since the business key is also in the
    // process instance
    // stored here for convenience and in case we want to move the data to a
    // different storage for
    // archiving...
    @Column(name = "BUSINESS_KEY_", length = 250)
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(final String businessKey) {
        if (this.businessKey != null) {
            LOGGER.warn("changing business key from {} to {}, this shouldn't happen in normal operation", this.businessKey, businessKey);
        }
        this.businessKey = businessKey;
    }

}
