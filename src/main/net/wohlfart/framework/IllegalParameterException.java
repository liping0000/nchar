package net.wohlfart.framework;

import org.jboss.seam.annotations.ApplicationException;
import org.jboss.seam.annotations.exception.Redirect;

@Redirect(viewId = "/pages/user/home.xhtml")
@ApplicationException(end = true)
public class IllegalParameterException extends Exception {


    public IllegalParameterException(final String string) {
        super(string);
    }

}
