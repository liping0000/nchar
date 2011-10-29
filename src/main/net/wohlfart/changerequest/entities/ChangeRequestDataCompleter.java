package net.wohlfart.changerequest.entities;

import java.util.List;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

@Name("changeRequestDataCompleter")
@BypassInterceptors
public class ChangeRequestDataCompleter {

    private Session session;

    @SuppressWarnings("unchecked")
    @Transactional
    public List<String> completeManagerName(final String query) {
        if (session == null) {
            session = (Session) Component.getInstance("hibernateSession");
        }
        final List<String> results = session
           .getNamedQuery(ChangeRequestData.FIND_MANAGER_NAME_BY_NAME_LIKE)
           .setParameter("name", query)
           .list();
        return results;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<String> completeCustomerName(final String query) {
        if (session == null) {
            session = (Session) Component.getInstance("hibernateSession");
        }
        final List<String> results = session
            .getNamedQuery(ChangeRequestData.FIND_CUSTOMER_NAME_BY_NAME_LIKE)
            .setParameter("name", query)
            .list();
        return results;
    }

}
