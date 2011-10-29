package net.wohlfart.changerequest.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;
import net.wohlfart.refdata.entities.ChangeRequestUnitItem;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("changeRequestUnitSelect")
@Scope(ScopeType.CONVERSATION)
public class ChangeRequestUnitSelect implements Serializable {


    private final static Logger           LOGGER             = LoggerFactory.getLogger(ChangeRequestUnitSelect.class);

    protected Long                        unitId;

    protected ChangeRequestData           changeRequestData;
    // in sync
    protected Long                        productId;
    protected List<ChangeRequestUnitItem> availableUnitItems = new ArrayList<ChangeRequestUnitItem>();

    @In(value = "hibernateSession")
    protected Session                     hibernateSession;

    @Create
    public void startup() {
        changeRequestData = (ChangeRequestData) Contexts.getConversationContext().get(ChangeRequestData.CHANGE_REQUEST_DATA);
        final ChangeRequestUnit unit = changeRequestData.getChangeRequestUnit();
        unitId = unit == null ? null : unit.getId();
        final ChangeRequestProduct product = changeRequestData.getChangeRequestProduct();
        productId = product == null ? null : product.getId();
        refreshUnitList();
    }

    public List<ChangeRequestUnitItem> getAvailableUnitItems() {
        final ChangeRequestProduct product = changeRequestData.getChangeRequestProduct();
        if (product == null) {
            if (productId != null) {
                productId = null;
                refreshUnitList();
            }
        } else {
            if (!product.getId().equals(productId)) {
                productId = product.getId();
                refreshUnitList();
            }
        }
        LOGGER.info("availableUnitItems: " + availableUnitItems);
        return availableUnitItems;
    }

    @BypassInterceptors
    public Long getUnitId() {
        return unitId;
    }

    @Transactional
    public void setUnitId(final Long unitId) {
        this.unitId = unitId;
        if (unitId != null) {
            final ChangeRequestUnit unit = (ChangeRequestUnit) hibernateSession.get(ChangeRequestUnit.class, unitId);
            changeRequestData.setChangeRequestUnit(unit);
        } else {
            changeRequestData.setChangeRequestUnit(null);
        }
    }

    @Transactional
    protected void refreshUnitList() {
        if (productId != null) {
            availableUnitItems = ChangeRequestUnitItem.getEnabledSelectForProduct(hibernateSession, true, productId);
        } else {
            availableUnitItems = new ArrayList<ChangeRequestUnitItem>();
        }
        LOGGER.debug("refreshed availablePartItems list: {}", availableUnitItems);
    }

}
