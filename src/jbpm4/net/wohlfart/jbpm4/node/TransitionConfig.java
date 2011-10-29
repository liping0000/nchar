package net.wohlfart.jbpm4.node;

import java.io.Serializable;


import org.jbpm.api.listener.EventListener;
import org.jbpm.api.listener.EventListenerExecution;
import org.jbpm.pvm.internal.wire.WireContext;
import org.jbpm.pvm.internal.wire.descriptor.AbstractDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * this is a ugly hack to provide a transition with additional data to be used
 * in the UI, we implement an event listener that contains the data but don't do
 * anything in the notify method
 * 
 * 
 * config data we need here:
 * 
 * 
 * - comment none/optional/required - receiver user none/optional/required -
 * receiver group none/optional/required - does the transition spawn off a
 * subexecution (this is already configured in the activity node) - group of
 * users to select from if user is optional/required - group of groups to select
 * from if group is optional/required
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class TransitionConfig extends AbstractDescriptor implements Serializable, EventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(TransitionConfig.class);

    private ISelectConfig groupSelectConfig;
    private ISelectConfig userSelectConfig;
    private TimerSelectConfig timerSelectConfig;

    private UserRemarkConfig userRemarkConfig;

    
    
    @Override
    public void notify(final EventListenerExecution execution) throws Exception {
        LOGGER.info("notify called, execution is {}", execution);
    }

    @Override
    public Object construct(final WireContext wireContext) {
        LOGGER.info("construct called, context is {}", wireContext);
        return null;
    }

    
    
    /**
     * @return true if a user select popup should be shown
     */
    public Boolean getUser() {
        LOGGER.info("getUser called, userSelectConfig is: {}", userSelectConfig);
        return (userSelectConfig != null);
    }

    /**
     * @return true if a group select popup should be shown
     */
    public Boolean getGroup() {
        LOGGER.info("getGroup called, groupSelectConfig is: {}", groupSelectConfig);
        return (groupSelectConfig != null);
    }

    /**
     * @return true if a timer select popup should be shown
     */
    public Boolean getTimer() {
        LOGGER.info("getTimer called, timerSelectConfig is: {}", timerSelectConfig);
        return (timerSelectConfig != null);
    }

    /**
     * @return true if a timer select popup should be shown
     */
    public Boolean getRemark() {
        LOGGER.info("getRemark called, userRemarkConfig is: {}", userRemarkConfig);
        return (userRemarkConfig != null);
    }

    
    
    public ISelectConfig getGroupSelectConfig() {
        LOGGER.info("getGroupSelectConfig called, setting to: {}", groupSelectConfig);
        return groupSelectConfig;
    }
    public void setGroupSelectConfig(ISelectConfig groupSelectConfig) {
        LOGGER.info("setGroupSelectConfig called, setting to: {}", groupSelectConfig);
        this.groupSelectConfig = groupSelectConfig;
    }

    
    public ISelectConfig getUserSelectConfig() {
        LOGGER.info("getUserSelectConfig called, userSelectConfig is: {}", userSelectConfig);
        return userSelectConfig;
    }
    public void setUserSelectConfig(ISelectConfig userSelectConfig) {
        LOGGER.info("setUserSelectConfig called, setting to: {}", userSelectConfig);
        this.userSelectConfig = userSelectConfig;
    }
    
    
    public TimerSelectConfig getTimerSelectConfig() {
        LOGGER.info("getUserSelectConfig called, setting to: {}", timerSelectConfig);
        return timerSelectConfig;
    }
    public void setTimerSelectConfig(TimerSelectConfig timerSelectConfig) {
        LOGGER.info("setUserSelectConfig called, setting to: {}", timerSelectConfig);
        this.timerSelectConfig = timerSelectConfig;
    }

    public void getUserRemarkConfig(UserRemarkConfig userRemarkConfig) {
        LOGGER.info("setUserRemarkConfig called, setting to: {}", userRemarkConfig);
        this.userRemarkConfig = userRemarkConfig;
    }

    public void setUserRemarkConfig(UserRemarkConfig userRemarkConfig) {
        LOGGER.info("setUserRemarkConfig called, setting to: {}", userRemarkConfig);
        this.userRemarkConfig = userRemarkConfig;
    }

}
