package net.wohlfart.jbpm4.node;

import org.jbpm.api.task.Participation;

import net.wohlfart.authentication.entities.CharmsUser;


public abstract class AbstractSelectConfig implements ISelectConfig {

    /**
     * Participation.CANDIDATE:
     *   the user or member of a group is in a list of pooled actors for the task
     * 
     * Participation.OWNER:
     *   the user or member of a group gets his/her own task, will be set up as
     *   assignee of the task
     * 
     * 
     */
    @Override
    public String getParticipationRole() {
        return Participation.CANDIDATE;
    }

}
