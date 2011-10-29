package net.wohlfart.jbpm4.node;


import java.io.Serializable;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.queries.JbpmTransitionUserTable;

import org.jbpm.api.task.Participation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * this class is responsible for providing a configurable set of
 * users for a transition
 * 
 * @author Michael Wohlfart
 *
 */
public class UserSelectConfig extends AbstractSelectConfig implements ISelectConfig, Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserSelectConfig.class);
    
    // we want to directly assign the selected user to the task
    private String participationRole = Participation.OWNER;

    private String baseEjbqlExpression = "select new " 
        + JbpmTransitionUserTable.Row.class.getName() 
        + "(tu.id, "
        + "tu.firstname, tu.lastname, tu.name, " // name is the login name
        + "tu.actorId, " 
        + "tu.label, " 
        + "tu.enabled, tu.unlocked, tu.credentialsExpire, tu.accountExpire, " 
        + "tu.memberships.size " 
        + ")" 
        + " from "
        + CharmsUser.class.getName() + " tu";

    @Override
    public String getQlExpression(String fragment, final CharmsUser charmsUser) {     
        StringBuffer qlExpression = new StringBuffer(baseEjbqlExpression);
        boolean whereUsed = false;

        whereUsed = appendFragmentFilter(fragment, qlExpression, whereUsed);

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
            hqlExpression.append("( (lower(tu.name) like concat('%', lower('");
            hqlExpression.append(fragment);
            hqlExpression.append("'), '%')) ");
            hqlExpression.append(" or (lower(tu.lastname) like concat('%', lower('");
            hqlExpression.append(fragment);
            hqlExpression.append("'), '%')) ");
            hqlExpression.append(" or (lower(tu.firstname) like concat('%', lower('");
            hqlExpression.append(fragment);
            hqlExpression.append("'), '%')) )");
        }
        LOGGER.debug("select is: {}", hqlExpression);
        return result;
    }

    
    @Override
    public String getParticipationRole() {
        return this.participationRole;
    }
    public void setParticipationRole(String participationRole) {
        this.participationRole = participationRole;
    }

}
