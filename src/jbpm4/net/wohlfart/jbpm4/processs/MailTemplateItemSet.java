package net.wohlfart.jbpm4.processs;

import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * implemenst a set of mail template items all assigned to the same name
 * 
 */
public class MailTemplateItemSet implements Serializable {


    private final static Logger                    LOGGER = LoggerFactory.getLogger(MailTemplateItemSet.class);

    private final String                           name;
    private final List<? extends MailTemplateItem> items;

    public MailTemplateItemSet(final String name, final List<? extends MailTemplateItem> items) {
        this.name = name;
        this.items = items;
        LOGGER.debug("created mailTemplateItemSet: {}, {} ", name, items);
    }

    public String getName() {
        return name;
    }

    public List<? extends MailTemplateItem> getItems() {
        return items;
    }
}
