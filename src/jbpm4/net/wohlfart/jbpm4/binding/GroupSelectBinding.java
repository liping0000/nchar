package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.node.GroupActionSelectConfig;
import net.wohlfart.jbpm4.node.GroupNameSelectConfig;
import net.wohlfart.jbpm4.node.GroupPermissionSelectConfig;
import net.wohlfart.jbpm4.node.GroupSelectConfig;
import net.wohlfart.jbpm4.node.ISelectConfig;

import org.apache.commons.lang.StringUtils;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * this class is resolving the xml d efinition from the workflow
 * into the right config instance for resolving groups for a workflow transition
 * 
 * 
 * @author Michael Wohlfart
 */
public class GroupSelectBinding extends AbstractSelectBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(GroupSelectBinding.class);

    private static final String TAG = "groupSelect";

    public GroupSelectBinding() {
        super(TAG);
        LOGGER.debug("constructor finished");
    }

    @Override
    public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
        LOGGER.info("parseJpdl called with element {}, parse {}, parser {}", new Object[] { element, parse, parser });

        // read the possible parameters
        final String groupNames = XmlUtil.attribute(element, ISelectConfig.GROUP_NAMES, null, null);
        final String action = XmlUtil.attribute(element, ISelectConfig.ACTION, null, null);
        final String permissionAction = XmlUtil.attribute(element, ISelectConfig.PERMISSION_ACTION, null, null);
        final String permissionTarget = XmlUtil.attribute(element, ISelectConfig.PERMISSION_TARGET, null, null);
        final String participationRole = XmlUtil.attribute(element, ISelectConfig.PARTICIPATION_ROLE, null, null);

        
        // sanity checks
        if ( (groupNames != null)
                && ((action != null) || (permissionAction != null) || (permissionTarget != null)) ) {
            LOGGER.warn("no other parameter allowed if you use {}", ISelectConfig.GROUP_NAMES);
            return null;
        }
        if ( (action != null)
                && ((groupNames != null) || (permissionAction != null) || (permissionTarget != null)) ) {
            LOGGER.warn("no other parameter allowed if you use {}", ISelectConfig.ACTION);
            return null;
        }
        if ( ((permissionAction != null) && (permissionTarget == null))
                || ((permissionAction == null) && (permissionTarget != null)) ) {
            LOGGER.warn("please configure {} and {}", ISelectConfig.PERMISSION_ACTION, ISelectConfig.PERMISSION_TARGET);
            return null;
        }
        if ( ((permissionAction != null) && (permissionTarget != null))
                && ((action != null) || (groupNames != null)) ) {
            LOGGER.warn("no other parameter allowed if you use {} and {}", ISelectConfig.PERMISSION_ACTION, ISelectConfig.PERMISSION_TARGET);
            return null;
        }

       
        if (groupNames != null) {
            GroupNameSelectConfig groupNameSelectConfig = new GroupNameSelectConfig();
            if (!StringUtils.isEmpty(groupNames)) {
                
                final String[] array = StringUtils.split(groupNames, ',');
                for (int i = 0; i < array.length; i++) {
                    array[i] = array[i].trim();
                }
                groupNameSelectConfig.setGroupNames(array);
            }
            
            if (!StringUtils.isEmpty(participationRole)) {
                groupNameSelectConfig.setParticipationRole(participationRole);
            }
            LOGGER.info("configured groupNameSelectConfig {}", groupNameSelectConfig);
            return groupNameSelectConfig;
        }


        if (action != null) {
            GroupActionSelectConfig groupActionSelectConfig = new GroupActionSelectConfig();
            groupActionSelectConfig.setAction(action);
            LOGGER.warn("configured groupActionSelectConfig {}", groupActionSelectConfig);
            if (!StringUtils.isEmpty(participationRole)) {
                groupActionSelectConfig.setParticipationRole(participationRole);
            }
            return groupActionSelectConfig;
        }

        
        if ((permissionAction != null) && (permissionTarget != null)) {
            GroupPermissionSelectConfig groupPermissionSelectConfig = new GroupPermissionSelectConfig();
            groupPermissionSelectConfig.setPermissionAction(permissionAction);
            groupPermissionSelectConfig.setPermissionTarget(permissionTarget);
            LOGGER.warn("configured groupPermissionSelectConfig {}", groupPermissionSelectConfig);
            if (!StringUtils.isEmpty(participationRole)) {
                groupPermissionSelectConfig.setParticipationRole(participationRole);
            }
            return groupPermissionSelectConfig;
        }
        
        GroupSelectConfig groupSelectConfig = new GroupSelectConfig();
        LOGGER.info("no parameters config, returning plain config");
        if (!StringUtils.isEmpty(participationRole)) {
            groupSelectConfig.setParticipationRole(participationRole);
        }
        return groupSelectConfig;
    }

}
