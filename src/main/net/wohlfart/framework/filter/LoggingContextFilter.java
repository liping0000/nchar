package net.wohlfart.framework.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.entities.CharmsUser;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.web.AbstractFilter;
import org.slf4j.MDC;

/**
 * This filter adds the authenticated user name to the log4j mapped diagnostic
 * context so that it can be included in formatted log output if desired.
 * 
 * we put some information in a Mapped Diagnostic Context see:
 * http://seamframework.org/Documentation/LoggingUserActivityWithAFilter
 * http://logback.qos.ch/manual/mdc.html
 * 
 */
@Startup
@Scope(ScopeType.APPLICATION)
@Name("loggingContextFilter")
@BypassInterceptors
@Filter(around = "org.jboss.seam.web.ajax4jsfFilter")
public class LoggingContextFilter extends AbstractFilter {

    /** Identifier for the username "diagnostic context". */
    public static final String USERNAME_CONTEXT = "loguser";
    public static final String HOST_CONTEXT     = "loghost";

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException,
            ServletException {

        if (servletRequest instanceof HttpServletRequest) {

            final HttpServletRequest req = (HttpServletRequest) servletRequest;
            // if (matchesRequestPath(req)) {
            final HttpSession session = req.getSession();
            final Object o = session.getAttribute(CharmsUserIdentityStore.AUTHENTICATED_USER);
            if (o instanceof CharmsUser) {
                final CharmsUser charmsUser = (CharmsUser) o;
                final String username = charmsUser.getName();
                if (username != null) {
                    MDC.put(USERNAME_CONTEXT, username);
                }

            }
            // AUTHENTICATED_USER
            // Object o =
            // session.getAttribute("org.jboss.seam.security.identity");
            // if (o instanceof Identity) {
            // Identity identity = (Identity) o;
            // Principal principal = identity.getPrincipal();
            // // the identity is stored as attribute even if the user is not
            // logged in!
            // // FIXME: check if the same happens with the authenticatedUser
            // (this might end up as security bug)
            // // try: Object o =
            // session.getAttribute(CharmsIdentityStore.AUTHENTICATED_USER);
            // if (identity.isLoggedIn() && (principal != null)) {
            // String username = principal.getName();
            // if (username != null) {
            // MDC.put(USERNAME_CONTEXT, username + "x");
            // }
            // }
            // }

            // }

            final String remoteAddress = req.getRemoteAddr();
            if (remoteAddress != null) {
                MDC.put(HOST_CONTEXT, remoteAddress);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);

        MDC.remove(HOST_CONTEXT);
        MDC.remove(USERNAME_CONTEXT);
    }
}
