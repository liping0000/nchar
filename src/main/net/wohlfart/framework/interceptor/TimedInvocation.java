package net.wohlfart.framework.interceptor;

import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;

/**
 * TimedInvocation is an invocation (i.e. a method call) which is being counted
 * and timed.
 */
public class TimedInvocation implements Comparable<TimedInvocation> {

    private long         dt;
    private int          calls = 1;
    private final Method method;

    public TimedInvocation(final Method method, final long dt) {
        this.method = method;
        this.dt = dt;
    }

    public long getDt() {
        return dt;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        final String className = method.getDeclaringClass().getName();
        final String shortendName = className.substring(method.getDeclaringClass().getPackage().getName().length() + 1);
        // dt is calculated with System.nanoTime() we want it in ms
        final String duration = StringUtils.leftPad((dt / 1e6) + " ms", 20);
        final String nCallStr = StringUtils.leftPad(String.valueOf(calls), 4);
        return duration + nCallStr + "   " + shortendName + "." + method.getName() + "()";
    }

    public void anotherCall(final long dt) {
        this.dt += dt;
        calls++;
    }

    public int getCalls() {
        return calls;
    }

    @Override
    public int compareTo(final TimedInvocation o) {
        return -Long.valueOf(dt).compareTo(o.dt);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + calls;
        result = prime * result + (int) (dt ^ (dt >>> 32));
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TimedInvocation other = (TimedInvocation) obj;
        if (calls != other.calls) {
            return false;
        }
        if (dt != other.dt) {
            return false;
        }
        if (method == null) {
            if (other.method != null) {
                return false;
            }
        } else if (!method.equals(other.method)) {
            return false;
        }
        return true;
    }

}
