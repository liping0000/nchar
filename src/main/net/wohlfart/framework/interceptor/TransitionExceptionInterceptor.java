package net.wohlfart.framework.interceptor;

// import org.jboss.seam.annotations.intercept.AroundInvoke;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.jboss.seam.annotations.intercept.AroundInvoke;
import org.jboss.seam.annotations.intercept.Interceptor;
import org.jboss.seam.annotations.intercept.InterceptorType;
import org.jboss.seam.core.BijectionInterceptor;
import org.jboss.seam.core.ConversationInterceptor;
import org.jboss.seam.core.ConversationalInterceptor;
import org.jboss.seam.core.EventInterceptor;
import org.jboss.seam.core.MethodContextInterceptor;
import org.jboss.seam.core.SynchronizationInterceptor;
import org.jboss.seam.ejb.RemoveInterceptor;
import org.jboss.seam.ejb.SeamInterceptor;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.intercept.InvocationContext;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.persistence.HibernateSessionProxyInterceptor;
import org.jboss.seam.persistence.ManagedEntityInterceptor;
import org.jboss.seam.security.SecurityInterceptor;
import org.jboss.seam.transaction.TransactionInterceptor;
import org.jbpm.api.JbpmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor(stateless = true, type = InterceptorType.SERVER,
// type=InterceptorType.CLIENT, // need CLIENT to intercept the
// SecurityInterceptor
around = { BijectionInterceptor.class, MethodContextInterceptor.class, ConversationInterceptor.class, SynchronizationInterceptor.class,
        ConversationalInterceptor.class, RemoveInterceptor.class, SeamInterceptor.class, SecurityInterceptor.class, TransactionInterceptor.class,
        EventInterceptor.class, HibernateSessionProxyInterceptor.class, ManagedEntityInterceptor.class })
public class TransitionExceptionInterceptor implements Serializable /*
                                                                     * extends
                                                                     * AbstractInterceptor
                                                                     */{

    // this class needs to be serializable for mojarra-2.0.1 (JSF2)


    private final static Logger LOGGER = LoggerFactory.getLogger(TransitionExceptionInterceptor.class);

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext ctx) throws Exception {

        final FacesMessages facesMessages = FacesMessages.instance();
        try {
            return ctx.proceed();
        } catch (final JbpmException ex) {
            facesMessages.addFromResourceBundle(Severity.FATAL, "jbpm error: " + ex.toString());
            LOGGER.error("parameters were: {}", ctx.getParameters(), ex);
            ex.printStackTrace();
            return "invalid";
            // } catch (TransientObjectException ex) {
            // facesMessages.addFromResourceBundle(Severity.WARN, "jbpm error: "
            // + ex.toString());
            // LOGGER.error(ex.toString() + " parameters were: " +
            // ctx.getParameters());
            // ex.printStackTrace();
            // return "invalid";
        } catch (final InvocationTargetException ex) {
            // just to test if we can catch hardcore errors:
            facesMessages.addFromResourceBundle(Severity.FATAL, "reflection error: " + ex.toString());
            LOGGER.error("parameters were: {}", ctx.getParameters(), ex);
            ex.printStackTrace();
            return "invalid";
        }

    }

}
