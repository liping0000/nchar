package net.wohlfart.jbpm4.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.jbpm4.activity.CreateBusinessKeyActivity;
import net.wohlfart.jbpm4.node.ISelectConfig;
import net.wohlfart.jbpm4.node.TransitionConfig;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.jbpm.api.task.Participation;
import org.jbpm.pvm.internal.task.ParticipationImpl;
import org.jbpm.pvm.internal.task.SwimlaneImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "CHARMS_TRNS_DATA")
public class TransitionData implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(CreateBusinessKeyActivity.class);

    public static final String TRANSITION_DATA  = "transitionData";

    private Long               id;
    private Integer            version;

    private SwimlaneImpl       swimlane;
    private String             receiverLabel;

    // private Long receiverUserId;
    // private String receiverUserActorId;
    private CharmsUser         receiverUser;

    // private Long receiverGroupId;
    // private String receiverGroupActorId;
    private CharmsRole         receiverGroup;

    private Date               dueDate;
    private Date               remindDate;
    private String             remindInterval;
    private String             message;
    // unique within a choice
    private String             transitionName;
    // parent object
    private TransitionChoice   transitionChoice;

    // this is transient and not stored in the database!
    // however it is (re)initialized as soon as a task is rendered in the UI
    private TransitionConfig   transitionConfig;

    // in the created table for this class there is the CHOICE_ID_
    // and the NAME_ column in order to assign this transition data to a
    // transition choice...

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_TRNS_DATA") })
    @GeneratedValue(generator = "sequenceGenerator")
    @AccessType("field")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }
    public void setId(final Long id) {
        this.id = id;
    }

    @Version
    @AccessType("field")
    @Column(name = "VERSION_")
    public Integer getVersion() {
        return version;
    }

    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @OneToOne
    @AccessType("field")
    @JoinColumn(name = "SWIMLANE_ID_")
    public SwimlaneImpl getSwimlane() {
        return swimlane;
    }

    public void setSwimlane(final SwimlaneImpl swimlane) {
        this.swimlane = swimlane;
    }

    @AccessType("field")
    @Column(name = "RECEIVER_LABEL_")
    public String getReceiverLabel() {
        return receiverLabel;
    }

    public void setReceiverLabel(final String swimlaneLabel) {
        receiverLabel = swimlaneLabel;
    }

    @ManyToOne(targetEntity = CharmsRole.class)
    @AccessType("field")
    @JoinColumn(name = "RCVR_GRP_ID_")
    public CharmsRole getReceiverGroup() {
        return receiverGroup;
    }

    public void setReceiverGroup(final CharmsRole receiverGroup) {
        this.receiverGroup = receiverGroup;
    }

    @ManyToOne(targetEntity = CharmsUser.class)
    @AccessType("field")
    @JoinColumn(name = "RCVR_USR_ID_")
    public CharmsUser getReceiverUser() {
        return receiverUser;
    }

    public void setReceiverUser(final CharmsUser receiverUser) {
        this.receiverUser = receiverUser;
    }

    @AccessType("field")
    @Column(name = "DUE_DATE_")
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(final Date dueDate) {
        this.dueDate = dueDate;
    }

    @AccessType("field")
    @Column(name = "REMIND_DATE_")
    public Date getRemindDate() {
        return remindDate;
    }

    public void setRemindDate(final Date remindDate) {
        this.remindDate = remindDate;
    }

    @AccessType("field")
    @Column(name = "REMIND_INTERVAL_")
    public String getRemindInterval() {
        return remindInterval;
    }

    public void setRemindInterval(final String remindInterval) {
        this.remindInterval = remindInterval;
    }

    @AccessType("field")
    @Column(name = "MESSAGE_", length = 2024)
    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    @ManyToOne
    @AccessType("field")
    @JoinColumn(name = "CHOICE_ID_")
    public TransitionChoice getTransitionChoice() {
        return transitionChoice;
    }

    public void setTransitionChoice(final TransitionChoice transitionChoice) {
        this.transitionChoice = transitionChoice;
    }

    @AccessType("field")
    @Column(name = "TRANSITION_NAME_")
    public String getTransitionName() {
        return transitionName;
    }

    public void setTransitionName(final String transitionName) {
        this.transitionName = transitionName;
    }

    // ---------------- transietn stuff follows ----

    @Transient
    public Long getReceiverUserId() {
        // return receiverUserId;
        if (receiverUser == null) {
            return null;
        } else {
            return receiverUser.getId();
        }
    }

    public void setReceiverUserId(final Long receiverUserId) {
        throw new IllegalArgumentException("use setReceiverUser()");
        // this.receiverUserId = receiverUserId;
    }

    @Transient
    public String getReceiverUserActorId() {
        // return receiverUserActorId;
        if (receiverUser == null) {
            return null;
        } else {
            return receiverUser.getActorId();
        }
    }

    public void setReceiverUserActorId(final String receiverUserActorId) {
        throw new IllegalArgumentException("use setReceiverUser()");
        // this.receiverUserActorId = receiverUserActorId;
    }

    @Transient
    public Long getReceiverGroupId() {
        // return receiverGroupId;
        if (receiverGroup == null) {
            return null;
        } else {
            return receiverGroup.getId();
        }
    }

    public void setReceiverGroupId(final Long receiverGroupId) {
        throw new IllegalArgumentException("use setReceiverGroup()");
        // this.receiverGroupId = receiverGroupId;
    }

    @Transient
    public String getReceiverGroupActorId() {
        // return receiverGroupActorId;
        if (receiverGroup == null) {
            return null;
        } else {
            return receiverGroup.getActorId();
        }
    }

    public void setReceiverGroupActorId(final String receiverGroupActorId) {
        throw new IllegalArgumentException("use setReceiverGroup()");
        // this.receiverGroupActorId = receiverGroupActorId;
    }

    /**
     * create a set of participatians for a spawning transition
     *  note we use the actorId here
     *  
     *  Participation.OWNER:
     *   group: each member of the group will get its own spawned task
     *   user: user will be assigned no matter what
     *   
     *  Participation.CANDIDATE:  
     *   user/group will be copied over
     *  
     *  
     *  FIXME: this is used in the spawn signal...
     */
    @Transient
    public Set<ParticipationImpl> getNextParticipations() {
        LOGGER.debug("calculating next participations");

        final Set<ParticipationImpl> participations = new HashSet<ParticipationImpl>();

        final String uid = getReceiverUserActorId();   
        final String gid = getReceiverGroupActorId();


        if (!StringUtils.isEmpty(gid)) {           
            String role = Participation.CANDIDATE; // default
            if (transitionConfig != null) {
                ISelectConfig groupSelectConfig = transitionConfig.getGroupSelectConfig();
                if (groupSelectConfig != null) {
                    role = groupSelectConfig.getParticipationRole();
                }
            }
            participations.add(new ParticipationImpl(null, gid, role));
        }

        if (!StringUtils.isEmpty(uid)) {          
            String role = Participation.CANDIDATE; // default
            if (transitionConfig != null) {
                ISelectConfig userSelectConfig = transitionConfig.getUserSelectConfig();
                if (userSelectConfig != null) {
                    role = userSelectConfig.getParticipationRole();
                }
            }
            participations.add(new ParticipationImpl(uid, null, role));
        }

        return participations;
    }

    /**
     * create a set of participatians for a spawning transition note we use the
     * actorId here
     * 
     * FIXME: this is used in the "normal" signal
     */
    @Transient
    public SwimlaneImpl getNextSwimlane() {
        LOGGER.debug("calculating next swimlane");

        final String receiverUserActorId = getReceiverUserActorId();
        final String receiverGroupActorId = getReceiverGroupActorId();

        if (StringUtils.isEmpty(receiverUserActorId) && StringUtils.isEmpty(receiverGroupActorId)) {
            return null;
        }

        final SwimlaneImpl swimlane = new SwimlaneImpl();
        swimlane.setAssignee(receiverUserActorId);
        if (!StringUtils.isEmpty(receiverGroupActorId)) {
            swimlane.addCandidateGroup(receiverGroupActorId);
        }

        return swimlane;
    }


    /**
     * 
     * 
     * @return
     */
    @Transient
    public TransitionConfig getConfig() {
        return transitionConfig;
    }
    public void setConfig(final TransitionConfig transitionConfig) {
        this.transitionConfig = transitionConfig;
    }

}
