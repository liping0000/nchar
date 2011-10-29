package net.wohlfart.changerequest.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestCodeItem;
import net.wohlfart.refdata.entities.ChangeRequestProduct;

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

@Name("changeRequestCodeSelect")
@Scope(ScopeType.CONVERSATION)
public class ChangeRequestCodeSelect implements Serializable {


    private final static Logger           LOGGER             = LoggerFactory.getLogger(ChangeRequestCodeSelect.class);

    protected Long                        codeId;

    protected ChangeRequestData           changeRequestData;

    // in sync:
    protected Long                        productId;
    protected List<ChangeRequestCodeItem> availableCodeItems = new ArrayList<ChangeRequestCodeItem>();

    @In(value = "hibernateSession")
    protected Session                     hibernateSession;

    @Create
    public void startup() {
        changeRequestData = (ChangeRequestData) Contexts.getConversationContext().get(ChangeRequestData.CHANGE_REQUEST_DATA);
        final ChangeRequestCode code = changeRequestData.getChangeRequestCode();
        codeId = code == null ? null : code.getId();
        final ChangeRequestProduct product = changeRequestData.getChangeRequestProduct();
        productId = product == null ? null : product.getId();
        refreshCodeList();
    }

    public List<ChangeRequestCodeItem> getAvailableCodeItems() {
        final ChangeRequestProduct product = changeRequestData.getChangeRequestProduct();
        if (product == null) {
            if (productId != null) {
                productId = null;
                refreshCodeList();
            }
        } else {
            if (!product.getId().equals(productId)) {
                productId = product.getId();
                refreshCodeList();
            }
        }
        LOGGER.info("availableCodeItems: " + availableCodeItems);
        return availableCodeItems;
    }

    @BypassInterceptors
    public Long getCodeId() {
        return codeId;
    }

    @Transactional
    public void setCodeId(final Long codeId) {
        this.codeId = codeId;
        if (codeId != null) {
            final ChangeRequestCode code = (ChangeRequestCode) hibernateSession.get(ChangeRequestCode.class, codeId);
            changeRequestData.setChangeRequestCode(code);
        } else {
            changeRequestData.setChangeRequestCode(null);
        }
    }

    @Transactional
    protected void refreshCodeList() {
        if (productId != null) {
            availableCodeItems = ChangeRequestCodeItem.getEnabledSelectForProduct(hibernateSession, true, productId);
        } else {
            availableCodeItems = new ArrayList<ChangeRequestCodeItem>();
        }
        LOGGER.debug("refreshed availableCodeItems list: {}", availableCodeItems);
    }

}
