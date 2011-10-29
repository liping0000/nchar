package net.wohlfart.framework.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "CHARMS_FOLDER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "FOLDER_TYPE_", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("F")
public class CharmsFolder implements Serializable {


    private Long                 id;
    private Integer              version;
    private String               name;

    private String               comment;

    private List<CharmsFolder>   children  = new ArrayList<CharmsFolder>();
    private CharmsFolder         parent;
    private CharmsFolder         root;

    private List<CharmsDocument> documents = new ArrayList<CharmsDocument>();

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_FOLDER") })
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

    @Column(name = "NAME_")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Column(name = "COMMENT_")
    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    @ManyToOne
    @JoinColumn(name = "ROOT_ID_")
    public CharmsFolder getRoot() {
        return root;
    }

    public void setRoot(final CharmsFolder root) {
        this.root = root;
    }

    @ManyToOne
    @JoinColumn(name = "PARENT_ID_")
    public CharmsFolder getParent() {
        return parent;
    }

    public void setParent(final CharmsFolder parent) {
        this.parent = parent;
    }

    @OneToMany(mappedBy = "parent")
    public List<CharmsFolder> getChildren() {
        return children;
    }

    public void setChildren(final List<CharmsFolder> children) {
        this.children = children;
    }

    // make sure that we manually persist the documents when they are ready!
    @OneToMany(mappedBy = "folder")
    public List<CharmsDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(final List<CharmsDocument> documents) {
        this.documents = documents;
    }

    // ------- convenience methods

    @Transient
    public Integer getFileCount() {
        return getDocuments().size();
    }
    // @Override
    // public void finalize() {
    // // FIXME: make sure we delete all files on exit
    // }

}
