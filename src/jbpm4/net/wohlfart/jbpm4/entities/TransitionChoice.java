package net.wohlfart.jbpm4.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

// see:
// http://www.velocityreviews.com/forums/t364715-hibernate-mapping-directly-to-a-hashmap-or-subclass.html
// http://i-proving.ca/space/Technologies/Hibernate/Hibernate+Annotation+Examples/Collection+of+Elements
// http://docs.jboss.org/hibernate/stable/annotations/reference/en/html/entity.html

@NamedQueries({ @NamedQuery(name = TransitionChoice.FIND_BY_TID, query = "from TransitionChoice where tid = :tid") })
@Entity
@Table(name = "CHARMS_TRNS_CHOICE")
public class TransitionChoice implements Serializable {

    public static final String FIND_BY_TID = "TransitionChoice.FIND_BY_TID";

    public static final String TRANSITION_CHOICE = "transitionChoice";  

    private Long id;
    private Integer version;

    // the task.getDbid() / id of the task
    private Long tid;
    private Map<String, TransitionData> transitions       = new HashMap<String, TransitionData>();
    private String selected;  // the selected facet

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_TRNS_CHOICE") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Version
    @Column(name = "VERSION_")
    public Integer getVersion() {
        return version;
    }

    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @OneToMany(mappedBy = "transitionChoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKeyColumn(name = "TRANSITION_NAME_")
    public Map<String, TransitionData> getTransitions() {
        return transitions;
    }

    public void setTransitions(final Map<String, TransitionData> transitions) {
        this.transitions = transitions;
    }

    @Transient
    public void addTransition(final String transitionName) {
        final TransitionData data = new TransitionData();
        data.setTransitionChoice(this);
        transitions.put(transitionName, data);
    }

    @Column(name = "TASK_DBID_")
    public Long getTid() {
        return tid;
    }

    public void setTid(final Long tid) {
        this.tid = tid;
    }

    @Column(name = "SELECTED_")
    public String getSelected() {
        return selected;
    }

    public void setSelected(final String selected) {
        this.selected = selected;
    }

    @Transient
    public TransitionData getData(final String key) {
        TransitionData data = transitions.get(key);
        if (data == null) {
            data = new TransitionData();
            transitions.put(key, data);
        }
        return data;
    }

    @Transient
    public boolean hasData(final String facet) {
        // at this point we get an 
        // org.hibernate.ObjectNotFoundException: No row with the given identifier exists: [net.wohlfart.authentication.entities.CharmsUser#10]
        // if a user with assigned task was accidently deleted
        return transitions.containsKey(facet);
    }

}
