package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.node.GroupActionSelectConfig;
import net.wohlfart.jbpm4.node.GroupNameSelectConfig;
import net.wohlfart.jbpm4.node.GroupPermissionSelectConfig;
import net.wohlfart.jbpm4.node.GroupSelectConfig;
import net.wohlfart.jbpm4.node.ISelectConfig;
import net.wohlfart.jbpm4.node.TimerSelectConfig;
import net.wohlfart.jbpm4.node.UserRemarkConfig;

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
public class UserRemarkBinding extends AbstractSelectBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserRemarkBinding.class);

    private static final String TAG = "userRemark";

    public UserRemarkBinding() {
        super(TAG);
        LOGGER.debug("constructor finished");
    }

    @Override
    public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
        LOGGER.info("parseJpdl called with element {}, parse {}, parser {}", new Object[] { element, parse, parser });

        return new UserRemarkConfig();
    }

}
