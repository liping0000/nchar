package net.wohlfart.framework.search;

import java.io.Serializable;
import java.util.Date;

import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
import net.wohlfart.framework.entities.CharmsDocument;
import net.wohlfart.framework.entities.CharmsWorkflowData;

public class SearchResultItem implements Serializable {


    private final Object entity;

    private Date         date;

    public SearchResultItem(final Object entity) {
        this.entity = entity;
        if (entity instanceof ChangeRequestData) {
            date = ((ChangeRequestData) entity).getSubmitDate();
        }
    }

    public Object getEntity() {
        return entity;
    }

    public Date getSubmitDate() {
        return date;
    }

    // this is ugly code, JSF doesn't permit instanceof in the view
    // FIXME: maybe we can use a interface for all entities that are search-able
    // and generalize the code to avoid instanceof checks...

    public boolean isChangeRequestMessageEntry() {
        return (entity instanceof ChangeRequestMessageEntry);
    }

    public boolean isChangeRequestData() {
        return (entity instanceof CharmsWorkflowData);
    }

    public boolean isCharmsDocument() {
        return (entity instanceof CharmsDocument);
    }

}
