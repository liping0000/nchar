package net.wohlfart.changerequest.entities;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import net.wohlfart.framework.entities.CharmsUid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*  @formatter:off */
@NamedQueries({ 
    
    @NamedQuery(
          name = ChangeRequestBusinessKey.FIND_LAST_VALUE, 
          query = "from ChangeRequestBusinessKey " 
              + "   where sortIndex = ( "
              + "         select max(sortIndex) from ChangeRequestBusinessKey " 
              + "           where prefix = :prefix " 
              + "             and location = :location "
              + "             and year = :year " 
              + "   ) " 
              + "   and prefix = :prefix " 
              + "   and location = :location " 
              + "   and year = :year ") 
})
/*  @formatter:on */
        
@Entity
@DiscriminatorValue("CHREQ")
public class ChangeRequestBusinessKey extends CharmsUid {

    private final static Logger          LOGGER               = LoggerFactory.getLogger(ChangeRequestBusinessKey.class);

    // we need all fields of this key in order to sort and query for the latest
    // key
    private String                       prefix;
    private String                       location;
    private String                       year;

    private String                       sortIndex;

    @Transient
    // created lazy
    private String                       value                = null;

    public static final String           FIND_LAST_VALUE      = "ChangeRequestBusinessKey.FIND_LAST_VALUE";

    private static final String          YEAR_PATTERN         = "yyyy";
    private static final String          INDEX_NUMBER_PATTERN = "00000";

    // thread safe since Java 1.4(merlin-beta2)
    public static final SimpleDateFormat YEAR_FORMAT          = new SimpleDateFormat(YEAR_PATTERN);

    @Column(name = "PREFIX_", updatable = false)
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    @Column(name = "LOCATION_", updatable = false)
    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    @Column(name = "YEAR_", updatable = false)
    public String getYear() {
        return year;
    }

    public void setYear(final String year) {
        this.year = year;
    }

    @Column(name = "SORT_INDEX_", updatable = false)
    public String getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(final String sortIndex) {
        this.sortIndex = sortIndex;
    }

    /**
     * set the last (queried) business key and this method calculates and sets
     * the next sort index this means it creates the next business key, which is
     * not yet persisted since we don't want any session stuff here
     * 
     * @param lastBusinessKey
     */
    @Transient
    public void calculateAndSetNextSortIndex(final ChangeRequestBusinessKey lastBusinessKey) {
        final String nextIndex = new DecimalFormat(ChangeRequestBusinessKey.INDEX_NUMBER_PATTERN).format(Integer.parseInt(lastBusinessKey.getSortIndex()) + 1);
        LOGGER.debug("calculated next sort index: {}", nextIndex);
        setSortIndex(nextIndex);
    }

    /**
     * this method is used when starting on an empty database and we don't have
     * any business keys yet
     */
    @Transient
    public void setStartSortIndex() {
        setSortIndex(ChangeRequestBusinessKey.INDEX_NUMBER_PATTERN);
    }

    /**
     * this is what gets applied in the process instance a unique string
     */
    @Override
    @Transient
    public String getValue() {
        if (value == null) {
            final StringBuilder key = new StringBuilder();
            key.append(prefix);
            key.append("-");
            key.append(location);
            key.append("-");
            key.append(year);
            key.append("-");
            key.append(sortIndex);
            value = key.toString();
        }
        return value;
    }

}
