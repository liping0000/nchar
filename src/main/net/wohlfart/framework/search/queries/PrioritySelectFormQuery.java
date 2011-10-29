package net.wohlfart.framework.search.queries;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import net.wohlfart.changerequest.entities.Priority;
import net.wohlfart.framework.search.FormField;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrioritySelectFormQuery extends AbstractFormQuery {


    private final static Logger LOGGER = LoggerFactory.getLogger(PrioritySelectFormQuery.class);

    public PrioritySelectFormQuery(
    /* String id, */
    final String msgCode, final String fieldname, final Priority[] enumeration) {
        super(/* id, */msgCode, fieldname, AbstractFormQuery.REFERENCE);

        final List<SelectItem> refSelects = new ArrayList<SelectItem>();
        int count = 0;
        for (final Priority p : enumeration) {
            final SelectItem item = new SelectItem();
            item.setLabel(p.getMsgCode());
            item.setValue("" + count);
            count++;
            refSelects.add(item);
        }
        setRefSelects(refSelects);
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
