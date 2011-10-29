package net.wohlfart.framework.search.queries;

import java.util.List;

import javax.faces.model.SelectItem;

import net.wohlfart.framework.search.FormField;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceFormQuery extends AbstractFormQuery {


    private final static Logger LOGGER = LoggerFactory.getLogger(ReferenceFormQuery.class);

    public ReferenceFormQuery(
    /* String id, */
    final String msgCode, final String fieldname, final List<? extends SelectItem> refSelects) {
        super(/* id, */msgCode, fieldname, AbstractFormQuery.REFERENCE, refSelects);
    }

    @Override
    public BooleanClause getBooleanClause(final FormField formField) throws ParseException {
        LOGGER.debug("returning query for field: " + formField);
        final String value = formField.getValue1();
        if (StringUtils.isEmpty(value)) {
            // user didn't select a value for this formfield
            return null;
        } else {
            final QueryParser parser = createQueryParser();
            // new QueryParser(fieldname, new StandardAnalyzer());
            parser.setAllowLeadingWildcard(true);
            return new BooleanClause(parser.parse(value), BooleanClause.Occur.MUST);
        }
    }

}
