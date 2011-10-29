package net.wohlfart.changerequest.entities;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*  @formatter:off */
@NamedQueries({ 
    @NamedQuery(
         name = ChangeRequestImpactSheet.FIND_BY_PID, 
         query = "from ChangeRequestImpactSheet where processInstanceId = :pid") 
})
/*  @formatter:on */

@Entity
@Table(name = "CHREQ_IMPACTSHEET")
public class ChangeRequestImpactSheet extends AbstractSheet implements Serializable {


    private final static Logger LOGGER                        = LoggerFactory.getLogger(ChangeRequestImpactSheet.class);

    // max length of the content
    public static final int     MAX_CONTENT_LENGTH            = 2024;
    public static final int     MAX_NAME_LENGTH               = 250;
    public static final int     MAX_COMMENT_LENGTH            = 512;

    // this is the name in the conversation context, used for the factory and
    // for manually removing/placing the bean into context
    public final static String  CHANGE_REQUEST_IMPACTSHEET    = "changeRequestImpactSheet";

    // this is he name of the variable in the process context holding the
    // id of this entity
    public final static String  CHANGE_REQUEST_IMPACTSHEET_ID = "impactsheetId";

    public static final String  FIND_BY_PID                   = "ChangeRequestImpactSheet.FIND_BY_PID";

    private Long                id;
    private Integer             version;

    // the process id is not know when we persist the databean the first time
    // this is the root process instance/execution, subexecutions may have
    // different ids
    // this is set at startup time and never changed afterwards
    private Long                processInstanceId;
    private String              businessKey;

    private String              name;
    // the richtext components
    private String              comment;
    private Long                size;                                                                                   // size
                                                                                                                         // in
                                                                                                                         // bytes
    private String              content;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHREQ_IMPACTSHEET") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Version
    @Column(name = "VERSION_")
    public Integer getVersion() {
        return version;
    }

    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @Column(name = "NAME_", length = MAX_NAME_LENGTH)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    // this can be null on the initial save since the process instance is
    // created afterwards,
    // however it should never be null
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

    // -------------- getters and setters --------------

    @Column(name = "COMMENT_", length = MAX_COMMENT_LENGTH)
    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    // the size of the content data, doesn't change
    @Column(name = "SIZE_", nullable = false)
    public Long getSize() {
        return size;
    }

    @Override
    public void setSize(final Long size) {
        this.size = size;
    }

    /**
     * the raw data must be not null, max size must be set for derby, since
     * fallback is 255
     */
    @Lob
    // for derby, see:
    // http://opensource.atlassian.com/projects/hibernate/browse/HHH-2614
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "CONTENT_", nullable = false, length = Integer.MAX_VALUE - 1)
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(final String content) {
        this.content = content;
    }

    @Transient
    public void initContent() {
        InputStream stream = null;
        try {
            stream = this.getClass().getResourceAsStream("initialImpactSheet.html");
            initContent(stream);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
