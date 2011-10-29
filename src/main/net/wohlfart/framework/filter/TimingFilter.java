package net.wohlfart.framework.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.wohlfart.framework.interceptor.TimedInvocation;
import net.wohlfart.framework.interceptor.TimingInterceptor;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.web.AbstractFilter;

@Scope(ScopeType.APPLICATION)
@Name("timingFilter")
@BypassInterceptors
@Install(value = false, precedence = Install.APPLICATION)
@Filter
public class TimingFilter extends AbstractFilter implements Serializable {


    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws IOException,
            ServletException {

        TimingInterceptor.callChain.get().clear();

        chain.doFilter(servletRequest, servletResponse);

        final Collection<TimedInvocation> invocations = TimingInterceptor.callChain.get().values();

        final List<TimedInvocation> list = new ArrayList<TimedInvocation>(invocations);
        Collections.sort(list);

        for (final TimedInvocation timedInvocation : list) {
            System.out.println(timedInvocation);
        }

    }

}
