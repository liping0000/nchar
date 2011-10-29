package net.wohlfart.changerequest;

import org.jboss.seam.ScopeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.wohlfart.framework.ItemConverter;
import net.wohlfart.framework.TranslateableHome;
import net.wohlfart.framework.i18n.CustomResourceLoader;
import net.wohlfart.framework.sort.SortableMoves;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestCodeItem;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;
import net.wohlfart.refdata.entities.ChangeRequestUnitItem;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.validator.InvalidStateException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("changeRequestProductHome")
public class ChangeRequestProductHome extends TranslateableHome<ChangeRequestProduct> {


    private final static Logger LOGGER                 = LoggerFactory.getLogger(ChangeRequestProductHome.class);

    private static final String CHANGE_REQUEST_PRODUCT = "changeRequestProduct";

    @Override
    protected String getNameInContext() {
        return CHANGE_REQUEST_PRODUCT;
    }

    @Override
    public String getBundleName() {
        return CustomResourceLoader.CHREQ_PROD_BUNDLE_NAME;
    }

    // some form data
    private List<ChangeRequestUnitItem>          selectedUnitItems  = new ArrayList<ChangeRequestUnitItem>();
    private List<ChangeRequestUnitItem>          availableUnitItems = new ArrayList<ChangeRequestUnitItem>();

    // some more form data
    private List<ChangeRequestCodeItem>          selectedCodeItems  = new ArrayList<ChangeRequestCodeItem>();
    private List<ChangeRequestCodeItem>          availableCodeItems = new ArrayList<ChangeRequestCodeItem>();

    // the item converter for the list shuttle
    private ItemConverter<ChangeRequestCodeItem> changeRequestCodeItemConverter;
    // the item converter for the lists huttle
    private ItemConverter<ChangeRequestUnitItem> changeRequestUnitItemConverter;

    @Transactional
    @Factory(value = CHANGE_REQUEST_PRODUCT)
    public ChangeRequestProduct getChangeRequestProduct() {
        LOGGER.debug("getChangeRequestProduct called");
        final ChangeRequestProduct product = getInstance();
        setupUnits(product);
        setupCodes(product);
        return product;
    }

    private void setupUnits(final ChangeRequestProduct product) {
        final Session hibernateSession = getSession();
        final List<ChangeRequestUnitItem> allParts = ChangeRequestUnitItem.getSelect(hibernateSession);

        // create a map for the converter to translate from id to CharmsRoleItem
        final HashMap<Long, ChangeRequestUnitItem> partItemMap = new HashMap<Long, ChangeRequestUnitItem>();
        availableUnitItems = new ArrayList<ChangeRequestUnitItem>();
        for (final ChangeRequestUnitItem item : allParts) {
            final Long id = new Long(item.getValue().toString());
            partItemMap.put(id, item);
            availableUnitItems.add(item); // the selected items will be removed
                                          // later
        }
        // init the converter with the map
        changeRequestUnitItemConverter = new ItemConverter<ChangeRequestUnitItem>(partItemMap);

        // split off the selected and the available items
        final List<ChangeRequestUnit> parts = product.getUnits();
        selectedUnitItems = new ArrayList<ChangeRequestUnitItem>();
        if (parts != null) { // memberships of that user
            for (final ChangeRequestUnit part : parts) {
                final Long id = part.getId();
                // the role is selected and no longer available:
                selectedUnitItems.add(partItemMap.get(id));
                availableUnitItems.remove(partItemMap.get(id));
            }
        }
    }

    private void setupCodes(final ChangeRequestProduct product) {
        final Session hibernateSession = getSession();
        final List<ChangeRequestCodeItem> allCodes = ChangeRequestCodeItem.getSelect(hibernateSession);

        // create a map for the converter to translate from id
        final HashMap<Long, ChangeRequestCodeItem> errorItemMap = new HashMap<Long, ChangeRequestCodeItem>();
        availableCodeItems = new ArrayList<ChangeRequestCodeItem>();
        for (final ChangeRequestCodeItem item : allCodes) {
            final Long id = new Long(item.getValue().toString());
            errorItemMap.put(id, item);
            availableCodeItems.add(item);
        }

        // init the converter with the map
        changeRequestCodeItemConverter = new ItemConverter<ChangeRequestCodeItem>(errorItemMap);

        // split off the selected and the available items
        final List<ChangeRequestCode> codes = product.getCodes();
        selectedCodeItems = new ArrayList<ChangeRequestCodeItem>();
        if (codes != null) { // memberships of that user
            for (final ChangeRequestCode code : codes) {
                final Long id = code.getId();
                // the role is selected and no longer available:
                selectedCodeItems.add(errorItemMap.get(id));
                availableCodeItems.remove(errorItemMap.get(id));
            }
        }
    }

    private void attachUnits(final ChangeRequestProduct product) {
        final Session hibernateSession = getSession();
        // remove all units
        product.getUnits().clear();
        // flushing here is important, otherwise adding the units in another
        // order would
        // result in an occasional constraint validation since we are sorting a
        // xref entry
        // before the original version wich has the same unit & product ids
        hibernateSession.flush();
        for (final ChangeRequestUnitItem unit : selectedUnitItems) {
            final ChangeRequestUnit changeRequestUnit = (ChangeRequestUnit) hibernateSession
                    .load(ChangeRequestUnit.class, new Long(unit.getValue().toString()));
            // adding the xrefs in the new order
            product.getUnits().add(changeRequestUnit);
        }
        // flush again just to make sure the stuff is inserted
        hibernateSession.flush();
    }

    private void attachCodes(final ChangeRequestProduct product) {
        final Session hibernateSession = getSession();
        // remove all codes
        product.getCodes().clear();
        // flush to write back to the database
        hibernateSession.flush();
        for (final ChangeRequestCodeItem code : selectedCodeItems) {
            final ChangeRequestCode changeRequestCode = (ChangeRequestCode) hibernateSession
                    .load(ChangeRequestCode.class, new Long(code.getValue().toString()));
            product.getCodes().add(changeRequestCode);
        }
        // flush again just to make sure the stuff is inserted
        hibernateSession.flush();
    }

    @Override
    @Transactional
    public String update() {
        final ChangeRequestProduct product = getInstance();
        final Session hibernateSession = getSession();
        // check if the product already exists, there is an index on the default
        // name
        final Long collisionId = (Long) hibernateSession.getNamedQuery(ChangeRequestProduct.FIND_ID_BY_DEFAULT_NAME)
                .setParameter("defaultName", getInstance().getDefaultName()).uniqueResult();
        // since this is an update we might hit the same entity, the only way to
        // tell
        // is compare the ids, because this is an update not a persist, the
        // current entity
        // should already have an id
        if ((collisionId != null) && (!collisionId.equals(product.getId()))) {
            // an entity with the same default name already exists
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add("Name is already taken");
            return "invalid";
        }

        attachUnits(product);
        attachCodes(product);

        final String result = super.update();
        hibernateSession.flush();
        return result; // returns "updated"
    }

    @Override
    @Transactional
    public String persist() {
        final ChangeRequestProduct product = getInstance();
        final Session hibernateSession = getSession();
        try {
            // check if the name is already used
            final int found = hibernateSession.getNamedQuery(ChangeRequestProduct.FIND_BY_DEFAULT_NAME)
                    .setParameter("defaultName", getInstance().getDefaultName()).list().size();

            if (found != 0) {
                final FacesMessages facesMessages = FacesMessages.instance();
                facesMessages.add("Name is already taken");
                return "invalid";
            }

            product.setupSortIndex(hibernateSession);
            attachUnits(product);
            attachCodes(product);

            // FIXME: the default save message is pushed into FacesMessages
            // and stays there even if something goes wrong afterwards...
            final String result = super.persist();

            // hibernateSession.refresh(product);

            hibernateSession.flush();
            return result; // returns "persisted"
        } catch (final HibernateException ex) {
            // validation exception
            // clean the session
            hibernateSession.clear();
            LOGGER.warn("persist failed, this should be caught in the UI layer and not happen here, we try to recover by returning 'invalid' as view-id", ex);
            ex.printStackTrace();
            return "invalid";
        } catch (final InvalidStateException ex) {
            // validation exception
            // clean the session
            hibernateSession.clear();
            LOGGER.warn("persist failed, this should be caught in the UI layer and not happen here, we try to recover by returning 'invalid' as view-id", ex);
            ex.printStackTrace();
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
        // FIXME: reorder the sort index, check if we can delete at all since we
        // might be tied to a foreign key
        try {
            final String result = super.remove();
            hibernateSession.createQuery("update " + ChangeRequestProduct.class.getName() + " t1 " + " set sortIndex = " // this
                                                                                                                         // is
                                                                                                                         // a
                                                                                                                         // poor
                                                                                                                         // man's
                                                                                                                         // rownum
                    + " (select count(*) + 1 from " + ChangeRequestProduct.class.getName() + " t2 where t2.sortIndex < t1.sortIndex ) " + " where 1=1 ")
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

    /* -------------- UI helpers for the form -------------- */

    @Transactional
    public String sortUp() {
        final Session hibernateSession = getSession();
        final FacesMessages facesMessages = FacesMessages.instance();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortUp");
        SortableMoves.moveUp(ChangeRequestProduct.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

    @Transactional
    public String sortDown() {
        final Session hibernateSession = getSession();
        final FacesMessages facesMessages = FacesMessages.instance();
        facesMessages.addFromResourceBundle(Severity.INFO, "table.sortDown");
        SortableMoves.moveDown(ChangeRequestProduct.class, (Long) getId(), hibernateSession);
        hibernateSession.flush();
        return "refreshed";
    }

    @BypassInterceptors
    public List<ChangeRequestCodeItem> getSelectedCodeItems() {
        return selectedCodeItems;
    }

    @BypassInterceptors
    public void setSelectedCodeItems(final List<ChangeRequestCodeItem> selectedCodeItems) {
        this.selectedCodeItems = selectedCodeItems;
    }

    @BypassInterceptors
    public List<ChangeRequestCodeItem> getAvailableCodeItems() {
        return availableCodeItems;
    }

    @BypassInterceptors
    public void setAvailableCodeItems(final List<ChangeRequestCodeItem> availableCodeItems) {
        this.availableCodeItems = availableCodeItems;
    }

    @BypassInterceptors
    public List<ChangeRequestUnitItem> getSelectedUnitItems() {
        return selectedUnitItems;
    }

    @BypassInterceptors
    public void setSelectedUnitItems(final List<ChangeRequestUnitItem> selectedUnitItems) {
        this.selectedUnitItems = selectedUnitItems;
    }

    @BypassInterceptors
    public List<ChangeRequestUnitItem> getAvailableUnitItems() {
        return availableUnitItems;
    }

    @BypassInterceptors
    public void setAvailableUnitItems(final List<ChangeRequestUnitItem> availableUnitItems) {
        this.availableUnitItems = availableUnitItems;
    }

    // ----------- the item converters needed in the UI -----------------

    /* the converter for the roleItems used in the form list shuttle */
    @BypassInterceptors
    public ItemConverter<ChangeRequestCodeItem> getChangeRequestCodeItemConverter() {
        return changeRequestCodeItemConverter;
    }

    /* the converter for the roleItems used in the form list shuttle */
    @BypassInterceptors
    public ItemConverter<ChangeRequestUnitItem> getChangeRequestUnitItemConverter() {
        return changeRequestUnitItemConverter;
    }

}
