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
 * resolves "parentActor" and "previousActor"
 * 
 * resolve to the current assignee in the parent execution
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class PreviousExecutionTokenResolver implements IAddressTokenResolver {

    private final static Logger      LOGGER = LoggerFactory.getLogger(PreviousExecutionTokenResolver.class);

    public static final List<String> TOKENS = new ArrayList<String>(Arrays.asList(new String[] { "parentActor", "previousActor" }));

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

        LOGGER.debug("PreviousExecutionTokenResolver running");

        final ExecutionImpl parentExecution = executionImpl.getParent();
        if (parentExecution == null) {
            LOGGER.debug("execution has no parent to resolve previous actor, execution id is {}", executionImpl.getDbid());
            return null;
        }
        final String parentExecutionId = parentExecution.getId();
        final List<String> actorIds = session
                .createQuery("select a.historyTask.assignee from " + HistoryTaskInstanceImpl.class.getName() + " a " + " where a.executionId = :executionId" // return
                                                                                                                                                             // from
                                                                                                                                                             // the
                                                                                                                                                             // parent
                                                                                                                                                             // execution
                        + " and a.historyTask.assignee is not null " // only
                                                                     // return
                                                                     // non
                                                                     // null
                                                                     // assignees
                        + " order by a.historyTask.createTime desc").setFirstResult(1).setMaxResults(1).setParameter("executionId", parentExecutionId).list();

        // get the latest one
        String previousActorId = null;
        if ((actorIds != null) && (actorIds.size() >= 1)) {
            LOGGER.debug("previous actors found in parent execution, ids are {}, executionid is {}", actorIds, executionImpl.getDbid());
            previousActorId = actorIds.get(0);
        } else {
            LOGGER.debug("no previous actor found in parent execution, ids are {}, executionid is {}", actorIds, executionImpl.getDbid());
            return null;
        }

        final List<CharmsUser> result = session.getNamedQuery(CharmsUser.FIND_BY_ACTOR_ID).setParameter("actorId", previousActorId).list();

        LOGGER.debug("resolved previous actor in parent: {} ", previousActorId);

        if ((result == null) || (result.size() == 0)) {
            LOGGER.error("can't find assigned CharmsUser object for actorId: {}", previousActorId);
            return null;
        }

        // found something
        return new HashSet(result);
    }

}
