package net.wohlfart.authorization.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.authorization.targets.IPermissionTargetDescriptor;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * a description for an object on which a user or a group can act in some way
 * the object must provide a unique targetString 
 * note that there are two types of objects
 * - classes which describe a whole set of objects like a certain workflow step
 *   of all users, they can be identified with the targetString only
 *   
 * - a single object like a special user this objects have to be identified with
 *   the target string and the id of the user,there is no id in this class
 *   since the entity might not yet exist
 * 
 * @author Michael Wohlfart
 */

/*  @formatter:off */
@NamedQueries({ 
    @NamedQuery(
          name = CharmsPermissionTarget.FIND_BY_TARGET_STRING, 
          query = "from CharmsPermissionTarget where targetString = :targetString ") 
})
/*  @formatter:on */

@Entity
@Name("charmsPermissionTarget")
@Scope(ScopeType.CONVERSATION)
@Table(name = "CHARMS_PERM_TARGET")
public class CharmsPermissionTarget implements Serializable, IPermissionTargetDescriptor {

    public static final String FIND_BY_TARGET_STRING = "CharmsPermissionTarget.FIND_BY_TARGET_STRING";

    public static final int MAX_DESCRIPTION_LENGTH = 2024;

    private Long id;
    private Integer version;

    // an string that describes an object that is to be acted upon in some way
    // this might be a class or instance or a static string identifying a
    // workflow step for example
    private String targetString;


    // the intended action to be performed on the target, this might be a comma
    // separated list,
    // see:
    // http://www.seamframework.org/Community/HowToGrantPermissionsWithoutRules
    private List<CharmsTargetAction> actions = new ArrayList<CharmsTargetAction>();
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
                @Parameter(name = "segment_value", value = "CHARMS_TARGET") 
    })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    @AccessType("field")
    public Long getId() {
        return id;
    }
    public void setId(final Long id) {
        this.id = id;
    }

    @Version
    @AccessType("field")
    @Column(name = "VERSION_")
    public Integer getVersion() {
        return version;
    }
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    /**
     * the target of the action, can be a role, a user, an action, a workflow
     * step, a special view, a menu item, ...
     * 
     * @return
     */
    @Override
    @AccessType("field")
    @Column(name = "TARGET_STRING_", 
            nullable = false, 
            unique = true)
    public String getTargetString() {
        return targetString;
    }
    public void setTargetString(final String targetString) {
        this.targetString = targetString;
    }

    /**
     * the actions to be performed by the recipient on the target
     * 
     * @return
     */
    @OneToMany(mappedBy = "target")
    @Cascade(value = { org.hibernate.annotations.CascadeType.DELETE })
    @AccessType("field")
    public List<CharmsTargetAction> getActions() {
        return actions;
    }
    public void setActions(final List<CharmsTargetAction> actions) {
        // remove blanks, this is important for the selects
        this.actions = actions;
    }

    /**
     * some description for the end user
     * 
     * @return
     */
    @Override
    @AccessType("field")
    @Column(name = "DESCRIPTION_", 
            length = MAX_DESCRIPTION_LENGTH, 
            nullable = true)
    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description = description;
    }

    @Transient
    public void addAction(final CharmsTargetAction action) {
        action.setTarget(this);
        actions.add(action);
    }

    @Transient
    public String getAllActionString() {
        final List<String> list = new ArrayList<String>();
        // return null if there is nothing to join anyways
        if ((actions == null) || (actions.size() == 0)) {
            return null;
        }
        for (final CharmsTargetAction action : actions) {
            list.add(action.getName());
        }
        return StringUtils.join(list, ",");
    }

    @Override
    @Transient
    public String[] getAllActions() {
        final List<String> list = new ArrayList<String>();
        for (final CharmsTargetAction action : actions) {
            list.add(action.getName());
        }
        return list.toArray(new String[list.size()]);
    }

}
