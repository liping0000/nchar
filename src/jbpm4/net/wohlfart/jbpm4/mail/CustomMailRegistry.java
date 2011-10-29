package net.wohlfart.jbpm4.mail;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.pvm.internal.email.impl.MailTemplate;
import org.jbpm.pvm.internal.email.impl.MailTemplateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * we use custom mail producer, this is just a dummy to make jbpm4's config
 * parser happy....
 */
public class CustomMailRegistry extends MailTemplateRegistry {


    private final static Logger             LOGGER    = LoggerFactory.getLogger(CustomMailRegistry.class);

    private final Map<String, MailTemplate> templates = new HashMap<String, MailTemplate>();

    /**
     * this usually is set up by the wiring stuff when parsing the mail template
     * definitions...
     */
    @Override
    public void addTemplate(final String templateName, final MailTemplate template) {
        LOGGER.warn("addTemplate called for: {} , {} we don't use predefined templates do we? ", templateName, template);
        templates.put(templateName, template);
    }

    /**
     * returning the templates when needed we use custommail producer, this
     * should never be called..
     */
    @Override
    public MailTemplate getTemplate(final String templateName) {
        LOGGER.warn("getTemplate called for: {} this shouldn't happen since we don't use predefined templates ", templateName);
        final MailTemplate template = templates.get(templateName);
        if (template == null) {
            LOGGER.error("no template found, returning null ");
            return null;
        } else {
            return template;
        }
    }
}
