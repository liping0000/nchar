package net.wohlfart.changerequest;

import org.jboss.seam.ScopeType;

import java.util.Iterator;

import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.CustomResourceLoader;
import net.wohlfart.framework.sort.SortableMoves;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestProduct;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.validator.InvalidStateException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("changeRequestCodeHome")
public class ChangeRequestCodeHome extends TranslateableHome<ChangeRequestCode> {


    private final static Logger LOGGER              = LoggerFactory.getLogger(ChangeRequestCodeHome.class);

    private static final String CHANGE_REQUEST_CODE = "changeRequestCode";

    @Override
    protected String getNameInContext() {
        return CHANGE_REQUEST_CODE;
    }

    @Override
    public String getBundleName() {
        return CustomResourceLoader.CHREQ_CODE_BUNDLE_NAME;
    }

    @Transactional
    @Factory(value = CHANGE_REQUEST_CODE)
    public ChangeRequestCode getChangeRequestCode() {
        // the getInstance method is tuned and also inits the translation map...
        return super.getInstance();
    }

    @Override
    @Transactional
    public String update() {
        final ChangeRequestCode code = getInstance();
        final Session hibernateSession = getSession();

        // check if the product already exists, there is an index on the default
        // name
        final ChangeRequestCode collision = (ChangeRequestCode) getSession().getNamedQuery(ChangeRequestCode.FIND_BY_DEFAULT_NAME)
                .setParameter("defaultName", getInstance().getDefaultName()).uniqueResult();
        // since this is an update we might hit the same entity, the only way to
        // tell
        // is compare the ids, since this is an update not a persist, the
        // current entity
        // should already have an id
        if ((collision != null) && (!collision.getId().equals(code.getId()))) {
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
        final ChangeRequestCode code = getInstance();
        final Session hibernateSession = getSession();

        try {
            // check if the name is already used
            final int found = hibernateSession.getNamedQuery(ChangeRequestCode.FIND_BY_DEFAULT_NAME)
                    .setParameter("defaultName", getInstance().getDefaultName()).list().size();

            if (found != 0) {
                final FacesMessages facesMessages = FacesMessages.instance();
                facesMessages.add("Name is already taken");
                return "invalid";
            }

            code.setupSortIndex(hibernateSession);
            final String result = super.persist(); // this might raise an
                                                   // exception for empty
                                                   // non-null fields

            hibernateSession.flush();
            return result; // returns "persisted"
        } catch (final HibernateException ex) {
            hibernateSession.clear();
            // this might happen if we persist with null properties
            LOGGER.warn("persist failed this should be caught in the UI layer and not happen here, we try to recover by returning 'invalid' as view-id", ex);
            return "invalid";
        } catch (final InvalidStateException ex) {
            hibernateSession.clear();
            ex.printStackTrace();
            // validation exception
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
        final Session hibernateSession = getSession();
        // FIXME: check if we can delete at all since we
        // might be tied to a foreign key in an already running workflow
        // instance
        try {
            // remove foreign keys first:
            instance = getInstance();
            final Iterator<ChangeRequestProduct> products = instance.getProduct().iterator();
            while (products.hasNext()) {
                final ChangeRequestProduct product = products.next();
                product.getCodes().remove(instance);
                products.remove();
                hibernateSession.persist(product);
            }
            hibernateSession.persist(instance);

            // remove entity
            final String result = super.remove();

            hibernateSession.createQuery("update " + ChangeRequestCode.class.getName() + " t1 " + " set sortIndex = " // this
                                                                                                                      // is
                                                                                                                      // a
                                                                                                                      // poor
                                                                                                                      // man's
                                                                                                                      // rownum
                    + " (select count(*) + 1 from " + ChangeRequestCode.class.getName() + " t2 where t2.sortIndex < t1.sortIndex ) " + " where 1=1 ")
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
        final Session hibernateSession = getSession();
        final FacesMessages facesMessages = FacesMessages.instance();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortUp");
        SortableMoves.moveUp(ChangeRequestCode.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

    @Transactional
    public String sortDown() {
        final Session hibernateSession = getSession();
        final FacesMessages facesMessages = FacesMessages.instance();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortDown");
        SortableMoves.moveDown(ChangeRequestCode.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

}
