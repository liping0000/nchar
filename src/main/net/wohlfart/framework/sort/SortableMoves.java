package net.wohlfart.framework.sort;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortableMoves {

    private final static Logger LOGGER = LoggerFactory.getLogger(SortableMoves.class);

    // FIXME: simplify this class

    // FIXME: all this updateing happens behind the entityManager so
    // we end up with tons of problems if an entity inside the entityManager
    // is inconsistent compared to an entity in the database...

    public static void moveUp(final Class<? extends ISortable> clazz, final Long id, final Session hibernateSession) {

        LOGGER.debug("moving up id " + id);

        final Integer sortIndex = (Integer) hibernateSession.createQuery("select sortIndex from " + clazz.getName() + " where id = :id").setParameter("id", id)
                .uniqueResult();

        LOGGER.debug("moving up sortIndex " + sortIndex);

        // remember the current and move it out of the way, this is the one we
        // want to move
        hibernateSession.createQuery("update " + clazz.getName() + " set sortIndex = 0 where sortIndex = :sortIndex ").setParameter("sortIndex", sortIndex)
                .executeUpdate();

        // get the next one and move it one back to the former currents position
        hibernateSession.createQuery("update " + clazz.getName() + " set sortIndex = :sortIndex where sortIndex = (:sortIndex - 1) ")
                .setParameter("sortIndex", sortIndex).executeUpdate();

        // now more the former current one to the next ones position
        hibernateSession.createQuery("update " + clazz.getName() + " set sortIndex = (:sortIndex - 1) where sortIndex = 0 ")
                .setParameter("sortIndex", sortIndex).executeUpdate();

        hibernateSession.flush();
        hibernateSession.clear();
    }

    public static void moveDown(final Class<? extends ISortable> clazz, final Long id, final Session hibernateSession) {

        LOGGER.debug("moving down id " + id);

        final Integer sortIndex = (Integer) hibernateSession.createQuery("select sortIndex from " + clazz.getName() + " where id = :id").setParameter("id", id)
                .uniqueResult();

        LOGGER.debug("moving down sortIndex " + sortIndex);

        // remember the current and move it out of the way, this is the one we
        // want to move
        hibernateSession.createQuery("update " + clazz.getName() + " set sortIndex = 0 where sortIndex = :sortIndex ").setParameter("sortIndex", sortIndex)
                .executeUpdate();

        // get the next one and move it one back to the former currents position
        hibernateSession.createQuery("update " + clazz.getName() + " set sortIndex = :sortIndex where sortIndex = (:sortIndex + 1) ")
                .setParameter("sortIndex", sortIndex).executeUpdate();

        // now more the former current one to the next ones position
        hibernateSession.createQuery("update " + clazz.getName() + " set sortIndex = (:sortIndex + 1) where sortIndex = 0 ")
                .setParameter("sortIndex", sortIndex).executeUpdate();

        hibernateSession.flush();
        hibernateSession.clear();
    }

}
