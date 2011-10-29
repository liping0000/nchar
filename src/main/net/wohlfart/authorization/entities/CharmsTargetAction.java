package net.wohlfart.authorization.entities;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

/**
 * an action that can be performed on an object by a user or a group
 * 
 * @author Michael Wohlfart
 */
@Entity
@Table(name = "CHARMS_TRGT_ACT", uniqueConstraints = { @UniqueConstraint(columnNames = { "NAME_", "TARGET_ID_" }) })
public class CharmsTargetAction implements Serializable {


    public static final int MAX_DESCRIPTION_LENGTH = 2024;

    // must limit this since MySQL doesn't support longer texts on indexed
    // columns
    public static final int MAX_NAME_LENGTH        = 255;

    private Long id;
    private Integer version;

    private String name;
    private CharmsPermissionTarget target;
    // user editable description
    private String description;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(
        name = "sequenceGenerator", 
        strategy = "org.hibernate.id.enhanced.TableGenerator", 
        parameters = { 
              @Parameter(name = "segment_value", value = "CHARMS_TRGT_ACT") 
    })
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
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    
    @Column(name = "NAME_", 
            length = MAX_NAME_LENGTH, 
            nullable=false)
    @AccessType("field")
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    
    @ManyToOne //(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "TARGET_ID_", nullable = false)
    @AccessType("field")
    public CharmsPermissionTarget getTarget() {
        return target;
    }
    public void setTarget(final CharmsPermissionTarget target) {
        this.target = target;
    }

    /**
     * some description for the end user
     * 
     * @return
     */
    @Column(name = "DESCRIPTION_", length = MAX_DESCRIPTION_LENGTH, nullable = true)
    @AccessType("field")
    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description = description;
    }

}
