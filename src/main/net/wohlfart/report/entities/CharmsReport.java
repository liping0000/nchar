package net.wohlfart.report.entities;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.CustomResourceLoader;
import net.wohlfart.framework.i18n.ITranslateable;
import net.wohlfart.framework.sort.ISortable;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.validator.NotNull;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

// see:
// http://opensource.atlassian.com/projects/hibernate/secure/attachment/12728/patch.txt
// for an extended example of hibernate annotations

/*  @formatter:off */
@NamedQueries({ 
    
    @NamedQuery(
         name = CharmsReport.FIND_BY_DEFAULT_NAME, 
         query = "from CharmsReport where defaultName = :defaultName"),
         
    @NamedQuery(
         name = CharmsReport.FIND_NEXT_INDEX_VALUE, 
         query = "select max(sortIndex) + 1 from CharmsReport"),
         
    @NamedQuery(
         name = CharmsReport.COUNT_QUERY, 
         query = "select count(*) from CharmsReport") 
})
/*  @formatter:on */
@Entity
@Name("charmsReport")
@Scope(CONVERSATION)
@Table(name = "CHARMS_REPORT")
public class CharmsReport implements ITranslateable, ISortable, Serializable {

    static final long                  serialVersionUID       = -1L;

    public static final String         FIND_BY_DEFAULT_NAME   = "CharmsReport.FIND_BY_DEFAULT_NAME";
    public static final String         FIND_NEXT_INDEX_VALUE  = "CharmsReport.FIND_NEXT_INDEX_VALUE";
    public static final String         COUNT_QUERY            = "CharmsReport.COUNT_QUERY";

    public static final int            MAX_DESCRIPTION_LENGTH = 2024;
    public static final int            MAX_NAME_LENGTH        = 50;

    private Long                       id;
    private Integer                    version;

    private String                     description;

    private transient CharmsReportBlob reportBlob;
    private Long                       size;

    private Date                       lastModified;

    // default name
    private String                     defaultName;
    // this product can be selected for new workflows
    private Boolean                    enabled                = true;
    // i18n code
    private String                     messageCode;
    // sort column
    private Integer                    sortIndex;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_REPORT") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    @Version
    @Column(name = "VERSION_")
    private Integer getVersion() {
        return version;
    }

    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @Column(name = "DESCRIPTION_", length = MAX_DESCRIPTION_LENGTH)
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    // the size of the content data, doesn't change
    @Column(name = "SIZE_", nullable = false)
    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // effective a one-to-one because of the unique
    @JoinColumn(name = "REPORT_BLOB_ID_", unique = true)
    private CharmsReportBlob getReportBlob() {
        return reportBlob;
    }

    private void setReportBlob(final CharmsReportBlob charmsBlob) throws IOException {
        reportBlob = charmsBlob;
    }

    @Transient
    public InputStream getContentStream() throws SQLException {
        final CharmsReportBlob charmsBlob = getReportBlob();
        return charmsBlob.getContent().getBinaryStream();
    }

    public void setContentStream(final InputStream sourceStream, final long size, final Session session) throws IOException {
        CharmsReportBlob charmsBlob = getReportBlob();
        if (charmsBlob == null) {
            charmsBlob = new CharmsReportBlob();
        }
        // Blob blob = Hibernate.createBlob(sourceStream);
        // FIXME: see:
        // http://i-proving.ca/space/Technologies/Hibernate/Blobs+and+Hibernate
        // for compression/ optimization with usertypes etc...
        charmsBlob.setContent(Hibernate.createBlob(sourceStream, size, session));
        setReportBlob(charmsBlob);
    }

    @Override
    @NotNull
    @Column(name = "DEFAULT_NAME_", unique = true, nullable = false, length = 50)
    public String getDefaultName() {
        return defaultName;
    }

    @Override
    public void setDefaultName(final String defaultName) {
        this.defaultName = defaultName;
    }

    @Override
    @Column(name = "MSG_CODE_")
    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(final String messageCode) {
        this.messageCode = messageCode;
    }

    @Override
    @Column(name = "SORT_INDEX_", unique = true, nullable = false)
    public Integer getSortIndex() {
        return sortIndex;
    }

    @Override
    public void setSortIndex(final Integer sortIndex) {
        this.sortIndex = sortIndex;
    }

    @Column(name = "ENABLED_")
    @Type(type = "yes_no")
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    @Column(name = "UPDATE_")
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    // --------- implementing the interfaces

    /**
     * initial setup of the sort index has to be called before persisting
     */
    @Override
    @Transient
    public void setupSortIndex(final Session hibernateSession) {
        final Object object = hibernateSession.getNamedQuery(CharmsReport.FIND_NEXT_INDEX_VALUE).uniqueResult();
        if (object == null) {
            sortIndex = 1;
        } else {
            sortIndex = new Integer(object.toString());
        }
    }

    /**
     * initial setup of the message code has to be called after persisting, then
     * update the DB by persisting with the new value
     */
    @Override
    @Transient
    public void setupMessageCode() {
        setMessageCode(CustomResourceLoader.CHARMS_REPORT_BUNDLE_NAME + TranslateableHome.MESSAGE_BUNDLE_ID_NUMBER_FORMAT.format(id));
    }

    @Transient
    public String getSizeString() {

        String formatted = "unknown";
        if (size == null) {
            formatted = "empty";
        } else if (size > 1000000000) {
            formatted = Long.toString(size / 1000000000) + " GB";
        } else if (size > 1000000) {
            formatted = Long.toString(size / 1000000) + " MB";
        } else if (size > 1000) {
            formatted = Long.toString(size / 1000) + " KB";
        } else {
            formatted = Long.toString(size) + " bytes";
        }
        return formatted;
    }
}
