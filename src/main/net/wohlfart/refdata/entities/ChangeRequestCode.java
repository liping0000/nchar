package net.wohlfart.refdata.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.CustomResourceLoader;
import net.wohlfart.framework.i18n.ITranslateable;
import net.wohlfart.framework.sort.ISortable;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.validator.NotNull;

/*  @formatter:off */
@NamedQueries({ 
    
    @NamedQuery(
         name = ChangeRequestCode.FIND_BY_DEFAULT_NAME, 
         query = "from ChangeRequestCode where defaultName = :defaultName"),
         
    @NamedQuery(
         name = ChangeRequestCode.FIND_NEXT_INDEX_VALUE, 
         query = "select max(sortIndex) + 1 from ChangeRequestCode"),
         
    @NamedQuery(
         name = ChangeRequestCode.COUNT_QUERY, 
         query = "select count(*) from ChangeRequestCode") 
})
/*  @formatter:on */

@Entity
//not yet: @Audited
@Table(name = "CHREQ_CODE")
public class ChangeRequestCode implements ITranslateable, ISortable, Serializable {


    public static final String        FIND_BY_DEFAULT_NAME  = "ChangeRequestCode.FIND_BY_DEFAULT_NAME";
    public static final String        FIND_NEXT_INDEX_VALUE = "ChangeRequestCode.FIND_NEXT_INDEX_VALUE";
    public static final String        COUNT_QUERY           = "ChangeRequestCode.COUNT_QUERY";

    private Long                      id;
    private Integer                   version;

    // default name
    private String                    defaultName;
    // this product can be selected for new workflows
    private Boolean                   enabled               = true;
    // i18n code
    private String                    messageCode;
    // sort column
    private Integer                   sortIndex;

    private Set<ChangeRequestProduct> product               = new HashSet<ChangeRequestProduct>();

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHREQ_CODE") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    @Override
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

    @NotNull
    @Override
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

    // inverse side of a sorted many-to-many relationship
    // cascade={CascadeType.REMOVE} deletes all products for which this error is
    // registered
    @ManyToMany(mappedBy = "codes")
    public Set<ChangeRequestProduct> getProduct() {
        return product;
    }

    public void setProduct(final Set<ChangeRequestProduct> product) {
        this.product = product;
    }

    // --------- implementing the interfaces

    /**
     * initial setup of the sort index has to be called before persisting
     */
    @Override
    @Transient
    public void setupSortIndex(final Session hibernateSession) {
        final Object object = hibernateSession.getNamedQuery(ChangeRequestCode.FIND_NEXT_INDEX_VALUE).uniqueResult();
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
        setMessageCode(CustomResourceLoader.CHREQ_CODE_BUNDLE_NAME + TranslateableHome.MESSAGE_BUNDLE_ID_NUMBER_FORMAT.format(id));
    }

}
