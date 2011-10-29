package net.wohlfart.jbpm4.node;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import org.hibernate.validator.Validator;
import org.jbpm.api.listener.EventListener;
import org.jbpm.api.listener.EventListenerExecution;
import org.jbpm.pvm.internal.wire.WireContext;
import org.jbpm.pvm.internal.wire.descriptor.AbstractDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

/**
 * this is a ugly hack to provide a transition with additional data to be used
 * in the UI, we implement an event listener that contains the data but don't do
 * anything in the notify method
 * 
 * this class is used to validate if a user selected transition can be taken
 * 
 * @author Michael Wohlfart
 * 
 */
public class TransitionValidator extends AbstractDescriptor implements Serializable, EventListener, Validator<Annotation> {

    private final static Logger LOGGER = LoggerFactory.getLogger(TransitionValidator.class);
    
    
    @Override
    public void notify(final EventListenerExecution execution) throws Exception {
        LOGGER.warn("notify called, execution is {}", execution);
    }

    @Override
    public Object construct(final WireContext wireContext) {
        LOGGER.warn("construct called, context is {}", wireContext);
        return null;
    }

    @Override
    public boolean isValid(Object value) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void initialize(Annotation parameters) {
        // TODO Auto-generated method stub      
    }
  
}
