package net.wohlfart.email;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsMembership;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.jbpm.pvm.internal.model.ExecutionImpl;

/**
 * this class provides some tools for resolving address tokens
 * 
 * @author Michael Wohlfart
 * 
 */
public abstract class AbstractTokenResolver implements IAddressTokenResolver {

    @Override
    abstract public Set<CharmsUser> resolve(String expression, ExecutionImpl executionImpl, Session session);

    /**
     * resolve a group actor id and return any users within the group
     * 
     * @param actorId
     * @param session
     * @return
     */
    protected Collection<? extends CharmsUser> resolveGroupId(final String actorId, final Session session) {
        final Set<CharmsUser> result = new HashSet<CharmsUser>();

        if (actorId != null) {
            final CharmsRole group = (CharmsRole) session.getNamedQuery(CharmsRole.FIND_BY_ACTOR_ID).setParameter("actorId", actorId).uniqueResult();
            if (group != null) {
                for (final CharmsMembership m : group.getMemberships()) {
                    result.add(m.getCharmsUser());
                }
            }
        }
        return result;
    }

    /**
     * resolve an actor id and return the actual User
     * 
     * @param actorId
     * @param session
     * @return
     */
    protected Collection<? extends CharmsUser> resolveUserId(final String actorId, final Session session) {
        final Set<CharmsUser> result = new HashSet<CharmsUser>();

        if (actorId != null) {
            final CharmsUser user = (CharmsUser) session.getNamedQuery(CharmsUser.FIND_BY_ACTOR_ID).setParameter("actorId", actorId).uniqueResult();
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

}
