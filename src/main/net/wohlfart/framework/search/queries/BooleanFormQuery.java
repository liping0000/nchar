package net.wohlfart.framework.search.queries;

import net.wohlfart.framework.search.FormField;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BooleanFormQuery extends AbstractFormQuery {


    private final static Logger LOGGER = LoggerFactory.getLogger(BooleanFormQuery.class);

    public BooleanFormQuery(
    /* String id, */
    final String msgCode, final String fieldname) {
        super(/* id, */msgCode, fieldname, AbstractFormQuery.BOOLEAN);
    }

    @Override
    public BooleanClause getBooleanClause(final FormField formField) throws ParseException {
        LOGGER.debug("returning query for field: " + formField);
        // String fieldname = query.getFieldname();
        // parser.setAllowLeadingWildcard(true);
        if ((formField.getValueBoolean() != null) && (formField.getValueBoolean() != true)) {
            return null;
        } else {
            final QueryParser parser = createQueryParser();
            // new QueryParser(Version.LUCENE_29, fieldname, new
            // StandardAnalyzer(Version.LUCENE_29));
            return new BooleanClause(parser.parse("true"), BooleanClause.Occur.MUST);
        }
    }

}
