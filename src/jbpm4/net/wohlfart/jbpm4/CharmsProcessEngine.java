package net.wohlfart.jbpm4;

import org.hibernate.cfg.Configuration;
import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.cfg.ConfigurationImpl;
import org.jbpm.pvm.internal.env.EnvironmentFactory;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.env.PvmEnvironment;
import org.jbpm.pvm.internal.env.SpringContext;
import org.jbpm.pvm.internal.processengine.ProcessEngineImpl;
import org.jbpm.pvm.internal.wire.descriptor.ProvidedObjectDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * this is basically the spring process engine enhanced with the seam context
 * 
 * @author Michael Wohlfart
 * 
 */
public class CharmsProcessEngine extends ProcessEngineImpl implements EnvironmentFactory, ProcessEngine {

    private final static Logger          LOGGER           = LoggerFactory.getLogger(CharmsProcessEngine.class);
    // private static final Log log =
    // Log.getLog(CharmsProcessEngine.class.getName());

    private static final long            serialVersionUID = 1L;

    // applicationContext is not serializable!
    private transient ApplicationContext applicationContext;

    public static ProcessEngine create(final ConfigurationImpl configuration) {
        CharmsProcessEngine processEngine = null;
        ApplicationContext applicationContext = null;

        applicationContext = (ApplicationContext) configuration.getApplicationContext();

        processEngine = new CharmsProcessEngine();
        processEngine.applicationContext = applicationContext;
        processEngine.initializeProcessEngine(configuration);

        final LocalSessionFactoryBean localSessionFactoryBean = processEngine.get(LocalSessionFactoryBean.class);
        final Configuration hibernateConfiguration = localSessionFactoryBean.getConfiguration();
        processEngine.processEngineWireContext.getWireDefinition().addDescriptor(new ProvidedObjectDescriptor(hibernateConfiguration, true));

        // processEngine.checkDb(configuration);

        return processEngine;
    }

    @Override
    public EnvironmentImpl openEnvironment() {
        final PvmEnvironment environment = new PvmEnvironment(this);

        LOGGER.debug("opening jbpm-spring {}", environment);

        // this is the last context we lookup:
        // not needed any more, we use a custom ScriptManager now...
        // environment.setContext(new SeamContext());
        //

        environment.setContext(new SpringContext(applicationContext));

        installAuthenticatedUserId(environment);
        installProcessEngineContext(environment);
        installTransactionContext(environment);

        return environment;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Class<T> type) {
        final T candidateComponent = super.get(type);

        if (candidateComponent != null) {
            return candidateComponent;
        }

        final String[] names = applicationContext.getBeanNamesForType(type);

        if (names.length >= 1) {
            if ((names.length > 1) && LOGGER.isWarnEnabled()) {
                LOGGER.warn("Multiple beans for type {} found. Returning the first result.", type);
            }

            return (T) applicationContext.getBean(names[0]);
        }
        return null;
    }

    @Override
    public Object get(final String key) {
        if (applicationContext.containsBean(key)) {
            return applicationContext.getBean(key);
        }
        return super.get(key);
    }
}
