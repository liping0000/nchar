package net.wohlfart.jbpm4.processs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a single mail template item id name pair
 * 
 * @author Michael Wohlfart
 * 
 */
public class MailTemplateItem {

    private final static Logger LOGGER = LoggerFactory.getLogger(MailTemplateItem.class);

    private final Long          id;
    private final String        name;

    public MailTemplateItem(final Long id, final String name) {
        this.id = id;
        this.name = name;
        LOGGER.debug("created mailTemplateItem: {}, {} ", id, name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
