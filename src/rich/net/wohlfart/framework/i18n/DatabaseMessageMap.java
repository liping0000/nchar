package net.wohlfart.framework.i18n;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.validator.Length;

/**
 * see: http://relation.to/2303.lace
 * http://lists.jboss.org/pipermail/jboss-user/2007-September/082671.html
 * 
 * caching: http://docs.jboss.org/seam/1.2.0.PATCH1/reference/en/html/cache.html
 * for database stored messages in seam
 * 
 * @author Michael Wohlfart
 * 
 */

/*  @formatter:off */
@NamedQueries({
     @NamedQuery(
          name = DatabaseMessageMap.FIND_BY_NAME_AND_LOCALE_ID, 
          query = "from DatabaseMessageMap where name = :name and localeId = :localeId"),
          
     @NamedQuery(
          name = DatabaseMessageMap.DELETE_BY_NAME_AND_MSG_CODE, 
          query = "delete from DatabaseMessageMap where name = :name and messageCode = :messageCode") 
})
/*  @formatter:on */

@Entity
@Table(name = "CHARMS_MSGBUNDLE", uniqueConstraints = { @UniqueConstraint(name = "UC_MSGBNMLD", columnNames = { "NAME_", "LOCALE_ID_" }) })
public class DatabaseMessageMap {

    // find a bundle for a locale, (e.g. get the bunle for the current locale to
    // do translations)
    public static final String  FIND_BY_NAME_AND_LOCALE_ID  = "DatabaseMessageMap.FIND_BY_NAME_AND_LOCALE_ID";

    // delete all translations from a bundle with the message code (e.g. for
    // business object deletion)
    public static final String  DELETE_BY_NAME_AND_MSG_CODE = "DatabaseMessageMap.DELETE_BY_NAME_AND_MSG_CODE";

    private Long                id;
    private Integer             version;
    private String              name;
    private String              localeId;
    private Map<String, String> messages                    = new HashMap<String, String>();

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_MSGBUNDLE") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    @Version
    @Column(name = "VERSION_")
    private Integer getVersion() {
        return version;
    }

    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @Column(name = "NAME_", nullable = false, length = 100)
    @Length(max = 100)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * localeId consists of:
     * 
     * - language which is a valid ISO Language Code. These codes are the
     * lower-case, two-letter codes as defined by ISO-639 (e.g. en) see:
     * http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt
     * 
     * - country which is a valid ISO Country Code (e.g. US, AU, ) see:
     * http://userpage.chemie.fu-berlin.de/diverse/doc/ISO_3166.html
     * 
     * - variant which is a vendor or browser-specific code. For example, use
     * WIN for Windows, MAC for Macintosh, and POSIX for POSIX (e.g.
     * Traditional, WIN)
     * 
     * see: http://java.sun.com/j2se/1.4.2/docs/api/java/util/Locale.html
     * 
     * @return
     */
    @Column(name = "LOCALE_ID_", nullable = false, length = 20)
    @Length(max = 20)
    public String getLocaleId() {
        return localeId;
    }

    public void setLocaleId(final String localeId) {
        this.localeId = localeId;
    }

    /*
     * we need a non lazy load here, otherwise we get
     * org.hibernate.LazyInitializationException in the CustomResourceLoader
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Cascade({ CascadeType.ALL })
    @JoinTable(name = "CHARMS_MESSAGES", 
            joinColumns = @JoinColumn(name = "MSG_BUNDLE_ID_"))
    // @MapKey(columns={@Column(name="MSG_CODE_", length=250, nullable=false)})
    @MapKeyColumn(name = "MSG_CODE_", length = 250, nullable = false)
    @Column(name = "VALUE_", length = 2500, nullable = false)
    public Map<String, String> getMessages() {
        return messages;
    }

    public void setMessages(final Map<String, String> messages) {
        this.messages = messages;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((localeId == null) ? 0 : localeId.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DatabaseMessageMap other = (DatabaseMessageMap) obj;
        if (localeId == null) {
            if (other.localeId != null) {
                return false;
            }
        } else if (!localeId.equals(other.localeId)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
