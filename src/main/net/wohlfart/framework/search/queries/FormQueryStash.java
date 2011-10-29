package net.wohlfart.framework.search.queries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * this class organizes the possible search term generators ("form query") for
 * each field in the search form, note that the parameters are not stored in the
 * form query but rather in the form field
 * 
 * @author Michael Wohlfart
 * 
 */
public class FormQueryStash implements Serializable {


    // the possible search terms for each field
    private final List<AbstractFormQuery>            formQueries     = new ArrayList<AbstractFormQuery>();
    private final HashMap<String, AbstractFormQuery> formQueriesHash = new HashMap<String, AbstractFormQuery>();

    public void add(final AbstractFormQuery formQuery) {
        final String nextId = formQueries.size() + "";
        formQueries.add(formQuery);
        formQueriesHash.put(nextId, formQuery);
        formQuery.setId(nextId);
    }

    public List<AbstractFormQuery> getFormQueries() {
        return formQueries;
    }

    public AbstractFormQuery getFormQuery(final String id) {
        return formQueriesHash.get(id);
    }

    public boolean containsQueryId(final String queryId) {
        return formQueriesHash.containsKey(queryId);
    }

}
