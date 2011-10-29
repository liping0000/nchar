package net.wohlfart.framework.properties;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
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
 * 
 * this class is used to simplify editing of properties, during runtime the
 * values are transformed and stored in a simple hashmap see:
 * AbstractPropertiesHashMap
 * 
 * 
 * @author Michael Wohlfart
 * 
 */

/*  @formatter:off */
@NamedQueries({ 
    
    @NamedQuery(
         name = CharmsPropertySet.FIND_BY_NAME_AND_TYPE, 
         query = "from CharmsPropertySet where name = :name and type = :type"),
         
    @NamedQuery(
         name = CharmsPropertySet.DELETE_BY_NAME_AND_TYPE, 
         query = "delete from CharmsPropertySet where name = :name and type = :type") 
         
})
/*  @formatter:on */

// no two permissions for the same target and recipient:
@Entity
@Table(name = "CHARMS_PROP_SET", 
       uniqueConstraints = {@UniqueConstraint(columnNames = { "NAME_", "SET_TYPE_" }) })
public class CharmsPropertySet implements Serializable {

    // find a bundle for a locale, (e.g. get the bunle for the current locale to do translations)
    public static final String FIND_BY_NAME_AND_TYPE   = "CharmsPropertySet.FIND_BY_NAME_AND_TYPE";

    // delete all translations from a bundle with the message code (e.g. for
    // business object deletion)
    public static final String DELETE_BY_NAME_AND_TYPE = "CharmsPropertySet.DELETE_BY_NAME_AND_TYPE";

    // name to use in the name attribute of the property set for the application
    // properties, for user level properties we use the username instead
    public static final String APPLICATION_NAME        = "application";

    private Long id;
    private Integer version;
    private CharmsPropertySetType type;
    private String name;
    private Map<String, CharmsProperty> properties = new HashMap<String, CharmsProperty>();

    // due to MySQL index size limits the following column sizes must not exeed
    // 1000Byte (333/150 utf8 chars)
    public static final int MAX_PROP_TYPE_LENGTH = 20;
    public static final int MAX_PROP_NAME_LENGTH = 120;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(
            name = "sequenceGenerator", 
            strategy = "org.hibernate.id.enhanced.TableGenerator", 
            parameters = { @Parameter(
                    name = "segment_value", 
                    value = "CHARMS_PROPERTY_SET")
    })
    @GeneratedValue(generator = "sequenceGenerator")
    @AccessType("field")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }
    public void setId(final Long id) {
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

    /**
     * this can be the username, the name of a template or the name for the
     * application which is defined as CharmsPropertySet.APPLICATION_NAME
     * 
     * 
     * @return
     */
    @Length(max = 100)
    @AccessType("field")
    @Column(name = "NAME_", nullable = false, length = MAX_PROP_NAME_LENGTH)
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * this is the set type, marking this set as application, user or template set
     * 
     * @return
     */
    @Enumerated(EnumType.STRING)
    @AccessType("field")
    @Column(name = "SET_TYPE_", nullable = false, length = MAX_PROP_TYPE_LENGTH)
    public CharmsPropertySetType getType() {
        return type;
    }
    public void setType(final CharmsPropertySetType type) {
        this.type = type;
    }

    @ElementCollection(fetch = FetchType.EAGER, targetClass = CharmsProperty.class)
    @JoinColumn(name = "SET_ID_")
    @AccessType("field")
    @MapKeyColumn(name = "NAME_")
    public Map<String, CharmsProperty> getProperties() {
        return properties;
    }

    public void setProperties(final Map<String, CharmsProperty> properties) {
        this.properties = properties;
    }

    // ------------ convenience methods -------

    @Transient
    protected void putProperty(final CharmsProperty property) {
        final String key = property.getName();
        properties.put(key, property);
    }

    @Transient
    protected CharmsProperty getProperty(final String key) {
        if (properties.containsKey(key)) {
            return properties.get(key);
        } else {
            return null;
        }
    }

    // ----- client accessory methods ----

    @Transient
    public String getPropertyAsString(final String key, final String defaultValue) {
        if (properties.containsKey(key)) {
            return properties.get(key).getValue();
        } else {
            return defaultValue;
        }
    }

    @Transient
    public Date getPropertyAsDate(final String key, final Date defaultValue) {
        if (properties.containsKey(key)) {
            final String value = properties.get(key).getValue();
            final Long lastLogon = Long.parseLong(value);
            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(lastLogon);
            return cal.getTime();
        } else {
            return defaultValue;
        }
    }

    @Transient
    public Long getPropertyAsLong(final String key, final Long defaultValue) {
        if (properties.containsKey(key)) {
            final String value = properties.get(key).getValue();
            final Long result = Long.parseLong(value);
            return result;
        } else {
            return defaultValue;
        }
    }

    @Transient
    public DateFormat getPropertyAsDateFormat(final String key, final DateFormat defaultFormatter) {
        if (properties.containsKey(key)) {
            final String value = properties.get(key).getValue();
            final SimpleDateFormat result = new SimpleDateFormat(value);
            return result;
        } else {
            return defaultFormatter;
        }
    }

    @Transient
    public CharmsMementoState getPropertyAsMementoState(final String key, final CharmsMementoState defaultState) {
        if (properties.containsKey(key)) {
            final String value = properties.get(key).getValue();
            final CharmsMementoState result = new CharmsMementoState();
            result.setValue(value);
            return result;
        } else {
            return defaultState;
        }
    }

    @Transient
    public boolean hasKey(final String key) {
        return properties.containsKey(key);
    }
    
    /**
     * clone a list of property elements in order to edit them by the user
     * @return
     */
    @Transient
    public ArrayList<CharmsPropertyItem> getList() {
        // a select would probably be easier here
        ArrayList<CharmsPropertyItem> list = new ArrayList<CharmsPropertyItem>();
        Set<Entry<String, CharmsProperty>> entries = properties.entrySet();
        for (Entry<String, CharmsProperty> entry : entries) {
            CharmsProperty charmsProperty = entry.getValue();
            CharmsPropertyItem charmsPropertyItem = new CharmsPropertyItem();
            charmsPropertyItem.setName(charmsProperty.getName());
            charmsPropertyItem.setValue(charmsProperty.getValue());
            charmsPropertyItem.setType(charmsProperty.getType());
            list.add(charmsPropertyItem);
        }
        return list;
    }

    @Transient
    public void setList(ArrayList<CharmsPropertyItem> list) {
        // FIXME: store the items in the set
    }

}
