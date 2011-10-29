package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.activity.AbstractActivity;
import net.wohlfart.jbpm4.activity.AutomaticTaskActivity;

import org.apache.commons.lang.StringUtils;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * simple binding of a automatic task, most of the work is done in the parent
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class AutomaticTaskBinding extends AbstractScriptNodeBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(AutomaticTaskBinding.class);

    public static final String  TAG    = "automaticTaskActivity";

    public AutomaticTaskBinding() {
        super(TAG);
        LOGGER.debug("constructor finished");
    }

    @Override
    public Object parseJpdl(final Element element, final Parse parse, final JpdlParser parser) {
        LOGGER.info("parseJpdl called with element {}, parse {}, parser {}", new Object[] { element, parse, parser });

        // create a new activity
        final AutomaticTaskActivity automaticTaskActivity = new AutomaticTaskActivity();

        // check if we have a name for generating the automatic task
        final String taskName = XmlUtil.attribute(element, // element
                AbstractActivity.ATTRIBUTE_TASK_NAME, // attribute for the task
                                                      // name to be generated
                // true, // required, we need a name
                parse, // parse run
                null); // default value
        if (StringUtils.isEmpty(taskName)) {
            LOGGER.warn("attribute \"{}\" missing, check the problem log for the parse", AbstractActivity.ATTRIBUTE_TASK_NAME);
        }
        automaticTaskActivity.setName(taskName);

        parseScript(automaticTaskActivity, element, parse);

        // return the created activity
        return automaticTaskActivity;
    }
}
