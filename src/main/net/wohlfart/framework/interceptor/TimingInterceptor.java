package net.wohlfart.framework.interceptor;

// import org.jboss.seam.annotations.intercept.AroundInvoke;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import org.jboss.seam.intercept.InvocationContext;
import org.jboss.seam.persistence.HibernateSessionProxyInterceptor;
import org.jboss.seam.persistence.ManagedEntityInterceptor;
import org.jboss.seam.security.SecurityInterceptor;
import org.jboss.seam.transaction.TransactionInterceptor;

/**
 * see: http://www.seamframework.org/Community/
 * SeamPerformanceProblemRewardingWorkaround#comment12105
 * 
 * @author Michael Wohlfart
 * 
 */

@Interceptor(stateless = true, type = InterceptorType.SERVER,
// type=InterceptorType.CLIENT, // need CLIENT to intercept the
// SecurityInterceptor

around = { BijectionInterceptor.class, MethodContextInterceptor.class, ConversationInterceptor.class, SynchronizationInterceptor.class,
        ConversationalInterceptor.class, RemoveInterceptor.class, SeamInterceptor.class, SecurityInterceptor.class, TransactionInterceptor.class,
        EventInterceptor.class, HibernateSessionProxyInterceptor.class, ManagedEntityInterceptor.class })
public class TimingInterceptor implements Serializable {


    public final static CallChain callChain = new CallChain();

    @AroundInvoke
    public Object timeCall(final InvocationContext invocation) throws Exception {
        final long t0 = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            final long dt = System.nanoTime() - t0;
            callChain.addInvocation(invocation, dt);
        }
    }

    // -----------------------------------------------------------------------------

    /**
     * A call chain is the set of invocations on methods (annotated with
     * MeasureCalls) that a request issued on its way through the application
     * stack.
     */
    public static class CallChain extends ThreadLocal<Map<Method, TimedInvocation>> {

        @Override
        protected Map<Method, TimedInvocation> initialValue() {
            return new HashMap<Method, TimedInvocation>();
        }

        public void addInvocation(final InvocationContext invocation, final long dt) {
            final Map<Method, TimedInvocation> invocations = get();
            final Method method = invocation.getMethod();
            if (!invocations.containsKey(method)) {
                invocations.put(method, new TimedInvocation(invocation.getMethod(), dt));
            } else {
                final TimedInvocation timedInvocation = invocations.get(method);
                timedInvocation.anotherCall(dt);
            }
        }

        public int totalNumberOfInvocations() {
            final Map<Method, TimedInvocation> invocations = get();
            final Collection<TimedInvocation> timedInvocationCollection = invocations.values();
            int totCalls = 0;
            for (final TimedInvocation invocation : timedInvocationCollection) {
                totCalls += invocation.getCalls();
            }
            return totCalls;
        }
    }
}
