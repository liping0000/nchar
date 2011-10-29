package net.wohlfart.email.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;


/**
 * this class implements a email template with a set of receiver and a set of
 * translations, multiple email templates may have the same name the only unique
 * column is the primary key
 * 
 * 
 * @author Michael Wohlfart
 * 
 */

/* @formatter:off */
@NamedQueries({ 
    
    @NamedQuery(
            name = CharmsEmailTemplate.FIND_BY_NAME, 
            query = "from CharmsEmailTemplate where name = :name"),

    @NamedQuery(
            name = CharmsEmailTemplate.FIND_BY_ID, 
            query = "from CharmsEmailTemplate where id = :id"),

    @NamedQuery(
            name = CharmsEmailTemplate.FIND_BY_DELEGATE_ID, 
            query = "from CharmsEmailTemplate where mailDelegateId = :delegateId"),

    @NamedQuery(
            name = CharmsEmailTemplate.FIND_ID_BY_NAME, 
            query = "select id from CharmsEmailTemplate where name = :name"),

    @NamedQuery(
            name = CharmsEmailTemplate.FIND_SUBJECT_BY_ID, 
            query = "select subject from CharmsEmailTemplate where id = :id"),

    @NamedQuery(
            name = CharmsEmailTemplate.FIND_SENDER_EXPRESSION_BY_ID, 
            query = "select sender from CharmsEmailTemplate where id = :id") 
            
})
/* @formatter:on */


@Entity
@Table(name = "CHARMS_ETMPL")
public class CharmsEmailTemplate implements Serializable {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CharmsEmailTemplate.class);


    public static final String FIND_BY_NAME                 = "CharmsEmailTemplate.FIND_BY_NAME";
    public static final String FIND_BY_ID                   = "CharmsEmailTemplate.FIND_BY_ID";
    public static final String FIND_BY_DELEGATE_ID          = "CharmsEmailTemplate.FIND_BY_DELEGATE_ID";
    public static final String FIND_ID_BY_NAME              = "CharmsEmailTemplate.FIND_ID_BY_NAME";
    public static final String FIND_SUBJECT_BY_ID           = "CharmsEmailTemplate.FIND_SUBJECT_BY_ID";
    public static final String FIND_SENDER_EXPRESSION_BY_ID = "CharmsEmailTemplate.FIND_SENDER_EXPRESSION_BY_ID";


    public static final int MAX_SUBJECT_LENGTH      = 255;
    // oracle acts up with strings having more than 4000 chars, 
    // we use a clob/blobs for the mail body here
    public static final int MAX_BODY_LENGTH         = 5000;
    private static final int MAX_DESCRIPTION_LENGTH = 2024;

    private Long id;
    private Integer version;
    private Date lastModified;

    private String name;
    private String description;
    private Boolean enabled;

    private String sender;
    private List<CharmsEmailTemplateReceiver> receiver = new ArrayList<CharmsEmailTemplateReceiver>();

    // default content here:
    private String subject;
    private String body;

    private Map<String, CharmsEmailTemplateTranslation> translations = new HashMap<String, CharmsEmailTemplateTranslation>();

    // pattern is thread safe, matcher is not
    // see: http://www.javamex.com/tutorials/regular_expressions/thread_safety.shtml
    public static final Pattern TEMPLATE_PATTERN = Pattern.compile(
            "^\\s*"             // all kinds of whitespace at the beginning
            + "<template.*>"    // start tag might include xmlns etc.
            + "\\s*"            // whitespace or not
            + "(\\S+)"          // capture anything besides whitespace in group 1 \\S = [^\\s]
            + "\\s*"            // whitespace or not
            + "</template>"     // end tag
            + "\\s*$"           // all kinds of whitespace at the ending
    );

    // seems like this is no longer used
    public static String calculateTemplateName(final String configuration) {
        if (configuration == null) {
            return null;
        } else {
            final Matcher matcher = TEMPLATE_PATTERN.matcher(configuration);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }
    }

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
                            value = "CHARMS_ETMPL") })
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

    @Column(name = "ENABLED_")
    @Type(type = "yes_no")
    public Boolean getEnabled() {
        return enabled;
    }
    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    @Column(name = "LAST_MODIFIED_", nullable = false)
    public Date getLastModified() {
        return lastModified;
    }
    public void setLastModified(final Date lastModfied) {
        lastModified = lastModfied;
    }

    /**
     * we allow multiple templates with the same name
     * this means there are multiple emails sent when an email for
     * a non unique tempatename is triggered! 
     */
    @Column(name = "NAME_", unique = false)
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    // additional info from the user, just some plain text here
    @Column(name = "DESCRIPTION_", length = MAX_DESCRIPTION_LENGTH)
    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description = description;
    }

    // email sender this is an expression for the sender
    @Column(name = "SENDER_")
    public String getSender() {
        return sender;
    }
    public void setSender(final String sender) {
        this.sender = sender;
    }

    // "template" is the property in the target 
    // type (the CharmsEmailTemplateReceiver class)
    @OneToMany(mappedBy = "template", 
               fetch = FetchType.EAGER, 
               cascade = CascadeType.ALL)
    public List<CharmsEmailTemplateReceiver> getReceiver() {
        return receiver;
    }
    public void setReceiver(final List<CharmsEmailTemplateReceiver> receiver) {
        this.receiver = receiver;
    }

    // this is the default values without translation
    @Column(name = "SUBJECT_", length = MAX_SUBJECT_LENGTH)
    public String getSubject() {
        return subject;
    }
    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @Lob
    @Column(name = "BODY_", length = MAX_BODY_LENGTH)
    public String getBody() {
        return body;
    }
    public void setBody(final String body) {
        this.body = body;
    }

    // this is looked up in the CharmsEmailTemplateTranslation
    public static final String MAP_KEY_COLUMN = "LOCALE_ID_";

    @OneToMany(mappedBy = "template", 
               fetch = FetchType.EAGER, 
               cascade = CascadeType.ALL)
    @MapKeyColumn(name = MAP_KEY_COLUMN)
    public Map<String, CharmsEmailTemplateTranslation> getTranslations() {
        return translations;
    }
    public void setTranslations(final Map<String, CharmsEmailTemplateTranslation> translations) {
        this.translations = translations;
    }

}
