package net.wohlfart.jbpm4.node;


import java.io.Serializable;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.entities.CharmsPermission;

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
public class UserPermissionSelectConfig extends AbstractSelectConfig implements ISelectConfig, Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserPermissionSelectConfig.class);

    private String permissionTarget = null;
    private String permissionAction = null;
    
    private String participationRole = Participation.OWNER;

    private String baseEjbqlExpression =         
        " select distinct tu "
        + " from "

        + CharmsUser.class.getSimpleName() + " tu "
        + " left join tu.memberships m "
        + " left join m.charmsRole.contained r2 "
        + ","

        + CharmsPermission.class.getSimpleName() + " p ";



    @Override
    public String getQlExpression(String fragment, final CharmsUser charmsUser) {     
        StringBuffer qlExpression = new StringBuffer(baseEjbqlExpression);
        boolean whereUsed = false;

        whereUsed = appendActionFilter(permissionAction, qlExpression, whereUsed);
        whereUsed = appendRecipientFilter(qlExpression, whereUsed);
        whereUsed = appendTargetFilter(permissionTarget, qlExpression, whereUsed);
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


    protected boolean appendRecipientFilter(StringBuffer hqlExpression, boolean whereUsed) {
        boolean result = whereUsed;
        if (whereUsed) {
            hqlExpression.append(" and ");
        } else {
            hqlExpression.append(" where ");
            result = true;
        }

        // -- -- link the recipient --- ---
        // the permission is for a contained role and the name matches or for a membership role and the name matches
        hqlExpression.append(" ( ( ( r2.name = p.recipient or m.charmsRole.name = p.recipient )"      );        
        hqlExpression.append(" and p.discriminator = '" + CharmsPermission.ROLE + "' )");
        hqlExpression.append(" or ");
        // the permission is for a user and the name matches
        hqlExpression.append("   ( ( tu.name = p.recipient ) ");
        hqlExpression.append(" and p.discriminator = '" + CharmsPermission.USER + "' ) )");

        LOGGER.debug("select is: {}", hqlExpression);       
        return result;
    }

    protected boolean appendTargetFilter(String permissionTarget, StringBuffer hqlExpression, boolean whereUsed) {
        boolean result = whereUsed;
        if (whereUsed) {
            hqlExpression.append(" and ");
        } else {
            hqlExpression.append(" where ");
            result = true;
        }

        // --- --- link to the target, might be user, user as member of a group or subgroup... --- ---      
        // target links directly to the selected role..
        hqlExpression.append("( ");
        hqlExpression.append(" p.target = '" + permissionTarget + "' ");
        hqlExpression.append(")");

        LOGGER.debug("select is: {}", hqlExpression);       
        return result;
    }

    protected boolean appendActionFilter(String action, StringBuffer hqlExpression, boolean whereUsed) {
        boolean result = whereUsed;
        if (whereUsed) {
            hqlExpression.append(" and ");
        } else {
            hqlExpression.append(" where ");
            result = true;
        }    

        hqlExpression.append("(");

        hqlExpression.append("( p.action like concat('%,', '" );
        hqlExpression.append(action);
        hqlExpression.append("', ',%') )");

        hqlExpression.append(" or ");

        hqlExpression.append("( p.action like concat('%,', '" );
        hqlExpression.append(action);
        hqlExpression.append("') )");

        hqlExpression.append(" or ");

        hqlExpression.append("( p.action like concat('" );
        hqlExpression.append(action);
        hqlExpression.append("', ',%') )");

        hqlExpression.append(" or ");

        hqlExpression.append("( p.action like '" );
        hqlExpression.append(action);
        hqlExpression.append("' )");

        hqlExpression.append(")");

        LOGGER.debug("select is: {}", hqlExpression);
        return result;
    }

    public void setPermissionTarget(String permissionTarget) {
        this.permissionTarget = permissionTarget;
    }
    public void setPermissionAction(String permissionAction) {
        this.permissionAction = permissionAction;
    }
    
    
    @Override
    public String getParticipationRole() {
        return this.participationRole;
    }
    public void setParticipationRole(String participationRole) {
        this.participationRole = participationRole;
    }

}
