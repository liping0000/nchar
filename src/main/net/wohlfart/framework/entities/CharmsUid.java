package net.wohlfart.framework.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * table to store uid information currently only used for the process id
 * 
 * @author Michael Wohlfart
 * 
 */

@Entity
@Table(name = "CHARMS_UID", uniqueConstraints = { @UniqueConstraint(columnNames = { "SEQUENCE_TYPE_", "VALUE_" }) // we
                                                                                                                  // have
                                                                                                                  // unique
                                                                                                                  // values
                                                                                                                  // within
                                                                                                                  // each
// sequence
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "SEQUENCE_TYPE_", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("SIMPLE")
public class CharmsUid {

    private Long    id;
    private Integer version;
    private Date    lastModified;

    private String  value;       // the actual sequence number as needed by the
                                  // business layer

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_UID") })
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

    @Column(name = "VALUE_", length = 250, nullable = false)
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Column(name = "LAST_MODIFIED_", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Date lastModfied) {
        lastModified = lastModfied;
    }

}
