package net.wohlfart.jbpm4;

import java.util.Set;

import org.jboss.seam.contexts.Contexts;
import org.jbpm.pvm.internal.env.Context;

@Deprecated
public class SeamContext implements Context {

    @Override
    public Object get(final String key) {
        return Contexts.lookupInStatefulContexts(key);
    }

    @Override
    public <T> T get(final Class<T> type) {
        // we don't do any class lookups here
        return null;
    }

    @Override
    public String getName() {
        return "seamContext";
    }

    @Override
    public boolean has(final String key) {
        return (Contexts.lookupInStatefulContexts(key) != null);
    }

    @Override
    public Set<String> keys() {
        throw new UnsupportedOperationException("not supported on " + SeamContext.class.getName());
    }

    @Override
    public Object set(final String key, final Object value) {
        throw new UnsupportedOperationException("not supported on " + SeamContext.class.getName());
    }

}
