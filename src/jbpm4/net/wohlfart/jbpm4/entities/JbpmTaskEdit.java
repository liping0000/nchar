package net.wohlfart.jbpm4.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import net.wohlfart.framework.queries.ProcessTaskTable;
import net.wohlfart.jbpm4.activity.CustomTaskActivity;
import net.wohlfart.jbpm4.queries.AbstractTransitionTableQuery;
import net.wohlfart.jbpm4.queries.JbpmTransitionUserTable;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jbpm.pvm.internal.history.model.HistoryTaskImpl;
import org.jbpm.pvm.internal.job.JobImpl;
import org.jbpm.pvm.internal.job.TimerImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.CONVERSATION)
@Name(JbpmTaskEdit.JBPM_TASK_EDIT)
public class JbpmTaskEdit implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmTaskEdit.class);

    protected static final String JBPM_TASK_EDIT = "jbpmTaskEdit";
    
    private Long dbid;
    
    private TaskImpl taskImpl;
    

    
    
    public void setDbid(Long dbid) {
        this.dbid = dbid;
        LOGGER.warn("setDbid called: " + dbid);
        // setting the taskid means we have to query for the task
        Session session = (Session) Component.getInstance("hibernateSession");
        
        taskImpl = (TaskImpl) session
            .createCriteria(TaskImpl.class)
            .add(Restrictions.eq("dbid", dbid))
            .uniqueResult();
    }
    public Long getDbid() {
        LOGGER.warn("getDbid called: " + dbid);
        return dbid;
    }

    
    public void setTaskId(String taskId) {
        //.taskId = taskId;
        LOGGER.warn("setTaskId called: " + taskId);
    }
    public String getTaskId() {
        if (taskImpl == null) {
            return null;
        }
        LOGGER.warn("getTaskId called: " + taskImpl.getId());
        return taskImpl.getId();
    }

    
    public void setTaskName(String taskName) {
        // this.taskName = taskName;
        LOGGER.warn("setTaskName called: " + taskName);
    }
    public String getTaskName() {
        if (taskImpl == null) {
            return null;
        }
        LOGGER.warn("getTaskName called: " + taskImpl.getName());
        return taskImpl.getName();
    }
    
    
    public void setDueDate(Date duedate) {
        taskImpl.setDuedate(duedate);
        LOGGER.warn("setDueDate called: " + duedate);
    }
    public Date getDueDate() {
        if (taskImpl == null) {
            return null;
        }
        LOGGER.warn("getTaskName called: " + taskImpl.getName());
        return taskImpl.getDuedate();
    }


    public void setActorId(String actorId) {
        // this.taskName = taskName;
        LOGGER.warn("setActorId called: " + actorId);
    }
    public String getActorId() {
        if (taskImpl == null) {
            return null;
        }
        LOGGER.warn("getTaskName called: " + taskImpl.getName());
        return taskImpl.getAssignee();
    }
    
    
    @SuppressWarnings("unchecked")
    @Transactional
    public void save() {
        LOGGER.warn("persisting {} duedate is {}", taskImpl, taskImpl.getDuedate());
        Session session = (Session) Component.getInstance("hibernateSession");
        ExecutionImpl execution = taskImpl.getExecution();

        
        // update the task
        session.update(taskImpl);
        
        // also update the history task:
        HistoryTaskImpl historyTaskImpl = (HistoryTaskImpl) session.get(HistoryTaskImpl.class, taskImpl.getDbid());
        historyTaskImpl.setDuedate(taskImpl.getDuedate());
        session.update(historyTaskImpl);
               
        List<TimerImpl> timers = 
        session.createCriteria(TimerImpl.class)
        .add(Restrictions.eq("execution", execution))
        .add(Restrictions.eq("eventName", CustomTaskActivity.TASK_DUE_EVENT))
//      the reminder is unchanged:
//        .add(Restrictions.disjunction()
//                .add(Restrictions.eq("eventName", CustomTaskActivity.TASK_REMIND_EVENT))
//                .add(Restrictions.eq("eventName", CustomTaskActivity.TASK_DUE_EVENT))                
//        )
        .list();
        
        for (TimerImpl timer:timers) {
            timer.setDueDate(taskImpl.getDuedate());
            session.update(timer);
        }
        
        LOGGER.info("found jobs: {}", timers);
                        
        session.flush();
        
        // we have to refresh the task table
        ProcessTaskTable processTaskTable = (ProcessTaskTable) Component.getInstance(ProcessTaskTable.PROCESS_TASK_TABLE);
        processTaskTable.refresh();       
    }

}
