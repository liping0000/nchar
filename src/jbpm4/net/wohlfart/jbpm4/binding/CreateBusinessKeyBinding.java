package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.activity.CreateBusinessKeyActivity;

import org.apache.commons.lang.StringUtils;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * 
 * parse a custom business key generator node
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class CreateBusinessKeyBinding extends AbstractScriptNodeBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(CreateBusinessKeyBinding.class);

    private static final String TAG    = "createBusinessKey";

    public CreateBusinessKeyBinding() {
        super(TAG);
        LOGGER.debug("constructor finished");
    }

    @Override
    public Object parseJpdl(final Element element, final Parse parse, final JpdlParser parser) {
        LOGGER.info("parseJpdl called with element {}, parse {}, parser {}", new Object[] { element, parse, parser });

        final CreateBusinessKeyActivity createBusinessKeyActivity = new CreateBusinessKeyActivity();

        // need the prefix attribute
        final String prefix = XmlUtil.attribute(element, // element
                CreateBusinessKeyActivity.ATTRIBUTE_PREFIX_NAME, // attribute
                                                                 // name
                // true, // required
                parse, // parse run
                null); // default value
        if (StringUtils.isEmpty(prefix)) {
            LOGGER.warn("attribute \"{}\" missing, check the problem log for the parse", CreateBusinessKeyActivity.ATTRIBUTE_PREFIX_NAME);
        }
        createBusinessKeyActivity.setPrefix(prefix);

        // need the location attribute
        final String location = XmlUtil.attribute(element, // element
                CreateBusinessKeyActivity.ATTRIBUTE_LOCATION_NAME, // attribute
                                                                   // name
                // true, // required
                parse, // parse run
                null); // default value
        if (StringUtils.isEmpty(prefix)) {
            LOGGER.warn("attribute \"{}\" missing, check the problem log for the parse", CreateBusinessKeyActivity.ATTRIBUTE_LOCATION_NAME);
        }
        createBusinessKeyActivity.setLocation(location);

        parseScript(createBusinessKeyActivity, element, parse);

        // return the created activity
        return createBusinessKeyActivity;
    }
}
