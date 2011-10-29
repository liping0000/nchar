package net.wohlfart.authorization.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.validator.NotNull;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.permission.PermissionAction;
import org.jboss.seam.annotations.security.permission.PermissionDiscriminator;
import org.jboss.seam.annotations.security.permission.PermissionRole;
import org.jboss.seam.annotations.security.permission.PermissionTarget;
import org.jboss.seam.annotations.security.permission.PermissionUser;

/**
 * a permissions to to perform an action on an object granted to a user or a
 * group
 * 
 * see: http://shane.bryzak.com/blog/articles/seam_security_gets_an_upgrade for
 * the basic concepts
 * 
 * see: http://www.seamframework.org/Community/HowToGrantPermissionsWithoutRules
 * for database persisted permissions
 * 
 * see: http://java.dzone.com/articles/acl-security-in-seam?page=0,2
 * 
 * a permission consists of the following components:
 * 
 * A target, which is an object that is to be acted upon in some way
 * 
 * An action, the intended action to be performed on the target
 * 
 * A recipient, the user or role entity that is given the right to perform the
 * specified action on the target
 * 
 * 
 * scheme:
 *                       action
 *   discriminator   ---------------->  target
 *   receiver                           targetId
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
/*  @formatter:off */
@NamedQueries({
    @NamedQuery(
          name = CharmsPermission.FIND_BY_RECIPIENT_USER_AND_TARGET, 
          query = "from CharmsPermission where "
                + " recipient = :recipient "
                + " and target = :target "
                + " and discriminator = '" + CharmsPermission.USER + "'"),
                
    @NamedQuery(
          name = CharmsPermission.FIND_BY_RECIPIENT_ROLE_AND_TARGET, 
          query = "from CharmsPermission where "
                + " recipient = :recipient "
                + " and target = :target "
                + " and discriminator = '" + CharmsPermission.ROLE + "'"),
                
    @NamedQuery(
          name = CharmsPermission.FIND_BY_RECIPIENT_ROLEONLY_AND_TARGET, 
          query = "from CharmsPermission where "
                + " recipient = :recipient "
                + " and target = :target "
                + " and discriminator = '" + CharmsPermission.ROLEONLY + "'"),
                
    @NamedQuery(
          name = CharmsPermission.FIND_BY_RECIPIENT_ROLE_EITHER_AND_TARGET,
          query = "from CharmsPermission where "
                + " recipient = :recipient "
                + " and target = :target "
                + " and ( (discriminator = '" + CharmsPermission.ROLE + "')" 
                + " or (discriminator = '" + CharmsPermission.ROLEONLY + "') )"),
        
    @NamedQuery(
          name = CharmsPermission.FIND_BY_RECIPIENT_DISCRIMINATOR_AND_TARGET, 
          query = "from CharmsPermission where "
                + " recipient = :recipient "
                + " and target = :target "
                + " and discriminator = :discriminator "),
         
    @NamedQuery(
          name = CharmsPermission.FIND_BY_TARGET, 
          query = "from CharmsPermission where target = :target"),
    
    @NamedQuery(
          name = CharmsPermission.FIND_USER_ID_BY_ID, 
          query = "select u.id from CharmsUser u where u.name in "
                + " ( select p.recipient from CharmsPermission p where p.id = :id ) "),
                
    @NamedQuery(
          name = CharmsPermission.FIND_ROLE_ID_BY_ID, 
          query = "select r.id from CharmsRole r where r.name in "
                + " ( select p.recipient from CharmsPermission p where p.id = :id ) "),
                
    @NamedQuery(
          name = CharmsPermission.REMOVE_FOR_ROLE_NAME, 
          query = "delete CharmsPermission where recipient = :name " 
              + " and ( (discriminator = '" + CharmsPermission.ROLE + "')" 
              + " or (discriminator = '" + CharmsPermission.ROLEONLY + "') )"),
                
    @NamedQuery(
          name = CharmsPermission.REMOVE_FOR_USER_NAME, 
          query = "delete CharmsPermission where recipient = :name " 
              + " and discriminator = '" + CharmsPermission.USER + "'"),
                
    @NamedQuery(
          name = CharmsPermission.MOVE_TO_NEW_ROLE_NAME, 
          query = "update CharmsPermission set recipient = :newName where recipient = :oldName "
                + " and ( (discriminator = '" + CharmsPermission.ROLE + "')" 
                + " or (discriminator = '" + CharmsPermission.ROLEONLY + "') )"),
                
    @NamedQuery(
          name = CharmsPermission.MOVE_TO_NEW_USER_NAME, 
          query = "update CharmsPermission set recipient = :newName where recipient = :oldName "
                + " and discriminator = '" + CharmsPermission.USER + "'"),
                
    // all whitespace needs to be removed from action in order for this to work...
    @NamedQuery(
          name = CharmsPermission.FIND_BY_TARGET_AND_ACTION, 
          query = "from CharmsPermission where target = :target"
                + " and (  ( action like concat('%,', :action, ',%') )" 
                + "    or  ( action like concat('%,', :action) )"
                + "    or  ( action like concat(:action, ',%') )" 
                + "    or  ( action like :action )  )") 
                                                                                                          
})
/*  @formatter:on */

@Entity
//not yet: @Audited
@Name("charmsPermission")
@Scope(ScopeType.CONVERSATION)
// no two permissions for the same target and recipient,
// this unique contraint creates an index in MySQL which is limited to 1000Bytes (333 utf-8 chars)
@Table(name = "CHARMS_PERMISSION", 
       uniqueConstraints = { @UniqueConstraint(columnNames = { "RECIPIENT_", "DISCRIMINATOR_", "TARGET_", "TARGET_ID_" }) })
public class CharmsPermission implements Serializable {

    // means the permission is granted to a role but not to the members of the role
    // the recipient must be a valid id of a role
    public static final String ROLE                                       = "role";
    // means the permission is granted to a user
    // the recipient must be a valid id of a user
    public static final String USER                                       = "user";
    // means the permission is granted to a role and its subroles and its
    // containing users
    // the recipient must be a valid id of a role
    // FIXME: remove this: XXX
    @Deprecated
    public static final String ROLEONLY                                   = "roleonly";
    // see: JapPermissionStore : private enum Discrimination { user, role,
    // either }

    public static final String FIND_BY_RECIPIENT_USER_AND_TARGET          = "CharmsPermission.FIND_BY_RECIPIENT_USER_AND_TARGET";
    public static final String FIND_BY_RECIPIENT_ROLE_AND_TARGET          = "CharmsPermission.FIND_BY_RECIPIENT_ROLE_AND_TARGET";
    public static final String FIND_BY_RECIPIENT_ROLEONLY_AND_TARGET      = "CharmsPermission.FIND_BY_RECIPIENT_ROLEONLY_AND_TARGET";
    
    public static final String FIND_BY_RECIPIENT_ROLE_EITHER_AND_TARGET   = "CharmsPermission.FIND_BY_RECIPIENT_ROLE_EITHER_AND_TARGET";
    public static final String FIND_BY_RECIPIENT_DISCRIMINATOR_AND_TARGET = "FIND_BY_RECIPIENT_DISCRIMINATOR_AND_TARGET";
    public static final String FIND_BY_TARGET_AND_ACTION                  = "CharmsPermission.FIND_BY_TARGET_AND_ACTION";
    public static final String FIND_BY_TARGET                             = "CharmsPermission.FIND_BY_TARGET";
    
    public static final String FIND_USER_ID_BY_ID                         = "CharmsPermission.FIND_USER_ID_BY_ID";
    public static final String FIND_ROLE_ID_BY_ID                         = "CharmsPermission.FIND_ROLE_ID_BY_ID";
    
    public static final String REMOVE_FOR_ROLE_NAME                       = "CharmsPermission.REMOVE_FOR_ROLE_NAME";
    public static final String REMOVE_FOR_USER_NAME                       = "CharmsPermission.REMOVE_FOR_USER_NAME";
    
    public static final String MOVE_TO_NEW_ROLE_NAME                      = "CharmsPermission.MOVE_TO_NEW_ROLE_NAME";
    public static final String MOVE_TO_NEW_USER_NAME                      = "CharmsPermission.MOVE_TO_NEW_USER_NAME";

    
    
    public static final int MAX_DESCRIPTION_LENGTH = 2024;

    // the following length must not exceed 333 (1000 UTF-8 chars) on MySQL:
    public static final int MAX_RECIPIENT_LENGTH = 50;                                                 
    // TODO: we need to change the recipient to use actor ids which can be limited
    public static final int MAX_DISCRIMINATOR_LENGTH = 5;
    public static final int MAX_TARGET_LENGTH = 120;

    private Long id;
    private Integer version;

    
    
    // the user or role entity that is given the right to perform the specified action on the target
    private String recipient;
    // permission intended for a user or a role
    private String discriminator;
 
    // the intended action to be performed on the target, this might be a comma separated list,
    // see:  http://www.seamframework.org/Community/HowToGrantPermissionsWithoutRules
    private String action;

    // an object that is to be acted upon in some way
    private String target;
    // if the target is an instance this is the unique hibernate id of the target
    private Long targetId;
    
    
    // user editable description
    private String description;

    /**
     * @return generated unique id for this table
     * @formatter:off
     */
    @Id
    @GenericGenerator( 
            name = "sequenceGenerator", 
            strategy = "org.hibernate.id.enhanced.TableGenerator", 
            parameters = { @Parameter(name = "segment_value", value = "CHARMS_PERMISSION") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    @AccessType("field")
    public Long getId() {
        return id;
    }
    @SuppressWarnings("unused")
    private void setId(final Long id) {
        this.id = id;
    }

    /** @return hibernate version for this entity */
    @SuppressWarnings("unused")
    @Version
    @Column(name = "VERSION_")
    @AccessType("field")
    private Integer getVersion() {
        return version;
    }
    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    /**
     * 
     * this is the role or the user getting the permission to act on the target,
     * more or less the subject that receives the permission
     * 
     * @return
     */
    @NotNull   
    //@PermissionUser
    //@PermissionRole
    @AccessType("field")
    @Column(name = "RECIPIENT_", 
            nullable = false, 
            length = MAX_RECIPIENT_LENGTH)
    public String getRecipient() {
        return recipient;
    }
    public void setRecipient(final String recipient) {
        this.recipient = recipient;
    }
    
    /**
     * indicating the permission (the recipient) is for user or roles, see:
     * http://docs.jboss.org/seam/latest/reference/en-US/html/security.html
     * 
     * @return
     */
    //@PermissionDiscriminator
    @NotNull
    @AccessType("field")
    // limit to 5 because of MySQL 1000byte index limit, see: http://bugs.mysql.com/bug.php?id=4541
    @Column(name = "DISCRIMINATOR_", 
            nullable = false, 
            length = MAX_DISCRIMINATOR_LENGTH)
    public String getDiscriminator() {
        return discriminator;
    }
    public void setDiscriminator(final String discriminator) {
        this.discriminator = discriminator;
    }
  

    /**
     * the target of the action, can be a role, a user, an action, a workflow
     * step, a special view, a menu item, ...
     * 
     * 
     * @return
     */
    //@PermissionTarget
    @NotNull   
    @AccessType("field")
    @Column(name = "TARGET_", 
            nullable = false, 
            length = MAX_TARGET_LENGTH)
    public String getTarget() {
        return target;
    }
    public void setTarget(final String target) {
        this.target = target;
    }
    
    /**
     * identifier for a target instance, only used when the target is an instance
     * and not a class 
     * @return
     */
    @AccessType("field")
    @Column(name = "TARGET_ID_",
            nullable = true)
    public Long getTargetId() {
        return targetId;
    }
    public void setTargetId(final Long targetId) {
        this.targetId = targetId;
    }
   
  
    
    

    /**
     * the actions to be performed by the recipient on the target
     * 
     * @return
     */
    //@PermissionAction
    @NotNull   
    @AccessType("field")
    @Column(name = "ACTION_", nullable = false)
    public String getAction() {
        return action;
    }
    public void setAction(final String action) {
        // remove blanks, this is important for the selects
        this.action = StringUtils.deleteWhitespace(action);
    }

  
    
  

    @Transient
    public Boolean isRolePermission() {
        return ROLE.equals(discriminator);
    }

    @Transient
    public Boolean isUserPermission() {
        return USER.equals(discriminator);
    }
    
    

    /*
     * @Transient public Boolean isRoleOnlyPermission() { return
     * ROLEONLY.equals(discriminator); }
     * 
     * @Transient public Boolean isRoleOrRoleOnlyPermission() { return
     * ROLE.equals(discriminator) || ROLEONLY.equals(discriminator); }
     */
    /**
     * some description for the end user
     * 
     * @return
     */
    @AccessType("field")
    @Column(name = "DESCRIPTION_", 
            nullable = true, 
            length = MAX_DESCRIPTION_LENGTH)
    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description = description;
    }

}
