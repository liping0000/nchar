package net.wohlfart.authentication.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.persistence.UniqueConstraint;

import net.wohlfart.refdata.entities.ChangeRequestProduct;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.classic.Lifecycle;
import org.hibernate.envers.Audited;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotNull;
import org.hibernate.validator.Pattern;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * @author Michael Wohlfart
 */

// http://opensource.atlassian.com/projects/hibernate/secure/attachment/12728/patch.txt
// for an extended example of hibernate annotations


/*  @formatter:off */
@NamedQueries({
    @NamedQuery(
            name = CharmsRole.FIND_BY_ID, 
            query = "from CharmsRole where id = :id"),

    @NamedQuery(
          name = CharmsRole.FIND_BY_NAME, 
          query = "from CharmsRole where name = :name"),

    @NamedQuery(
          name = CharmsRole.FIND_NAME_BY_CLASSIFICATION_AND_NAME_LIKE, 
          query = "select distinct r.name from CharmsRole r where "
              +   " classification = :classification "
              +   " and lower(r.name) like concat('%', lower(:name), '%')"),

    @NamedQuery(
          name = CharmsRole.FIND_NAME_BY_NOT_CLASSIF_AND_NAME_LIKE, 
          query = "select distinct r.name from CharmsRole r where "
              +   " (classification != :classification or classification is null) "
              +   " and lower(r.name) like concat('%', lower(:name), '%')"),

    @NamedQuery(
          name = CharmsRole.FIND_BY_CLASSIFICATION_AND_NAME, 
          query = "from CharmsRole where classification = :classification and name = :name"),

    @NamedQuery(
          name = CharmsRole.FIND_ID_BY_NAME, 
          query = "select r.id from CharmsRole r where name = :name"),

    @NamedQuery(
          name = CharmsRole.FIND_ACTOR_ID_BY_NAME, 
          query = "select r.actorId from CharmsRole r where name = :name"),

    @NamedQuery(
          name = CharmsRole.FIND_NAME_BY_ID, 
          query = "select r.name from CharmsRole r where id = :id"),

    @NamedQuery(
          name = CharmsRole.FIND_NAME_BY_ACTOR_ID, 
          query = "select r.name from CharmsRole r where actorId = :actorId"),

    @NamedQuery(
          name = CharmsRole.FIND_BY_ACTOR_ID, 
          query = "from CharmsRole where actorId = :actorId") 

})
/*  @formatter:on  */


@Entity
// not yet: @Audited
@Name("charmsRole")
@Scope(ScopeType.CONVERSATION)
@Table(name = "CHARMS_ROLE")
public class CharmsRole implements Serializable, IActorIdHolder, Lifecycle, PreUpdateEventListener {

    // find the CharmsRole
    public static final String FIND_BY_ID                                = "CharmsRole.FIND_BY_ID";
    public static final String FIND_BY_NAME                              = "CharmsRole.FIND_BY_NAME";
    public static final String FIND_BY_CLASSIFICATION_AND_NAME           = "CharmsRole.FIND_BY_CLASSIFICATION_AND_NAME";
    public static final String FIND_BY_ACTOR_ID                          = "CharmsRole.FIND_BY_ACTOR_ID";
    // find the name
    public static final String FIND_NAME_BY_ID                           = "CharmsRole.FIND_NAME_BY_ID";
    public static final String FIND_NAME_BY_ACTOR_ID                     = "CharmsRole.FIND_NAME_BY_ACTOR_ID";
    // misc
    public static final String FIND_NAME_BY_CLASSIFICATION_AND_NAME_LIKE = "CharmsRole.FIND_NAME_BY_CLASSIFICATION_AND_NAME_LIKE";
    public static final String FIND_NAME_BY_NOT_CLASSIF_AND_NAME_LIKE    = "CharmsRole.FIND_NAME_BY_NOT_CLASSIF_AND_NAME_LIKE";
    public static final String FIND_ID_BY_NAME                           = "CharmsRole.FIND_ID_BY_NAME";
    public static final String FIND_ACTOR_ID_BY_NAME                     = "CharmsRole.FIND_ACTOR_ID_BY_NAME";

    // prefix to distinguish user and role actor ids in jbpm
    public static final String GROUP_ACTOR_PREFIX             = "Group:";

    public static final int MAX_DESCRIPTION_LENGTH = 2024;
    public static final int MAX_NAME_LENGTH = 50;


    // hibernate internal
    private Long id;
    private Integer version;

    // groupname
    private String name;
    // string for the workflow engine, unique for user and groups/roles
    private String actorId;
    // generated
    private String label;


    private String description;

    // roles contained in this role, turns this role into a group of roles...
    // see:  http://communities.bmc.com/communities/docs/DOC-9902
    // A materialized path is a technique for encoding a tree in a flat data structure
    //private String materializedPath;                                                            
    // subroles of this role, the permission of this role are inherited to the
    // subroles...
    private Set<CharmsRole> upstream = new HashSet<CharmsRole>();
    private Set<CharmsRole> downstream = new HashSet<CharmsRole>();
    //private CharmsRole            downstream;
    // conditional roles can not be statically granded to a user, they are
    // probably rule based
    // or for example a role that is granded for all users in a certain subnet
    // or to users on a special weekday, or temporary granded if they get a
    // certain task assigned

    // an application managed set of contained roles
    // SQL doesn't allow to query recursive data structures, so we need to
    // flatten the tree in order to access all contained subroles
    private Set<CharmsRole> contained = new HashSet<CharmsRole>();
    private Set<CharmsRole> container = new HashSet<CharmsRole>();

    private Boolean conditional = false;

    // organizational roles are visible to the end user in certain selects

    //private Boolean organizational = false;
    private RoleClassification classification = RoleClassification.DEFAULT;

    // users which are members of this role
    private Set<CharmsMembership> memberships = new HashSet<CharmsMembership>();

    /**
     * @return generated unique id for this table
     * @formatter:off
     */
    @Id
    @GenericGenerator(
            name = "sequenceGenerator", 
            strategy = "net.wohlfart.authentication.entities.CharmsActorIdGenerator", 
            parameters = { 
                    @Parameter(
                            name = "segment_value", 
                            value = "CHARMS_ROLE") })
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
     * the role name this is also the recipient in the permission definition so
     * we need 255 to have equal columns in the DB
     * 
     * @return login
     */
    @Override
    @NotNull
    @Pattern(regex = "[0-9a-zA-Z._\\ -]*",
            message = "Invalid Character in role name")
    @Length(min = 2, max = MAX_NAME_LENGTH)
    @AccessType("field")
    @Column(name = "NAME_", 
            unique = true, 
            nullable = false, 
            length = MAX_NAME_LENGTH)
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * needed by the actorId generator to create a 
     * user and role unique id
     */
    @Override
    @Transient
    public String getActorIdPrefix() {
        return GROUP_ACTOR_PREFIX;
    }

    /**
     * @return
     */
    @Override
    @AccessType("field")
    @Column(name = "GRP_ACTOR_ID_", 
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
     * Role groups or upstream roles are simply groups of roles, they allow 
     * roles to become members of other roles. For example, if your application 
     * requires a superuser role that should have all the privileges of the user 
     * role plus some extra higher level privileges, then you could use a role 
     * group for this (i.e. make superuser a member of the user group).
     */
    @ManyToMany(targetEntity = CharmsRole.class)
    @JoinTable(name = "CHARMS_UPSTRM_ROLES", 
            uniqueConstraints = @UniqueConstraint(columnNames = {"UP_ID_", "DOWN_ID_"}),
            joinColumns = @JoinColumn(name = "DOWN_ID_", nullable = false, updatable = false), 
            inverseJoinColumns = @JoinColumn(name = "UP_ID_", nullable = false, updatable = false)
    )
    @Cascade({ CascadeType.REFRESH, CascadeType.SAVE_UPDATE, CascadeType.MERGE })
    @OrderBy("name")
    public Set<CharmsRole> getUpstream() {
        return upstream;
    }
    public void setUpstream(final Set<CharmsRole> upstream) {
        this.upstream = upstream;
    }
    /** reverse side of the 1 level role hierarchy  */
    @ManyToMany(targetEntity = CharmsRole.class, mappedBy="upstream")
    @Cascade({ CascadeType.REFRESH, CascadeType.SAVE_UPDATE, CascadeType.MERGE })
    public Set<CharmsRole> getDownstream() {
        return downstream;
    }
    public void setDownstream(final Set<CharmsRole> downstream) {
        this.downstream = downstream;
    }


    // the flattened structure of the role hierarchy
    @ManyToMany(targetEntity = CharmsRole.class)
    @JoinTable(name = "CHARMS_INCL_ROLES", 
            uniqueConstraints = @UniqueConstraint(columnNames = {"INCL_ID_", "MAIN_ID_"}),
            joinColumns = @JoinColumn(name = "MAIN_ID_", nullable = false, updatable = false), 
            inverseJoinColumns = @JoinColumn(name = "INCL_ID_", nullable = false, updatable = false)
    )
    @Cascade({ CascadeType.REFRESH, CascadeType.SAVE_UPDATE, CascadeType.MERGE })
    @OrderBy("name")
    public Set<CharmsRole> getContained() {
        return contained;
    }
    public void setContained(final Set<CharmsRole> contained) {
        this.contained = contained;
    }
    
    @ManyToMany(targetEntity = CharmsRole.class, mappedBy="contained")
    @Cascade({ CascadeType.REFRESH, CascadeType.SAVE_UPDATE, CascadeType.MERGE })
    public Set<CharmsRole> getContainer() {
        return container;
    }
    public void setContainer(final Set<CharmsRole> container) {
        this.container = container;
    }


    /**
     * @return conditional is a role that may be assigned to a user
     * based on some roles or other external conditions
     */
    @Type(type = "yes_no")
    @AccessType("field")
    @Column(name = "CONDITIONAL_")
    public Boolean getConditional() {
        return conditional;
    }
    public void setConditional(final Boolean conditional) {
        this.conditional = conditional;
    }

    /**
     * @return
     
    @AccessType("field")
    @Column(name = "ORGANIZATIONAL_")
    @Type(type = "yes_no")
    @Deprecated
    public Boolean getOrganizational() {
        return organizational;
    }
    public void setOrganizational(final Boolean organizational) {
        this.organizational = organizational;
    }
    */
    
    @Enumerated(EnumType.STRING)
    @AccessType("field")
    @Column(name = "CLASSIFICATION_", nullable = true, length = 20)
    public RoleClassification getClassification() {
        return classification;
    }
    public void setClassification(final RoleClassification classification) {
        this.classification = classification;
    }



    /** @return membership, linking users to groups */
    @OneToMany(mappedBy = "charmsRole")
    @AccessType("field")
    @Cascade({ CascadeType.DELETE /*, CascadeType.SAVE_UPDATE, CascadeType.MERGE*/ }) // delete membership if we delete the group
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
    public void calculateLabel() {
        label = name + " (" + memberships.size() + ")";
    }

    @Transient
    public void calculateContainedRoles(Session hibernateSession) {
        calculateContainedRoles(new HashSet<Long>(), hibernateSession);
    }
    
    @Transient
    public void calculateContainedRoles(Set<Long> visitedIds, Session hibernateSession) {
        if (visitedIds.contains(this.id)) {
            return;
        } else {
            visitedIds.add(this.id);
        }
        //hibernateSession..refresh(this);

        // collect all upstream contained roles ids
        Set<CharmsRole> newContained = new HashSet<CharmsRole>();
        for (CharmsRole role : upstream) {
            //hibernateSession.refresh(role);
            newContained.addAll(role.getContained());
            newContained.add(role);
        }

        // calculate the ids and remove what is no longer in there
        HashSet<Long> newContainedIds = new HashSet<Long>();
        for (CharmsRole role : newContained) {
            newContainedIds.add(role.getId());
        }
        Iterator<CharmsRole> oldIter = contained.iterator();
        while (oldIter.hasNext()) {
            CharmsRole role = oldIter.next();
            //hibernateSession.refresh(role);
            if (newContainedIds.contains(role.getId())) {
                // role stays, is already in the set
            } else {
                // role is not in the new set, has to go
                oldIter.remove();
            }
        }

        // add the new roles
        HashSet<Long> oldContainedIds = new HashSet<Long>();
        for (CharmsRole role : contained) {
            //hibernateSession.refresh(role);
            oldContainedIds.add(role.getId());
        }
        Iterator<CharmsRole> newIter = newContained.iterator();
        while (newIter.hasNext()) {
            CharmsRole role = newIter.next();
            if (oldContainedIds.contains(role.getId())) {
                // role is already in there              
            } else {
                // not yet in there, new role, add it
                //hibernateSession.refresh(role);
                contained.add(role);
            }
        }

        // propagate to downstream that something changed here
        HashSet<Long> currentContainedIds = new HashSet<Long>();
        for (CharmsRole role : contained) {
            currentContainedIds.add(role.getId());
        }
        for (CharmsRole role : downstream) {
            role.calculateContainedRoles(visitedIds, hibernateSession);
        }        
    }
    
    

    // this seems to be pretty pointless, we collect all users
    // of all upstream roles
    @Transient
    public Set<CharmsUser> getAllMemberUsers() {
        final Set<CharmsUser> users = new HashSet<CharmsUser>();
        collectAllMemberUsers(users);
        return users;
    }

    @Transient
    private void collectAllMemberUsers(final Set<CharmsUser> current) {
        final Set<CharmsMembership> memberships = getMemberships();
        for (final CharmsMembership membership : memberships) {
            current.add(membership.getCharmsUser());
        }
        final Set<CharmsRole> roles = getUpstream();
        for (final CharmsRole role : roles) {
            role.collectAllMemberUsers(current);
        }
    }

    @Transient
    public Set<CharmsUser> getMemberUsers() {
        final Set<CharmsUser> users = new HashSet<CharmsUser>();
        collectMemberUsers(users);
        return users;
    }

    @Transient
    private void collectMemberUsers(final Set<CharmsUser> current) {
        final Set<CharmsMembership> memberships = getMemberships();
        for (final CharmsMembership membership : memberships) {
            current.add(membership.getCharmsUser()); // sets never contain the
            // same object twice
            // ...if equals is implemented correct
        }
    }

    // we implement the lifecycle interface here since PrePersist/PreUpdate
    // won't work with hibernate session, only with JTA
    // see:
    // http://www.javabeat.net/articles/9-interceptors-in-hibernate-orm-framework-an-introducti-2.html
    // the only reason for this is to calculate the label
    @Override
    public boolean onDelete(final Session s) throws CallbackException {
        return NO_VETO;
    }

    @Override
    public void onLoad(final Session s, final Serializable id) {
        // nothing to do here
    }

    @Override
    public boolean onSave(final Session s) throws CallbackException {
        calculateLabel();
        calculateContainedRoles(s);
        return NO_VETO;
    }

    /**
     * Note that onUpdate() is not called every time the object's persistent 
     * state is updated. It is called only when a transient object is passed 
     * to Session.update()
     * -> "update" means update the object in the session
     * 
     * see: http://docs.atlassian.com/hibernate2/2.1.8/reference/persistent-classes.html
     */
    @Override
    public boolean onUpdate(final Session s) throws CallbackException {
        //calculateLabel();
        //calculateContainedRoles();
        return NO_VETO;
    }

    
    // org.hibernate.AssertionFailure: collection [net.wohlfart.authentication.entities.CharmsRole.contained] was not processed by flush()
    // see: http://www.jroller.com/jshingler/entry/org_hibernate_assertionfailure_collection_was
    // see: http://stackoverflow.com/questions/1701600/hibernate-gorm-collection-was-not-processed-by-flush

    /**
     * this method is called by our custom preUpdate listener and is called whenever
     * the database is updated with new properties from the session, note this
     * has nothing to do with the onUpdate calls 
     * -> "update" means update the database
     */
    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        calculateLabel();
        //calculateContainedRoles();
        //event.getSession().update(this);
        //event.getEntity().
        return NO_VETO;
    }
 
}
