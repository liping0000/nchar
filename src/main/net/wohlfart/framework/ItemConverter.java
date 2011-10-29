package net.wohlfart.framework;

import java.io.Serializable;
import java.util.HashMap;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;

import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: check if we really need this calls or maybe simplyfy the UI/actions
// Converter must be annotated with @BypassInterceptors, we inject
// the lookup map in the constructor
//
// for special converters we can use:
// @org.jboss.seam.annotations.faces.Converter(forClass=CharmsRoleItem.class)
@BypassInterceptors
public class ItemConverter<E> implements Converter, Serializable {


    private final static Logger    LOGGER = LoggerFactory.getLogger(ItemConverter.class);

    private final HashMap<Long, E> itemMap;

    public ItemConverter(final HashMap<Long, E> itemMap) {
        this.itemMap = itemMap;
    }

    @Override
    public Object getAsObject(final FacesContext context, final UIComponent component, final String string) {
        final Long id = new Long(string);
        if (itemMap.containsKey(id)) {
            return itemMap.get(id);
        }
        LOGGER.error("returning null in ItemConverter for " + " id: " + string + " component: " + component + " context: " + context,
                new IllegalArgumentException());
        return null;
    }

    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final Object item) {
        return ((SelectItem) item).getValue().toString();
    }
}
