package net.wohlfart.framework.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.Priority;

import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.validator.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * this is a generic workflow data object to store data that are relevant to all
 * kind of workflows a specific wokflow has to provide a subclass to implement
 * workflow specific data
 * 
 * - title (short description) 
 * - business key 
 * - priority 
 * - initialize data 
 * - finish data
 * 
 * @author Michael Wohlfart
 * 
 */

/*  @formatter:off */
@Indexed
@AnalyzerDef(
        name = "bkeyanalyzer", 
        //charFilters = @CharFilterDef(factory = HTMLStripCharFilterFactory.class), 
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), 
        filters = { @TokenFilterDef(factory = LowerCaseFilterFactory.class) })

@NamedQueries({ 
    
    @NamedQuery(
        name = CharmsWorkflowData.FIND_BY_PID, 
        query = "from CharmsWorkflowData where processInstanceId = :pid") 
        
})
/*  @formatter:on */

@Entity
@Table(name = "CHARMS_WFL_DATA")
@Inheritance(strategy = InheritanceType.JOINED)
public class CharmsWorkflowData implements Serializable {


    private final static Logger LOGGER      = LoggerFactory.getLogger(CharmsWorkflowData.class);

    // public static final String CHARMS_WORKFLOW_DATA = "charmsWorkflowData";

    public static final String  FIND_BY_PID = "CharmsWorkflowData.FIND_BY_PID";

    private Long                processInstanceId;
    private String              businessKey;
    private String              title;
    private Priority            priority;

    // the completeness is not relevant here and stored in the tasks...

    private Date                submitDate;
    private CharmsUser          submitUser;

    private Date                finishDate;
    private CharmsUser          finishUser;

    private Long                id;
    private Integer             version;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "WKFL_DATA") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    @DocumentId
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

    
    // FIXME: for the search we use the standard analyzer which lowercases the search
    // so we need a lowercasing for the indexer too.....
    //@Length(min = 1, max = 250)
    @Column(name = "BUSINESS_KEY_", length = 250)
    @Field(index = Index.TOKENIZED, store = Store.YES, analyzer = @Analyzer(definition = "bkeyanalyzer"))
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(final String businessKey) {
        if (this.businessKey != null) {
            LOGGER.warn("changing business key from {} to {}, this shouldn't happen in normal operation", this.businessKey, businessKey);
        }
        this.businessKey = businessKey;
    }

    @Length(min = 1, max = 250)
    @Column(name = "TITLE_", length = 250)
    // @Field(index = Index.TOKENIZED, store = Store.YES)
    @Field(index = Index.TOKENIZED, store = Store.YES, analyzer = @Analyzer(definition = "htmlanalyzer"))
    // @TokenizerDef(factory = HTMLStripStandardTokenizerFactory.class)
    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    // HTMLStripStandardTokenizerFactory
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "PRIORITY_", nullable = true)
    // this is manually index in the data bridge
    // @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public Priority getPriority() {
        return priority;
    }

    public void setPriority(final Priority priority) {
        this.priority = priority;
    }

    @Column(name = "SUBMIT_DATE_", nullable = true)
    @Field(index = Index.UN_TOKENIZED)
    @DateBridge(resolution = Resolution.SECOND)
    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(final Date submitDate) {
        this.submitDate = submitDate;
    }

    @ManyToOne()
    @JoinColumn(name = "SUBMIT_USER_ID_", nullable = true)
    public CharmsUser getSubmitUser() {
        return submitUser;
    }

    public void setSubmitUser(final CharmsUser submitUser) {
        this.submitUser = submitUser;
    }

    @Column(name = "FINISH_DATE_", nullable = true)
    @Field(index = Index.UN_TOKENIZED)
    @DateBridge(resolution = Resolution.SECOND)
    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(final Date finishDate) {
        this.finishDate = finishDate;
    }

    @ManyToOne()
    @JoinColumn(name = "FINISH_USER_ID_")
    public CharmsUser getFinishUser() {
        return finishUser;
    }

    public void setFinishUser(final CharmsUser finishUser) {
        this.finishUser = finishUser;
    }

    @Transient
    public String getTitleString() {
        if (getFinishDate() != null) {
            return " - " + getBusinessKey() + " abgeschlossen ";
        } else if (getBusinessKey() == null) {
            return " - noch nicht eingereicht ";
        } else {
            return " - " + getBusinessKey() + " in Bearbeitung ";
        }
    }

}
