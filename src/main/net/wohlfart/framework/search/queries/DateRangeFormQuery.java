package net.wohlfart.framework.search.queries;

import net.wohlfart.framework.search.FormField;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateRangeFormQuery extends AbstractFormQuery {


    private final static Logger LOGGER = LoggerFactory.getLogger(DateRangeFormQuery.class);

    protected DateRangeFormQuery(
    /* String id, */
    final String msgCode, final String fieldname, final String dataType) {
        super(/* id, */msgCode, fieldname, AbstractFormQuery.DATE_RANGE);
    }

    @Override
    public BooleanClause getBooleanClause(final FormField formField) throws ParseException {
        LOGGER.debug("returning query for field: " + formField);

        // TODO Auto-generated method stub
        return null;
    }

}
