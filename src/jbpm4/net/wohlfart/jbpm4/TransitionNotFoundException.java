package net.wohlfart.jbpm4;

import org.jbpm.api.JbpmException;

public class TransitionNotFoundException extends JbpmException {


    public TransitionNotFoundException(final String string) {
        super(string);
    }
}
