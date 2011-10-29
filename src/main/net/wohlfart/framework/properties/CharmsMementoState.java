package net.wohlfart.framework.properties;

import java.io.Serializable;

import org.richfaces.json.JSONException;
import org.richfaces.json.JSONMap;
import org.richfaces.json.JSONObject;

public class CharmsMementoState implements Serializable {


    // we use json to store complex objects into a single database properties field:
    private JSONObject delegate = new JSONObject();

    // keys for the json map
    // private static final String FRAGMENT_KEY = "fragment";
    // private static final String SORT_COLUMN_KEY = "sortColumn";
    // private static final String PAGE_SIZE_KEY = "pageSize";

    public void setValue(final String value) {
        try {
            delegate = new JSONObject(value);
        } catch (final JSONException e) {
            e.printStackTrace();
        }
    }

    public String getValue() {
        return delegate.toString();
    }

    public void put(final String key, final Integer integer) {
        try {
            delegate.put(key, integer);
        } catch (final JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void put(final String key, final String string) {
        try {
            delegate.put(key, string);
        } catch (final JSONException ex) {
            ex.printStackTrace();
        }
    }

    public Integer getInt(final String key) {
        try {
            return delegate.getInt(key);
        } catch (final JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String getString(final String key) {
        try {
            return delegate.getString(key);
        } catch (final JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /*
     * public Integer getPageSize() { try { return object.getInt(PAGE_SIZE_KEY);
     * } catch (JSONException e) { return null; } } public void
     * setPageSize(Integer pageSize) { try { object.put(PAGE_SIZE_KEY,
     * pageSize); } catch (JSONException e) { e.printStackTrace(); } }
     * 
     * 
     * public String getSortColumn() { try { return
     * object.getString(SORT_COLUMN_KEY); } catch (JSONException e) {
     * e.printStackTrace(); return null; } } public void setSortColumn(String
     * sortColumn) { try { object.put(SORT_COLUMN_KEY, sortColumn); } catch
     * (JSONException e) { e.printStackTrace(); } }
     * 
     * 
     * public String getFragment() { try { return
     * object.getString(FRAGMENT_KEY); } catch (JSONException e) {
     * e.printStackTrace(); return null; } } public void setFragment(String
     * fragment) { try { object.put(FRAGMENT_KEY, fragment); } catch
     * (JSONException e) { e.printStackTrace(); } }
     */
}
