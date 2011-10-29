package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.activity.AbstractActivity;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public abstract class AbstractScriptNodeBinding extends JpdlBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractScriptNodeBinding.class);

    public AbstractScriptNodeBinding(final String tagName) {
        super(tagName);
    }

    /**
     * helper method called by subclasses to parse a script tag for the activity
     * this method sets the script language and the script code in theabstract
     * activity
     * 
     * @param abstractActivity
     * @param element
     * @param parse
     */
    protected void parseScript(final AbstractActivity abstractActivity, final Element element, final Parse parse) {
        // check if we have a script element
        final Element scriptElement = XmlUtil.element(element, AbstractActivity.TAG_SCRIPT_NAME);
        LOGGER.info("parsed scriptElement: " + scriptElement);

        if (scriptElement != null) {
            // we have a script element, get the content and the language
            // attribute
            // and set it in the activity
            final String language = XmlUtil.attribute(scriptElement, AbstractActivity.ATTRIBUTE_SCRIPTLANGUAGE_NAME,
            // false, // obsolete
                    null, // parse, // null parse so we don't get any errors
                          // attached in case the attribute is not present
                    "groovy");
            LOGGER.info("parsed language: " + language);
            abstractActivity.setScriptLanguage(language);

            final Element textElement = XmlUtil.element(scriptElement, AbstractActivity.TAG_SCRIPT_TEXT);
            if (textElement == null) {
                LOGGER.warn("no text element found for script, this is considered as a bug in the process definition");
            } else {
                final String script = XmlUtil.getContentText(textElement);
                LOGGER.info("parsed textElement: " + script);
                abstractActivity.setScript(script);
            }
        }
    }

}
