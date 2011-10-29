package net.wohlfart.email;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.ParticipationImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * see: http://community.jboss.org/thread/119215?tstart=0
 * 
 * return the candidates for a task
 * 
 * @author Michael Wohlfart
 * 
 */
public class CandidateAssigneeTokenResolver extends AbstractTokenResolver {

    private final static Logger      LOGGER = LoggerFactory.getLogger(CandidateAssigneeTokenResolver.class);

    // the expressions this class can resolve:
    public static final List<String> TOKENS = new ArrayList<String>(Arrays.asList(new String[] { "candidateActor", "assignedActor" }));

    @Override
    public Set<CharmsUser> resolve(final String expression, final ExecutionImpl executionImpl, final Session session) {

        if (!TOKENS.contains(expression)) {
            return null;
        }

        return doResolve(executionImpl, session);
    }

    Set<CharmsUser> doResolve(final ExecutionImpl executionImpl, final Session session) {
        final Set<CharmsUser> result = new HashSet<CharmsUser>();

        // IMPORTANT: in order for this to work, we need a flushed session, make
        // sure this
        // is done in the caller...
        final StringBuilder hql = new StringBuilder();
        hql.append("select ");
        hql.append("t ");
        hql.append("from ");
        hql.append(TaskImpl.class.getName());
        hql.append(" as t ");
        hql.append(" where executionId = '" + executionImpl.getId() + "'");
        hql.append(" and activityName = '" + executionImpl.getActivityName() + "'");
        hql.append(" and state = 'open'");

        final TaskImpl taskImpl = (TaskImpl) session.createQuery(hql.toString()).uniqueResult();

        LOGGER.debug("taskImpl: {}", taskImpl);

        final Set<ParticipationImpl> participations = taskImpl.getParticipations();
        LOGGER.debug("participations: {}", participations);
        for (final ParticipationImpl participation : participations) {
            String actorId;

            actorId = participation.getUserId();
            if (actorId != null) {
                result.addAll(resolveUserId(actorId, session));
            }

            actorId = participation.getGroupId();
            if (actorId != null) {
                result.addAll(resolveGroupId(actorId, session));
            }

        }

        LOGGER.debug("   result: {}", result);
        return result;
    }

}
