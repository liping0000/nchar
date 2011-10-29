package net.wohlfart.authentication;

import java.util.List;

import org.hibernate.Session;
import org.jbpm.api.identity.Group;
import org.jbpm.api.identity.User;
import org.jbpm.internal.log.Log;
import org.jbpm.pvm.internal.identity.spi.IdentitySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class implements the jbpm4 identity session but is not yet used since we
 * use identity config from seam this class is needed in the jbpm4.cfg.xml file
 * 
 * TODO: delegate stuff to seam FIXME: we can save a lot of custom sql hackery
 * if this component is implemented!
 * 
 * @author Michael Wohlfart
 * 
 */
public class JbpmIdentitySessionImpl implements IdentitySession {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmIdentitySessionImpl.class);

    private Session session;

    @Override
    public String createGroup(final String groupName, final String groupType, final String parentGroupId) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public void createMembership(final String userId, final String groupId, final String role) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public String createUser(final String userId, final String givenName, final String familyName, final String businessEmail) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public void deleteGroup(final String groupId) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public void deleteMembership(final String userId, final String groupId, final String role) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public void deleteUser(final String userId) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public Group findGroupById(final String groupId) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public List<Group> findGroupsByUser(final String userId) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public List<Group> findGroupsByUserAndGroupType(final String userId, final String groupType) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public User findUserById(final String userId) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public List<User> findUsers() {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public List<User> findUsersByGroup(final String groupId) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    @Override
    public List<User> findUsersById(final String... userIds) {
        throw new IllegalArgumentException("method not yet implemented");
    }

    public void setSession(final Session session) {
        this.session = session;
        LOGGER.debug("session is set to: ", this.session);      
    }
}
