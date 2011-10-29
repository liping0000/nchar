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
public class ChangeRequestUnitItem extends SelectItem {

    private final String messageCode;

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestUnitItem> getSelect(Session session) {
        return session.createQuery(
            "select new " 
            + ChangeRequestUnitItem.class.getName() 
            + "(p.id, p.messageCode, p.defaultName) " 
            + " from "
            + ChangeRequestUnit.class.getName()
            + " p"
            + " order by p.sortIndex")
            .list();
    }

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestUnitItem> getEnabledSelect(Session session, boolean enabled) {
        return session.createQuery(
            "select new " 
            + ChangeRequestUnitItem.class.getName() 
            + "(p.id, p.messageCode, p.defaultName) " 
            + " from " 
            + ChangeRequestUnit.class.getName()
            + " p" 
            + " where p.enabled = :enabled " 
            + " order by p.sortIndex")
            .setParameter("enabled", enabled)
            .list();
    }

    @SuppressWarnings("unchecked")
    public static List<ChangeRequestUnitItem> getEnabledSelectForProduct(Session session, boolean enabled, Long productId) {
        return session.createQuery(
            "select new " 
            + ChangeRequestUnitItem.class.getName() 
            + "(unit.id, unit.messageCode, unit.defaultName) " 
            + " from "
            + ChangeRequestProduct.class.getName() 
            + " prod " 
            + " join prod.units unit " 
            + "  where prod.enabled = :enabled "
            + "    and prod.id = :productId " 
            + " order by POSITION_ ")
            .setParameter("enabled", enabled)
            .setParameter("productId", productId)
            .list();
    }

    public ChangeRequestUnitItem(final Long value, final String messageCode, final String defaultName) {
        super(value, defaultName);
        this.messageCode = messageCode;
    }

    public String getDefaultName() {
        return getLabel();
    }

    public String getMessageCode() {
        return messageCode;
    }
}
