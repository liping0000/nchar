package net.wohlfart.user;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.model.SelectItem;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.authentication.CharmsIdentity;
import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.targets.CharmsSearchTargetSetup;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.Priority;
import net.wohlfart.framework.search.AbstractWorkflowBridgeAdaptor;
import net.wohlfart.framework.search.FormField;
import net.wohlfart.framework.search.FullTextSessionImpl;
import net.wohlfart.framework.search.SearchResultItem;
import net.wohlfart.framework.search.queries.AbstractFormQuery;
import net.wohlfart.framework.search.queries.BooleanFormQuery;
import net.wohlfart.framework.search.queries.DateExistsFormQuery;
import net.wohlfart.framework.search.queries.DateExistsNotFormQuery;
import net.wohlfart.framework.search.queries.FormQueryStash;
import net.wohlfart.framework.search.queries.PrioritySelectFormQuery;
import net.wohlfart.framework.search.queries.ReferenceFormQuery;
import net.wohlfart.framework.search.queries.StringFormQuery;
import net.wohlfart.refdata.entities.ChangeRequestCodeItem;
import net.wohlfart.refdata.entities.ChangeRequestProductItem;
import net.wohlfart.refdata.entities.ChangeRequestUnitItem;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.Version;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * hibernate search action see:
 * http://onjava.com/pub/a/onjava/2007/05/24/using-the
 * -lucene-query-parser-without-lucene.html?page=4
 * 
 * @author Michael Wohlfart
 * 
 */

@Scope(ScopeType.CONVERSATION)
@Name("searchActionBean")
public class SearchActionBean extends AbstractActionBean implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(SearchActionBean.class);

    private final static int DEFAULT_MAX_RESULTS = 10;

    private final static DecimalFormat SEARCHTIME_FORMAT = new DecimalFormat("##0.0##");

    @In(value = "hibernateSession")
    private Session hibernateSession;

    // the security frameworks entity for the current user, needed to figure out
    // the
    // access permissions for the search
    @In(value = "org.jboss.seam.security.identity")
    CharmsIdentity charmsIdentity;

    @In(value = CharmsUserIdentityStore.AUTHENTICATED_USER)
    CharmsUser charmsUser;

    // the query is build adding subqueries to this root query
    private BooleanQuery rootQuery;

    // the search expression
    private String expression;

    private Boolean advanced = false;
    private Boolean expert = false;
    private Boolean normal = true;
    // the visible fields to search, for each field a random formQuery can
    // be chosen
    private List<FormField> formFields;

    // all available form queries
    private FormQueryStash formQueryStash;

    // the search result
    private List<SearchResultItem> resultList;

    private int firstResult;
    private int maxResults;
    //
    private int resultSize;

    // just to have a nice user frontend
    private Long searchTime;

    // flags for the UI
    private boolean nextExists;
    private boolean previousExists;

    protected static final List<SelectItem> selectItems = Arrays.asList(new SelectItem[] { 
            new SelectItem("2", "2"), 
            new SelectItem("5", "5"),
            new SelectItem("10", "10"), 
            new SelectItem("25", "25"), 
            new SelectItem("50", "50"), 
            new SelectItem("100", "100") });

    @Create
    @Transactional
    public void create() {
        setFirstResult(0);
        setMaxResults(DEFAULT_MAX_RESULTS);

        LOGGER.debug("create called for SearchActionBean");
        formQueryStash = new FormQueryStash();
        // Priority:
        formQueryStash.add(new PrioritySelectFormQuery("changeRequestDataBean.priority", "priority", Priority.values()));
        // Kennung/Business Key:
        formQueryStash.add(new StringFormQuery("processInstance.key", "businessKey"));
        // Project Number
        formQueryStash.add(new StringFormQuery("changeRequestDataBean.projectIdNumber", "projectIdNumber"));
        // Manager Name
        formQueryStash.add(new StringFormQuery("changeRequestDataBean.managerName", "managerName"));
        // Component ID
        formQueryStash.add(new StringFormQuery("changeRequestDataBean.moduleIdNumber", "moduleIdNumber"));
        // Item ID
        formQueryStash.add(new StringFormQuery("changeRequestDataBean.itemIdNumber", "itemIdNumber"));
        // Customer Name
        formQueryStash.add(new StringFormQuery("changeRequestDataBean.customerName", "customerName"));
        // Title / short Decription
        formQueryStash.add(new StringFormQuery("changeRequestDataBean.title", "title"));

        // product reference
        final List<ChangeRequestProductItem> productItems = ChangeRequestProductItem.getSelect(hibernateSession);
        formQueryStash.add(new ReferenceFormQuery("changeRequestReferenceBean.product", "changeRequestProduct", productItems));

        // unit reference
        final List<ChangeRequestUnitItem> unitItems = ChangeRequestUnitItem.getSelect(hibernateSession);
        formQueryStash.add(new ReferenceFormQuery("changeRequestReferenceBean.unit", "changeRequestUnit", unitItems));

        // code reference
        final List<ChangeRequestCodeItem> codeItems = ChangeRequestCodeItem.getSelect(hibernateSession);
        formQueryStash.add(new ReferenceFormQuery("changeRequestReferenceBean.code", "changeRequestCode", codeItems));

        // goodwill boolean
        formQueryStash.add(new BooleanFormQuery("page.user.search.onlyGoodwill", "goodwill"));
        // standard boolean
        formQueryStash.add(new BooleanFormQuery("page.user.search.onlyStandard", "standard"));

        // finish date exists boolean
        formQueryStash.add(new DateExistsFormQuery("page.user.search.onlyFinished", "finishDate"));
        // finish date exists not boolean
        formQueryStash.add(new DateExistsNotFormQuery("page.user.search.onlyNotFinished", "finishDate"));

        // Submitter User Name
        formQueryStash.add(new StringFormQuery("page.user.search.submittingUserName", "submittingUserName"));
        // Assigner User Name (the TQM), not used atm
        // formQueryStash.add(new
        // StringFormQuery("page.user.search.assigningUserName",
        // "assigningUserName"));
        // Processor/PE User Name (the current user)
        formQueryStash.add(new StringFormQuery("page.user.search.processingUserName", "processingUserName"));

        // setup the form fields by setting up a seperate form field for
        // each defined query
        final List<AbstractFormQuery> list = formQueryStash.getFormQueries();
        formFields = new ArrayList<FormField>(list.size());
        for (final AbstractFormQuery query : list) {
            final FormField field = new FormField();
            field.setSelectedQueryId(query.getId());
            formFields.add(field);
        }
    }

    // for the UI
    @BypassInterceptors
    public int getFirstOnPage() {
        return firstResult + 1;
    }

    @BypassInterceptors
    public int getLastOnPage() {
        final int lastOnPage = firstResult + maxResults;

        if (lastOnPage > resultSize) {
            return resultSize;
        } else {
            return lastOnPage;
        }
    }

    @BypassInterceptors
    public List<SearchResultItem> getResultList() {
        LOGGER.debug("getResultList called: {}", resultList);
        return resultList;
    }

    @BypassInterceptors
    public Boolean getAdvancedSearch() {
        return advanced;
    }

    @BypassInterceptors
    public void doAdvancedSearch() {
        LOGGER.debug("doAdvancedSearch called");
        normal = false;
        advanced = true;
        expert = false;
    }

    @BypassInterceptors
    public Boolean getNormalSearch() {
        return normal;
    }

    @BypassInterceptors
    public void doNormalSearch() {
        LOGGER.debug("doNormalSearch called");
        normal = true;
        advanced = false;
        expert = false;
    }

    @BypassInterceptors
    public Boolean getExpertSearch() {
        return expert;
    }

    @BypassInterceptors
    public void doExpertSearch() {
        LOGGER.debug("doExpertSearch called");
        normal = false;
        advanced = false;
        expert = true;
    }

    /**
     * return the fulltext search expression
     * 
     * @return
     */
    @BypassInterceptors
    public String getExpression() {
        return expression;
    }

    @BypassInterceptors
    public void setExpression(final String expression) {
        this.expression = expression;
    }

    @BypassInterceptors
    public int getMaxResults() {
        return maxResults;
    }

    @BypassInterceptors
    public void setMaxResults(final int maxResults) {
        this.maxResults = maxResults;
    }

    @BypassInterceptors
    public int getFirstResult() {
        return firstResult;
    }

    @BypassInterceptors
    public void setFirstResult(final int firstResult) {
        this.firstResult = firstResult;
    }

    @BypassInterceptors
    public int getResultSize() {
        return resultSize;
    }

    @BypassInterceptors
    public boolean isPreviousExists() {
        return previousExists;
    }

    @BypassInterceptors
    public boolean isNextExists() {
        return nextExists;
    }

    @BypassInterceptors
    public Long getSearchTime() {
        return searchTime;
    }

    @BypassInterceptors
    public String getTimeString() {
        if (searchTime == null) {
            LOGGER.warn("searchtime is null, probably a programming error");
            return "-";
        }
        // searchTime is in nano secs: nano -> micro (1e3) -> milli(1e6) -> 1(1e9)
        return SEARCHTIME_FORMAT.format(searchTime.floatValue() / 1e9);
    }

    // ------------- form magic ---------------------------

    // see: http://www.roseindia.net/jsf/componenetInstanceBinding.shtml

    @BypassInterceptors
    public List<FormField> getFormFields() {
        LOGGER.debug("getFormFields called: {}", formFields);
        return formFields;
    }

    @BypassInterceptors
    public List<AbstractFormQuery> getFormQueries() {
        LOGGER.debug("getFormQueries called, stash is: {}", formQueryStash);
        return formQueryStash.getFormQueries();
    }

    @BypassInterceptors
    public AbstractFormQuery getFormQuery(final String key) {
        LOGGER.debug("getFormQuery called: {}", key);
        return formQueryStash.getFormQuery(key);
    }

    // adding another row in the form
    @BypassInterceptors
    public void addField() {
        LOGGER.debug("addField called");
        formFields.add(new FormField());
    }

    // remove a search field from the form
    @BypassInterceptors
    public void subField() {
        // this should come from a parameter
        final int number = formFields.size() - 1;

        LOGGER.debug("subField called: {}", number);
        if ((number >= 0) && (number < formFields.size())) {
            formFields.remove(number);
        } else {
            LOGGER.warn("subField called with invalid number {}, field array is of size {}", number, formFields.size());
        }
    }

    // ------------- page actions ---------------------------

    // paging actions from the UI
    @Transactional
    public void next() {
        LOGGER.debug("next called");
        firstResult = firstResult + maxResults;
        doSearch();
    }

    @Transactional
    public void last() {
        LOGGER.debug("last called, first result before: {}", firstResult);
        final int lastPageElements = resultSize % maxResults;
        if (lastPageElements == 0) {
            firstResult = resultSize - maxResults;
        } else {
            firstResult = resultSize - lastPageElements;
        }
        LOGGER.debug("last called, first result after: {}", firstResult);
        doSearch();
    }

    @Transactional
    public void first() {
        LOGGER.debug("first called");
        firstResult = 0;
        doSearch();
    }

    @Transactional
    public void previous() {
        LOGGER.debug("previous called");
        firstResult = firstResult - maxResults;
        doSearch();
    }

    // ------------- private methods ---------------------------

    /**
     * the actual search action this is triggered from the UI see:
     * http://docs.jboss
     * .org/hibernate/stable/search/reference/en/html/search-query.html
     * 
     * triggered on form submit
     */
    @Transactional
    public String search() {
        firstResult = 0;
        try {
            return doSearch();
        } catch (final SearchException ex) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.search.searchException", ex.toString());
            ex.printStackTrace();
            LOGGER.error("recovering from error");
            return "invalid";
        }
    }

    public String clear() {
        firstResult = 0;
        expression = null;
        for (final FormField field : formFields) {
            field.clear();
        }
        return "cleared";
    }

    @SuppressWarnings("unchecked")
    public List<ChangeRequestData> getChangeRequestDataList() {
        // hibernate search's full text session
        // FullTextSession fullTextSession =
        // org.hibernate.search.Search.getFullTextSession(hibernateSession);
        final FullTextSession fullTextSession = new FullTextSessionImpl(hibernateSession);
        final org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(rootQuery);
        final List<ChangeRequestData> rawResultList = fullTextQuery.list();
        return rawResultList;
    }

    /**
     * 
     * this method des the real work....
     * 
     * 
     * FIXME: this method is called during paging and adds a FacesMessages
     * however the message part in the UI is not refreshed on paging...
     * 
     * FIXME: check if we can do sorting here
     * 
     */
    @SuppressWarnings("rawtypes")
    private String doSearch() {
        LOGGER.debug("search called for expression: {}", expression);

        // FIXME: this is the way to deal with faces messages, performance wise
        // that is
        final FacesMessages facesMessages = FacesMessages.instance();

        if (StringUtils.isEmpty(expression) && (!advanced)) {
            facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.search.expressionRequired");
            // ooops, we have no search here
            return "searched";
        }

        // we add any user queries to this base query
        rootQuery = new BooleanQuery();
        // adding a query is done like this:
        // rootQuery.add(perm_query, true, false);
        // rootQuery.add(user_query, true, false);

        // hibernate search's full text session
        // FullTextSession fullTextSession =
        // org.hibernate.search.Search.getFullTextSession(hibernateSession);
        final FullTextSession fullTextSession = new FullTextSessionImpl(hibernateSession);

        try {
            // the document fields that will be searched, note there are no reference
            // entities like machine, region and code since they are translated
            final String[] fields = new String[] { 
                    "projectIdNumber", 
                    "managerName", 
                    "initiateDate", 
                    "moduleIdNumber", 
                    "itemIdNumber", 
                    "customerName",
                    "goodwillText", 
                    "standardText", 
                    "problemDescription", 
                    "proposalDescription", 
                    "conclusionDescription", 
                    "historyDescription", 
                    "businessKey",
                    "title", 
                    AbstractWorkflowBridgeAdaptor.PARTICIPATING_USER_NAME, 
                    AbstractWorkflowBridgeAdaptor.MESSAGE_TEXT };

            // simple expression search
            if (!StringUtils.isEmpty(expression)) {
                // shoud be same as:
                final MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_29, fields, new StandardAnalyzer(Version.LUCENE_29));
                // user may use leading wildcards which is bad for
                // performance...
                parser.setAllowLeadingWildcard(true);
                final org.apache.lucene.search.Query expressionQuery = parser.parse(expression);
                rootQuery.add(expressionQuery, BooleanClause.Occur.MUST);
                LOGGER.debug("rootQuery for expression is {}", rootQuery.toString());
            }

            // the advanced field search, collecting all queries from the form fields
            boolean hasAdvanced = false;
            if (advanced) { // advanced search fields are visible in the UI
                for (final FormField field : formFields) {
                    // get the selected query for each field (changing a query
                    // is not yet implemented)
                    final String queryId = field.getSelectedQueryId();
                    if (!formQueryStash.containsQueryId(queryId)) { // someone might hack the queryId in the form
                        LOGGER.warn("selectedQueryId: {} not found in the query hash, something wrong with the search form or someone is trying to hack the form");
                    } else {
                        try {
                            final BooleanClause booleanClause = formQueryStash.getFormQuery(queryId) // get the query from the field
                                    .getBooleanClause(field); // feed the form values into the formQuery and let the query generate the clause
                            if (booleanClause != null) { // is null if there is no search criteria selected for this field (no values)
                                hasAdvanced = true; // mark mthis search as having advanced search queries
                                rootQuery.add(booleanClause);
                            }
                        } catch (final ParseException ex) {
                            ex.printStackTrace();
                            LOGGER.warn("error parsing value: {} for creating query, parsing errors should be caught by the query and make them return null");
                        }

                    }
                }
            }

            // a criteria to filter out the not submitted documents field3:[* TO *]
            // see: http://osdir.com/ml/solr-user.lucene.apache.org/2009-09/msg00198.html
            final QueryParser parser = new QueryParser(Version.LUCENE_29, "submitDate", new StandardAnalyzer(Version.LUCENE_29));
            parser.setAllowLeadingWildcard(true);
            final org.apache.lucene.search.Query mustBeSubmittedQuery = parser.parse("*"); // not
                                                                                           // null
            rootQuery.add(mustBeSubmittedQuery, BooleanClause.Occur.MUST);


            if (charmsIdentity.hasPermission(CharmsSearchTargetSetup.TARGET_STRING, CharmsSearchTargetSetup.ALL)) {
                // no restrictions, may search everything
                
            } else if (charmsIdentity.hasPermission(CharmsSearchTargetSetup.TARGET_STRING, CharmsSearchTargetSetup.PARTICIPATED)) {
                // add a query condition that limits the result to participated changerequests
                final QueryParser permParser = new QueryParser(
                        Version.LUCENE_29, 
                        AbstractWorkflowBridgeAdaptor.PARTICIPATING_USER_ID, 
                        new StandardAnalyzer(Version.LUCENE_29));
                final org.apache.lucene.search.Query permQuery = permParser.parse(charmsUser.getId().toString());
                rootQuery.add(permQuery, BooleanClause.Occur.MUST);
                
            } else if (charmsIdentity.hasPermission(CharmsSearchTargetSetup.TARGET_STRING, CharmsSearchTargetSetup.SUBMITTED)) {
                // add a query condition that limits the result to submitted changerequests
                final QueryParser permParser = new QueryParser(
                        Version.LUCENE_29, 
                        AbstractWorkflowBridgeAdaptor.SUBMITTING_USER_ID, 
                        new StandardAnalyzer(Version.LUCENE_29));
                final org.apache.lucene.search.Query permQuery = permParser.parse(charmsUser.getId().toString());
                rootQuery.add(permQuery, BooleanClause.Occur.MUST);
                
            } else {
                // fallback: this shouldn't happen since the user should not see this page at all
                LOGGER.warn("configuration problem for the search permissions, user is given access to submitted processes, missing permission");
                final QueryParser permParser = new QueryParser(
                        Version.LUCENE_29, 
                        AbstractWorkflowBridgeAdaptor.SUBMITTING_USER_ID, 
                        new StandardAnalyzer(Version.LUCENE_29));
                final org.apache.lucene.search.Query permQuery = permParser.parse(charmsUser.getId().toString());
                rootQuery.add(permQuery, BooleanClause.Occur.MUST);
            }

            LOGGER.info("the lucene search expression is: >{}<", rootQuery.toString());

            // ----- start meassuring the search time
            final long t0 = System.nanoTime();
            // wrap Lucene query in a javax.persistence.Query
            final org.hibernate.search.FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(rootQuery);
            fullTextQuery.setFirstResult(firstResult);
            fullTextQuery.setMaxResults(maxResults);

            // execute search and set the properties
            final List rawResultList = fullTextQuery.list();
            resultList = wrapResultList(rawResultList);
            resultSize = fullTextQuery.getResultSize();
            previousExists = firstResult > 0;
            nextExists = (firstResult + resultList.size()) < resultSize;
            searchTime = System.nanoTime() - t0;
            // ----- stop meassuring the search time

            LOGGER.debug("result list:");
            for (final Object object : resultList) {
                LOGGER.debug("object: {} class is {}", object, object.getClass().getName());
            }

            LOGGER.debug("resultSize: {}", resultSize);
            LOGGER.debug("maxResults: {}", maxResults);
            LOGGER.debug("firstResult: {}", firstResult);

            // figure out a nice message for the user...
            if (StringUtils.isEmpty(expression) && (!hasAdvanced)) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.search.expressionRequired");
            } else if ((resultSize == 0) && (StringUtils.isEmpty(expression))) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.search.zeroResultsNoExpression");
            } else if (resultSize == 0) {
                facesMessages.addFromResourceBundle(Severity.ERROR, "page.user.search.zeroResultsWithExpression", expression);
            } else if ((resultSize == 1) && (StringUtils.isEmpty(expression))) {
                facesMessages.addFromResourceBundle(Severity.INFO, "page.user.search.singleResultNoExpression");
            } else if (resultSize == 1) {
                facesMessages.addFromResourceBundle(Severity.INFO, "page.user.search.singleResultWithExpression", expression);
            } else if ((resultSize > 1) && (StringUtils.isEmpty(expression))) {
                facesMessages.addFromResourceBundle(Severity.INFO, "page.user.search.resultListNoExpression", resultSize);
            } else if (resultSize > 1) {
                facesMessages.addFromResourceBundle(Severity.INFO, "page.user.search.resultListWithExpression", resultSize, expression);
            }

        } catch (final org.apache.lucene.queryParser.ParseException ex) {
            LOGGER.warn("ParseException", ex);
            facesMessages.addFromResourceBundle(Severity.INFO, "page.user.search.searchException", ex.getLocalizedMessage());
        }
        return "searched";
    }

    @SuppressWarnings("rawtypes")
    private List<SearchResultItem> wrapResultList(final List rawResultList) {
        final List<SearchResultItem> wrappedResultList = new ArrayList<SearchResultItem>();
        for (final Object object : rawResultList) {
            wrappedResultList.add(new SearchResultItem(object));
        }
        return wrappedResultList;
    }

    /**
     * the faces trace API uses the toString Method to display some information
     * about the components in the UI we need to make sure Seam's Bijection
     * doesn't kick in and gives us an exception
     */
    @Override
    @BypassInterceptors
    public String toString() {
        return super.toString();
    }

}
