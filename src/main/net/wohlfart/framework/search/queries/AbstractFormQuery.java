package net.wohlfart.framework.search.queries;

import java.io.Serializable;
import java.util.List;

import javax.faces.model.SelectItem;

import net.wohlfart.framework.search.FormField;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.util.Version;

/**
 * this class defines one possible query, the concrete user selected or entered
 * values for the search are in the SearchField
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public abstract class AbstractFormQuery extends SelectItem implements Serializable {


    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(AbstractFormQuery.class);

    public String                      fieldname;
    public String                      dataType;

    // the type of data we need for this field, this is used in the UI
    public static final String         DATE           = "DATE";
    public static final String         DATE_RANGE     = "DATE_RANGE";
    public static final String         STRING         = "STRING";
    public static final String         STRING_RANGE   = "STRING_RANGE";
    public static final String         INETEGER       = "INETEGER";
    public static final String         INETEGER_RANGE = "INETEGER_RANGE";
    public static final String         BOOLEAN        = "BOOLEAN";
    public static final String         REFERENCE      = "REFERENCE";
    // the list of possible select items if this is a REFERENCE query
    private List<? extends SelectItem> refSelects;

    protected AbstractFormQuery(/* String id, */
    final String msgCode, final String fieldname, final String dataType) {
        super(/* id */null, msgCode);
        this.fieldname = fieldname;
        this.dataType = dataType;
    }

    protected AbstractFormQuery(/* String id, */
    final String msgCode, final String fieldname, final String dataType, final List<? extends SelectItem> refSelects) {
        super(/* id */null, msgCode);
        this.fieldname = fieldname;
        this.dataType = dataType;
        this.refSelects = refSelects;
    }

    public void setId(final String id) {
        super.setValue(id);
    }

    public String getId() {
        return (String) super.getValue();
    }

    public String getMsgCode() {
        return super.getLabel();
    }

    public String getFieldname() {
        return fieldname;
    }

    public String getDataType() {
        return dataType;
    }

    protected void setRefSelects(final List<? extends SelectItem> refSelects) {
        this.refSelects = refSelects;
    }

    public List<? extends SelectItem> getRefSelects() {
        return refSelects;
    }

    public abstract BooleanClause getBooleanClause(FormField formField) throws ParseException;

    // a service for the subclasses, provide them with a query parser...
    protected QueryParser createQueryParser() {
        return new QueryParser(Version.LUCENE_29, fieldname, new StandardAnalyzer(Version.LUCENE_29));
    }

}
