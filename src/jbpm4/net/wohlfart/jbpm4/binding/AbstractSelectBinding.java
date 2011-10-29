package net.wohlfart.jbpm4.binding;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSelectBinding extends JpdlBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractSelectBinding.class);

    public AbstractSelectBinding(final String tagName) {
        super(tagName);
        LOGGER.debug("constructor finished");
    }

}
