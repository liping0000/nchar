package net.wohlfart.terminal.commands;

import java.util.List;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestCostSheet;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestFolder;
import net.wohlfart.changerequest.entities.ChangeRequestImpactSheet;
import net.wohlfart.changerequest.entities.ChangeRequestMessageEntry;
import net.wohlfart.terminal.IRemoteCommand;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.TypedValue;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Transactional;
import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.processengine.ProcessEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * this class fixes the user id fields in the message entities
 * 
 * @author Michael Wohlfart
 * 
 */
public class PerformFixExecutionVariables implements IRemoteCommand {

    private final static Logger LOGGER         = LoggerFactory.getLogger(PerformFixExecutionVariables.class);

    public static final String COMMAND_STRING = "fix executionVariables";

    @Override
    public boolean canHandle(final String commandLine) {
        return StringUtils.startsWith(StringUtils.trim(commandLine), COMMAND_STRING);
    }

    @Override
    @Transactional
    public String doHandle(final String commandLine) {
        final Session hibernateSession = (Session) Component.getInstance("hibernateSession", true);
        LOGGER.debug("hibernateSession is: {}", hibernateSession);

        hibernateSession.getTransaction().begin();

        // the variables we create need to be created withing an jbpm4 environment...       
        final ProcessEngineImpl processEngine = (ProcessEngineImpl) Component.getInstance("processEngine", true);
        // at this point we know for sure we have a task
        final EnvironmentImpl environment = processEngine.openEnvironment();
        try {
            doTheFix(hibernateSession);           
        } catch (Exception ex) {
            environment.close();
        }

        LOGGER.debug("flushing...");
        hibernateSession.flush();

        LOGGER.debug("committing...");
        hibernateSession.getTransaction().commit();

        return COMMAND_STRING + " done";
    }

    @SuppressWarnings("unchecked")
    private void doTheFix(final Session hibernateSession) {
        
        // we need 6 variables in each ongoing execution:
//   DBID_ CLASS_  DBVERSION_  KEY_    CONVERTER_  HIST_   EXECUTION_  TASK_   LOB_    DATE_VALUE_ DOUBLE_VALUE_   CLASSNAME_  LONG_VALUE_ STRING_VALUE_   TEXT_VALUE_ EXESYS_ 
//        1093    hib-long    0   changeRequestCostSheet  <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestCostSheet  181 <NULL>  <NULL>  <NULL>  
//        1096    hib-long    0   changeRequestData   <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestData   181 <NULL>  <NULL>  <NULL>  
//        1098    hib-long    0   changeRequestFolder <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestFolder 181 <NULL>  <NULL>  <NULL>  
//        1097    hib-long    0   messageEntry    <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestMessageEntry   1470    <NULL>  <NULL>  <NULL>  
//        1095    hib-long    0   changeRequestImpactSheet    <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestImpactSheet    181 <NULL>  <NULL>  <NULL>  
//        1094    hib-long    0   changeRequestMessageTree    <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestMessageEntry   1470    <NULL>  <NULL>  <NULL>  


// find all executions
      
        // list the ongoing root executions
        final List<ExecutionImpl> roots = (List<ExecutionImpl>) hibernateSession
            .createCriteria(ExecutionImpl.class)
            .add(Restrictions.eq("state", "active-root"))
            .list();
        
        final List<ExecutionImpl> subexs = (List<ExecutionImpl>) hibernateSession
            .createCriteria(ExecutionImpl.class)
            .add(Restrictions.eq("state", "active-concurrent"))
            .list();       
       
        
        // the root executions
        for (ExecutionImpl execution : roots) {
            findVariables(execution, hibernateSession);            
            long pid = execution.getDbid();           
            
            ChangeRequestMessageEntry changeRequestMessageTree = (ChangeRequestMessageEntry) hibernateSession
                    .getNamedQuery(ChangeRequestMessageEntry.FIND_ROOT_BY_PID).setParameter("pid", pid).uniqueResult();
            
            execution.createVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_MESSAGE_TREE, changeRequestMessageTree);
            execution.createVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE, changeRequestMessageTree);
            
            if (changeRequestMessageTree.getContent() != null) {
                LOGGER.warn("we found a root message entry without which is not empty, pid: {}, content: {}",
                        changeRequestMessageTree.getProcessInstanceId(), changeRequestMessageTree.getContent() );
            }
        }
        
        
        // the subprocesses
        for (ExecutionImpl execution : subexs) {         
            long pid = execution.getDbid();       
            
            // there might be multiple message entries in a tree structure
            // for an execution, for example a
            // task that has been forwarded multiple times...
            // we want the one on the top of the root
            List<ChangeRequestMessageEntry> changeRequestMessageTreeList = (List<ChangeRequestMessageEntry>) hibernateSession
                    .getNamedQuery(ChangeRequestMessageEntry.FIND_ROOT_BY_PID).setParameter("pid", pid).list();
            
            for (ChangeRequestMessageEntry entry : changeRequestMessageTreeList) {
                // must have a parent since we are in a subexecution here...
                // the entry whose parent is no lonmger within this execution wins....
                if (entry.getParent().getProcessInstanceId() != pid) {
                    execution.createVariable(ChangeRequestMessageEntry.CHANGE_REQUEST_CURRENT_MESSAGE, entry);                   
                }
            }
            
             
        }


    }
    
    private void findVariables(ExecutionImpl execution, Session hibernateSession) {
        long pid = execution.getDbid();
        
        ChangeRequestCostSheet changeRequestCostSheet = (ChangeRequestCostSheet) hibernateSession
                .getNamedQuery(ChangeRequestCostSheet.FIND_BY_PID).setParameter("pid", pid).uniqueResult();

        execution.createVariable(ChangeRequestCostSheet.CHANGE_REQUEST_COSTSHEET, changeRequestCostSheet);
        
        ChangeRequestData changeRequestData = (ChangeRequestData) hibernateSession
                .getNamedQuery(ChangeRequestData.FIND_BY_PID).setParameter("pid", pid).uniqueResult();
        execution.createVariable(ChangeRequestData.CHANGE_REQUEST_DATA, changeRequestData);
        
        ChangeRequestFolder changeRequestFolder = (ChangeRequestFolder) hibernateSession
                .getNamedQuery(ChangeRequestFolder.FIND_BY_PID).setParameter("pid", pid).uniqueResult();
        execution.createVariable(ChangeRequestFolder.CHANGE_REQUEST_FOLDER, changeRequestFolder);
        
        ChangeRequestImpactSheet changeRequestImpactSheet = (ChangeRequestImpactSheet) hibernateSession
                .getNamedQuery(ChangeRequestImpactSheet.FIND_BY_PID).setParameter("pid", pid).uniqueResult();
        execution.createVariable(ChangeRequestImpactSheet.CHANGE_REQUEST_IMPACTSHEET, changeRequestImpactSheet);
        
    }
    

    @Override
    public String getUsage() {
        return COMMAND_STRING + ": fix the execution variables by recreating all variables for the executions";
    }

}
