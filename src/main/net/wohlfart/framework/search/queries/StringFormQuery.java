package net.wohlfart.framework.search.queries;

import net.wohlfart.framework.search.FormField;


import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringFormQuery extends AbstractFormQuery {


    private final static Logger LOGGER = LoggerFactory.getLogger(StringFormQuery.class);

    public StringFormQuery(
    /* String id, */
    final String msgCode, final String fieldname) {
        super(/* id, */msgCode, fieldname, AbstractFormQuery.STRING);
    }

    @Override
    public BooleanClause getBooleanClause(final FormField formField) throws ParseException {
        LOGGER.debug("returning query for field: " + formField);
        if (StringUtils.isEmpty(formField.getValue1())) {
            return null;
        } else {
            final QueryParser parser = createQueryParser();
            // new QueryParser(fieldname, new StandardAnalyzer());
            parser.setAllowLeadingWildcard(true);
            return new BooleanClause(parser.parse(formField.getValue1()), BooleanClause.Occur.MUST);
        }
    }

}
