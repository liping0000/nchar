package net.wohlfart.workflow;

import java.io.Serializable;
import java.util.Date;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.framework.queries.ProcessTaskTable;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("editTaskDates")
// @AutoCreate
public class EditTaskDates extends AbstractActionBean implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(EditTaskDates.class);

    private Long                dbid;

    @In(value = "hibernateSession")
    private Session             hibernateSession;

    @In
    private ProcessTaskTable    processTaskTable;

    private Date                duedate;

    // this method gets called on each ajax request
    @Transactional
    public void setTaskDbid(final Long dbid) {
        LOGGER.debug("setting dbid: " + dbid + " in EditTaskDates ");
        this.dbid = dbid;
        final TaskImpl taskImpl = (TaskImpl) hibernateSession.get(TaskImpl.class, dbid);
        duedate = taskImpl.getDuedate();
    }

    // this method is called for outjection and on each ajax request
    public Long getTaskDbid() {
        LOGGER.debug("getting dbid: " + dbid + " in EditTaskDates ");
        return dbid;
    }

    public Date getDuedate() {
        return duedate;
    }

    public void setDuedate(final Date duedate) {
        this.duedate = duedate;
    }

    @Transactional
    public void persist() {

        // try to find the current task
        final TaskImpl taskImpl = (TaskImpl) hibernateSession.get(TaskImpl.class, dbid);
        if (taskImpl != null) {
            taskImpl.setDuedate(duedate);
            // HistoryTaskImpl histTaskImpl =
            // (HistoryTaskImpl)hibernateSession.get(HistoryTaskImpl.class,
            // dbid);
            // histTaskImpl.setDuedate(duedate);
            hibernateSession.flush();
        }

        processTaskTable.refresh();
    }

    /**
     * the faces trace API uses the toString Method to display some information
     * about the components in the UI we need to make sure Seam's Bijection
     * doesn't kick in and gives us an exception
     */
    @Override
    @BypassInterceptors
    public String toString() {
        return super.toString();
    }

}
