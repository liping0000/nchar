package net.wohlfart.framework.search;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormField implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(FormField.class);

    // the queryId that is seletced and that will be used for this fields query
    // in the xpert version we will be able to change this but for now its fixed
    // and set in the constructor
    private String              selectedQueryId;

    // we need max two values for the range queries
    private String              value1;
    private String              value2;
    private Boolean             valueBoolean;

    public FormField() {
    }

    public FormField(final String selectedQueryId) {
        this.selectedQueryId = selectedQueryId;
    }

    public void clear() {
        value1 = null;
        value2 = null;
        valueBoolean = null;
    }

    public void setSelectedQueryId(final String selectedQueryId) {
        LOGGER.debug("selectedQueryId: " + selectedQueryId);
        this.selectedQueryId = selectedQueryId;
    }

    public String getSelectedQueryId() {
        return selectedQueryId;
    }

    public void setValue1(final String value1) {
        LOGGER.debug("setValue1: " + value1);
        this.value1 = value1;
    }

    public String getValue1() {
        return value1;
    }

    public void setValue2(final String value2) {
        LOGGER.debug("setValue2: " + value2);
        this.value2 = value2;
    }

    public String getValue2() {
        return value2;
    }

    public void setValueBoolean(final Boolean valueBoolean) {
        LOGGER.debug("setValueBoolean: " + valueBoolean);
        this.valueBoolean = valueBoolean;
    }

    public Boolean getValueBoolean() {
        return valueBoolean;
    }

    /**
     * this is where the magic happens, we turn the following data into a query
     * - value1, value2 -
     * 
     * 
     * public BooleanClause getBooleanClause(HashMap<String, AbstractFormQuery>
     * queryHash) {
     * 
     * if (!queryHash.containsKey(selectedQueryId)) {
     * LOGGER.warn("selectedQueryId: {} not found in the query hash"); return
     * null; }
     * 
     * try { BooleanClause booleanClause =
     * queryHash.get(selectedQueryId).getBooleanClause(this); return
     * booleanClause; } catch (ParseException ex) { ex.printStackTrace();
     * LOGGER.warn("error parsing value: {} for creating query"); }
     * 
     * return null; }
     */
}
