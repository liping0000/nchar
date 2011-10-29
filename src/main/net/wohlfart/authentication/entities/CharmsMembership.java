package net.wohlfart.authentication.entities;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * A membership defines a relation between a user and a group
 * 
 * according to page 304 in persistence with hibernate
 * 
 * 
 * @author Michael Wohlfart
 */

/*  @formatter:off */
@NamedQueries({ 
    @NamedQuery(
        name = CharmsMembership.FIND_GROUP_IDS_BY_USER_ID, 
        query = "select m.id.roleId from CharmsMembership m where id.userId = :userId"),
        
    @NamedQuery(
        name = CharmsMembership.FIND_USER_IDS_BY_GROUP_ID, 
        query = "select m.id.userId from CharmsMembership m where id.roleId = :roleId") 
            
})
/*  @formatter:on */

@Entity
// not yet: @Audited
@Table(name = "CHARMS_MEMBERSHIP")
public class CharmsMembership implements Serializable {

    public static final String FIND_GROUP_IDS_BY_USER_ID = "CharmsMembership.FIND_GROUP_IDS_BY_USER_ID";
    public static final String FIND_USER_IDS_BY_GROUP_ID = "CharmsMembership.FIND_USER_IDS_BY_GROUP_ID";

    @Embeddable
    // removed static to check if this gives tomcats deserialization a chance to
    // recreate the session after restart, however this gives us
    // "No Default Constructor found" Exceptions
    public static class Id implements Serializable {

        @Column(name = "USER_ID_")
        private Long userId;

        @Column(name = "ROLE_ID_")
        private Long roleId;

        private Id() {
        }

        public Id(final Long userId, final Long roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        @Override
        public boolean equals(final Object o) {
            if ((o != null) && (o instanceof Id)) {
                final Id that = (Id) o;
                return userId.equals(that.userId) && roleId.equals(that.roleId);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return (userId.hashCode() + roleId.hashCode()) % Integer.MAX_VALUE;
        }
    }


    @EmbeddedId
    private Id id = new Id();


    @ManyToOne
    @JoinColumn(name = "USER_ID_", 
                insertable = false, 
                updatable = false, 
                nullable = false)
    private CharmsUser charmsUser;


    @ManyToOne
    @JoinColumn(name = "ROLE_ID_", 
                insertable = false, 
                updatable = false, 
                nullable = false)
    private CharmsRole charmsRole;

    /* additional columns in the join table:
    @Column(name = "ADDED_BY_")
    private String username;
    
    @Column(name = "ADDED_ON_")
    private Date dateAdded = new Date();
    */
    
    
    @SuppressWarnings("unused")
    private CharmsMembership() {
    }

    public CharmsMembership(
            final CharmsUser charmsUser, 
            final CharmsRole charmsRole) {

        // Set fields
        this.charmsUser = charmsUser;
        this.charmsRole = charmsRole;

        // Set identifier values
        id.userId = charmsUser.getId();
        id.roleId = charmsRole.getId();

        // Guarantee referential integrity
        charmsUser.getMemberships().add(this);
        charmsRole.getMemberships().add(this);
    }


    public CharmsUser getCharmsUser() {
        return charmsUser;
    }
    public void setCharmsUser(final CharmsUser charmsUser) {
        this.charmsUser = charmsUser;
    }


    public CharmsRole getCharmsRole() {
        return charmsRole;
    }
    public void setCharmsRole(final CharmsRole charmsRole) {
        this.charmsRole = charmsRole;
    }

    /*
     
      // some additional fields
     
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }


    public Date getDateAdded() {
        return dateAdded;
    }
    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }
    */
    

    
    // --- serializion tests...
    // we had some trouble with serializazion on tomcat6 restarts, manual
    // serialization seems to have resolved this...
    
    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(id.roleId);
        oos.writeObject(id.userId);
    }

    // assumes "static java.util.Date aDate;" declared
    private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        final Long roleId = (Long) ois.readObject();
        final Long userId = (Long) ois.readObject();
        // Read/initialize additional fields
        id = new Id(userId, roleId);
    }

}
