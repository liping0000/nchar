package net.wohlfart.email.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;


/**
 * email template tramslation contains body and subject of an email, n:1
 * relationship to the mail template
 * 
 * @author Michael Wohlfart
 * 
 */

@Entity
@Table(name = "CHARMS_ETMPL_TRANS")
public class CharmsEmailTemplateTranslation implements Serializable {


    private Long id;
    private Integer version;

    // used in the parents hash as key
    private String localeId;

    // the parent
    private CharmsEmailTemplate template;

    // translated content here:
    private String subject;
    private String body;

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
                            value = "CHARMS_ETMPL_TRANS") })
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
    @Column(name = "BODY_", 
            length = CharmsEmailTemplate.MAX_BODY_LENGTH)
    public String getBody() {
        return body;
    }
    public void setBody(final String body) {
        this.body = body;
    }

    @Column(name = "SUBJECT_", 
            length = CharmsEmailTemplate.MAX_SUBJECT_LENGTH)
    public String getSubject() {
        return subject;
    }
    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @ManyToOne
    @JoinColumn(name = "TEMPLATE_ID_", 
                nullable = false)
    // link to the parent
    public CharmsEmailTemplate getTemplate() {
        return template;
    }
    public void setTemplate(final CharmsEmailTemplate template) {
        this.template = template;
    }

    @Column(name = CharmsEmailTemplate.MAP_KEY_COLUMN, 
            nullable = false)
    public String getLocaleId() {
        return localeId;
    }
    public void setLocaleId(final String localeId) {
        this.localeId = localeId;
    }

}
