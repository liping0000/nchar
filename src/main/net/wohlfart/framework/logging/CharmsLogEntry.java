package net.wohlfart.framework.logging;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * table to store uid information currently only used for the process id
 * 
 * FIXME: logging should use a independant connection/session tot he database...
 * 
 * @author Michael Wohlfart
 * 
 */

@Entity
@Table(name = "CHARMS_LOG")
public class CharmsLogEntry {

    public static final int MAX_MESSAGE_LENGTH = 1024;
    public static final int MAX_LOGGER_LENGTH  = 255;

    private Long            id;
    private Integer         version;

    private CharmsLogger    logger;
    private CharmsLogLevel  level;
    private String          message;
    private Date            date               = Calendar.getInstance().getTime();

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_LOG") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Version
    @Column(name = "VERSION_")
    public Integer getVersion() {
        return version;
    }

    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    // constructor for hibernates select call
    public CharmsLogEntry() {

    }

    public CharmsLogEntry(final CharmsLogger logger, final CharmsLogLevel level, final String message) {
        this.logger = logger;
        this.level = level;
        this.message = StringUtils.abbreviate(message, MAX_MESSAGE_LENGTH);
        date = Calendar.getInstance().getTime();
    }

    public CharmsLogEntry(final CharmsLogger logger, final CharmsLogLevel level, final String message, final Date date) {
        this.logger = logger;
        this.level = level;
        this.message = StringUtils.abbreviate(message, MAX_MESSAGE_LENGTH);
        this.date = date;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "LEVEL_", nullable = false, length = 10)
    public CharmsLogLevel getLevel() {
        return level;
    }

    public void setLevel(final CharmsLogLevel level) {
        this.level = level;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "LOGGER_", length = MAX_LOGGER_LENGTH, nullable = false)
    public CharmsLogger getLogger() {
        return logger;
    }

    public void setLogger(final CharmsLogger logger) {
        this.logger = logger;
    }

    @Column(name = "MESSSAGE_", length = MAX_MESSAGE_LENGTH, nullable = false)
    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = StringUtils.abbreviate(message, MAX_MESSAGE_LENGTH);
    }

    @Column(name = "DATE_", nullable = false)
    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

}
