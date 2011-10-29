package net.wohlfart.jbpm4;

import org.jbpm.api.ProcessEngine;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * copy of: org.jbpm.pvm.internal.processengine.SpringHelper
 * 
 * this class is used in the spring config to initialize a workfow engine
 */
public class CharmsJbpmSpringHelper implements ApplicationContextAware {

    protected ApplicationContext applicationContext;
    protected String             jbpmCfg = "jbpm.cfg.xml"; // the default value,
                                                           // overridden
                                                           // with jbpm4.cfg.xml

    public void setJbpmCfg(final String jbpmCfg) {
        this.jbpmCfg = jbpmCfg;
    }

    @Override
    // called by spring
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ProcessEngine createProcessEngine() {
        final ProcessEngine engine = new CharmsJbpmConfigurationImpl().springInitiated(applicationContext).setResource(jbpmCfg).buildProcessEngine();

        return engine;
    }

}
