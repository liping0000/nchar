package net.wohlfart.authentication.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.authorization.CustomHash;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.classic.Lifecycle;
//import org.hibernate.envers.Audited;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotNull;
import org.hibernate.validator.Pattern;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

// see: http://www.hibernate.org/hib_docs/validator/reference/en/html_single/
// see: http://www.jasypt.org/seam2.html for transparent encrypting database
// fields

// see:
// http://depressedprogrammer.wordpress.com/2008/06/24/jsf-seam-validation-custom-messages-annotations-internationalization/
// for form validation of hibernate annotations

// see: http://www.hibernate.org/hib_docs/annotations/reference/en/html_single/
// for result mappings, named queries are checked on startup


/**
 * @author Michael Wohlfart
 */

/*  @formatter:off */
@NamedQueries({
    @NamedQuery(
            name = CharmsUser.FIND_BY_NAME, 
            query = "from CharmsUser where name = :name"),
            
    @NamedQuery(
            name = CharmsUser.FIND_ID_BY_NAME, 
            query = "select u.id from CharmsUser u where name = :name"),
            
    @NamedQuery(
            name = CharmsUser.FIND_ACTOR_ID_BY_NAME, 
            query = "select u.actorId from CharmsUser u where name = :name"),
            
    @NamedQuery(
            name = CharmsUser.FIND_NAME_BY_ID, 
            query = "select u.name from CharmsUser u where id = :id"),
            
    @NamedQuery(
            name = CharmsUser.FIND_NAME_BY_ACTOR_ID, 
            query = "select u.name from CharmsUser u where actorId = :actorId"),
            
    @NamedQuery(
            name = CharmsUser.FIND_BY_ID, 
            query = "from CharmsUser where id = :id"),
            
    @NamedQuery(
            name = CharmsUser.FIND_ID_BY_FULLNAME, 
            query = "select id from CharmsUser where concat(concat(firstname, ' '), lastname)  = :fullname"),
            
    @NamedQuery(
            name = CharmsUser.FIND_FULLNAME_BY_ACTOR_ID, 
            query = "select concat(concat(firstname, ' '), lastname) from CharmsUser where actorId = :actorId"),
            
    @NamedQuery(
            name = CharmsUser.FIND_FULLNAME_BY_ID, 
            query = "select concat(concat(firstname, ' '), lastname) from CharmsUser where id = :id"),
            
    @NamedQuery(
            name = CharmsUser.FIND_BY_ACTOR_ID, 
            query = "from CharmsUser where actorId = :actorId"),
            
    @NamedQuery(
            name = CharmsUser.FIND_BY_ACTOR_IDS, 
            query = "from CharmsUser where actorId in ( :actorIds )"),
            
    @NamedQuery(
            name = CharmsUser.COUNT_QUERY, 
            query = "select count(*) from CharmsUser"), // this
                                                                                               
    // return all user in the pool or with the actorid, distinct: we only want the user once
    @NamedQuery(
            name = CharmsUser.FIND_BY_GROUP_OR_ACTOR_IDS, 
            query = "select distinct u "            
                  + " from CharmsUser u " 
                  + "   where u.actorId in ( :actorIds )" 
                  + "     or u.id in ("
                  + "       select m.charmsUser.id "
                  + "          from CharmsMembership m" 
                  + "           where m.charmsRole.actorId in ( :actorIds )" 
                  + "     )"), 
    // find all userids within a named group
    @NamedQuery(
            name = CharmsUser.FIND_IDS_BY_GROUP_NAME, 
            query = "select distinct m.charmsUser.id " 
                + "  from CharmsMembership m"
                + "    where m.charmsRole.name = :name " 
                + "  )") 
})
/*  @formatter:on  */


@Entity
//@Audited
@Name("charmsUser")
@Scope(ScopeType.CONVERSATION)
@Table(name = "CHARMS_USER")
public class CharmsUser implements Serializable, IActorIdHolder, Lifecycle, PreUpdateEventListener {

    // find the CharmsUser
    public static final String FIND_BY_GROUP_OR_ACTOR_IDS = "CharmsUser.FIND_BY_GROUP_OR_ACTOR_IDS";
    public static final String FIND_BY_ACTOR_IDS          = "CharmsUser.FIND_BY_ACTOR_IDS";
    public static final String FIND_BY_ACTOR_ID           = "CharmsUser.FIND_BY_ACTOR_ID";
    public static final String FIND_BY_NAME               = "CharmsUser.FIND_BY_NAME";
    public static final String FIND_BY_ID                 = "CharmsUser.FIND_BY_ID";
    // find the name
    public static final String FIND_NAME_BY_ID            = "CharmsUser.FIND_NAME_BY_ID";
    public static final String FIND_NAME_BY_ACTOR_ID      = "CharmsUser.FIND_NAME_BY_ACTOR_ID";
    // find ids
    public static final String FIND_ID_BY_NAME            = "CharmsUser.FIND_ID_BY_NAME";
    public static final String FIND_IDS_BY_GROUP_NAME     = "CharmsUser.FIND_IDS_BY_GROUP_NAME";
    public static final String FIND_ID_BY_FULLNAME        = "CharmsUser.FIND_ID_BY_FULLNAME";
    // find fullname
    public static final String FIND_FULLNAME_BY_ACTOR_ID  = "CharmsUser.FIND_FULLNAME_BY_ACTOR_ID";
    public static final String FIND_FULLNAME_BY_ID        = "CharmsUser.FIND_FULLNAME_BY_ID";
    // find actor id
    public static final String FIND_ACTOR_ID_BY_NAME      = "CharmsUser.FIND_ACTOR_ID_BY_NAME";
    // count
    public static final String COUNT_QUERY                = "CharmsUser.COUNT_QUERY";

    // prefix to distinguish user and role actor ids in jbpm
    public static final String ACTOR_PREFIX               = "Actor:";

    // set to "none" to use unencrypted passwords:
    // public static final String PASSWORD_HASH_FUNCTION = "none";
    public static final String PASSWORD_HASH_FUNCTION     = CustomHash.ALGORITHM_SSHA;

    // some constants used all over the entities
    public static final int MAX_DESCRIPTION_LENGTH = 2024;
    public static final int MAX_LOGIN_LENGTH = 20;
    public static final int MAX_FIRSTNAME_LENGTH = 60;
    public static final int MAX_LASTNAME_LENGTH = 60;
    public static final int MAX_EMAIL_LENGTH = 100;
    // default password when creating users without a password or a null
    // password
    // it is not a valid password hash so the user will never be able to log in
    public static final String LOCKED_PASSWD              = "--locked--";


    // hibernate internal
    private Long id;
    private Integer version;

    // login / accountname
    private String name;
    // string for the workflow engine, unique for user and groups/roles
    private String actorId;
    // generated
    private String label;

    
    private String passwd;

    // must be non null for the IdentityManager
    private Boolean enabled = true;                                   

    private Gender gender;
    private String firstname;
    private String lastname;
    private String email;

    // private Set<CharmsRole> roles = new HashSet<CharmsRole>();

    private Set<CharmsMembership> memberships = new HashSet<CharmsMembership>();

    // additional stuff every user needs
    private String timezoneId;
    private String themeId;
    private String localeId;


    // need a new password
    private Date credentialsExpire;

    private Date accountExpire;


    private Boolean unlocked = true;


    private String externalId1;

    private String externalId2;


    private String description;

    /** @return generated unique id for this entity */
    @Id
    @GenericGenerator(
            name = "sequenceGenerator", 
            strategy = "net.wohlfart.authentication.entities.CharmsActorIdGenerator", 
            parameters = { 
                    @Parameter(
                            name = "segment_value", 
                            value = "CHARMS_USER") })
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
     * @return name, this is also the user principal according to the seam IDM
     *         as well as the recipient in the permission table, so we need 255
     *         chars identity.getPrincipal().getName() used in the session
     *         initializer returns this value
     */
    @Override
    @NotNull
    @Pattern(regex = "[0-9a-zA-Z._\\ -]*", 
             message = "Invalid Character in name")
    @Length(min = 2, max = MAX_LOGIN_LENGTH,
              message = "size must be more than {min} and less than {max}")
    @AccessType("field")
    @Column(name = "NAME_", 
            unique = true, 
            nullable = false, 
            length = MAX_LOGIN_LENGTH)
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    @Transient
    public String getActorIdPrefix() {
        return ACTOR_PREFIX;
    }

    /**
     * @return
     */
    @Override
    @AccessType("field")
    @Column(name = "ACTOR_ID_", 
            unique = true, 
            nullable = false, 
            length = CharmsActorIdGenerator.ACTOR_ID_SIZE)
    public String getActorId() {
        return actorId;
    }
    @Override
    public void setActorId(final String actorId) {
        this.actorId = actorId;
    }

    /**
     * @return encrypted/digested passwd password 
     * may be null which means the user can't log in 
     */
    @AccessType("field")
    @Column(name = "PASSWD_", nullable = true, length = 100)
    // passwd will be encrypted if we use the identity manager, this is a seam
    // feature
    // we don't use yet
    // @UserPassword(hash = PASSWORD_HASH_FUNCTION)
    // @Pattern(regex="[0-9a-zA-Z]*") the password contains a tag if it is
    // encrypted, see Hash
    // this constraint doesn't make any sense since we hash the password
    // anyways...
    // @Length(min=6,max=100,
    // message="Pasword length must be between 6..50 charcters")
    public String getPasswd() {
        return passwd;
    }
    public void setPasswd(final String passwd) {
        this.passwd = passwd;
    }

    /**
     * the column may be null in a broken DB, that's why we we don't use
     * primitive types here
     * 
     * @return
     */
    @AccessType("field")
    @Column(name = "IS_ENABLED_")
    @Type(type = "yes_no")
    public Boolean getEnabled() {
        return enabled;
    }
    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return
     */
    @AccessType("field")
    @Column(name = "IS_UNLOCKED_")
    @Type(type = "yes_no")
    public Boolean getUnlocked() {
        return unlocked;
    }
    public void setUnlocked(final Boolean unlocked) {
        this.unlocked = unlocked;
    }

    /** @return membership, linking users to groups */
    @OneToMany(mappedBy = "charmsUser")
    @AccessType("field")
    @Cascade({ CascadeType.DELETE }) // delete memberships if we delete the user
    public Set<CharmsMembership> getMemberships() {
        return memberships;
    }
    /** @param memberships */
    public void setMemberships(final Set<CharmsMembership> memberships) {
        this.memberships = memberships;
    }

    /**
     * @return
     */
    @Enumerated(EnumType.STRING)
    @AccessType("field")
    @Column(name = "GENDER_", nullable = true, length = 10)
    public Gender getGender() {
        return gender;
    }

    /**
     * @param gender
     */
    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    /**
     * @return
     */
    // @UserFirstName
    @Length(max = MAX_FIRSTNAME_LENGTH)
    @AccessType("field")
    @Column(name = "FIRSTNAME_", nullable = true, length = MAX_FIRSTNAME_LENGTH)
    public String getFirstname() {
        return firstname;
    }

    /**
     * @param firstname
     */
    public void setFirstname(final String firstname) {
        this.firstname = firstname;
    }

    /**
     * @return
     */
    // @UserLastName
    @Length(max = MAX_LASTNAME_LENGTH)
    @AccessType("field")
    @Column(name = "LASTNAME_", nullable = true, length = MAX_LASTNAME_LENGTH)
    public String getLastname() {
        return lastname;
    }

    /**
     * @param lastname
     */
    public void setLastname(final String lastname) {
        this.lastname = lastname;
    }

    /**
     * @return
     */
    @AccessType("field")
    @Column(name = "EMAIL_", nullable = true, length = MAX_EMAIL_LENGTH)
    public String getEmail() {
        return email;
    }

    /**
     * @param email
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * @return
     */
    @Length(max = 100)
    @AccessType("field")
    @Column(name = "TIMEZONE_ID_", nullable = true, length = 100)
    public String getTimezoneId() {
        return timezoneId;
    }

    /**
     * @param timezoneId
     */
    public void setTimezoneId(final String timezoneId) {
        this.timezoneId = timezoneId;
    }

    /**
     * @return
     */
    @Length(max = 100)
    @AccessType("field")
    @Column(name = "THEME_ID_", nullable = true, length = 100)
    public String getThemeId() {
        return themeId;
    }

    /**
     * @param themeId
     */
    public void setThemeId(final String themeId) {
        this.themeId = themeId;
    }

    /**
     * the locale id is a string that consists of a language, acountry and a
     * variant, see the localeSelector for more
     * 
     * @return
     */
    @Length(max = 100)
    @AccessType("field")
    @Column(name = "LOCALE_ID_", nullable = true, length = 100)
    public String getLocaleId() {
        return localeId;
    }

    /**
     * @param localeId
     */
    public void setLocaleId(final String localeId) {
        this.localeId = localeId;
    }

    /**
     * @return
     */
    @AccessType("field")
    @Column(name = "CREDENTIALS_EXPIRE_", nullable = true)
    public Date getCredentialsExpire() {
        if (credentialsExpire == null) {
            return null;
        } else {
            return new Date(credentialsExpire.getTime());
        }
    }

    /**
     * @param credentialsExpire
     */
    public void setCredentialsExpire(final Date credentialsExpire) {
        if (credentialsExpire == null) {
            this.credentialsExpire = null;
        } else {
            this.credentialsExpire = new Date(credentialsExpire.getTime());
        }
    }

    /**
     * @return
     */
    @AccessType("field")
    @Column(name = "ACCOUNT_EXPIRE_", nullable = true)
    public Date getAccountExpire() {
        if (accountExpire == null) {
            return null;
        } else {
            return new Date(accountExpire.getTime());
        }
    }

    /**
     * @param accountExpire
     */
    public void setAccountExpire(final Date accountExpire) {
        if (accountExpire == null) {
            this.accountExpire = null;
        } else {
            this.accountExpire = new Date(accountExpire.getTime());
        }
    }

    /**
     * @return
     */
    @AccessType("field")
    @Column(name = "EXTERNAL_ID1_", nullable = true)
    public String getExternalId1() {
        return externalId1;
    }

    /**
     * @param externalId1
     */
    public void setExternalId1(final String externalId1) {
        this.externalId1 = externalId1;
    }

    /**
     * @return
     */
    @AccessType("field")
    @Column(name = "EXTERNAL_ID2_", nullable = true)
    public String getExternalId2() {
        return externalId2;
    }

    /**
     * @param externalId2
     */
    public void setExternalId2(final String externalId2) {
        this.externalId2 = externalId2;
    }

    /**
     * @return
     */
    @AccessType("field")
    @Column(name = "DESCRIPTION_", length = MAX_DESCRIPTION_LENGTH)
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return
     */
    @Override
    @AccessType("field")
    @Column(name = "LABEL_")
    public String getLabel() {
        return label;
    }

    // this property is created on save/update events
    /**
     * @param label
     */
    @SuppressWarnings("unused")
    private void setLabel(final String label) {
        this.label = label;
    }

    @Transient
    @Deprecated
    // use getLabel
    public String getFullname() {
        if ((firstname == null) && (lastname == null)) {
            return name;
        } else if (lastname == null) {
            return firstname;
        } else if (firstname == null) {
            return lastname;
        } else {
            return firstname + " " + lastname;
        }
    }

    /**
     * this is called on save and on update
     */
    private void calculateLabel() {
        if ((firstname == null) && (lastname == null)) {
            label = name;
        } else if (lastname == null) {
            label = firstname;
        } else if (firstname == null) {
            label = lastname;
        } else {
            label = firstname + " " + lastname;
        }
    }

    // we implement the lifecycle interface here since PrePersist/PreUpdate
    // won't work with hibernate session, only with JTA
    // see:
    // http://www.javabeat.net/articles/9-interceptors-in-hibernate-orm-framework-an-introducti-2.html
    @Override
    public boolean onDelete(final Session s) throws CallbackException {
        return NO_VETO;
    }

    @Override
    public void onLoad(final Session s, final Serializable id) {
        // nothing special to do on load
    }

    @Override
    public boolean onSave(final Session s) throws CallbackException {
        calculateLabel();
        return NO_VETO;
    }

    /**
     * Note that onUpdate() is not called every time the object's persistent 
     * state is updated. It is called only when a transient object is passed 
     * to Session.update()
     * -> "update" means update the entity in the session
     * 
     * see: http://docs.atlassian.com/hibernate2/2.1.8/reference/persistent-classes.html
     */
    @Override
    public boolean onUpdate(final Session s) throws CallbackException {
        calculateLabel();
        return NO_VETO;
    }
    
    /**
     * this method is called by our custom preUpdate listener and is called whenever
     * the database is updated with new properties from the session, note this
     * has nothing to do with the onUpdate calls 
     * -> "update" means update the database
     */
    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        calculateLabel();
        return NO_VETO;
    }
    
    @Override
    public String toString() {
        return name + " [" + id + "]";
    }
    
}
