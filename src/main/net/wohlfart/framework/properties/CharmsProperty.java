package net.wohlfart.framework.properties;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.validator.Length;

/**
 * see: http://relation.to/2303.lace
 * http://lists.jboss.org/pipermail/jboss-user/2007-September/082671.html
 * 
 * caching: http://docs.jboss.org/seam/1.2.0.PATCH1/reference/en/html/cache.html
 * for database stored messages in seam
 * 
 * @author Michael Wohlfart
 * 
 */

@Entity
//name is unique within the set
@Table(name = "CHARMS_PROP_VAL",
       uniqueConstraints = {@UniqueConstraint(columnNames = { "NAME_", "SET_ID_" }) })
public class CharmsProperty implements Serializable {

    private Long id;
    private Integer version;
    private String name;
    private CharmsPropertyType type = CharmsPropertyType.STRING;
    private String value;

    private CharmsPropertySet propertySet;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(
            name = "sequenceGenerator", 
            strategy = "org.hibernate.id.enhanced.TableGenerator", 
            parameters = { @Parameter(
                    name = "segment_value", 
                    value = "CHARMS_PROP_VAL") 
    })
    @GeneratedValue(generator = "sequenceGenerator")
    @AccessType("field")
    @Column(name = "ID_")
    protected Long getId() {
        return id;
    }
    protected void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    @Version
    @AccessType("field")
    @Column(name = "VERSION_")
    private Integer getVersion() {
        return version;
    }
    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @Length(max = 100)
    @AccessType("field")
    @Column(name = "NAME_", nullable = false, length = 100)
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * this is the type of the property, meaning string, date, number boolean...
     * for a single property
     * 
     * @return
     */
    @Enumerated(EnumType.STRING)
    @AccessType("field")
    @Column(name = "PROP_TYPE_", nullable = false, length = 20)
    public CharmsPropertyType getType() {
        return type;
    }
    public void setType(CharmsPropertyType type) {
        this.type = type;
    }

    
    @Length(max = 2048)
    @AccessType("field")
    @Column(name = "VALUE_", nullable = true, length = 2048)
    public String getValue() {
        return value;
    }
    public void setValue(final String value) {
        this.value = value;
    }

    @ManyToOne(targetEntity = CharmsPropertySet.class, optional = false)
    @JoinColumn(name = "SET_ID_")
    public CharmsPropertySet getPropertySet() {
        return propertySet;
    }
    public void setPropertySet(final CharmsPropertySet propertySet) {
        this.propertySet = propertySet;
    }

}
