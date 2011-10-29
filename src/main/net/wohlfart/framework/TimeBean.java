package net.wohlfart.framework;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.ConversationEntries;
import org.jboss.seam.core.ConversationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(APPLICATION)
@Name("timeBean")
@BypassInterceptors
@Startup(depends = "org.jboss.seam.core.applicationContext")
// probably not needed
public class TimeBean {

    private final static Logger LOGGER   = LoggerFactory.getLogger(TimeBean.class);

    private static final Long   INTERVAL = 3000L;   // 3 sec timer interval

    // session timeout is 5 secs after the timer interval but at least 1 minute:
    // private static final Long SESSION_TIMEOUT = Math.max((((3000L / 1000L) +
    // 5) / 60L), 1);

    /*
     * //private static final LogProvider log =
     * Logging.getLogProvider(CustomMessageLoader.class);
     * 
     * @In Locale locale;
     * 
     * private final static String DATE_FORMAT_STRING =
     * "EEEE, dd MMMM yyyy, HH:mm, zzzz";
     * 
     * // cache the formatter so we don't need a new one on each request with
     * the current locale HashMap<Locale, SimpleDateFormat> formatters = new
     * HashMap<Locale, SimpleDateFormat>();
     * 
     * // // @return the current date and time in a localized string // public
     * String getDate() { //log.debug("getDate called"); if
     * (!this.formatters.containsKey(this.locale)) { SimpleDateFormat
     * simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_STRING, this.locale);
     * this.formatters.put(this.locale, simpleDateFormat); } return
     * this.formatters
     * .get(this.locale).format(Calendar.getInstance().getTime()); }
     */

    // @Asynchronous
    public Date getDate() {
        final ConversationEntries conversationEntries = ConversationEntries.getInstance();
        if ((conversationEntries == null) || (conversationEntries.size() == 0)) {
            LOGGER.debug("no conversations found");
        } else {
            final Set<ConversationEntry> orderedEntries = new TreeSet<ConversationEntry>();
            orderedEntries.addAll(conversationEntries.getConversationEntries());
            LOGGER.debug("conversations found:");
            for (final ConversationEntry entry : orderedEntries) {
                LOGGER.debug(" ID: " + entry.getId() + " description: " + entry.getDescription());
            }
        }

        return Calendar.getInstance().getTime();
    }

    public void trigger() {
        LOGGER.debug("timer bean triggered");
    }

    public Long getTimeout() {
        return INTERVAL; // 3 sec by default
    }

}
