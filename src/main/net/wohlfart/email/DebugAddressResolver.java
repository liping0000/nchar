package net.wohlfart.email;

import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a debuging class, used in the resolver chain for resolving address
 * tokens the resolvers are tried one by one this is the last in the chain and
 * indicates that the previous resolvers failed.
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class DebugAddressResolver implements IAddressTokenResolver {

    private final static Logger LOGGER = LoggerFactory.getLogger(DebugAddressResolver.class);

    @Override
    public Set<CharmsUser> resolve(final String expression, final ExecutionImpl executionImpl, final Session session) {

        LOGGER.debug("breakpoint");

        return null;
    }

}
