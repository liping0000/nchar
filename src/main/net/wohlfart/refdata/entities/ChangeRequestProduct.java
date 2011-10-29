package net.wohlfart.refdata.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.CustomResourceLoader;
import net.wohlfart.framework.i18n.ITranslateable;
import net.wohlfart.framework.sort.ISortable;

import org.hibernate.Session;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.validator.NotNull;

/*  @formatter:off */
@NamedQueries({
     @NamedQuery(
          name = ChangeRequestProduct.FIND_ID_BY_DEFAULT_NAME, 
          query = "select id from ChangeRequestProduct where defaultName = :defaultName"),
          
     @NamedQuery(
          name = ChangeRequestProduct.FIND_BY_DEFAULT_NAME, 
          query = "from ChangeRequestProduct where defaultName = :defaultName"),
          
     @NamedQuery(
          name = ChangeRequestProduct.FIND_NEXT_INDEX_VALUE, 
          query = "select max(sortIndex) + 1 from ChangeRequestProduct "),
          
     @NamedQuery(
          name = ChangeRequestProduct.COUNT_QUERY, 
          query = "select count(*) from ChangeRequestProduct "),
          
     @NamedQuery(
          name = ChangeRequestProduct.FIND_BY_ID, 
          query = "from ChangeRequestProduct where id = :id") 
})        
/*  @formatter:on */
        
@Entity
//not yet: @Audited
@Table(name = "CHREQ_PROD")
public class ChangeRequestProduct implements ITranslateable, ISortable, Serializable {


    public static final String FIND_ID_BY_DEFAULT_NAME = "ChangeRequestProduct.FIND_ID_BY_DEFAULT_NAME";
    public static final String FIND_BY_DEFAULT_NAME    = "ChangeRequestProduct.FIND_BY_DEFAULT_NAME";
    public static final String FIND_NEXT_INDEX_VALUE   = "ChangeRequestProduct.FIND_NEXT_INDEX_VALUE";
    public static final String COUNT_QUERY             = "ChangeRequestProduct.COUNT_QUERY";
    public static final String FIND_BY_ID              = "ChangeRequestProduct.FIND_BY_ID";

    private Long                    id;
    private Integer                 version;

    // default name
    private String                  defaultName;
    // this product can be selected for new workflows
    private Boolean                 enabled                 = true;
    // i18n code
    private String                  messageCode;
    // sort column
    private Integer                 sortIndex;

    // ordered list of parts for this product
    private List<ChangeRequestUnit> units                   = new ArrayList<ChangeRequestUnit>();

    // ordered list of errors for this product
    private List<ChangeRequestCode> codes                   = new ArrayList<ChangeRequestCode>();

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHREQ_PROD") })
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

    @Override
    @NotNull
    @Column(name = "DEFAULT_NAME_", unique = true, nullable = false, length = 50)
    // @UniqueProperty(entityName=
    // "net.wohlfart.charms.entity.ChangeRequestProduct", fieldName = "name",
    // message = "name not unique" )
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

    // the noninverse side of a many-to-many relationship
    // see Hibernate in Action page 302
    @ManyToMany(fetch = FetchType.LAZY)
    /* ( targetEntity = ChangeRequestUnit.class) */
    @Cascade(value = { org.hibernate.annotations.CascadeType.SAVE_UPDATE })
    // @ElementCollection(targetClass=ChangeRequestUnit.class)
    @JoinTable(name = "CHREQ_PROD_UNITS",
         uniqueConstraints = { @UniqueConstraint(columnNames = { "PRODUCT_ID_", "UNIT_ID_" }) },
         joinColumns = { @JoinColumn(name = "PRODUCT_ID_", nullable = false, updatable = false) }, 
         inverseJoinColumns = { @JoinColumn(name = "UNIT_ID_", nullable = false, updatable = false) })
    @IndexColumn(name = "POSITION_", base = 1, nullable = false)
    public List<ChangeRequestUnit> getUnits() {
        return units;
    }
    public void setUnits(final List<ChangeRequestUnit> units) {
        this.units = units;
    }

    // the noninverse side of a many-to-many relationship
    // see Hibernate in Action page 302
    @ManyToMany(fetch = FetchType.LAZY)
    @Cascade(value = { org.hibernate.annotations.CascadeType.SAVE_UPDATE })
    // @ElementCollection(targetClass=ChangeRequestCode.class)
    @JoinTable(name = "CHREQ_PROD_CODES", 
            uniqueConstraints = { @UniqueConstraint(columnNames = { "PRODUCT_ID_", "CODE_ID_" }) }, 
            joinColumns = { @JoinColumn(name = "PRODUCT_ID_", nullable = false, updatable = false) }, 
            inverseJoinColumns = { @JoinColumn(name = "CODE_ID_", nullable = false, updatable = false) })
    @IndexColumn(name = "POSITION_", base = 1, nullable = false)
    public List<ChangeRequestCode> getCodes() {
        return codes;
    }
    public void setCodes(final List<ChangeRequestCode> codes) {
        this.codes = codes;
    }

    @Column(name = "ENABLED_")
    @Type(type = "yes_no")
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    // --------- implementing the interfaces

    /**
     * initial setup of the sort index has to be called before persisting
     * 
     */
    @Override
    @Transient
    public void setupSortIndex(final Session hibernateSession) {
        final Object object = hibernateSession.getNamedQuery(ChangeRequestProduct.FIND_NEXT_INDEX_VALUE).uniqueResult();
        if (object == null) {
            sortIndex = 1;
        } else {
            sortIndex = new Integer(object.toString());
        }
    }

    /**
     * initial setup of the message code has to be called after persisting, then
     * update the DB by persisting with the new value
     * 
     */
    @Override
    @Transient
    public void setupMessageCode() {
        setMessageCode(CustomResourceLoader.CHREQ_PROD_BUNDLE_NAME + TranslateableHome.MESSAGE_BUNDLE_ID_NUMBER_FORMAT.format(id));
    }

}
