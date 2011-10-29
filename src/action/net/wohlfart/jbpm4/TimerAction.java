package net.wohlfart.jbpm4;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;

import java.util.Date;

import net.wohlfart.framework.AbstractEntityHome;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.framework.EntityNotFoundException;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jbpm.pvm.internal.job.TimerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.CONVERSATION)
@Name("timerAction")
public class TimerAction extends AbstractEntityHome<TimerImpl> {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimerAction.class);

    private Date duedate;
    private String repeat;
    private int  retries;
    private String signalName;
    private String eventName;

    public static final String  JBPM_TIMER = "jbpmTimer";

    @Override
    protected String getNameInContext() {
        return JBPM_TIMER;
    }

    public String setTimerDbid(final String s) {
        LOGGER.debug("setting id called: >{}< old id is: >{}<", s, getId());
        try {
            final Long id = new Long(StringUtils.defaultIfEmpty(s, "0"));
            if (!id.equals(getId() == null ? 0L : getId())) {
                // FIXME: we have no factory for this, no why remove it ?
                // Contexts.getConversationContext().remove(JBPM_TIMER);
                clearInstance();
                if (id > 0) {
                    setId(id);
                }
                initInstance();
                refresh();
            }
            return "valid";
        } catch (final EntityNotFoundException e) {
            LOGGER.info("EntityNotFoundException");
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "entity not found");
            return "invalid";
        } catch (final NumberFormatException e) {
            LOGGER.info("NumberFormatException");
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.FATAL, "entity not found");
            return "invalid";
        }
    }


    @Transactional
    public void refresh() {
        LOGGER.debug("init instance called");
        // init the name attribute
        final TimerImpl timer = getInstance();
        duedate = timer.getDueDate();
        repeat = timer.getRepeat();
        retries = timer.getRetries();
        signalName = timer.getSignalName();
        eventName = timer.getEventName();

        LOGGER.debug("duedate: " + duedate);
        LOGGER.debug("repeat: " + repeat);
        LOGGER.debug("retries: " + retries);
        LOGGER.debug("signalName: " + signalName);
        LOGGER.debug("eventName: " + eventName);
    }

    @Override
    public String cancel() {
        return super.cancel();
    }

    @Override
    @Transactional
    public String update() {
        final Session session = (Session) Component.getInstance("hibernateSession");
        // get the latest version because it is very likely that the timer
        // has changed after retrieving it from the database
        final TimerImpl timer = getInstance();
        session.refresh(timer);
        timer.setDueDate(duedate);
        timer.setRepeat(repeat);
        timer.setRetries(retries);
        timer.setEventName(eventName);
        timer.setSignalName(signalName);
        super.update();

        session.flush();
        return "updated";
    }

    @Override
    @Transactional
    public String remove() {
        final Session session = (Session) Component.getInstance("hibernateSession");
        super.remove();

        session.flush();
        return "removed";
    }

    @BypassInterceptors
    public Date getDuedate() {
        return duedate;
    }
    @BypassInterceptors
    public void setDuedate(final Date duedate) {
        this.duedate = duedate;
    }

    @BypassInterceptors
    public String getRepeat() {
        return repeat;
    }
    @BypassInterceptors
    public void setRepeat(final String repeat) {
        this.repeat = repeat;
    }

    @BypassInterceptors
    public int getRetries() {
        return retries;
    }
    @BypassInterceptors
    public void setRetries(final int retries) {
        this.retries = retries;
    }

    @BypassInterceptors
    public String getSignalName() {
        return signalName;
    }
    @BypassInterceptors
    public void setSignalName(final String signalName) {
        this.signalName = signalName;
    }

    @BypassInterceptors
    public String getEventName() {
        return eventName;
    }
    @BypassInterceptors
    public void setEventName(final String eventName) {
        this.eventName = eventName;
    }

}
