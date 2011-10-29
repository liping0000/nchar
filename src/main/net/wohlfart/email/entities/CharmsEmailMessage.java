package net.wohlfart.email.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;


/**
 * this class implements a sent email in the database, it has a single sender
 * and a single receiver
 * 
 * 
 * @author Michael Wohlfart
 * 
 */

@Entity
@Table(name = "CHARMS_EMSG")
public class CharmsEmailMessage implements Serializable {

    // content needs to be a lob in order to have more than 4000 charms on
    // oracle
    private static final int MAX_CONTENT_LENGTH = 4048; // 8096;
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_KEY_LENGTH = 255;


    private Long id;
    private Integer version;

    private String sender;
    private String receiver;
    private String subject;
    private String content;

    // might be something else but a business key
    private String key;

    private Date create;
    private Date sent;

    /**
     * @return generated unique id for this table
     * @formatter:off
     */
    @Id
    @GenericGenerator(
            name = "sequenceGenerator", 
            strategy = "org.hibernate.id.enhanced.TableGenerator", 
            parameters = { 
                    @Parameter(
                            name = "segment_value", 
                            value = "CHARMS_EMSG") })
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
    @Column(name = "VERSION_")
    @AccessType("field")
    public Integer getVersion() {
        return version;
    }

    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @Lob
    @Column(name = "CONTENT_", length = MAX_CONTENT_LENGTH)
    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        // the content may contain an error stacktrace which is longer than the
        // max length
        this.content = StringUtils.left(content, MAX_CONTENT_LENGTH);
    }

    @Column(name = "SUBJECT_", length = MAX_SUBJECT_LENGTH, nullable = false)
    // we don't want to send emails without subject
    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @Column(name = "RECEIVER_", nullable = false)
    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(final String receiver) {
        this.receiver = receiver;
    }

    @Column(name = "KEY_", length = MAX_KEY_LENGTH)
    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @Column(name = "SENT_")
    public Date getSent() {
        return sent;
    }

    public void setSent(final Date sent) {
        this.sent = sent;
    }

    @Column(name = "CREATE_", nullable = false)
    public Date getCreate() {
        return create;
    }

    public void setCreate(final Date create) {
        this.create = create;
    }

    @Column(name = "SENDER_", nullable = false)
    public String getSender() {
        return sender;
    }

    public void setSender(final String sender) {
        this.sender = sender;
    }

}
