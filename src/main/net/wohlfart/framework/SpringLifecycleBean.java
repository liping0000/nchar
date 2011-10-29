package net.wohlfart.framework;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * this bean implements stuff that need to be done at application startup:
 * 
 * * create the lucene index as soon as we have a session fab
 * 
 * @author Michael Wohlfart
 * 
 */
// @Transactional not needed if we just want single methods in a transactional
// context
public class SpringLifecycleBean implements InitializingBean, DisposableBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(SpringLifecycleBean.class);

    private SessionFactory sessionFactory;
    private LuceneIndexer luceneIndexer;
    
    
    // config parameter
    private boolean doLuceneSetup = false;

    @Required
    public void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Required
    public void setLuceneIndexer(final LuceneIndexer luceneIndexer) {
        this.luceneIndexer = luceneIndexer;
    }
    
    
    // config to modify behaviour on startup
    
    public void setDoLuceneSetup(boolean doLuceneSetup) {
        this.doLuceneSetup = doLuceneSetup;
    }
    

    /**
     * called after all properties are set
     * 
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (doLuceneSetup) {
            setupLucene();
        } else {
//          LOGGER.warn("running lucene indexer on startup is off for testing, stacktrace is {} ", 
//          StringUtils.join(Thread.currentThread().getStackTrace(), "\n"));
          LOGGER.warn("running lucene indexer on startup is off for testing");
        }
    }

    @Override
    public void destroy() throws Exception {
        LOGGER.debug("spring triggered destroy");     
        deregisterAllDrivers();
        // TODO: tomcat restart problem, somehow we need to end all threads...
        // Sep 26, 2010 9:21:45 PM org.apache.catalina.loader.WebappClassLoader clearReferencesThreads
        // SEVERE: The web application [/charms] appears to have started a thread named [Resource Destroyer in BasicResourcePool.close()] but has failed to stop it. This is very likely to create a memory leak.
    }
    

    /**
     * setup the lucene indexer
     */
    private void setupLucene() {
        LOGGER.debug("running init method for lucene");
        luceneIndexer.initLuceneIndex(sessionFactory);
    }
    
    
    /**
     * remove JDBC drivers
     * 
     * @throws SQLException
     */
    private void deregisterAllDrivers() throws SQLException {
        // remove all drivers on shutdown/restart
        final Enumeration<Driver> drivers = java.sql.DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            final Driver driver = drivers.nextElement();
            java.sql.DriverManager.deregisterDriver(driver);
        }
    }

}
