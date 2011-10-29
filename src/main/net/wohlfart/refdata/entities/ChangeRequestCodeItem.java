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
public class ChangeRequestCodeItem extends SelectItem {

    private final String messageCode;

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestCodeItem> getSelect(Session session) {
        return session.createQuery(
            "select new " 
            + ChangeRequestCodeItem.class.getName() 
            + "(e.id, e.messageCode, e.defaultName) " 
            + " from " 
            + ChangeRequestCode.class.getName()
            + " e"
            + " order by e.sortIndex")
            .list();
    }

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestCodeItem> getEnabledSelect(Session session, boolean enabled) {
        return session.createQuery(    
            "select new " 
            + ChangeRequestCodeItem.class.getName() 
            + "(e.id, e.messageCode, e.defaultName) " 
            + " from " 
            + ChangeRequestCode.class.getName()
            + " e" 
            + " where e.enabled = :enabled " 
            + " order by e.sortIndex")
            .setParameter("enabled", enabled)
            .list();
    }

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestCodeItem> getEnabledSelectForProduct(Session session, boolean enabled, Long productId) {
        return session.createQuery(
            "select new "
            + ChangeRequestCodeItem.class.getName() 
            + "(code.id, code.messageCode, code.defaultName) " 
            + " from "
            + ChangeRequestProduct.class.getName() 
            + " prod " 
            + " join prod.codes code " 
            + "  where prod.enabled = :enabled "
            + "    and prod.id = :productId " 
            + " order by POSITION_ ")
            .setParameter("enabled", enabled)
            .setParameter("productId", productId)
            .list();
    }

    public ChangeRequestCodeItem(final Long value, final String messageCode, final String defaultName) {
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
