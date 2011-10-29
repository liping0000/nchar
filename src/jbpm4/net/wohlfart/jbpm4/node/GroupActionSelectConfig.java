package net.wohlfart.jbpm4.node;


import java.io.Serializable;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.entities.CharmsPermission;
import net.wohlfart.authorization.targets.SeamRoleInstanceTargetSetup;
import net.wohlfart.jbpm4.queries.JbpmTransitionRoleTable;

import org.jbpm.api.task.Participation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * this class is responsible for providing a configurable set of
 * groups for a transitions
 * 
 * return all groups upon the current user has the permission to perform the
 * specified action
 * 
 * @author Michael Wohlfart
 *
 */
public class GroupActionSelectConfig extends AbstractSelectConfig implements ISelectConfig, Serializable{

    private final static Logger LOGGER = LoggerFactory.getLogger(GroupActionSelectConfig.class);

    private String action = null;
    
    private String participationRole = Participation.CANDIDATE;

    public final static String baseEjbqlExpression =
        " select distinct tr " // this is the target of the permission not the recipient!
        + " from "
        
        + CharmsRole.class.getSimpleName() + " tr "        // targetRole 
         + " left join tr.container tc, "                  // targteContainer
        
        + CharmsPermission.class.getSimpleName() + " p, "  // the permission links the recipient to the target
        
        + CharmsUser.class.getSimpleName() + " ru "        // recipientUser  
        + " left join ru.memberships rm "                  // recipientMembership
        + " left join rm.charmsRole.contained rc ";        // recipientContainer
           
    @Override
    public String getQlExpression(final String fragment, final CharmsUser charmsUser) {
        final StringBuffer qlExpression = new StringBuffer(baseEjbqlExpression);
        boolean whereUsed = false;

        whereUsed = appendRecipientFilter(charmsUser, qlExpression, whereUsed);
        whereUsed = appendActionFilter(action, qlExpression, whereUsed);
        whereUsed = appendTargetFilter(qlExpression, whereUsed);
        whereUsed = appendFragmentFilter(fragment, qlExpression, whereUsed);
       
        LOGGER.debug("resolved qlExpression {}, where used value: {}", qlExpression, whereUsed);
        return qlExpression.toString();
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
        // target links directly to the selected role..
        hqlExpression.append("( ");
        hqlExpression.append(" ( p.targetId = tr.id ");
        // the target is a user instance
        hqlExpression.append(" and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' )");
        
        hqlExpression.append(" or ");
        // target link to a container group which contains the user or a parent group of a container group
        hqlExpression.append("   ( p.targetId = tc.id ");
        // the target is a role instance
        hqlExpression.append(" and p.target = '" + SeamRoleInstanceTargetSetup.TARGET_STRING + "' ) ");
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
              
        // --- --- link to the recipient --- ---
        // the recipient might get the permission indirect via a group...
        
        hqlExpression.append(" ( ");
        
        // the recipient got the permission directly
        hqlExpression.append("  ( ( p.discriminator = '" + CharmsPermission.USER + "' ) and ( p.recipient = '");
        hqlExpression.append(charmsUser.getName());
        hqlExpression.append("' ) )  ");
        
        hqlExpression.append("    or ");
        // a group of which the recipient is a member of got the permission
        hqlExpression.append(" ( ( rc.name = p.recipient or rm.charmsRole.name = p.recipient )");        
        hqlExpression.append(" and ( p.discriminator = '" + CharmsPermission.ROLE + "') ");
        hqlExpression.append(" and ( ru.name = '");
        hqlExpression.append(charmsUser.getName());
        hqlExpression.append("' ) )  ");
              
        hqlExpression.append(" ) ");
 
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


    /**
     * append a like query for the name of the role
     * 
     * @param fragment
     * @param hqlExpression
     * @param whereUsed
     * @return
     */
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
            hqlExpression.append("( lower(tr.name) like concat('%', lower('" );
            hqlExpression.append(fragment);
            hqlExpression.append("'), '%')  )");
        }        
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
