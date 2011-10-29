package net.wohlfart.refdata.entities;

import java.util.List;

import javax.faces.model.SelectItem;

import org.hibernate.Session;

/**
 * used to select user in the role setup page or for assigning a task to a user
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class ChangeRequestProductItem extends SelectItem {

    private final String messageCode;

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestProductItem> getSelect(Session session) {
        return session.createQuery(
            "select new " 
            + ChangeRequestProductItem.class.getName() 
            + "(p.id, p.messageCode, p.defaultName) " 
            + " from "
            + ChangeRequestProduct.class.getName() 
            + " p" 
            + " order by p.sortIndex")
            .list();
    }

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestProductItem> getEnabledSelect(Session session, boolean enabled) {
        return session.createQuery(
            "select new " 
            + ChangeRequestProductItem.class.getName() 
            + "(p.id, p.messageCode, p.defaultName) " 
            + " from "
            + ChangeRequestProduct.class.getName() 
            + " p" 
            + " where p.enabled = :enabled " 
            + " order by p.sortIndex")
            .setParameter("enabled", enabled)
            .list();
    }

    public ChangeRequestProductItem(final Long value, final String messageCode, final String defaultName) {
        super(value, defaultName);
        this.messageCode = messageCode;
    }

    // @Override
    public String getDefaultName() {
        return getLabel();
    }

    // @Override
    public String getMessageCode() {
        return messageCode;
    }

}
