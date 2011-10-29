package net.wohlfart.authentication.entities;

import java.util.List;

import javax.faces.model.SelectItem;

import org.hibernate.Session;

/**
 * this class is used to select user 
 * 
 * FIXME: use the label instead of the name
 * 
 * @author Michael Wohlfart
 */
public class CharmsUserItem extends SelectItem {

    @SuppressWarnings("unchecked")
    public static List<CharmsUserItem> getSelect(Session session) {
        return session.createQuery(
          "select new " 
          + CharmsUserItem.class.getName() 
          + "(" 
          + "u.id," 
          + "u.firstname," 
          + "u.lastname," 
          + "u.name" 
          + ")" 
          + " from "
          + CharmsUser.class.getName() 
          + " u" 
          + " order by u.lastname").list();
    }

    public CharmsUserItem(final Long value, final String firstname, final String lastname, final String username) {
        super(value, ((lastname == null) ? "" : lastname) + ", " + ((firstname == null) ? "" : firstname) + " [" + username + "]");
    }

}
