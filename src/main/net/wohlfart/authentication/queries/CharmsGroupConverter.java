package net.wohlfart.authentication.queries;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import net.wohlfart.authentication.entities.CharmsRole;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Converter;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

@Name("charmsGroupConverter")
@Converter(forClass = CharmsRole.class, id = "charmsGroupConverter")
@BypassInterceptors
public class CharmsGroupConverter implements javax.faces.convert.Converter {

    @Override
    public Object getAsObject(final FacesContext context, final UIComponent component, final String string) {
        if (StringUtils.isNotEmpty(string) && StringUtils.isNumeric(string)) {
            final Long id = Long.parseLong(string);
            final Session session = (Session) Component.getInstance("hibernateSession");
            return session.get(CharmsRole.class, id);
        } else {
            return null;
        }
    }

    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final Object object) {
        if ((object != null) && (object instanceof CharmsRole)) {
            return ((CharmsRole) object).getId().toString();
        } else {
            return null;
        }
    }

}
