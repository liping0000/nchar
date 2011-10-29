package net.wohlfart.changerequest.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestProductItem;

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

@Name("changeRequestProductSelect")
@Scope(ScopeType.CONVERSATION)
public class ChangeRequestProductSelect implements Serializable {


    private final static Logger              LOGGER                = LoggerFactory.getLogger(ChangeRequestProductSelect.class);

    protected Long                           productId;
    protected List<ChangeRequestProductItem> availableProductItems = new ArrayList<ChangeRequestProductItem>();
    protected ChangeRequestData              changeRequestData;

    @In(value = "hibernateSession")
    protected Session                        hibernateSession;

    @Create
    public void startup() {
        // in order to inject it only once:
        changeRequestData = (ChangeRequestData) Contexts.getConversationContext().get(ChangeRequestData.CHANGE_REQUEST_DATA);
        final ChangeRequestProduct product = changeRequestData.getChangeRequestProduct();
        productId = product == null ? null : product.getId();
        refreshProductList();
    }

    @BypassInterceptors
    public Long getProductId() {
        return productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
        if (productId != null) {
            final ChangeRequestProduct product = (ChangeRequestProduct) hibernateSession.get(ChangeRequestProduct.class, productId);
            changeRequestData.setChangeRequestProduct(product);
        } else {
            changeRequestData.setChangeRequestProduct(null);
        }
    }

    @Transactional
    protected void refreshProductList() {
        availableProductItems = ChangeRequestProductItem.getEnabledSelect(hibernateSession, true);
        LOGGER.debug("refreshed availableProductItems list: {}", availableProductItems);
    }

    public List<ChangeRequestProductItem> getAvailableProductItems() {
        return availableProductItems;
    }

}
