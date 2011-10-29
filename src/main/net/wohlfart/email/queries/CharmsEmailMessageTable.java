package net.wohlfart.email.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.email.entities.CharmsEmailMessage;
import net.wohlfart.framework.AbstractTableQuery;

import org.hibernate.Query;
import org.hibernate.Session;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.persistence.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(CONVERSATION)
@Name("charmsEmailMessageTable")
public class CharmsEmailMessageTable extends AbstractTableQuery<CharmsEmailMessageTable.Row> {


    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsEmailMessageTable.class);

    @Override
    public void setup() {
        setEjbql("select new " + CharmsEmailMessageTable.Row.class.getName() + "(m.id, m.key, m.subject, m.receiver, m.sender, m.create) " + " from "
                + CharmsEmailMessage.class.getName() + " m");
        setMaxResults(10);
        setRestrictionLogicOperator("or");
        setUseWildcardAsCountQuerySubject(true);
        setOrderDirection("asc");
        setOrderColumn("m.create");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("m.key", "m.subject", "m.receiver", "m.sender", "m.create"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf(" new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, " count(m) ");

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
            restrictions.add(" lower(m.key) like concat('%', lower(#{charmsEmailMessageTable.fragment}), '%') ");
            restrictions.add(" lower(m.subject) like concat('%', lower(#{charmsEmailMessageTable.fragment}), '%') ");
            restrictions.add(" lower(m.receiver) like concat('%', lower(#{charmsEmailMessageTable.fragment}), '%') ");
            restrictions.add(" lower(m.sender) like concat('%', lower(#{charmsEmailMessageTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
        // go to the first page
        first();
    }

    /*
     * delete the current in the page visible emails
     */
    public String deleteVisible() {
        final Session hibernateSession = getPersistenceContext();
        /*
         * 16:29:21,649 [http-8080-exec-3] WARN
         * net.wohlfart.email.queries.CharmsEmailMessageTable - delete visible
         * called, hql select is: select new
         * net.wohlfart.email.queries.CharmsEmailMessageTable$Row(m.id, m.key,
         * m.subject, m.receiver, m.sender) from
         * net.wohlfart.email.entities.CharmsEmailMessage m where lower(m.key)
         * like concat('%', lower(:el1), '%') or lower(m.subject) like
         * concat('%', lower(:el2), '%') or lower(m.receiver) like concat('%',
         * lower(:el3), '%') or lower(m.sender) like concat('%', lower(:el4),
         * '%') order by key asc
         */

        final String ejbql = getRenderedEjbql();
        LOGGER.debug("delete visible called, hql select is: {}", ejbql);
        final int start = ejbql.indexOf("select ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, " delete ");

        String delete = stringBuilder.toString().trim();
        final int order = stringBuilder.lastIndexOf(" order by ");
        if (order > 0) {
            delete = stringBuilder.substring(0, order).toString().trim();
        }

        LOGGER.debug("calculated update is: {}", delete);

        final Query query = hibernateSession.createQuery(delete);
        setParameters(query, getQueryParameterValues(), 0);
        setParameters(query, getRestrictionParameterValues(), getQueryParameterValues().size());

        query.executeUpdate();
        return "deleted";
    }

    private void setParameters(final Query query, final List<Object> parameters, final int start) {
        for (int i = 0; i < parameters.size(); i++) {
            final Object parameterValue = parameters.get(i);
            if (isRestrictionParameterSet(parameterValue)) {
                query.setParameter(QueryParser.getParameterName(start + i), parameterValue);
            }
        }
    }

    public static class Row implements Serializable {


        private final Long   id;
        private final String key;
        private final String subject;
        private final String receiver;
        private final String sender;
        private final Date   create;

        public Row(final Long id, final String key, final String subject, final String receiver, final String sender, final Date create) {
            this.id = id;
            this.key = key;
            this.subject = subject;
            this.receiver = receiver;
            this.sender = sender;
            this.create = create;
        }

        public Long getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public String getSubject() {
            return subject;
        }

        public String getReceiver() {
            return receiver;
        }

        public String getSender() {
            return sender;
        }

        public Date getCreate() {
            return create;
        }
    }
}
