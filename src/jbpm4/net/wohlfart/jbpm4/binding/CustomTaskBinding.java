package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.activity.AbstractActivity;
import net.wohlfart.jbpm4.activity.CustomTaskActivity;

import org.apache.commons.lang.StringUtils;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class CustomTaskBinding extends AbstractScriptNodeBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomTaskBinding.class);

    private static final String TAG    = "customTaskActivity";

    public CustomTaskBinding() {
        super(TAG);
        LOGGER.debug("constructor finished");
    }

    @Override
    public Object parseJpdl(final Element element, final Parse parse, final JpdlParser parser) {
        LOGGER.info("parseJpdl called with element {}, parse {}, parser [}", new Object[] { element, parse, parser });

        final CustomTaskActivity customTaskActivity = new CustomTaskActivity();

        // the task to be created in this node
        final String task = XmlUtil.attribute(element, AbstractActivity.ATTRIBUTE_TASK_NAME, parse, null);
        if (StringUtils.isEmpty(task)) {
            LOGGER.warn("attribute \"{}\" missing, check the problem log for the parse", AbstractActivity.ATTRIBUTE_TASK_NAME);
        } else {
            customTaskActivity.setName(task);
        }

        // the form used for the human part of the task
        final String form = XmlUtil.attribute(element, CustomTaskActivity.ATTRIBUTE_FORM_NAME, parse, null);
        if (StringUtils.isEmpty(form)) {
            LOGGER.info("attribute \"{}\" missing for task named \"{}\", check the problem log for the parse", CustomTaskActivity.ATTRIBUTE_FORM_NAME, task);
        } else {
            customTaskActivity.setForm(form);
        }

        // check if there is a default group to assign the task, note it's the
        // groups name not the actor id
        final String groupName = XmlUtil.attribute(element, CustomTaskActivity.GROUP_ASSIGN, null, null);
        if (!StringUtils.isEmpty(groupName)) {
            customTaskActivity.setGroupActorName(groupName);
        }

        // signals that cause a new execution for a single selected swimlane or
        // for each user in a swimlane
        final String spawnSignals = XmlUtil.attribute(element, CustomTaskActivity.SPAWN_SIGNALS, null, null);
        if (!StringUtils.isEmpty(spawnSignals)) {
            final String[] array = StringUtils.split(spawnSignals, ',');
            for (int i = 0; i < array.length; i++) {
                array[i] = array[i].trim();
            }
            customTaskActivity.setSpawnSignals(array);
        }

        // signals that terminate the current execution
        final String termSignals = XmlUtil.attribute(element, CustomTaskActivity.TERM_SIGNALS, null, null);
        if (!StringUtils.isEmpty(termSignals)) {
            final String[] array = StringUtils.split(termSignals, ',');
            for (int i = 0; i < array.length; i++) {
                array[i] = array[i].trim();
            }
            customTaskActivity.setTermSignals(array);
        }

        // read the script
        parseScript(customTaskActivity, element, parse);
        return customTaskActivity;
    }
}
