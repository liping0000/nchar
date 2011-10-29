package net.wohlfart.framework.sort;

import org.hibernate.Session;

/**
 * implemented by all business objects that are sortable
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public interface ISortable {

    Integer getSortIndex();

    void setSortIndex(Integer sortIndex);

    void setupSortIndex(Session hibernateSession);

}
