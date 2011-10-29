package net.wohlfart;

import org.jbpm.api.Configuration;
import org.jbpm.api.ProcessEngine;
import org.jbpm.api.RepositoryService;
import org.jbpm.api.task.Task;
import org.jbpm.pvm.internal.cmd.CommandService;
import org.jbpm.pvm.internal.cmd.GetTaskCmd;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.processengine.ProcessEngineImpl;

@SuppressWarnings("unused")
public class Start {
    private static final String[] LOCATIONS = {"org/taha/applicationContext.xml"};

    private static volatile ProcessEngineImpl engine;

    public static void main(final String[] args) {
        new Start(). generateChangeLog();
    }
    
    /*
      liquibase --driver=oracle.jdbc.OracleDriver \
      --classpath=\path\to\classes:jdbcdriver.jar \
      --changeLogFile=com/example/db.changelog.xml \
      --url="jdbc:oracle:thin:@localhost:1521:XE" \
      --username=scott \
      --password=xxxxxx \
      generateChangeLog
     */
    private void generateChangeLog() {
        // get the database connection
    }
    
    
    
    private void setupProcessEngine() {
        engine = (ProcessEngineImpl) new Configuration().setResource("jbpm4.jvm.cfg.xml").buildProcessEngine();
        System.out.println("instanciated Engine in main: " + engine);
        new Start().doSomeStuff();
    }

    private void doSomeStuff() {
        try {
            /* EnvironmentImpl environment = */ engine.openEnvironment();
            //System.out.println("environment is: " + environment);

            RepositoryService repositoryService = EnvironmentImpl.getFromCurrent(RepositoryService.class);
            //System.out.println("RepositoryService is : " + repositoryService);     
            long pdCount = repositoryService.createProcessDefinitionQuery().count();
            System.out.println("pdcount is: " + pdCount);

            CommandService commandService = EnvironmentImpl.getFromCurrent(CommandService.class);
            //System.out.println("commandService is : " + commandService);
            Task task = commandService.execute(new GetTaskCmd("4711"));
            //System.out.println("task 4711 is : " + task);
        } finally {
            engine.close();
        }
    }

}
