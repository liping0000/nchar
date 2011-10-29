package net.wohlfart.jbpm4.node;


import java.io.Serializable;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.targets.SeamRoleInstanceTargetSetup;
import net.wohlfart.authorization.targets.SeamUserInstanceTargetSetup;

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
public class UserActionSelectConfig extends AbstractSelectConfig implements ISelectConfig, Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserActionSelectConfig.class);

    private String action = null;
    
    private String participationRole = Participation.OWNER;

    private String baseEjbqlExpression = 
        // u is the target of the permission not the recipient of the permission
        " select distinct tu " 
        + " from "

        // the user matched with all container roles for that user:
        + CharmsUser.class.getSimpleName() + " tu "        // targetUser  
        + " left join tu.memberships tm "                  // targetMembership
        + " left join tm.charmsRole.container tc, "        // targteContainer

        + CharmsPermission.class.getSimpleName() + " p, "  // the permission links the recipient to the target

        + CharmsUser.class.getSimpleName() + " ru "        // recipientUser  
        + " left join ru.memberships rm "                  // recipientMembership
        + " left join rm.charmsRole.contained rc ";         // recipientContainer               


    @Override
    public String getQlExpression(String fragment, final CharmsUser charmsUser) {     
        StringBuffer qlExpression = new StringBuffer(baseEjbqlExpression);
        boolean whereUsed = false;

        whereUsed = appendRecipientFilter(charmsUser, qlExpression, whereUsed);
        whereUsed = appendActionFilter(action, qlExpression, whereUsed);
        whereUsed = appendTargetFilter(qlExpression, whereUsed);
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

    protected boolean appendRecipientFilter(CharmsUser charmsUser, StringBuffer hqlExpression, boolean whereUsed) {
        boolean result = whereUsed;
        if (whereUsed) {
            hqlExpression.append(" and ");
        } else {
            hqlExpression.append(" where ");
            result = true;
        }        
        
        // --- --- link to the recipient might get the permission indirect via a group... --- ---     
        hqlExpression.append(" ( ");
        // the recipient got the permission directly
        hqlExpression.append("  ( ( p.discriminator = '" + CharmsPermission.USER + "' ) and ( p.recipient = '");
        hqlExpression.append(charmsUser.getName());       
        hqlExpression.append("' ) )  ");       
        hqlExpression.append("    or ");
        // a group of which the recipient is a member of got the permission
        hqlExpression.append(" ( ( rc.name = p.recipient or rm.charmsRole.name = p.recipient )");            
        hqlExpression.append(" and ( p.discriminator = '" + CharmsPermission.ROLE + "') )");       
        hqlExpression.append(")");

        LOGGER.debug("select is: {}", hqlExpression);
        return result;
    }

    protected boolean appendTargetFilter(StringBuffer hqlExpression, boolean whereUsed) {
        boolean result = whereUsed;
        if (whereUsed) {
            hqlExpression.append(" and ");
        } else {
            hqlExpression.append(" where ");
            result = true;
        }        

        // --- --- link to the target, might be user, user as member of a group or subgroup... --- ---       
        // permission target links directly to the target user..
        hqlExpression.append("( ");
        hqlExpression.append(" ( p.targetId = tu.id ");
        // the target is a user instance
        hqlExpression.append(" and p.target = '" + SeamUserInstanceTargetSetup.TARGET_STRING + "' )");

        hqlExpression.append(" or ");
        // the permission links to a group of which the target user is a direct member of
        hqlExpression.append("   ( p.targetId = tm.charmsRole.id ");
        // the target is a role instance
        hqlExpression.append(" and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' ) ");

        hqlExpression.append(" or ");
        // target link to a container group which contains one of the users member groups
        hqlExpression.append("   ( p.targetId = tc.id ");
        // the target is a role instance
        hqlExpression.append(" and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' ) ");
        hqlExpression.append(")");

        LOGGER.debug("select is: {}", hqlExpression);
        return result;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    
    @Override
    public String getParticipationRole() {
        return this.participationRole;
    }
    public void setParticipationRole(String participationRole) {
        this.participationRole = participationRole;
    }

}
