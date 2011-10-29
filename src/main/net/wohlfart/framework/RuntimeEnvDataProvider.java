package net.wohlfart.framework;

import java.io.File;

import javax.servlet.ServletContext;

import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * this class is encapsulating the Filesystem layout for the web application it
 * provides the directory and files for services like
 * 
 * * lucene * configuation * bootstraping data
 * 
 * we have to refactor for moving on a tomcat cluster since the directories
 * might not be available on all nodes of the cluster
 * 
 * this class is implemented as flyweight pattern and can be instantiated and
 * disposed by user code
 * 
 * @author Michael Wohlfart
 * 
 */
public class RuntimeEnvDataProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(RuntimeEnvDataProvider.class);

    public enum FileSystemLocation {
        LUCENE_DIRECTORY("/var/lucene"), BOOT_DIRECTORY("/boot"), JASPER_DIRECTORY("/var/jasper");

        // the path is relative to the application directory
        // (.../webapps/charms)
        private final String path;

        FileSystemLocation(final String path) {
            this.path = path;
        }

        public File getLocation() {
            return RuntimeEnvDataProvider.getLocation(path);
        }
    }

    private static File getLocation(final String path) {
        ServletContext servletContext = getServletContext();
        if (servletContext == null) {
            LOGGER.warn("Servlet context is null, returning null");
            return null;
        } else {
            final String realPath = getServletContext().getRealPath(path);
            LOGGER.debug("realPath is: {}", realPath);
            final File file = new File(realPath);
            return file;
        }
    }

    public static ServletContext getServletContext() {
        final XmlWebApplicationContext springContext = getSpringContext();
        if (springContext == null) {
            LOGGER.warn("Spring context is null, returning null");
            return null;
        } else {
            final ServletContext servletContext = springContext.getServletContext();
            // for testing this is a org.jboss.seam.mock.MockServletContext
            // in tomcat this should be a
            // org.apache.catalina.core.ApplicationContextFacade
            LOGGER.debug("servletContext is: {}", servletContext);
            LOGGER.debug("context class is: {}", servletContext.getClass());
            LOGGER.debug("context path is: {}", servletContext.getContextPath());
            return servletContext;
        }
    }

    // get the spring context
    public static XmlWebApplicationContext getSpringContext() {
        final Context seamContext = getSeamContext(); // this return null if no seam context is available
        if (seamContext == null) {
            LOGGER.warn("seam context is null, can't find spring context");
            return null;
        } else {
            final XmlWebApplicationContext springContext = (XmlWebApplicationContext) seamContext.get(SpringConstants.SPRING_CONTEXT_NAME);
            LOGGER.debug("springContext is: {} class is {}", springContext, springContext.getClass());
            return springContext;
        }
    }

    // get the seam context, for tests this might be null if no seam instance
    // has been booted up yet
    public static Context getSeamContext() {

        // FIXME: this is a hack for tests, shouldn't happen in production
        if (!Contexts.isEventContextActive() && !Contexts.isApplicationContextActive()) {
            LOGGER.warn("no application context active");
            if (!Lifecycle.isApplicationInitialized()) {
                LOGGER.warn("no application initialized, Lifecycle.beginCall() would fail, return null as SeamContext ");
                return null; 
            } else {
                LOGGER.warn("no application context active, performing Lifecycle.beginCall()");
                Lifecycle.beginCall();
            }
        }

        final Context seamContext = Contexts.getApplicationContext();
        LOGGER.debug("seamContext is: {} class is {}", seamContext, seamContext.getClass());
        return seamContext;
    }

}
