package net.wohlfart.report.entities;

import java.sql.Blob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "CHARMS_RPT_BLB")
public class CharmsReportBlob {

    private Long           id;
    private Integer        version;
    private transient Blob content;

    /**
     * @return generated unique id for this table
     */
    @Id
    @GenericGenerator(name = "sequenceGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = { @Parameter(name = "segment_value", value = "CHARMS_RPT_BLB") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    @Version
    @Column(name = "VERSION_")
    private Integer getVersion() {
        return version;
    }

    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    /**
     * the raw data must be not null, max size must be set for derby, since
     * fallback is 255 this is the compiled report
     */
    // updateable need to be false since we flush multiple times here and don't
    // want
    // to upload the binary data more than once
    @Lob
    // updatable to false because derby gives us an exception:
    // org.apache.derby.client.am.SqlException: Column 'CONTENT_' cannot accept
    // a NULL value.
    @Column(name = "CONTENT_", nullable = false, length = Integer.MAX_VALUE - 1)
    // @Basic(fetch=FetchType.LAZY) // not sure if this does anything
    public Blob getContent() {
        return content; // FIXME: rewrite the update in the action bean
    }

    public void setContent(final Blob content) {
        this.content = content;
    }

    /*
     * @Transient public InputStream getContentStream() throws SQLException { //
     * method scoped variable: Blob content = getContent(); if (content == null)
     * { throw new IllegalArgumentException("getContent returns null"); } return
     * content.getBinaryStream(); } public void setContentStream(final
     * InputStream sourceStream) throws IOException {
     * setContent(Hibernate.createBlob(sourceStream)); }
     */

}
