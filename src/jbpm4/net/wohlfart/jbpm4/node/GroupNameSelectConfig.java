package net.wohlfart.jbpm4.node;


import java.io.Serializable;
import java.util.Arrays;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.queries.JbpmTransitionRoleTable;

import org.jbpm.api.task.Participation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * this class is responsible for providing a configurable set of
 * groups for a transitions
 * 
 * 
 * @author Michael Wohlfart
 *
 */
public class GroupNameSelectConfig extends AbstractSelectConfig implements ISelectConfig, Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(GroupNameSelectConfig.class);

    private String[] groupNames = null;
    
    private String participationRole = Participation.CANDIDATE;

    private String baseEjbqlExpression = "select new " 
        + JbpmTransitionRoleTable.Row.class.getName()
        + "(r.id, " 
        + "r.name, " 
        + "r.actorId, " 
        + "r.label, "
        + "r.memberships.size, " 
        + "r.upstream.size "
        + ")" 
        + " from " 
        + CharmsRole.class.getName() + " r";

    @Override
    public String getQlExpression(final String fragment, final CharmsUser charmsUser) {
        final StringBuffer qlExpression = new StringBuffer(baseEjbqlExpression);
        boolean whereUsed = false;

        whereUsed = appendFragmentFilter(fragment, qlExpression, whereUsed);
        whereUsed = appendGroupNamesFilter(groupNames, qlExpression, whereUsed);

        LOGGER.debug("resolved qlExpression {}, where used value: {}", qlExpression, whereUsed);
        return qlExpression.toString();
    }


    protected boolean appendFragmentFilter(String fragment, StringBuffer hqlExpression, boolean whereUsed) {
        boolean result = whereUsed;
        // user provided string filter
        if ((fragment != null) && (fragment.trim().length() > 0)) {
            if (whereUsed) {
                hqlExpression.append(" and ");
            } else {
                hqlExpression.append(" where ");
                result = true;
            }
            hqlExpression.append("( lower(r.name) like concat('%', lower('" );
            hqlExpression.append(fragment);
            hqlExpression.append("'), '%')  )");
        }        
        LOGGER.debug("select is: {}", hqlExpression);
        return result;
    }

    protected boolean appendGroupNamesFilter(String[] names, StringBuffer hqlExpression, boolean whereUsed) {
        boolean result = whereUsed;
        // filter by groupnames
        if ((names != null) && (names.length > 0)) {
            if (whereUsed) {
                hqlExpression.append(" and ");
            } else {
                hqlExpression.append(" where ");
                result = true;
            }                
            // adding quotes to the strings...
            final String[] groups = new String[names.length];
            for (int i = 0; i < groupNames.length; i++) {
                groups[i] = "'" + groupNames[i] + "'";
            }
            hqlExpression.append(" ( r.name in (" + StringUtils.arrayToCommaDelimitedString(groups) + ") ) ");
        }        
        LOGGER.debug("select is: {}", hqlExpression);       
        return result;
    }

    
    public void setGroupNames(String[] groupNames) {
        this.groupNames = Arrays.copyOf(groupNames, groupNames.length);
    }
    
    
    @Override
    public String getParticipationRole() {
        return this.participationRole;
    }
    public void setParticipationRole(String participationRole) {
        this.participationRole = participationRole;
    }

}
