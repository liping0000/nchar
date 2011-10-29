package net.wohlfart.email;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.jbpm.pvm.internal.history.model.HistoryTaskInstanceImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * resolves the "assignedActor" token
 * 
 * resolve the assignee of the current task, there must be exactly one!
 * 
 * @author Michael Wohlfart
 */
public class SingleAssigneeTokenResolver implements IAddressTokenResolver {

    private final static Logger      LOGGER = LoggerFactory.getLogger(SingleAssigneeTokenResolver.class);

    public static final List<String> TOKENS = new ArrayList<String>(Arrays.asList(new String[] { "assignedActor" }));

    @Override
    public Set<CharmsUser> resolve(final String expression, final ExecutionImpl executionImpl, final Session session) {

        if (!TOKENS.contains(expression)) {
            return null;
        }

        return doResolve(executionImpl, session);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    Set<CharmsUser> doResolve(final ExecutionImpl executionImpl, final Session session) {

        LOGGER.debug("SingleAssigneeTokenResolver running");

        final Long activityDbid = executionImpl.getHistoryActivityInstanceDbid();

        LOGGER.debug(" activityDbid is: {}", activityDbid);
        LOGGER.debug(" executionImpl.getTask() is: {}", executionImpl.getTask());

        final List<String> actorIds = session
                .createQuery(
                        "select a.historyTask.assignee from " + HistoryTaskInstanceImpl.class.getName() + " a " + " where a.dbid = :activityDbid"
                                + " and a.historyTask.assignee is not null " // only
                                                                             // return
                                                                             // non
                                                                             // null
                                                                             // assignees
                ) // FIXME: check if the task is still open
                .setParameter("activityDbid", activityDbid).list();

        if ((actorIds == null) || (actorIds.size() == 0) || (actorIds.size() > 1)) {
            LOGGER.debug("null or multiple actorids found for execution, ids are {}, executionid is {} --> returning null, nothing found", actorIds,
                    executionImpl.getDbid());
            return null;
        }

        final String actorId = actorIds.get(0);
        LOGGER.debug("found exactly one actorid: {}", actorId);

        final List<CharmsUser> result = session.getNamedQuery(CharmsUser.FIND_BY_ACTOR_ID).setParameter("actorId", actorId /*
                                                                                                                            * actorIds
                                                                                                                            * .
                                                                                                                            * toArray
                                                                                                                            * (
                                                                                                                            * new
                                                                                                                            * String
                                                                                                                            * [
                                                                                                                            * actorIds
                                                                                                                            * .
                                                                                                                            * size
                                                                                                                            * (
                                                                                                                            * )
                                                                                                                            * ]
                                                                                                                            * )
                                                                                                                            */).list(); // we
                                                                                                                                        // need
                                                                                                                                        // a
                                                                                                                                        // list
                                                                                                                                        // as
                                                                                                                                        // result
                                                                                                                                        // anyways

        LOGGER.debug("resolved list of actors: {} for expression list: ", result);
        for (final String exp : actorIds) {
            LOGGER.debug("  >{}< ", exp);
        }

        if ((result == null) || (result.size() == 0)) {
            LOGGER.error("can't find assigned CharmsUser object for actorIds: {}", actorIds);
            return null;
        }

        // found something
        return new HashSet(result);
    }

}
