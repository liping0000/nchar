package net.wohlfart.email.freemarker;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.framework.entities.CharmsWorkflowData;
import net.wohlfart.todolist.entities.ToDoListData;

import org.hibernate.Session;
import org.jbpm.pvm.internal.history.model.HistoryTaskInstanceImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a simple bean that holds the data for the freemarker template processing,
 * this bean is recreated for each template run, whereas the templatre
 * configuration is a singleton and stays for the whole application runtime
 * 
 * 
 */
public class MailDataModel {

    private final static Logger LOGGER             = LoggerFactory.getLogger(MailDataModel.class);

    private CharmsUser          sender;
    private CharmsUser          receiver;
    private String              servletUrl;

    // for retrieving more data:
    private final Session       session;
    // private EntityManager entityManager;
    private final ExecutionImpl execution;

    // we cache this values for performance reasons:
    private CharmsWorkflowData  charmsWorkflowData = null;
    private ChangeRequestData   changeRequestData  = null;
    private ToDoListData        toDoListData       = null;

    public MailDataModel(final ExecutionImpl execution, final Session session) {

        if (execution == null) {
            LOGGER.warn("execution is null in MailDataModel, some properties won't be accessible");
        }

        this.execution = execution;
        this.session = session;
        // this.entityManager = entityManager;
    }

    public TaskImpl getTaskInstance() {
        if (execution == null) {
            return null;
        } else {
            return execution.getTask();
        }
    }

    public String getBusinessKey() {
        // make sure we have a process instance here
        if (execution == null) {
            return null;
        } else {
            // business key may be null for the draft
            return execution.getKey();
        }
    }

    public CharmsUser getSender() {
        return sender;
    }

    public void setSender(final CharmsUser sender) {
        this.sender = sender;
    }

    public CharmsUser getReceiver() {
        return receiver;
    }

    public void setReceiver(final CharmsUser receiver) {
        this.receiver = receiver;
    }

    public String getServletUrl() {
        return servletUrl;
    }

    public void setServletUrl(final String servletUrl) {
        this.servletUrl = servletUrl;
    }

    public String getTaskLink() {
        if (execution == null) {
            return null;
        }

        final Long executionId = execution.getDbid();
        LOGGER.debug("executionId is: " + executionId);

        final Long activityDbid = execution.getHistoryActivityInstanceDbid();

        final Long taskDbid = (Long) session
                .createQuery("select a.historyTask.dbid from " + HistoryTaskInstanceImpl.class.getName() + " a " + " where a.dbid = :activityDbid")
                .setParameter("activityDbid", activityDbid).uniqueResult();

        // create a link like:
        // http://localhost:8080/charms/pages/user/taskList.html?taskDbid=2&actionOutcome=doTask
        // for use in a freemarker template use: ${taskLink}

        return servletUrl + "/pages/wfl/dispatcher.html?taskDbid=" + taskDbid;
    }

    public CharmsWorkflowData getCharmsWorkflowData() {
        if (execution == null) {
            return null;
        }

        if (charmsWorkflowData != null) {
            return charmsWorkflowData; // return the cached value
        }

        final Long executionId = execution.getDbid();
        LOGGER.debug("executionId is: " + executionId);

        final ExecutionImpl instance = execution.getProcessInstance();
        final Long procInstanceId = instance.getDbid();
        LOGGER.debug("procInstanceId is: " + procInstanceId);

        charmsWorkflowData = (CharmsWorkflowData) session
                .createQuery(" from " + CharmsWorkflowData.class.getName() + " d " + " where d.processInstanceId = :processInstanceId")
                .setParameter("processInstanceId", procInstanceId).uniqueResult();

        LOGGER.debug("returning CharmsWorkflowData: " + charmsWorkflowData);
        return charmsWorkflowData;
    }

    public ChangeRequestData getChangeRequestData() {
        if (execution == null) {
            return null;
        }

        if (changeRequestData != null) {
            return changeRequestData; // return the cached value
        }

        final Long executionId = execution.getDbid();
        LOGGER.debug("executionId is: " + executionId);

        final ExecutionImpl instance = execution.getProcessInstance();
        final Long procInstanceId = instance.getDbid();
        LOGGER.debug("procInstanceId is: " + procInstanceId);

        changeRequestData = (ChangeRequestData) session
                .createQuery(" from " + ChangeRequestData.class.getName() + " d " + " where d.processInstanceId = :processInstanceId")
                .setParameter("processInstanceId", procInstanceId).uniqueResult();

        LOGGER.debug("returning ChangeRequestData: " + changeRequestData);
        return changeRequestData;
    }

    public ToDoListData getToDoListData() {
        if (execution == null) {
            return null;
        }

        if (toDoListData != null) {
            return toDoListData; // return the cached value
        }

        final Long executionId = execution.getDbid();
        LOGGER.debug("executionId is: " + executionId);

        final ExecutionImpl instance = execution.getProcessInstance();
        final Long procInstanceId = instance.getDbid();
        LOGGER.debug("procInstanceId is: " + procInstanceId);

        // business key may be null for the draft
        // this doesn' work on initial submit since the pid in the databean is
        // set during
        // createProcess callback which is too late
        toDoListData = (ToDoListData) session.createQuery(" from " + ToDoListData.class.getName() + " d " + " where d.processInstanceId = :processInstanceId")
                .setParameter("processInstanceId", procInstanceId).uniqueResult();

        LOGGER.debug("returning databean: " + toDoListData);
        return toDoListData;

    }

    // ------------ obsolete

    // ActivityImpl activity = (ActivityImpl)((ExecutionImpl)
    // execution).getEvent().getObservableElement();
    // execution.getTask() returns null...
    // Long taskDbid = (Long)
    // session.createQuery("select t.dbid from " + TaskImpl.class.getName() +
    // " t "
    // + " where execution.dbid = :executionId"
    // + " and t.activityName = :activityName ")
    // .setParameter("executionId", executionId)
    // .setParameter("activityName", activity.getName())
    // .uniqueResult();

    // LOGGER.debug(" ** using session for mail data model: "
    // + session.hashCode() + " conent: " + session + " thread is " +
    // Thread.currentThread()
    // + " transaction: " + session.getTransaction().hashCode() + " content: " +
    // session.getTransaction()
    // + " is active: " + session.getTransaction().isActive());

    // LOGGER.debug(" *** using session for mail data model: "
    // + session.hashCode() + " conent: " + session + " thread is " +
    // Thread.currentThread()
    // + " transaction: " + session.getTransaction().hashCode() + " content: " +
    // session.getTransaction()
    // + " is active: " + session.getTransaction().isActive());

    // business key may be null for the draft
    // this doesn' work on initial submit since the pid in the databean is set
    // during
    // createProcess callback which is too late
    // ChangeRequestData data = (ChangeRequestData)
    // session.createQuery(
    // " from "
    // + ChangeRequestData.class.getName() + " d "
    // + " where d.processInstanceId = :processInstanceId")
    // .setParameter("processInstanceId", procInstanceId)
    // .uniqueResult();
    // LOGGER.debug("returning databean: " + data);
    //
    //

    // business key may be null for the draft
    // this doesn' work on initial submit since the pid in the databean is set
    // during
    // createProcess callback which is too late
    // CharmsWorkflowData data = (CharmsWorkflowData)
    // session.createQuery(
    // " from "
    // + CharmsWorkflowData.class.getName() + " d "
    // + " where d.processInstanceId = :processInstanceId")
    // .setParameter("processInstanceId", procInstanceId)
    // .uniqueResult();

}
