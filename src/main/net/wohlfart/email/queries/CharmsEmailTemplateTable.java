package net.wohlfart.email.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.email.entities.CharmsEmailTemplate;
import net.wohlfart.framework.AbstractTableQuery;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;

@Scope(CONVERSATION)
@Name("charmsEmailTemplateTable")
public class CharmsEmailTemplateTable extends AbstractTableQuery<CharmsEmailTemplateTable.Row> {

    // private final static Logger LOGGER = LoggerFactory.getLogger(CharmsEmailTemplateTable.class);

    @Override
    public void setup() {
        setEjbql("select new " 
                + CharmsEmailTemplateTable.Row.class.getName() 
                + "(t.id, t.name, t.subject, t.enabled) " 
                + " from "
                + CharmsEmailTemplate.class.getName() 
                + " t ");
        setMaxResults(10);
        setRestrictionLogicOperator("or");
        setOrderDirection("asc");
        setOrderColumn("id");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("t.name", "t.subject"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf(" new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length())
        .append(ejbql)
        .replace(start, end, " count(t) ");

        final int order = stringBuilder.lastIndexOf(" order by ");
        if (order > 0) {
            return stringBuilder.substring(0, order).toString().trim();
        } else {
            return stringBuilder.toString().trim();
        }
    }

    @Override
    public void setFragment(final String fragment) {
        super.setFragment(fragment);
        if ((fragment == null) || (fragment.trim().length() == 0)) {
            setRestrictionExpressionStrings(new ArrayList<String>());
        } else {
            final List<String> restrictions = new ArrayList<String>();
            restrictions.add(" lower(t.name) like concat('%', lower(#{charmsEmailTemplateTable.fragment}), '%') ");
            restrictions.add(" lower(t.subject) like concat('%', lower(#{charmsEmailTemplateTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
        // go to the first page
        first();
    }

    @Transactional
    public void setSuspend(final Long id) {
        Session session = getSession();
        final CharmsEmailTemplate template = (CharmsEmailTemplate) session.get(CharmsEmailTemplate.class, id);
        template.setEnabled(false);
        session.flush();
        refresh();
    }

    @Transactional
    public void setResume(final Long id) {
        Session session = getSession();
        final CharmsEmailTemplate template = (CharmsEmailTemplate) session.get(CharmsEmailTemplate.class, id);
        template.setEnabled(true);
        session.flush();
        refresh();
    }

    public static class Row implements Serializable {

        private final Long    id;
        private final String  name;
        private final String  subject;
        private final Boolean enabled;

        public Row(final Long id, final String name, final String subject, final Boolean enabled) {
            this.id = id;
            this.name = name;
            this.subject = subject;
            this.enabled = enabled;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSubject() {
            return subject;
        }

        public Boolean getEnabled() {
            return enabled;
        }
    }
}
