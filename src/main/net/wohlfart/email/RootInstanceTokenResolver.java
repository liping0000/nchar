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
 * resolves "rootActor"
 * 
 * resolve the current root Actor, this is the actor that holds the main
 * execution
 * 
 * @author Michael Wohlfart
 */
public class RootInstanceTokenResolver implements IAddressTokenResolver {

    private final static Logger      LOGGER = LoggerFactory.getLogger(RootInstanceTokenResolver.class);

    public static final List<String> TOKENS = new ArrayList<String>(Arrays.asList(new String[] { "rootActor" }));

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

        final ExecutionImpl parentExecution = executionImpl.getParent();
        if (parentExecution == null) {
            LOGGER.debug("execution has no parent to resolve previous actor, execution id is {}", executionImpl.getDbid());
            return null;
        }
        final Long parentExecutionId = parentExecution.getDbid();
        final List<String> actorIds = session
                .createQuery("select a.historyTask.assignee from " + HistoryTaskInstanceImpl.class.getName() + " a " + " where a.executionId = :executionId" // return
                                                                                                                                                             // from
                                                                                                                                                             // the
                                                                                                                                                             // same
                                                                                                                                                             // execution
                        + " and a.historyTask.assignee is not null " // only
                                                                     // return
                                                                     // non
                                                                     // null
                                                                     // assignees
                        + " order by a.historyTask.createTime desc").setFirstResult(1).setMaxResults(1).setParameter("executionId", parentExecutionId).list();

        // get the latest one
        String rootActorId = null;
        if ((actorIds != null) && (actorIds.size() >= 1)) {
            LOGGER.debug("previous actors found in parent execution, ids are {}, executionid is {}", actorIds, executionImpl.getDbid());
            rootActorId = actorIds.get(0);
        } else {
            LOGGER.debug("no previous actor found in parent execution, ids are {}, executionid is {}", actorIds, executionImpl.getDbid());
            return null;
        }

        final List<CharmsUser> result = session.getNamedQuery(CharmsUser.FIND_BY_ACTOR_ID).setParameter("actorId", rootActorId).list();

        LOGGER.debug("resolved previous actor in parent: {} ", rootActorId);

        if ((result == null) || (result.size() == 0)) {
            LOGGER.error("can't find assigned CharmsUser object for actorId: {}", rootActorId);
            return null;
        }

        // found something
        return new HashSet(result);
    }

}
