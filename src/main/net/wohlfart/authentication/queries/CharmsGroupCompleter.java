package net.wohlfart.authentication.queries;

import java.util.List;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.RoleClassification;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

@Name("charmsGroupCompleter")
@BypassInterceptors
public class CharmsGroupCompleter {

    private Session session;

    private String  name;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<String> complete(final String query) {
        if (session == null) {
            session = (Session) Component.getInstance("hibernateSession");
        }
        /*
        final List<String> results = session
            .getNamedQuery(CharmsRole.FIND_NAME_BY_CLASSIFICATION_AND_NAME_LIKE)
            .setParameter("classification", RoleClassification.ORGANISATIONAL.toString())
            .setParameter("name", query)
            .list();
            */
        final List<String> results = session
        .getNamedQuery(CharmsRole.FIND_NAME_BY_NOT_CLASSIF_AND_NAME_LIKE)
        .setParameter("classification", RoleClassification.SHUTTLE.toString())
        .setParameter("name", query)
        .list();
        return results;
    }

}
