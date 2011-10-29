package net.wohlfart.jbpm4.node;


import java.io.Serializable;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.targets.SeamRoleInstanceTargetSetup;

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
public class GroupPermissionSelectConfig extends AbstractSelectConfig implements ISelectConfig, Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(GroupPermissionSelectConfig.class);

    private String permissionTarget = null;
    private String permissionAction = null;
    
    private String participationRole = Participation.CANDIDATE;

    public final static String baseEjbqlExpression = 
        " select distinct r1 "
        + " from "
        
        + CharmsRole.class.getSimpleName() + " r1 "
        + " left join r1.contained r2 "
        + ","
        
        + CharmsPermission.class.getSimpleName() + " p ";

    
    @Override
    public String getQlExpression(final String fragment, final CharmsUser charmsUser) {
        final StringBuffer qlExpression = new StringBuffer(baseEjbqlExpression);
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
            hqlExpression.append("( lower(r.name) like concat('%', lower('" );
            hqlExpression.append(fragment);
            hqlExpression.append("'), '%')  )");
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
              
        // --- --- link to the recipient --- ---
        // the recipient might get the permission indirect via a group...
        
        hqlExpression.append(" ( ");
        
        hqlExpression.append("  ( r2.name = p.recipient or r1.name = p.recipient )" );
        hqlExpression.append(" and p.discriminator = '" + CharmsPermission.ROLE + "' " );

        hqlExpression.append(" ) ");
 
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

    /**
     * append a like query checking if the name of the action is within the
     * comma seperated list of the permission action string
     * 
     * @param action
     * @param hqlExpression
     * @param whereUsed
     * @return
     */
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
