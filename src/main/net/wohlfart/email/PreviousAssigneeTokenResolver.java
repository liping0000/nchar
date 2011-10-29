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
 * Resolves "previousActor"
 * 
 * resolve the previous assignee of a task this is the assignee for an earlier
 * task in the same execution
 * 
 * @author Michael Wohlfart
 * 
 */
public class PreviousAssigneeTokenResolver implements IAddressTokenResolver {

    private final static Logger      LOGGER = LoggerFactory.getLogger(PreviousAssigneeTokenResolver.class);

    public static final List<String> TOKENS = new ArrayList<String>(Arrays.asList(new String[] { "previousActor" }));

    @Override
    public Set<CharmsUser> resolve(final String expression, final ExecutionImpl executionImpl, final Session session) {

        if (!TOKENS.contains(expression)) {
            return null;
        }

        return doResolve(executionImpl, session);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    Set<CharmsUser> doResolve(final ExecutionImpl executionImpl, final Session session) {

        // the previous actor is either the actor who performed the last
        // activity in the current execution before the current activity
        // or the last actor who performed the last activity in the super
        // execution

        LOGGER.debug("PreviousAssigneeTokenResolver running");

        final Long currentActivityDbid = executionImpl.getHistoryActivityInstanceDbid(); // the
                                                                                         // current
                                                                                         // activity
        final String executionId = executionImpl.getId();

        // debug the history tasks in this execution

        if (LOGGER.isDebugEnabled()) {
            final List<HistoryTaskInstanceImpl> list = session
                    .createQuery("from " + HistoryTaskInstanceImpl.class.getName() + " a " + " where a.executionId = :executionId") // return
                                                                                                                                    // from
                                                                                                                                    // the
                                                                                                                                    // same
                                                                                                                                    // execution
                                                                                                                                    // as
                                                                                                                                    // the
                                                                                                                                    // current
                                                                                                                                    // task
                    .setParameter("executionId", executionId).list();

            LOGGER.debug("all task instances: {}, enumerating tasks...", list);
            for (final HistoryTaskInstanceImpl task : list) {
                LOGGER.debug("  dbid: {}, assignee: {}, activity name: {}",
                        new Object[] { (task.getDbid()), task.getHistoryTask().getAssignee(), task.getActivityName() });
            }
        }
        final List<String> actorIds = session
                .createQuery("select a.historyTask.assignee from " + HistoryTaskInstanceImpl.class.getName() + " a " + " where a.executionId = :executionId" // return
                                                                                                                                                             // from
                                                                                                                                                             // the
                                                                                                                                                             // same
                                                                                                                                                             // execution
                                                                                                                                                             // as
                                                                                                                                                             // the
                                                                                                                                                             // current
                                                                                                                                                             // task
                        + " and a.dbid != :currentActivityDbid" // don't
                                                                // return
                                                                // the
                                                                // current
                                                                // activity/task
                        + " and a.historyTask.assignee is not null " // only
                                                                     // return
                                                                     // non
                                                                     // null
                                                                     // assignees
                        + " order by a.historyTask.createTime desc").setFirstResult(0).setMaxResults(1).setParameter("executionId", executionId)
                .setParameter("currentActivityDbid", currentActivityDbid).list();

        // get the latest one
        String previousActorId = null;
        if ((actorIds != null) && (actorIds.size() >= 1)) {
            LOGGER.debug("previous actors found for execution, ids are {}, executionid is {}", actorIds, executionImpl.getDbid());
            previousActorId = actorIds.get(0);
        } else {
            LOGGER.debug("no previous actor found for execution, ids are {}, executionid is {}", actorIds, executionImpl.getDbid());
            return null;
        }

        final List<CharmsUser> result = session.getNamedQuery(CharmsUser.FIND_BY_ACTOR_ID).setParameter("actorId", previousActorId).list();

        LOGGER.debug("resolved previous actor: {} ", previousActorId);

        if ((result == null) || (result.size() == 0)) {
            LOGGER.error("can't find assigned CharmsUser object for actorId: {}", previousActorId);
            return null;
        }

        // found something
        return new HashSet(result);
    }

}
