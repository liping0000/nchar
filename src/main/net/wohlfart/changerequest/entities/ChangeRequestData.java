package net.wohlfart.changerequest.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.framework.entities.CharmsWorkflowData;
import net.wohlfart.framework.search.ChangeRequestDataBridge;
import net.wohlfart.framework.search.TranslatableReferenceBridge;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;

import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.validator.Length;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/*
 * 
 * FIXME: The deprecated HTMLStripReader, HTMLStripWhitespaceTokenizerFactory
 * and HTMLStripStandardTokenizerFactory were removed. To strip HTML tags,
 * HTMLStripCharFilter should be used instead, and it works with any Tokenizer
 * of your choice. (SOLR-1657)
 * 
 * see: http://www.mail-archive.com/solr-user@lucene.apache.org/msg29506.html
 */

/*  @formatter:off */
@Indexed
@AnalyzerDef(
        name = "htmlanalyzer", 
        charFilters = @CharFilterDef(factory = HTMLStripCharFilterFactory.class), 
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), 
        filters = { @TokenFilterDef(factory = LowerCaseFilterFactory.class) })
@ClassBridge(
        name = "changeRequestData", 
        index = Index.TOKENIZED, 
        store = Store.YES, 
        impl = ChangeRequestDataBridge.class, 
        params = @Parameter(name = "init", value = "true"))  // dummy param to init hibernate session
@NamedQueries({
     
    @NamedQuery(
         name = ChangeRequestData.FIND_BY_PID, 
         query = "from ChangeRequestData where processInstanceId = :pid"),
         
    @NamedQuery(
         name = ChangeRequestData.FIND_MANAGER_NAME_BY_NAME_LIKE, 
         query = "select distinct d.managerName from ChangeRequestData d where lower(d.managerName) like concat('%', lower(:name), '%')"),
         
    @NamedQuery(
         name = ChangeRequestData.FIND_CUSTOMER_NAME_BY_NAME_LIKE, 
         query = "select distinct d.customerName from ChangeRequestData d where lower(d.customerName) like concat('%', lower(:name), '%')") 
         
})
/*  @formatter:on */
        
@Entity
@Table(name = "CHREQ_DATA")
public class ChangeRequestData extends CharmsWorkflowData implements Serializable {


    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(ChangeRequestData.class);

    // this is the name in the conversation context, used for the factory and
    // for manually removing/placing the bean into context
    public final static String   CHANGE_REQUEST_DATA             = "changeRequestData";

    // this is he name of the variable in the process context holding the
    // id of this entity
    // public final static String CHANGE_REQUEST_DATA_ID = "dataId";

    // max length of the textfields, used in tinymce charcount
    public static final int      MAX_CONTENT_LENGTH              = 2024;

    public static final String   FIND_BY_PID                     = "ChangeRequestData.FIND_BY_PID";
    public static final String   FIND_MANAGER_NAME_BY_NAME_LIKE  = "ChangeRequestData.FIND_MANAGER_NAME_BY_NAME_LIKE";
    public static final String   FIND_CUSTOMER_NAME_BY_NAME_LIKE = "ChangeRequestData.FIND_CUSTOMER_NAME_BY_NAME_LIKE";

    private String               projectIdNumber;

    private String               moduleIdNumber;
    private String               itemIdNumber;
    private String               customerName;
    private String               managerName;

    private ChangeRequestProduct changeRequestProduct;
    private ChangeRequestUnit    changeRequestUnit;
    private ChangeRequestCode    changeRequestCode;

    // the richtext components
    private String               problemDescription;
    private String               proposalDescription;
    private String               conclusionDescription;
    private String               historyDescription;

    private Boolean              goodwill;
    private String               goodwillText;

    private Boolean              standard;
    private String               standardText;

    // FIXME: default to false
    private Boolean              costA;
    private Boolean              costB;
    private Integer              costAmount;

    private Boolean              fastTrack;
    private Boolean              regularTrack;

    // ---- workflow related data

    // started the workflow (without submitting)
    private Date                 initiateDate;
    private CharmsUser           initiateUser;

    // start assigning by TQM
    private Date                 assignDate;
    private CharmsUser           assignUser;

    // start processing by PE
    private Date                 processDate;
    private CharmsUser           processUser;

    // start implementing by PE
    private Date                 implementDate;
    private CharmsUser           implementUser;

    @Length(max = 50)
    @Column(name = "PROJECT_ID_NUMBER_", length = 50)
    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getProjectIdNumber() {
        return projectIdNumber;
    }

    public void setProjectIdNumber(final String projectIdNumber) {
        this.projectIdNumber = projectIdNumber;
    }

    @Length(max = 100)
    @Column(name = "MANAGER_NAME_", length = 100)
    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(final String managerName) {
        this.managerName = managerName;
    }

    @Column(name = "INITIATE_DATE_")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public Date getInitiateDate() {
        return initiateDate;
    }

    public void setInitiateDate(final Date initiateDate) {
        this.initiateDate = initiateDate;
    }

    @ManyToOne
    @JoinColumn(name = "INITIATE_USER_ID_")
    public CharmsUser getInitiateUser() {
        return initiateUser;
    }

    public void setInitiateUser(final CharmsUser initiateUser) {
        this.initiateUser = initiateUser;
    }

    @Column(name = "ASSIGN_DATE_")
    public Date getAssignDate() {
        // clone to make sure we don't return a mutable object
        return assignDate;
    }

    public void setAssignDate(final Date assignDate) {
        this.assignDate = assignDate;
    }

    @ManyToOne()
    @JoinColumn(name = "ASSIGN_USER_ID_")
    public CharmsUser getAssignUser() {
        return assignUser;
    }

    public void setAssignUser(final CharmsUser assignUser) {
        this.assignUser = assignUser;
    }

    @Column(name = "PROCESS_DATE_")
    public Date getProcessDate() {
        return processDate;
    }

    public void setProcessDate(final Date processDate) {
        this.processDate = processDate;
    }

    @ManyToOne()
    @JoinColumn(name = "PROCESS_USER_ID_")
    public CharmsUser getProcessUser() {
        return processUser;
    }

    public void setProcessUser(final CharmsUser processUser) {
        this.processUser = processUser;
    }

    @Column(name = "IMPLEMENT_DATE_")
    public Date getImplementDate() {
        return implementDate;
    }

    public void setImplementDate(final Date implementDate) {
        this.implementDate = implementDate;
    }

    @ManyToOne()
    @JoinColumn(name = "IMPLEMENT_USER_ID_")
    public CharmsUser getImplementUser() {
        return implementUser;
    }

    public void setImplementUser(final CharmsUser implementUser) {
        this.implementUser = implementUser;
    }

    @Length(max = 255)
    @Column(name = "MODULE_ID_NUMBER_")
    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getModuleIdNumber() {
        return moduleIdNumber;
    }

    public void setModuleIdNumber(final String moduleIdNumber) {
        this.moduleIdNumber = moduleIdNumber;
    }

    @Length(max = 255)
    @Column(name = "ITEM_ID_NUMBER_")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public String getItemIdNumber() {
        return itemIdNumber;
    }

    public void setItemIdNumber(final String itemIdNumber) {
        this.itemIdNumber = itemIdNumber;
    }

    @Length(max = 255)
    @Column(name = "CUSTOMER_NAME_")
    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    @Column(name = "IS_GOODWILL_")
    @Type(type = "yes_no")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public Boolean getGoodwill() {
        return goodwill;
    }

    public void setGoodwill(final Boolean goodwill) {
        this.goodwill = goodwill;
    }

    @Length(max = 255)
    @Column(name = "GOODWILL_TEXT_")
    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getGoodwillText() {
        return goodwillText;
    }

    public void setGoodwillText(final String goodwillText) {
        this.goodwillText = goodwillText;
    }

    @Column(name = "IS_STANDARD_")
    @Type(type = "yes_no")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public Boolean getStandard() {
        return standard;
    }

    public void setStandard(final Boolean standard) {
        this.standard = standard;
    }

    @Length(max = 255)
    @Column(name = "STANDARD_TEXT_")
    @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getStandardText() {
        return standardText;
    }

    public void setStandardText(final String standardText) {
        this.standardText = standardText;
    }

    // --- foreign key fields ---

    @ManyToOne
    @JoinColumn(name = "PRODUCT_ID_")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES, bridge = @FieldBridge(impl = TranslatableReferenceBridge.class))
    public ChangeRequestProduct getChangeRequestProduct() {
        return changeRequestProduct;
    }

    public void setChangeRequestProduct(final ChangeRequestProduct changeRequestProduct) {
        this.changeRequestProduct = changeRequestProduct;
    }

    @ManyToOne
    @JoinColumn(name = "UNIT_ID_")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES, bridge = @FieldBridge(impl = TranslatableReferenceBridge.class))
    public ChangeRequestUnit getChangeRequestUnit() {
        return changeRequestUnit;
    }

    public void setChangeRequestUnit(final ChangeRequestUnit changeRequestUnit) {
        this.changeRequestUnit = changeRequestUnit;
    }

    @ManyToOne
    @JoinColumn(name = "CODE_ID_")
    @Field(index = Index.UN_TOKENIZED, store = Store.YES, bridge = @FieldBridge(impl = TranslatableReferenceBridge.class))
    public ChangeRequestCode getChangeRequestCode() {
        return changeRequestCode;
    }

    public void setChangeRequestCode(final ChangeRequestCode changeRequestCode) {
        this.changeRequestCode = changeRequestCode;
    }

    @Length(max = MAX_CONTENT_LENGTH)
    @Column(name = "PROBLEM_DESCRIPTION_", length = MAX_CONTENT_LENGTH)
    @Field(index = Index.TOKENIZED, store = Store.YES, analyzer = @Analyzer(definition = "htmlanalyzer"))
    // @TokenizerDef(factory = HTMLStripStandardTokenizerFactory.class)
    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(final String problemDescription) {
        this.problemDescription = problemDescription;
    }

    @Length(max = MAX_CONTENT_LENGTH)
    @Column(name = "PROPOSAL_DESCRIPTION_", length = MAX_CONTENT_LENGTH)
    @Field(index = Index.TOKENIZED, store = Store.YES, analyzer = @Analyzer(definition = "htmlanalyzer"))
    public String getProposalDescription() {
        return proposalDescription;
    }

    public void setProposalDescription(final String proposalDescription) {
        this.proposalDescription = proposalDescription;
    }

    @Length(max = MAX_CONTENT_LENGTH)
    @Column(name = "CONCLUSION_DESCRIPTION_", length = MAX_CONTENT_LENGTH)
    @Field(index = Index.TOKENIZED, store = Store.YES, analyzer = @Analyzer(definition = "htmlanalyzer"))
    public String getConclusionDescription() {
        return conclusionDescription;
    }

    public void setConclusionDescription(final String conclusionDescription) {
        this.conclusionDescription = conclusionDescription;
    }

    @Length(max = MAX_CONTENT_LENGTH)
    @Column(name = "HISTORY_DESCRIPTION_", length = MAX_CONTENT_LENGTH)
    @Field(index = Index.TOKENIZED, store = Store.YES, analyzer = @Analyzer(definition = "htmlanalyzer"))
    public String getHistoryDescription() {
        return historyDescription;
    }

    public void setHistoryDescription(final String historyDescription) {
        this.historyDescription = historyDescription;
    }

    @Column(name = "COST_A_")
    @Type(type = "yes_no")
    public Boolean getCostA() {
        return costA;
    }

    public void setCostA(final Boolean costA) {
        this.costA = costA;
    }

    @Column(name = "COST_B_")
    @Type(type = "yes_no")
    public Boolean getCostB() {
        return costB;
    }

    public void setCostB(final Boolean costB) {
        this.costB = costB;
    }

    @Column(name = "COST_AMOUNT_")
    public Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(final Integer costAmount) {
        this.costAmount = costAmount;
    }

    @Column(name = "REGULAR_TRACK_")
    @Type(type = "yes_no")
    public Boolean getRegularTrack() {
        return regularTrack;
    }

    public void setRegularTrack(final Boolean regularTrack) {
        this.regularTrack = regularTrack;
    }

    @Column(name = "FAST_TRACK_")
    @Type(type = "yes_no")
    public Boolean getFastTrack() {
        return fastTrack;
    }

    public void setFastTrack(final Boolean fastTrack) {
        this.fastTrack = fastTrack;
    }

    @Override
    @BypassInterceptors
    public String toString() {
        return this.getClass().getName() + " [" + getId() + "] " + " hashCode: " + hashCode() + " title: " + getTitle();
    }

}
