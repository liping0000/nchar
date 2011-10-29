package net.wohlfart.framework;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

public class Log4jTriggerEvaluator implements TriggeringEventEvaluator {

    @Override
    public boolean isTriggeringEvent(final LoggingEvent event) {
        return true;
    }

}
