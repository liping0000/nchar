package net.wohlfart.framework.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.web.AbstractFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Scope(ScopeType.APPLICATION)
@Name("sessionIdFilter")
@BypassInterceptors
@Filter(around = "org.jboss.seam.web.ajax4jsfFilter")
public class SessionIdFilter extends AbstractFilter {

    private final static Logger LOGGER = LoggerFactory.getLogger(SessionIdFilter.class);

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException {

        // pass along if this isn't a HttpServletRequest, no idea what to do
        // with it
        if (!(req instanceof HttpServletRequest)) {
            chain.doFilter(req, res);
            return;
        }

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        // Redirect requests with JSESSIONID in URL to clean version (old links
        // bookmarked/stored by bots). This is ONLY triggered if the request did
        // not also contain a JSESSIONID cookie! Which should be fine for
        // bots...
        if (request.isRequestedSessionIdFromURL()) {
            final String url = request.getRequestURL().append(request.getQueryString() != null ? "?" + request.getQueryString() : "").toString();
            LOGGER.warn("found URL with session id in URL string, redirecting to a clean URL: {}", url);
            // TODO: The url is clean, at least in Tomcat, which strips out
            // the JSESSIONID path parameter automatically (Jetty does not?!)
            response.setHeader("Location", url);
            response.sendError(HttpServletResponse.SC_MOVED_PERMANENTLY);
            return;
        }

        // Prevent rendering of JSESSIONID in URLs for all outgoing links
        final HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response) {

            @Override
            public String encodeRedirectUrl(final String url) {
                return url;
            }

            @Override
            public String encodeRedirectURL(final String url) {
                return url;
            }

            @Override
            public String encodeUrl(final String url) {
                return url;
            }

            @Override
            public String encodeURL(final String url) {
                return url;
            }
        };
        chain.doFilter(req, wrappedResponse);
    }

}
