package net.wohlfart.auditing;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

// this class is mapped by a hbm.xml file
@RevisionEntity(CharmsRevisionListener.class)
public class CharmsRevisionEntity /* extends DefaultRevisionEntity */implements Serializable {

    private static final long serialVersionUID = -1L;

    @Id
    @GeneratedValue
    @RevisionNumber
    private Long              id;

    @RevisionTimestamp
    private Long              timestamp;

    private String            username;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Date getRevisionDate() {
        return new Date(timestamp);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CharmsRevisionEntity)) {
            return false;
        }

        final CharmsRevisionEntity that = (CharmsRevisionEntity) o;

        if (!id.equals(that.id)) {
            return false;
        }
        if (!timestamp.equals(that.timestamp)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "DefaultRevisionEntity(id = " + id + ", revisionDate = " + DateFormat.getDateTimeInstance().format(getRevisionDate()) + ")";
    }
}
