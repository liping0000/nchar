package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.node.GroupActionSelectConfig;
import net.wohlfart.jbpm4.node.GroupNameSelectConfig;
import net.wohlfart.jbpm4.node.GroupPermissionSelectConfig;
import net.wohlfart.jbpm4.node.ISelectConfig;
import net.wohlfart.jbpm4.node.UserActionSelectConfig;
import net.wohlfart.jbpm4.node.UserGroupNameSelectConfig;
import net.wohlfart.jbpm4.node.UserPermissionSelectConfig;
import net.wohlfart.jbpm4.node.UserSelectConfig;

import org.apache.commons.lang.StringUtils;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


public class UserSelectBinding extends AbstractSelectBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserSelectBinding.class);

    private static final String TAG = "userSelect";

    public UserSelectBinding() {
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
        if ( participationRole != null ) {
            LOGGER.warn("no {} allowed in {}", ISelectConfig.PARTICIPATION_ROLE, TAG);
            return null;
        }
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


        
        // setup and return the configs...
        
        if (groupNames != null) {
            UserGroupNameSelectConfig userGroupNameSelectConfig = new UserGroupNameSelectConfig();
            if (!StringUtils.isEmpty(groupNames)) {
                
                final String[] array = StringUtils.split(groupNames, ',');
                for (int i = 0; i < array.length; i++) {
                    array[i] = array[i].trim();
                }
                userGroupNameSelectConfig.setGroupNames(array);
            }
            LOGGER.info("configured userGroupNameSelectConfig {}", userGroupNameSelectConfig);
            return userGroupNameSelectConfig;
        }


        if (action != null) {
            UserActionSelectConfig userActionSelectConfig = new UserActionSelectConfig();
            userActionSelectConfig.setAction(action);
            LOGGER.info("configured userActionSelectConfig {}", userActionSelectConfig);
            return userActionSelectConfig;
        }

        
        if ((permissionAction != null) && (permissionTarget != null)) {
            UserPermissionSelectConfig userPermissionSelectConfig = new UserPermissionSelectConfig();
            userPermissionSelectConfig.setPermissionAction(permissionAction);
            userPermissionSelectConfig.setPermissionTarget(permissionTarget);
            LOGGER.info("configured userPermissionSelectConfig {}", userPermissionSelectConfig);
            return userPermissionSelectConfig;
        }
        
        LOGGER.info("no parameters config, returning plain config");
        return new UserSelectConfig();
    }

}
