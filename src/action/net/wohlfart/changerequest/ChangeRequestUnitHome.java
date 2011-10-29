package net.wohlfart.changerequest;

import org.jboss.seam.ScopeType;

import java.util.Iterator;

import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.CustomResourceLoader;
import net.wohlfart.framework.sort.SortableMoves;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.validator.InvalidStateException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("changeRequestUnitHome")
public class ChangeRequestUnitHome extends TranslateableHome<ChangeRequestUnit> {


    private final static Logger LOGGER              = LoggerFactory.getLogger(ChangeRequestUnitHome.class);

    private static final String CHANGE_REQUEST_UNIT = "changeRequestUnit";

    @In(value = "hibernateSession")
    private Session             hibernateSession;

    @Override
    protected String getNameInContext() {
        return CHANGE_REQUEST_UNIT;
    }

    @Override
    public String getBundleName() {
        return CustomResourceLoader.CHREQ_UNIT_BUNDLE_NAME;
    }

    @Transactional
    @Factory(value = CHANGE_REQUEST_UNIT)
    public ChangeRequestUnit getChangeRequestUnit() {
        LOGGER.debug("getChangeRequestUnit called");
        return getInstance();
    }

    @Override
    @Transactional
    public String update() {
        final ChangeRequestUnit unit = getInstance();

        // check if the product already exists, there is an index on the default
        // name
        final ChangeRequestUnit collision = (ChangeRequestUnit) hibernateSession.getNamedQuery(ChangeRequestUnit.FIND_BY_DEFAULT_NAME)
                .setParameter("defaultName", getInstance().getDefaultName()).uniqueResult();
        // since this is an update we might hit the same entity, the only way to
        // tell
        // is compare the ids, since this is an update not a persist, the
        // current entity
        // should already have an id
        if ((collision != null) && (!collision.getId().equals(unit.getId()))) {
            // an entity with the same default name already exists
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add("Name is already taken");
            return "invalid";
        }

        // update also stores the translations
        final String result = super.update();
        hibernateSession.flush();
        return result; // returns "updated"
    }

    @Override
    @Transactional
    public String persist() {
        final ChangeRequestUnit part = getInstance();

        try {
            // check if the name is already used
            final int found = hibernateSession.getNamedQuery(ChangeRequestUnit.FIND_BY_DEFAULT_NAME)
                    .setParameter("defaultName", getInstance().getDefaultName()).list().size();

            if (found != 0) {
                final FacesMessages facesMessages = FacesMessages.instance();
                facesMessages.add("Name is already taken");
                return "invalid";
            }

            part.setupSortIndex(hibernateSession);
            final String result = super.persist();
            hibernateSession.flush();
            return result; // returns "persisted"
        } catch (final HibernateException ex) {
            // validation exception
            // clean the session
            hibernateSession.clear();
            LOGGER.warn("persist failed, this should be caught in the UI layer and not happen here, we try to recover by returning 'invalid' as view-id", ex);
            return "invalid";
        } catch (final InvalidStateException ex) {
            // validation exception
            // clean the session
            hibernateSession.clear();
            LOGGER.warn("persist failed, this should be caught in the UI layer and not happen here, we try to recover by returning 'invalid' as view-id", ex);
            return "invalid";
        }
    }

    @Override
    @Transactional
    public String remove() {
        if (getId() == null) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "No entity to delete, id is null");
            return "error";
        }
        // FIXME: check if we can delete at all since we
        // might be tied to a foreign key in an already running workflow
        // instance
        try {
            // remove foreign keys first:
            instance = getInstance();
            final Iterator<ChangeRequestProduct> products = instance.getProduct().iterator();
            while (products.hasNext()) {
                final ChangeRequestProduct product = products.next();
                product.getUnits().remove(instance);
                products.remove();
                hibernateSession.persist(product);
            }
            hibernateSession.persist(instance);

            // remove entity
            final String result = super.remove();

            hibernateSession.createQuery("update " + ChangeRequestUnit.class.getName() + " t1 " + " set sortIndex = " // this
                                                                                                                      // is
                                                                                                                      // a
                                                                                                                      // poor
                                                                                                                      // man's
                                                                                                                      // rownum
                    + " (select count(*) + 1 from " + ChangeRequestUnit.class.getName() + " t2 where t2.sortIndex < t1.sortIndex ) " + " where 1=1 ")
                    .executeUpdate();
            hibernateSession.flush();
            return result; // returns "removed"
        } catch (final Exception ex) {
            hibernateSession.clear();
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "Can't delete entity, it's probably needed for a workflow");
            LOGGER.info("entity not deleted  {}", ex);
            return "canceled"; // leave the page
        }
    }

    @Override
    public String cancel() {
        return super.cancel();
    }

    @Transactional
    public String sortUp() {
        final FacesMessages facesMessages = FacesMessages.instance();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortUp");
        SortableMoves.moveUp(ChangeRequestUnit.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

    @Transactional
    public String sortDown() {
        final FacesMessages facesMessages = FacesMessages.instance();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortDown");
        SortableMoves.moveDown(ChangeRequestUnit.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

}
