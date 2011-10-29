package net.wohlfart.admin;

import org.jboss.seam.ScopeType;
import net.wohlfart.email.entities.CharmsEmailMessage;
import net.wohlfart.framework.AbstractEntityHome;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.framework.EntityNotFoundException;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for showing a simple email message to the user, 
 * get the id and provide a factory instantiating the email for the id
 * there must be a page action setting the id with the setEntityId() method
 * from the super class
 * 
 * @author Michael Wohlfart
 */
@Scope(ScopeType.CONVERSATION)
@Name("charmsEmailMessageAction")
public class CharmsEmailMessageAction extends AbstractEntityHome<CharmsEmailMessage> {

    private static final String CHARMS_EMAIL_MESSAGE = "charmsEmailMessage";

    /**
     * the factory method
     * 
     * @return
     */
    @Transactional
    @Factory(value = CHARMS_EMAIL_MESSAGE)
    public CharmsEmailMessage getEmailMessage() {
        final CharmsEmailMessage charmsEmailMessage = getInstance();
        return charmsEmailMessage;
    }
  
    /**
     * needed for the setEntity implementation, 
     * to find and remove the entity fromt he conversation context
     */
    @Override
    protected String getNameInContext() {
        return CHARMS_EMAIL_MESSAGE;
    }
}
