package net.wohlfart.authorization;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.targets.IPermissionTargetDescriptor;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The target collection holds all permission targets for this application which
 * are encoded as strings and also includes their actions strings.
 * 
 * this is instanciated on demand
 * 
 * @author Michael Wohlfart
 * 
 */

public class PermissionTargetCollection extends HashMap<String, IPermissionTargetDescriptor> implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(PermissionTargetCollection.class);

    public PermissionTargetCollection(final Session hibernateSession) {
        initHash(hibernateSession);
    }

    @SuppressWarnings("unchecked")
    private void initHash(final Session hibernateSession) {
        LOGGER.debug("running initHash...");
        final List<CharmsPermissionTarget> list = hibernateSession.createQuery("from CharmsPermissionTarget").list();

        LOGGER.debug("list size is: " + list.size());

        for (final CharmsPermissionTarget target : list) {
            final String key = target.getTargetString();
            Hibernate.initialize(target.getActions()); // init lazy linked
            put(key, target);
            LOGGER.debug("adding: " + target);
        }
        LOGGER.debug("...finished select");
    }

}
