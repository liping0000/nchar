package net.wohlfart.framework.search.queries;

import net.wohlfart.framework.search.FormField;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateExistsNotFormQuery extends AbstractFormQuery {


    private final static Logger LOGGER = LoggerFactory.getLogger(DateExistsNotFormQuery.class);

    public DateExistsNotFormQuery(
    /* String id, */
    final String msgCode, final String fieldname) {
        super(/* id, */msgCode, fieldname, AbstractFormQuery.BOOLEAN);
    }

    @Override
    public BooleanClause getBooleanClause(final FormField formField) throws ParseException {
        LOGGER.debug("returning query for field: " + formField);
        if ((formField.getValueBoolean() != null) && (formField.getValueBoolean() != true)) {
            return null;
        } else {
            final QueryParser parser = createQueryParser();
            // new QueryParser(fieldname, new StandardAnalyzer());
            parser.setAllowLeadingWildcard(true);
            final org.apache.lucene.search.Query mustNotQuery = parser.parse("*"); // not
                                                                                   // null
            // query.add(mustBeSubmittedQuery, BooleanClause.Occur.MUST_NOT);
            return new BooleanClause(mustNotQuery, BooleanClause.Occur.MUST_NOT);
        }
    }

}
