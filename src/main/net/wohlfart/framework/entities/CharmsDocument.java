package net.wohlfart.framework.entities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.framework.mime.MimeTypeIcons;
import net.wohlfart.framework.search.CharmsDocumentBridge;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
// @Indexed
@ClassBridge(name = "charmsDocument", index = Index.TOKENIZED, store = Store.YES, impl = CharmsDocumentBridge.class)
@Table(name = "CHARMS_DOCUMENT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DOCUMENT_TYPE_", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("D")
public class CharmsDocument implements Serializable {

    static final long                    serialVersionUID   = -1L;

    private final static Logger          LOGGER             = LoggerFactory.getLogger(CharmsDocument.class);

    public static final int              MAX_COMMENT_LENGTH = 2024;
    // this is used to abbreviate too long filenames, must be more than 7 for
    // the abbreviation logig to work...
    public static final int              MAX_NAME_LENGTH    = 250;

    private Long                         id;
    private Integer                      version;
    private String                       name;

    private String                       comment;

    // private CharmsFolder parent;
    private String                       editorName;
    private CharmsUser                   editor;
    private String                       mimeType;
    private CharmsFolder                 folder;

    // stored data here before we move it into the blob
    private transient File               file;
    // the real data out of the DB, unable to serialize
    private transient CharmsDocumentBlob documentBlob;
    private Long                         size;                                                              // size
                                                                                                             // in
                                                                                                             // bytes
    private Date                         lastModified;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_DOCUMENT") })
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

    @Column(name = "NAME_", length = MAX_NAME_LENGTH)
    // @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Column(name = "COMMENT_", length = MAX_COMMENT_LENGTH)
    // @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    // the size of the content data, doesn't change
    @Column(name = "SIZE_", updatable = false, nullable = false)
    // @Field(index = Index.TOKENIZED, store = Store.YES)
    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // effective a one-to-one because of the unique
    @JoinColumn(name = "DOC_BLOB_ID_", unique = true)
    private CharmsDocumentBlob getDocumentBlob() {
        return documentBlob;
    }

    private void setDocumentBlob(final CharmsDocumentBlob documentBlob) throws IOException {
        this.documentBlob = documentBlob;
    }

    @Transient
    public InputStream getContentStream() throws SQLException {
        final CharmsDocumentBlob charmsBlob = getDocumentBlob();
        return charmsBlob.getContent().getBinaryStream();
    }

    @SuppressWarnings("deprecation")
    public void setContentStream(final InputStream sourceStream, final long size, final Session session) throws IOException {
        CharmsDocumentBlob charmsBlob = getDocumentBlob();
        if (charmsBlob == null) {
            charmsBlob = new CharmsDocumentBlob();
        }

        final Blob blob = Hibernate.createBlob(sourceStream);
        charmsBlob.setContent(blob);
        setDocumentBlob(charmsBlob);

        // using Hibernate.createBlob(sourceStream, size, currentSession)
        // doesn't work with the new hibernate version
        // // session is a seam proxy and will be casted to ( LobCreationContext
        // )
        // if (!(session instanceof LobCreationContext)) {
        // LOGGER.warn("session is not an instance of LobCreationContext, trying to get the classic session which implements LobCreationContext");
        // } else {
        // org.hibernate.classic.Session currentSession =
        // session.getSessionFactory().getCurrentSession();
        // if (!(currentSession instanceof LobCreationContext)) {
        // LOGGER.warn("currentSession is not an instance of LobCreationContext, trying to continue anyways...");
        // }
        // charmsBlob.setContent(Hibernate.createBlob(sourceStream, size,
        // currentSession));
        // setDocumentBlob(charmsBlob);
        // }

    }

    @ManyToOne
    @JoinColumn(name = "EDITOR_ID_")
    public CharmsUser getEditor() {
        return editor;
    }

    public void setEditor(final CharmsUser editor) {
        this.editor = editor;
    }

    // the editors real name
    @Column(name = "EDITOR_NAME_")
    // @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getEditorName() {
        return editorName;
    }

    public void setEditorName(final String editorName) {
        this.editorName = editorName;
    }

    @Column(name = "MIME_TYPE_", updatable = false, nullable = false, length = 250)
    // @Field(index = Index.TOKENIZED, store = Store.YES)
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * * the raw data
     */
    @ManyToOne
    @JoinColumn(name = "FOLDER_ID_")
    public CharmsFolder getFolder() {
        return folder;
    }

    public void setFolder(final CharmsFolder folder) {
        this.folder = folder;
    }

    /**
     * the raw data must be not null, max size must be set for derby, since
     * fallback is 255
     * 
     * @Lob
     * @Column(name="CONTENT_", updatable=false, nullable=false,
     *                          length=Integer.MAX_VALUE - 1) private Blob
     *                          getContent() { return this.content; } private
     *                          void setContent(Blob content) { this.content =
     *                          content; }
     */

    @Column(name = "LAST_MODIFIED_", updatable = false, nullable = false)
    // @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    // ---- helpers

    @Transient
    public String getIconName() {
        return MimeTypeIcons.getIconNameForMimetype(getMimeType());
    }

    @Transient
    public File getFile() {
        return file;
    }

    public void setFile(final File file) {
        this.file = file;
    }

    /*
     * @Transient public InputStream getContentStream() throws SQLException {
     * Blob c = getContent(); if (c == null) { throw new
     * IllegalArgumentException("getContent returns null"); } return
     * c.getBinaryStream(); } public void setContentStream(final InputStream
     * sourceStream) throws IOException {
     * setContent(Hibernate.createBlob(sourceStream)); }
     */

    // FIXME: for the UI, shouldn't be here at all
    @Transient
    public String getSizeString() {

        String formatted = "unknown";
        if (size == null) {
            formatted = "empty";
        } else if (size > 1000000000) {
            formatted = Long.toString(size / 1000000000) + " GB";
        } else if (size > 1000000) {
            formatted = Long.toString(size / 1000000) + " MB";
        } else if (size > 1000) {
            formatted = Long.toString(size / 1000) + " KB";
        } else {
            formatted = Long.toString(size) + " bytes";
        }
        return formatted;
    }

    // cleanup the filesystem when this object is garbage collected
    @Override
    protected void finalize() {
        if (file.exists()) {
            final boolean deleted = file.delete();
            if (!deleted) {
                LOGGER.warn("error deleting file {}", deleted);
            }
        }
        setFile(null);
    }
}
