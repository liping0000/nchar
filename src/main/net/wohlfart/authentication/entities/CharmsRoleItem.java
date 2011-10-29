package net.wohlfart.authentication.entities;

import java.util.List;

import javax.faces.model.SelectItem;

import org.hibernate.Session;

/**
 * this class is used to select role 
 * 
 * FIXME: use the label instead of the name
 *
 * @author Michael Wohlfart
 */
public class CharmsRoleItem extends SelectItem {

    @SuppressWarnings("unchecked")
    public static List<CharmsRoleItem> getShuttleSelect(Session session) {
        return session.createQuery(
          "select new " 
          + CharmsRoleItem.class.getName() 
          + "(" 
          + "r.id," 
          + "r.name" 
          + ")" 
          + " from " 
          + CharmsRole.class.getName() 
          + " r"
          + " where r.classification = :classification" 
          + " order by r.name")
          .setParameter("classification", RoleClassification.AUTHORIZATIONAL)
          .list();
    }
    
    @SuppressWarnings("unchecked")
    public static List<CharmsRoleItem> getNotShuttleSelect(Session session) {
        return session.createQuery(
          "select new " 
          + CharmsRoleItem.class.getName() 
          + "(" 
          + "r.id," 
          + "r.name" 
          + ")" 
          + " from " 
          + CharmsRole.class.getName() 
          + " r"
          + " where r.classification != :classification" 
          + " or r.classification is null "
          + " order by r.name")
          .setParameter("classification", RoleClassification.AUTHORIZATIONAL)
          .list();
    }

    @SuppressWarnings("unchecked")
    public static List<CharmsRoleItem> getAllSelect(Session session) {
        return  session.createQuery(
          "select new " 
          + CharmsRoleItem.class.getName() 
          + "(" 
          + "r.id," 
          + "r.name" 
          + ")" 
          + " from " 
          + CharmsRole.class.getName() 
          + " r"
          + " order by r.name").list();
    }

    
    public CharmsRoleItem(final Long value, final String label) {
        super(value, label);
    }

}
