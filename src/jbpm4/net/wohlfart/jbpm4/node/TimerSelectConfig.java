package net.wohlfart.jbpm4.node;


import java.io.Serializable;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.queries.JbpmTransitionUserTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * this class is responsible for providing a timer configuration
 * for the workflow
 * 
 * @author Michael Wohlfart
 *
 */
public class TimerSelectConfig implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimerSelectConfig.class);
       
    public TimerSelectConfig() {
        LOGGER.debug("created a TimerSelectConfig");
    }

}
