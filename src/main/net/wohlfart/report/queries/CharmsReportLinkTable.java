package net.wohlfart.report.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.wohlfart.framework.AbstractTableQuery;
import net.wohlfart.report.entities.CharmsReport;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * The EntityQuery component manages a JPQL query result set.
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(CONVERSATION)
@Name("charmsReportLinkTable")
public class CharmsReportLinkTable extends AbstractTableQuery<CharmsReportLinkTable.Row> {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CharmsReportLinkTable.class);


    @Override
    public void setup() {
        setEjbql("select new " + CharmsReportLinkTable.Row.class.getName() + "(r.id, " + " r.messageCode, r.defaultName ) " + " from "
                + CharmsReport.class.getName() + " r" + " where r.enabled = true " + " order by r.sortIndex");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("e.id", "e.sortIndex", "e.defaultName"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    public static class Row implements Serializable {


        private final Long   id;
        private final String messageCode;
        private final String defaultName;

        public Row(final Long id, final String messageCode, final String defaultName) {
            this.id = id;
            this.messageCode = messageCode;
            this.defaultName = defaultName;
        }

        public Long getId() {
            return id;
        }

        public String getMessageCode() {
            return messageCode;
        }

        public String getDefaultName() {
            return defaultName;
        }
    }
}
