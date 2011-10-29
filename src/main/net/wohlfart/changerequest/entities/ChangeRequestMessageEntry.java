package net.wohlfart.changerequest.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Parameter;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.hibernate.validator.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*  @formatter:off */

/*   this analyzer is defined in ChangeRequestData
@AnalyzerDef(
      name = "htmlanalyzer", 
      charFilters = @CharFilterDef(
              factory = HTMLStripCharFilterFactory.class), 
              tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), 
              filters = { @TokenFilterDef(factory = LowerCaseFilterFactory.class) 
})
*/
@NamedQueries({
    
     @NamedQuery(
          name = ChangeRequestMessageEntry.FIND_CURRENT_BY_EID, // find by execution id
          query = "from ChangeRequestMessageEntry where processInstanceId = :eid"), 
                                                                                                                                           
     @NamedQuery(
          name = ChangeRequestMessageEntry.FIND_ROOT_BY_PID, // root has no parent
          query = "from ChangeRequestMessageEntry where processInstanceId = :pid and parent is null") // root
})
/*  @formatter:on */

@Entity
@Table(name = "CHREQ_MSG_ENTRY")
public class ChangeRequestMessageEntry implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChangeRequestMessageEntry.class);

    public static final int MAX_CONTENT_LENGTH = 2024;
    public static final int MAX_TITLE_LENGTH = 250;

    // this is the name in the conversation context, used for the factory and
    // for manually removing/placing the bean into context
    public final static String CHANGE_REQUEST_MESSAGE_TREE = "changeRequestMessageTree";
    // this is he name of the variable in the process context holding the
    // id of this entity
    public final static String CHANGE_REQUEST_MESSAGE_TREE_ID = "messageTreeId";

    public static final String CHANGE_REQUEST_CURRENT_MESSAGE    = "messageEntry";
    public static final String CHANGE_REQUEST_CURRENT_MESSAGE_ID = "currentMessageId";

    public static final int MAX_FULLNAME_LENGTH  = CharmsUser.MAX_FIRSTNAME_LENGTH + CharmsUser.MAX_LASTNAME_LENGTH + 1;

    public static final String FIND_CURRENT_BY_EID = "ChangeRequestMessageEntry.FIND_CURRENT_BY_EID";
    public static final String FIND_ROOT_BY_PID = "ChangeRequestMessageEntry.FIND_ROOT_BY_PID";

    
    private Long id;
    private Integer version;

    private String title;
    private String content;
    private MessageType type;

    // the process id is not know when we persist the databean the first time
    // this is the root process instance/execution, subexecutions may have
    // different ids
    // this is set at startup time and never changed afterwards
    private Long processInstanceId;
    private String businessKey;

    private String authorFullname;
    private Long authorId;

    private String receiverFullname;
    private Long receiverId;

    private String receiverGroupname;
    private Long receiverGroupId;

    private Date timestamp;

    // private ChangeRequestDataBean data;

    private List<ChangeRequestMessageEntry> children = new ArrayList<ChangeRequestMessageEntry>();
    private ChangeRequestMessageEntry parent;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(
            name = "sequenceGenerator", 
            strategy = "org.hibernate.id.enhanced.TableGenerator", 
            parameters = { 
                    @Parameter(name = "segment_value", value = "CHREQ_MSG_ENTRY") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }
    @SuppressWarnings("unused")
    private void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    @Version
    @Column(name = "VERSION_")
    private Integer getVersion() {
        return version;
    }

    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    // this can be null on the initial save since the process instance is
    // created afterwards,
    // however it should never be null after persist and it should never
    // change...
    // @OneToOne
    // @JoinColumn(name="PROC_INST_ID_")
    // FIXME: add updateable=false here
    @AccessType("field")
    @Column(name = "PROC_INST_ID_")
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(final Long processInstanceId) {
        if (this.processInstanceId != null) {
            throw new IllegalArgumentException("processInstanceId can not be changed," 
                    + " was: " + this.processInstanceId 
                    + " trying to set to: " + processInstanceId + "keeping old id");
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

    @Enumerated(EnumType.STRING)
    @Column(name = "MSG_TYPE_", nullable = true)
    public MessageType getType() {
        return type;
    }
    public void setType(final MessageType type) {
        this.type = type;
    }

    @Length(max = MAX_TITLE_LENGTH)
    @Column(name = "TITLE_", length = MAX_TITLE_LENGTH)
    public String getTitle() {
        return title;
    }
    public void setTitle(final String title) {
        this.title = title;
    }

    @Length(max = MAX_CONTENT_LENGTH)
    @Column(name = "CONTENT_", length = MAX_CONTENT_LENGTH)
    @Field(index = Index.TOKENIZED, store = Store.YES,analyzer=@Analyzer(definition="htmlanalyzer"))
    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    @Transient
    public void setAuthor(final CharmsUser author) {
        setAuthorFullname(author.getLabel());
        setAuthorId(author.getId());
    }

    @Column(name = "AUTHOR_FULLNAME_", length = MAX_FULLNAME_LENGTH)
    // @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public String getAuthorFullname() {
        return authorFullname;
    }

    private void setAuthorFullname(final String authorFullname) {
        this.authorFullname = authorFullname;
    }

    @Column(name = "AUTHOR_ID_")
    public Long getAuthorId() {
        return authorId;
    }

    private void setAuthorId(final Long authorId) {
        this.authorId = authorId;
    }

    @Transient
    public void setReceiver(final CharmsUser receiver) {
        setReceiverFullname(receiver.getLabel());
        setReceiverId(receiver.getId());
    }

    @Transient
    public void setReceiverGroup(final CharmsRole receiver) {
        setReceiverGroupname(receiver.getLabel());
        setReceiverGroupId(receiver.getId());
    }

    @Column(name = "RECEIVER_GRPNAME_", length = MAX_FULLNAME_LENGTH)
    // @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public String getReceiverGroupname() {
        return receiverGroupname;
    }
    private void setReceiverGroupname(final String receiverGroupname) {
        this.receiverGroupname = receiverGroupname;
    }

    @Column(name = "RECEIVER_FULLNAME_", length = MAX_FULLNAME_LENGTH)
    // @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public String getReceiverFullname() {
        return receiverFullname;
    }
    private void setReceiverFullname(final String receiverFullname) {
        this.receiverFullname = receiverFullname;
    }

    @Column(name = "RECEIVER_ID_")
    public Long getReceiverId() {
        return receiverId;
    }
    private void setReceiverId(final Long receiverId) {
        this.receiverId = receiverId;
    }

    @Column(name = "RECEIVER_GRP_ID_")
    public Long getReceiverGroupId() {
        return receiverGroupId;
    }
    private void setReceiverGroupId(final Long receiverGroupId) {
        this.receiverGroupId = receiverGroupId;
    }

    @Column(name = "TIMESTAMP_")
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    // see: https://forum.hibernate.org/viewtopic.php?f=9&t=948004
    // for the magic about the mapping here
    @OneToMany(cascade = CascadeType.ALL)
    // (mappedBy = "parent")
    @JoinColumn(name = "PARENT_ID_")
    @IndexColumn(name = "IDX_", base = 1)
    public List<ChangeRequestMessageEntry> getChildren() {
        return children;
    }

    public void setChildren(final List<ChangeRequestMessageEntry> children) {
        this.children = children;
    }

    @ManyToOne
    // we don't change the parent
    @JoinColumn(name = "PARENT_ID_", insertable = false, updatable = false)
    public ChangeRequestMessageEntry getParent() {
        return parent;
    }

    public void setParent(final ChangeRequestMessageEntry parent) {
        this.parent = parent;
    }

    // convenience method to add a child entry
    @Transient
    // not really needed
    public void addChild(final ChangeRequestMessageEntry child) {
        children.add(child);
        child.setParent(this);
        child.setProcessInstanceId(getProcessInstanceId());
    }

    @Override
    @Transient
    // not really needed
    public String toString() {
        return this.getClass().getName() + " [" + getId() + "] " + " hashCode: " + hashCode() + " title: " + getTitle();
    }
    
    @Transient
    public int getDiscriminator() {
        // this is a hack to get i18n working, return 1 if the target of this messgae is a user, return 2 if the target is a group
        if ((receiverId != null) && (receiverGroupId != null)) {
            LOGGER.warn("we have a message with user and group receiver, using none in the UI");
            return 0;
        }
        
        if ((receiverId == null) && (receiverGroupId == null)) {
            LOGGER.debug("we have a message without user and group receiver, using none in the UI");
            return 0;
        }
        
        if (this.receiverId != null) {
            return 1;
        }
        
        if (this.receiverGroupId != null) {
            return 2;
        }
              
        return 0;
    }

}
