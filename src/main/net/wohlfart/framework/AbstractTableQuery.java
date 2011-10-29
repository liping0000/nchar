package net.wohlfart.framework;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.framework.HibernateEntityQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * implements some of the basic features of a page-able table to use this class
 * you have to subclass and override the following methods:
 * 
 * protected String getCountEjbql()
 * 
 * public void setFragment(String fragment)
 * 
 * 
 * @author Michal Wohlfart
 * 
 * @param <R>
 *            a row implementation used in the view layer
 */
public abstract class AbstractTableQuery<R> extends HibernateEntityQuery<R> {


    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractTableQuery.class);

    protected String fragment;

    // override in subclass to setup the ejbQuery...
    public abstract void setup();

    // this gives us all possible columns for ordering
    public abstract Set<String> getColumnsForOrder();

    @Override
    public void validate() {
        setup();
        super.validate();
    }

    protected static final List<SelectItem> selectItems = 
        Arrays.asList(new SelectItem[] { 
                new SelectItem("2", "2"), 
                new SelectItem("5", "5"),
                new SelectItem("10", "10"), 
                new SelectItem("25", "25"), 
                new SelectItem("50", "50"), 
                new SelectItem("100", "100") });

    protected AbstractTableQuery() {
        setMaxResults(25);
    }

    @BypassInterceptors
    public List<SelectItem> getSelectItems() {
        return selectItems;
    }

    @BypassInterceptors
    public String getFragment() {
        return fragment;
    }

    @BypassInterceptors
    public void setFragment(final String fragment) {
        this.fragment = fragment;
        // go to the first page
        first();
    }

    @BypassInterceptors
    public String getOrderForColumn(final String column) {
        if (column.equals(getOrderColumn())) {
            return getOrderDirection();
        }
        return "srt"; // used as css class
    }

    @BypassInterceptors
    public void toggleOrder(final String newOrderColumn) {

        if (!getColumnsForOrder().contains(newOrderColumn)) {
            LOGGER.warn("column {} not in list of orderable columns, orderable columns are: {}, check the UI there is a wrong link ", 
                    newOrderColumn, getColumnsForOrder());
            return;
        }

        final String currentOrderColumn = getOrderColumn();
        final String currentOrderDirection = getOrderDirection();

        if ((currentOrderColumn == null) || (!currentOrderColumn.equals(newOrderColumn))) {
            // initial order direction
            setOrderDirection("asc");
            setOrderColumn(newOrderColumn);
        } else {
            // toggle the order
            setOrderDirection("desc".equals(currentOrderDirection) ? "asc" : "desc");
            setOrderColumn(newOrderColumn);
        }

        // go to the first page
        first();
    }

    @Override
    public void setMaxResults(final Integer maxResults) {
        super.setMaxResults(maxResults);
        // go to the first page
        first();
    }

}
