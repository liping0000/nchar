package net.wohlfart.jbpm4.activity;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import net.wohlfart.changerequest.entities.ChangeRequestBusinessKey;

import org.jboss.seam.annotations.Transactional;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.hibernate.DbSessionImpl;
import org.jbpm.pvm.internal.history.model.HistoryProcessInstanceImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.util.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class creates a unique business key with the pattern:
 * 
 * <PREFX>-<LOCATION>-<YEAR>-<COUNT>
 * 
 * prefix and location can be configured in the workflow definition, year is
 * deducted from the current time and the count is resolved by looking up the
 * database table
 * 
 * 
 * 
 * FIXME: lookup execution variables here in order to override the default one
 * from the process definition meaning instead of using fixed meanings strings
 * here we should use implements EL expressions and resolve them within the
 * execution context...
 * 
 * We can keep the fixed strings and add EL
 * 
 * @author Michael Wohlfart
 * 
 */
public class CreateBusinessKeyActivity extends AbstractActivity {

    private final static Logger LOGGER                  = LoggerFactory.getLogger(CreateBusinessKeyActivity.class);

    // needed for xml parsing during binding
    public static final String  ATTRIBUTE_PREFIX_NAME   = "prefix";
    public static final String  ATTRIBUTE_LOCATION_NAME = "location";

    // the default values from the workflow definition
    private String              prefix;
    private String              location;

    @Override
    public void execute(final ActivityExecution execution) {
        execute((ExecutionImpl) execution);
    }

    /**
     * called when the workflow hits this activity
     */
    public void execute(final ExecutionImpl execution) {
        LOGGER.debug("entering execute for task activity {} in execution {}", getName(), execution.getId());

        // check if the key is null
        final String key = execution.getProcessInstance().getKey();
        if (key != null) {
            throw new IllegalArgumentException("business key should be null prior to setting, but is " + key
                    + " assigning a business key twice is an indication of a bug in the process definition ");
        }

        // do the work of creating a new key
        createAndAssignBusinessKey(execution, getPrefix(), getLocation(), Clock.getTime());

        // runScript();
        // new ScriptExpression(getScript(),
        // getScriptLanguage()).evaluateInScope(execution); // execution scope
        // is not used!!
        scriptActivity.perform(execution);

        // exit the node right away through the default transition
        execution.takeDefaultTransition();

        // this is called after the transition was made
        LOGGER.debug("exiting execute for task activity {}, the key after creation is {}", getName(), execution.getProcessInstance().getKey());
    }

    /**
     * this method creates a new business key and stores it in the Database the
     * method is synchronized to prevent race condition between multiple threads
     * this is the only
     * 
     * @param prefix
     * @param location
     * @param date
     * @return
     * 
     *         FIXME: we need tons of testcases tomake sure we don't have any
     *         race conditions here
     */
    @Transactional
    private synchronized ChangeRequestBusinessKey createAndAssignBusinessKey(final ActivityExecution execution, final String prefix, final String location,
            final Date date) {

        final DbSessionImpl dbSession = (DbSessionImpl) EnvironmentImpl.getCurrent().get(DbSession.class);

        final String year = ChangeRequestBusinessKey.YEAR_FORMAT.format(date);

        // get the last business key
        ChangeRequestBusinessKey lastKey = (ChangeRequestBusinessKey) dbSession.getSession().getNamedQuery(ChangeRequestBusinessKey.FIND_LAST_VALUE)
                .setParameter("prefix", prefix).setParameter("location", location).setParameter("year", year).uniqueResult();

        // create one if we don't have one yet
        if (lastKey == null) {
            // no key yet (for this year?), create the first one
            lastKey = new ChangeRequestBusinessKey();
            lastKey.setPrefix(prefix);
            lastKey.setLocation(location);
            lastKey.setYear(year);
            lastKey.setStartSortIndex(); // effectively setting to zero here
        }

        LOGGER.debug("found lastKey: " + lastKey + " value is; " + lastKey.getValue());
        // calculate the next key and store it in the DB
        final ChangeRequestBusinessKey newKey = new ChangeRequestBusinessKey();
        newKey.setPrefix(prefix);
        newKey.setLocation(location);
        newKey.setYear(year);
        newKey.setLastModified(Calendar.getInstance().getTime());
        newKey.calculateAndSetNextSortIndex(lastKey);

        final ExecutionImpl process = ((ExecutionImpl) execution.getProcessInstance());
        final String businessKeyString = newKey.getValue();
        // set the id in the process instance
        process.setKey(businessKeyString);
        // test to get the new business key into the DB sooner for the actions
        // we need this to put the business key into the data objects by the
        // script
        // code
        // session.persist(exe);

        LOGGER.info("process.getKey(): " + process.getKey() + " process.getId() " + process.getId() + " hash: " + process.hashCode());

        // set the key in the history instance...
        // Comparisons between 'BIGINT' and 'CHAR (UCS_BASIC)' are not
        // supported.
        // Types must be comparable. String types must also have matching
        // collation.
        // If collation does not match, a possible solution is to cast operands
        // to force
        // them to the default collation (e.g. SELECT tablename FROM
        // sys.systables WHERE CAST(tablename AS VARCHAR(128)) = 'T1')
        //
        // --> removed the ticks in the where clause
        //
        // this is fucking ugly!

        /*
         * dbSession .getSession() .createQuery("update " +
         * HistoryProcessInstanceImpl.class.getName() + " set key ='" +
         * businessKeyString + "'" + " where dbid = " + process.getDbid())
         * .executeUpdate();
         */

        final HistoryProcessInstanceImpl hist = (HistoryProcessInstanceImpl) dbSession.getSession().get(HistoryProcessInstanceImpl.class, process.getDbid()); // history
                                                                                                                                                              // has
                                                                                                                                                              // assigned
                                                                                                                                                              // key
        hist.setKey(businessKeyString);
        // persisting the new key in the history process
        dbSession.update(hist);
        dbSession.save(newKey);
        // don't do a flush here since we might have unsaved instances

        LOGGER.debug("created newKey: {}  value is {}. ", newKey, newKey.getValue());
        return newKey;
    }

    /**
     * fixed string for prefix
     */
    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * fixed string for location
     */
    public void setLocation(final String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public void signal(final ActivityExecution execution, final String signalName, final Map<String, ?> parameters) throws Exception {
        LOGGER.warn("no signal supposed to happen in create business key node, the control flow is not supposed to stoü here");
    }
}
