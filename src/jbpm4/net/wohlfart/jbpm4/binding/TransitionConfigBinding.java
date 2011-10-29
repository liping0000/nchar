package net.wohlfart.jbpm4.binding;

import java.util.List;

import net.wohlfart.jbpm4.node.ISelectConfig;
import net.wohlfart.jbpm4.node.TimerSelectConfig;
import net.wohlfart.jbpm4.node.TransitionConfig;
import net.wohlfart.jbpm4.node.UserRemarkConfig;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


public class TransitionConfigBinding extends JpdlBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(TransitionConfigBinding.class);

    private static final String TAG    = "transitionConfig";

    public TransitionConfigBinding() {
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
  
        final TransitionConfig transitionConfig = new TransitionConfig();

        // get the group select element
        final Element groupSelectElement = XmlUtil.element(transitionConfigElement, "groupSelect");
        if (groupSelectElement != null) {
            ISelectConfig groupSelectConfig = (ISelectConfig) parser.parseElement(groupSelectElement, parse);
            transitionConfig.setGroupSelectConfig(groupSelectConfig);
        }
        
        // get the user select element
        final Element userSelectElement = XmlUtil.element(transitionConfigElement, "userSelect");
        if (userSelectElement != null) {
            ISelectConfig userSelectConfig = (ISelectConfig) parser.parseElement(userSelectElement, parse);
            transitionConfig.setUserSelectConfig(userSelectConfig);
        }
        
        // get the timer element
        final Element timerSelectElement = XmlUtil.element(transitionConfigElement, "timerSelect");
        if (timerSelectElement != null) {
            TimerSelectConfig timerSelectConfig = (TimerSelectConfig) parser.parseElement(timerSelectElement, parse);
            transitionConfig.setTimerSelectConfig(timerSelectConfig);
        }
        
        // get the remark element
        final Element userRemarkElement = XmlUtil.element(transitionConfigElement, "userRemark");
        if (userRemarkElement != null) {
            UserRemarkConfig userRemarkConfig = (UserRemarkConfig) parser.parseElement(userRemarkElement, parse);
            transitionConfig.setUserRemarkConfig(userRemarkConfig);
        }
    
        // FIXME: we probably get the validation stuff here
        
        return transitionConfig;
    }
    
    
    


}
