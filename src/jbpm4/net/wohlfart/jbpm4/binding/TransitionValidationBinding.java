package net.wohlfart.jbpm4.binding;

import java.util.List;

import net.wohlfart.jbpm4.node.ISelectConfig;
import net.wohlfart.jbpm4.node.TransitionConfig;
import net.wohlfart.jbpm4.node.TransitionValidator;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


public class TransitionValidationBinding extends JpdlBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(TransitionValidationBinding.class);

    private static final String TAG    = "transitionValidation";

    public TransitionValidationBinding() {
        super(TAG);
        LOGGER.debug("constructor finished");
    }

    
    /*
     * Stacktrace for this method:
     * 
     * at java.lang.Thread.dumpStack(Thread.java:1206) 
     * at net.wohlfart.jbpm4.activity.TransitionConfigBinding.parseJpdl(TransitionConfigBinding.java:21) 
     * at org.jbpm.jpdl.internal.activity.JpdlBinding.parse(JpdlBinding.java:45) 
     * at org.jbpm.jpdl.internal.xml.JpdlParser.parseOnEvent(JpdlParser.java:444) 
     * at org.jbpm.jpdl.internal.xml.JpdlParser.parseTransitions(JpdlParse.java:511) 
     * at org.jbpm.jpdl.internal.xml.JpdlParser.parseActivities(JpdlParser.java:308) 
     * at org.jbpm.jpdl.internal.xml.JpdlParser.parseDocumentElement(JpdlParser.java:253) 
     * at org.jbpm.pvm.internal.xml.Parser.parseDocument(Parser.java:480)
     * 
     */

    @Override
    public Object parseJpdl(final Element transitionConfigElement, final Parse parse, final JpdlParser parser) {
        LOGGER.info("parseJpdl called with element {}, parse {}, parser {}", new Object[] { transitionConfigElement, parse, parser });
        final TransitionValidator transitionValidation = new TransitionValidator();
        return transitionValidation;
    }
    
}
