package net.wohlfart.changerequest.entities;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/*  @formatter:off */
@NamedQueries({ 
    
    @NamedQuery(
         name = ChangeRequestFacetState.FIND_BY_TID, 
         query = "from ChangeRequestFacetState where tid = :tid"),
         
    @NamedQuery(
         name = ChangeRequestFacetState.DELETE_BY_TID, 
         query = "delete from ChangeRequestFacetState where tid = :tid") 
         
})
/*  @formatter:on */
        
// @Entity
// @Table(name = "CHREQ_FACET_DATA")
public class ChangeRequestFacetState /*
                                      * extends HibernateLongVariable extends
                                      * Variable
                                      */implements Serializable {


    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(ChangeRequestFacetState.class);

    // name in the conversation context
    public static final String CHANGE_REQUEST_FACET_STATE = "changeRequestFacetState";

    public static final String FIND_BY_TID                = "ChangeRequestFacetState.FIND_BY_TID";
    public static final String DELETE_BY_TID              = "ChangeRequestFacetState.DELETE_BY_TID";

    private Long               id;
    private Integer            version;

    // the process id is not know when we persist the databean the first time
    private Long               tid;

    private String             visibleFacet               = "hidden";
    // private String visibleFacet = "reviewFacet";

    // deny/finish facet
    private String             conclusionMessage;

    // process facet
    private Date               processDueDate;
    private Long               processUserId;
    private Long               processRoleId;
    private String             processMessage;

    // implement facet
    private Date               implementDueDate;
    private Long               implementUserId;
    private Long               implementRoleId;
    private String             implementMessage;
    private String             implementedMessage;

    // forward facet
    private Date               forwardDueDate;
    private Long               forwardUserId;
    private Long               forwardRoleId;
    private String             forwardMessage;

    // review facet
    private Date               reviewDueDate;
    private Long               reviewUserId;                                                        // assigned
                                                                                                     // user
    private Long               reviewRoleId;
    private String             reviewMessage;                                                       // review
                                                                                                     // request
    private String             reviewedMessage;                                                     // review
                                                                                                     // response

    // handle facet
    private Date               handleDueDate;
    private Long               handleUserId;                                                        // assigned
                                                                                                     // user
    private Long               handleRoleId;
    private String             handleMessage;                                                       // instruction
    private String             handledMessage;                                                      // response

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHREQ_FACET_DATA") })
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

    @Column(name = "TID_", nullable = false, unique = true)
    public Long getTid() {
        return tid;
    }

    public void setTid(final Long tid) {
        this.tid = tid;
    }

    @Column(name = "VISIBLE_FACET_", length = 250)
    public String getVisibleFacet() {
        return visibleFacet;
    }

    public void setVisibleFacet(final String visibleFacet) {
        this.visibleFacet = visibleFacet;
    }

    @Column(name = "CONCLUSION_MESSAGE_", length = 1024)
    public String getConclusionMessage() {
        return conclusionMessage;
    }

    public void setConclusionMessage(final String conclusionMessage) {
        this.conclusionMessage = conclusionMessage;
    }

    @Column(name = "FORWARD_DUE_DATE_")
    public Date getForwardDueDate() {
        return forwardDueDate;
    }

    public void setForwardDueDate(final Date forwardDueDate) {
        this.forwardDueDate = forwardDueDate;
    }

    @Column(name = "FORWARD_USER_ID_")
    public Long getForwardUserId() {
        return forwardUserId;
    }

    public void setForwardUserId(final Long forwardUserId) {
        this.forwardUserId = forwardUserId;
    }

    @Column(name = "FORWARD_ROLE_ID_")
    public Long getForwardRoleId() {
        return forwardRoleId;
    }

    public void setForwardRoleId(final Long forwardRoleId) {
        this.forwardRoleId = forwardRoleId;
    }

    @Column(name = "FORWARD_MESSAGE_", length = 1024)
    public String getForwardMessage() {
        return forwardMessage;
    }

    public void setForwardMessage(final String forwardMessage) {
        this.forwardMessage = forwardMessage;
    }

    @Column(name = "REVIEW_DUE_DATE_")
    public Date getReviewDueDate() {
        return reviewDueDate;
    }

    public void setReviewDueDate(final Date reviewDueDate) {
        this.reviewDueDate = reviewDueDate;
    }

    @Column(name = "REVIEW_USER_ID_")
    public Long getReviewUserId() {
        return reviewUserId;
    }

    public void setReviewUserId(final Long reviewUserId) {
        this.reviewUserId = reviewUserId;
    }

    @Column(name = "REVIEW_ROLE_ID_")
    public Long getReviewRoleId() {
        return reviewRoleId;
    }

    public void setReviewRoleId(final Long reviewRoleId) {
        this.reviewRoleId = reviewRoleId;
    }

    @Column(name = "REVIEW_MESSAGE_", length = 1024)
    public String getReviewMessage() {
        return reviewMessage;
    }

    public void setReviewMessage(final String reviewMessage) {
        this.reviewMessage = reviewMessage;
    }

    @Column(name = "REVIEWED_MESSAGE_", length = 1024)
    public String getReviewedMessage() {
        return reviewedMessage;
    }

    public void setReviewedMessage(final String reviewedMessage) {
        this.reviewedMessage = reviewedMessage;
    }

    @Column(name = "HANDLE_DUE_DATE_")
    public Date getHandleDueDate() {
        return handleDueDate;
    }

    public void setHandleDueDate(final Date handleDueDate) {
        this.handleDueDate = handleDueDate;
    }

    @Column(name = "HANDLE_USER_ID_")
    public Long getHandleUserId() {
        return handleUserId;
    }

    public void setHandleUserId(final Long handleUserId) {
        this.handleUserId = handleUserId;
    }

    @Column(name = "HANDLE_ROLE_ID_")
    public Long getHandleRoleId() {
        return handleRoleId;
    }

    public void setHandleRoleId(final Long handleRoleId) {
        this.handleRoleId = handleRoleId;
    }

    @Column(name = "HANDLE_MESSAGE_", length = 1024)
    public String getHandleMessage() {
        return handleMessage;
    }

    public void setHandleMessage(final String handleMessage) {
        this.handleMessage = handleMessage;
    }

    @Column(name = "HANDLED_MESSAGE_", length = 1024)
    public String getHandledMessage() {
        return handledMessage;
    }

    public void setHandledMessage(final String handledMessage) {
        this.handledMessage = handledMessage;
    }

    @Column(name = "PROCESS_DUE_DATE_")
    public Date getProcessDueDate() {
        return processDueDate;
    }

    public void setProcessDueDate(final Date processDueDate) {
        this.processDueDate = processDueDate;
    }

    @Column(name = "PROCESS_USER_ID_")
    public Long getProcessUserId() {
        return processUserId;
    }

    public void setProcessUserId(final Long processUserId) {
        this.processUserId = processUserId;
    }

    @Column(name = "PROCESS_ROLE_ID_")
    public Long getProcessRoleId() {
        return processRoleId;
    }

    public void setProcessRoleId(final Long processRoleId) {
        this.processRoleId = processRoleId;
    }

    @Column(name = "PROCESS_MESSAGE_", length = 1024)
    public String getProcessMessage() {
        return processMessage;
    }

    public void setProcessMessage(final String processMessage) {
        this.processMessage = processMessage;
    }

    @Column(name = "IMPLEMENT_DUE_DATE_")
    public Date getImplementDueDate() {
        return implementDueDate;
    }

    public void setImplementDueDate(final Date implementDueDate) {
        this.implementDueDate = implementDueDate;
    }

    @Column(name = "IMPLEMENT_USER_ID_")
    public Long getImplementUserId() {
        return implementUserId;
    }

    public void setImplementUserId(final Long implementUserId) {
        this.implementUserId = implementUserId;
    }

    @Column(name = "IMPLEMENT_ROLE_ID_")
    public Long getImplementRoleId() {
        return implementRoleId;
    }

    public void setImplementRoleId(final Long implementRoleId) {
        this.implementRoleId = implementRoleId;
    }

    @Column(name = "IMPLEMENT_MESSAGE_", length = 1024)
    public String getImplementMessage() {
        return implementMessage;
    }

    public void setImplementMessage(final String implementMessage) {
        this.implementMessage = implementMessage;
    }

    @Column(name = "IMPLEMENTED_MESSAGE_", length = 1024)
    public String getImplementedMessage() {
        return implementedMessage;
    }

    public void setImplementedMessage(final String implementedMessage) {
        this.implementedMessage = implementedMessage;
    }

    // --- the request are

    @Transient
    public ChangeRequestMessageEntry createReviewRequest(final Session session, final CharmsUser sender, final CharmsUser receiver) {

        final ChangeRequestMessageEntry reviewRequest = new ChangeRequestMessageEntry();
        reviewRequest.setTimestamp(Calendar.getInstance().getTime());

        reviewRequest.setReceiver(receiver);
        reviewRequest.setAuthor(sender);

        reviewRequest.setTitle("Anforderung Gutachten");
        reviewRequest.setType(MessageType.REVIEW);
        reviewRequest.setContent(getReviewMessage());

        return reviewRequest;
    }

    @Transient
    public ChangeRequestMessageEntry createReviewedRequest(final Session session, final CharmsUser charmsUser) {
        final ChangeRequestMessageEntry reviewedRequest = new ChangeRequestMessageEntry();
        reviewedRequest.setTimestamp(Calendar.getInstance().getTime());

        reviewedRequest.setTitle("Gutachten durchgeführt");
        reviewedRequest.setType(MessageType.REVIEW_REPLY);
        reviewedRequest.setAuthor(charmsUser);

        reviewedRequest.setContent(getReviewedMessage());

        return reviewedRequest;
    }

    @Transient
    public ChangeRequestMessageEntry createHandledRequest(final Session session, final CharmsUser charmsUser) {
        final ChangeRequestMessageEntry handledRequest = new ChangeRequestMessageEntry();
        handledRequest.setTimestamp(Calendar.getInstance().getTime());

        handledRequest.setAuthor(charmsUser);

        handledRequest.setTitle("Umsetzung durchgeführt");
        handledRequest.setType(MessageType.HANDLE_REPLY);
        handledRequest.setContent(getHandledMessage());

        return handledRequest;
    }

    @Transient
    public ChangeRequestMessageEntry getAssignRequest(final Session session, final CharmsUser charmsUser) {
        final ChangeRequestMessageEntry processRequest = new ChangeRequestMessageEntry();
        processRequest.setTimestamp(Calendar.getInstance().getTime());

        final CharmsUser receiver = (CharmsUser) session
            .getNamedQuery(CharmsUser.FIND_BY_ID)
            .setParameter("id", getProcessUserId()) 
            .uniqueResult();

        processRequest.setReceiver(receiver);
        processRequest.setAuthor(charmsUser);

        processRequest.setTitle("Weiterleitung von TQM an PE");
        processRequest.setType(MessageType.ASSIGN);
        processRequest.setContent(getProcessMessage()); 

        return processRequest;
    }

    @Transient
    public ChangeRequestMessageEntry createForwardRequest(final Session session, final CharmsUser charmsUser) {
        final ChangeRequestMessageEntry forwardRequest = new ChangeRequestMessageEntry();
        forwardRequest.setTimestamp(Calendar.getInstance().getTime());

        final CharmsUser receiver = (CharmsUser) session.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", getForwardUserId()).uniqueResult();

        forwardRequest.setReceiver(receiver);
        forwardRequest.setAuthor(charmsUser);

        forwardRequest.setTitle("Weiterleitung");
        forwardRequest.setType(MessageType.FORWARD);
        forwardRequest.setContent(getForwardMessage());

        return forwardRequest;
    }

    @Transient
    public ChangeRequestMessageEntry createHandleRequest(final Session session, final CharmsUser charmsUser) {
        final ChangeRequestMessageEntry handleRequest = new ChangeRequestMessageEntry();
        handleRequest.setTimestamp(Calendar.getInstance().getTime());

        final CharmsUser receiver = (CharmsUser) session.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", getHandleUserId()).uniqueResult();

        handleRequest.setReceiver(receiver);
        handleRequest.setAuthor(charmsUser);

        handleRequest.setTitle("Beauftragung Umsetzung");
        handleRequest.setType(MessageType.HANDLE);
        handleRequest.setContent(getHandleMessage());

        return handleRequest;
    }

    public ChangeRequestMessageEntry createImplementRequest(final Session session, final CharmsUser charmsUser) {
        final ChangeRequestMessageEntry implementRequest = new ChangeRequestMessageEntry();
        implementRequest.setTimestamp(Calendar.getInstance().getTime());

        final CharmsUser receiver = (CharmsUser) session.getNamedQuery(CharmsUser.FIND_BY_ID).setParameter("id", getImplementUserId()).uniqueResult();

        implementRequest.setReceiver(receiver);
        implementRequest.setAuthor(charmsUser);

        implementRequest.setTitle("Beauftragung Umsetzung");
        implementRequest.setType(MessageType.IMPLEMENT);
        implementRequest.setContent(getImplementMessage());

        return implementRequest;
    }

    public ChangeRequestMessageEntry createImplementedRequest(final Session session, final CharmsUser charmsUser) {
        final ChangeRequestMessageEntry handledRequest = new ChangeRequestMessageEntry();
        handledRequest.setTimestamp(Calendar.getInstance().getTime());

        handledRequest.setAuthor(charmsUser);

        handledRequest.setTitle("Umsetzung durchgeführt");
        handledRequest.setType(MessageType.IMPLEMENT_REPLY);
        handledRequest.setContent(getImplementedMessage());

        return handledRequest;
    }

    /*
     * @Override protected Object getObject() { // TODO Auto-generated method
     * stub return null; }
     * 
     * @Override public boolean isStorable(Object arg0) { // TODO Auto-generated
     * method stub return true; }
     * 
     * @Override protected void setObject(Object arg0) { // TODO Auto-generated
     * method stub }
     */
}
