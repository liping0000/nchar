package net.wohlfart.refdata.queries;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.framework.AbstractTableQuery;
import net.wohlfart.framework.sort.SortableMoves;
import net.wohlfart.refdata.entities.ChangeRequestCode;

import org.hibernate.Session;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(CONVERSATION)
@Name(value = "changeRequestCodeTable")
public class ChangeRequestCodeTable extends AbstractTableQuery<ChangeRequestCodeTable.Row> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChangeRequestCodeTable.class);

    @Override
    public void setup() {
        setEjbql("select new " 
                + ChangeRequestCodeTable.Row.class.getName() 
                + "(e.id" 
                + ", e.sortIndex" 
                + ", e.messageCode" 
                + ", e.defaultName "
                + ", e.enabled ) " 
                + " from " 
                + ChangeRequestCode.class.getName() 
                + " e ");
        setMaxResults(10);
        setRestrictionLogicOperator("or");
        setOrderDirection("asc");
        setOrderColumn("e.sortIndex");
    }

    private final Set<String> sortColumns = new HashSet<String>(Arrays.asList("e.sortIndex", "e.defaultName"));

    @Override
    public Set<String> getColumnsForOrder() {
        return sortColumns;
    }

    @Override
    protected String getCountEjbql() {
        final String ejbql = getRenderedEjbql();
        final int start = ejbql.indexOf("select new ");
        final int end = ejbql.indexOf(" from ", start);

        final StringBuilder stringBuilder = new StringBuilder(ejbql.length()).append(ejbql).replace(start, end, "select count(e) ");

        final int order = stringBuilder.lastIndexOf(" order by ");
        if (order > 0) {
            return stringBuilder.substring(0, order).toString().trim();
        } else {
            return stringBuilder.toString().trim();
        }
    }

    @Override
    public void setFragment(final String fragment) {
        LOGGER.debug("setting new fragmnet, was '{}' setting to '{}'", this.fragment, fragment);
        super.setFragment(fragment);
        if ((fragment == null) || (fragment.trim().length() == 0)) {
            setRestrictionExpressionStrings(new ArrayList<String>());
        } else {
            final List<String> restrictions = new ArrayList<String>();
            restrictions.add(" lower(e.defaultName) like concat('%', lower(#{changeRequestCodeTable.fragment}), '%') ");
            setRestrictionExpressionStrings(restrictions);
        }
        // go to the first page
        first();
    }

    @Transactional
    public void sortUp(final Long id) {
        Session hibernateSession = getSession();
        final ChangeRequestCode changeRequestCode = (ChangeRequestCode) hibernateSession.load(ChangeRequestCode.class, id);
        SortableMoves.moveUp(ChangeRequestCode.class, changeRequestCode.getId(), hibernateSession);
        refresh();
    }

    @Transactional
    public void sortDown(final Long id) {
        Session hibernateSession = getSession();
        final ChangeRequestCode changeRequestCode = (ChangeRequestCode) hibernateSession.load(ChangeRequestCode.class, id);
        SortableMoves.moveDown(ChangeRequestCode.class, changeRequestCode.getId(), hibernateSession);
        refresh();
    }

    @Transactional
    public void setSuspend(final Long id) {
        Session hibernateSession = getSession();
        final ChangeRequestCode changeRequestCode = (ChangeRequestCode) hibernateSession.load(ChangeRequestCode.class, id);
        changeRequestCode.setEnabled(false);
        hibernateSession.flush();
        refresh();
    }

    @Transactional
    public void setResume(final Long id) {
        Session hibernateSession = getSession();
        final ChangeRequestCode changeRequestCode = (ChangeRequestCode) hibernateSession.load(ChangeRequestCode.class, id);
        changeRequestCode.setEnabled(true);
        hibernateSession.flush();
        refresh();
    }

    public static class Row implements Serializable {


        private final Long    id;
        private final Integer index;
        private final String  msgCode;
        private final String  name;
        private final Boolean enabled;

        public Row(final Long id, final Integer index, final String msgCode, final String name, final Boolean enabled) {
            this.id = id;
            this.index = index;
            this.msgCode = msgCode;
            this.name = name;
            this.enabled = enabled;
        }

        public Long getId() {
            return id;
        }

        public Integer getIndex() {
            return index;
        }

        public String getMsgCode() {
            return msgCode;
        }

        public String getName() {
            return name;
        }

        public Boolean getEnabled() {
            return enabled;
        }
    }
}
