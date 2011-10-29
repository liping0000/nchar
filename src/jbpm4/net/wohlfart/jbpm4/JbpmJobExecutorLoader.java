package net.wohlfart.jbpm4;

import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

public class JbpmJobExecutorLoader implements ApplicationListener<ApplicationEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmJobExecutorLoader.class);

    private ProcessEngine processEngine;

    @Required
    public void setProcessEngine(final ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextRefreshedEvent) {
            LOGGER.info("checking job executor...");
            final JobExecutor jobExecutor = processEngine.get(JobExecutor.class);
            if (!jobExecutor.isActive()) {
                LOGGER.info("starting job executor, idle time is {}, thread count is {}", jobExecutor.getIdleMillis(), jobExecutor.getNbrOfThreads());
                jobExecutor.start();
            } else {
                LOGGER.info("job executor is alread active");
            }
        } else if (applicationEvent instanceof ContextClosedEvent) {
            LOGGER.info("stopping process Engine");
            final JobExecutor jobExecutor = processEngine.get(JobExecutor.class);
            jobExecutor.stop(true); // true: join-> block until all threads are dead
        }
    }

}
