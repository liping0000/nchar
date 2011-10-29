package net.wohlfart.framework.debug;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * the following piece of xml needs to be in web.xml in order to get this
 * listener hooked up to the session and servlet context
 * 
 * <!-- custom listener for servlet context and http session tracking -->
 * <listener>
 * <listener-class>net.wohlfart.framework.HttpSessionCollectorListener
 * </listener-class> </listener>
 * 
 * on context init we put a arrayList into the servlet context
 * 
 * on sessionCreate/sessionDelete we add/remove the session from this list
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class HttpSessionCollector implements ServletContextListener, HttpSessionListener {

    private final static Logger LOGGER           = LoggerFactory.getLogger(HttpSessionCollector.class);

    public static final String  SESSION_LIST_KEY = "net.wohlfart.sessionList";

    // --- listeners for the servlet context

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        event.getServletContext().setAttribute(SESSION_LIST_KEY, new ArrayList<HttpSession>());
        LOGGER.info("contextInitialized");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        LOGGER.info("contextDestroyed");
    }

    // --- listeners for the http session

    @SuppressWarnings("unchecked")
    @Override
    public void sessionCreated(final HttpSessionEvent event) {
        ((List<HttpSession>) event.getSession().getServletContext().getAttribute(SESSION_LIST_KEY)).add(event.getSession());

        LOGGER.info("Session created; session id = " + event.getSession().getId());
        LOGGER.info("Number of active sessions: " + ((List<HttpSession>) event.getSession().getServletContext().getAttribute(SESSION_LIST_KEY)).size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sessionDestroyed(final HttpSessionEvent event) {
        LOGGER.info("sessionDestroyed");
        ((List<HttpSession>) event.getSession().getServletContext().getAttribute(SESSION_LIST_KEY)).remove(event.getSession());

        LOGGER.info("Session destroyed; session id = " + event.getSession().getId());
        LOGGER.info("Number of active sessions: " + ((List<HttpSession>) event.getSession().getServletContext().getAttribute(SESSION_LIST_KEY)).size());

    }

}
