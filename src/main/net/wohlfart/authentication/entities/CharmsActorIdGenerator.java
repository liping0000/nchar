package net.wohlfart.authentication.entities;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.enhanced.TableGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This generator uses the enhanced table id generator returning the current id
 * from the database.
 * 
 * This class is generating the actor ids for role and user.
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class CharmsActorIdGenerator extends TableGenerator {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsActorIdGenerator.class);

    public static final String DECIMAL_FORMAT_TEMPLATE = "a00000";

    public static final int ACTOR_ID_SIZE = 50;

    @Override
    public synchronized Serializable generate(
            final SessionImplementor session, final Object object) throws HibernateException {

        // let the parent generate the unique id
        final Serializable serializable = super.generate(session, object);
        LOGGER.debug("generating id: {} for class {}", serializable, object.getClass());

        if (object instanceof IActorIdHolder) {
            // just add the prefix to the id to distinguish between user and group
            if (serializable instanceof Long) {
                final IActorIdHolder actorIdHolder = (IActorIdHolder) object;
                final String prefix = actorIdHolder.getActorIdPrefix();
                final Long id = (Long) serializable;
                final String actorId = prefix + new DecimalFormat(DECIMAL_FORMAT_TEMPLATE).format(id);
                ((IActorIdHolder) object).setActorId(actorId);
            } else {
                LOGGER.warn("parent class doesn't return an Id of class Long, instead we got {}", serializable.getClass());
            }
        } else {
            LOGGER.warn("can't generate actorId for object {} of class {} object must implement IActorIdHolder", object, object.getClass());
        }

        return serializable;
    }
}
