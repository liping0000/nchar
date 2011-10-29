package net.wohlfart.email.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;


/**
 * email receiver expression n:1 relationship to the mail template
 * 
 * @author Michael Wohlfart
 * 
 */

/*  @formatter:off */
@NamedQueries({
    
    @NamedQuery(
            name = CharmsEmailTemplateReceiver.FIND_BY_TEMPLATE, 
            query = "from CharmsEmailTemplateReceiver where template = :template"),
            
    @NamedQuery(
            name = CharmsEmailTemplateReceiver.FIND_BY_TEMPLATE_ID, 
            query = "select t.receiver from CharmsEmailTemplate t " + "   where t.id = :id"),
            
    @NamedQuery(
            name = CharmsEmailTemplateReceiver.FIND_EXPRESSION_BY_TEMPLATE_ID, 
            query = "select r.addressExpression " 
                + " from CharmsEmailTemplate t "
                + "      join t.receiver r " 
                + "   where t.id = :id"),
                
    @NamedQuery(
            name = CharmsEmailTemplateReceiver.DELETE_FOR_TEMPLATE, 
            query = "delete from CharmsEmailTemplateReceiver rec "
                + "   where rec.template = :template and rec not in ( :recieverList )") 
                
})
/*  @formatter:on  */

   
@Entity 
@Table(name = "CHARMS_ETMPL_RCVR")   
public class CharmsEmailTemplateReceiver implements Serializable {

    public static final String FIND_BY_TEMPLATE               = "CharmsEmailTemplateReceiver.FIND_BY_TEMPLATE";
    public static final String FIND_BY_TEMPLATE_ID            = "CharmsEmailTemplateReceiver.FIND_BY_TEMPLATE_ID";
    public static final String FIND_EXPRESSION_BY_TEMPLATE_ID = "CharmsEmailTemplateReceiver.FIND_EXPRESSION_BY_TEMPLATE_ID";
    public static final String DELETE_FOR_TEMPLATE            = "CharmsEmailTemplateReceiver.DELETE_FOR_TEMPLATE";
    
    public static final int MAX_ADDRESS_EXPR_LENGTH      = 500;

    private Long id;
    private Integer version;

    private CharmsEmailTemplate template;

    private String addressExpression;

    /**
     * @return generated unique id for this table
     * @formatter:off
     */
    @Id
    @GenericGenerator(
            name = "sequenceGenerator", 
            strategy = "org.hibernate.id.enhanced.TableGenerator", 
            parameters = { 
                    @Parameter(
                            name = "segment_value", 
                            value = "CHARMS_ETMPL_RCVR") })
    @GeneratedValue(generator = "sequenceGenerator")
    @Column(name = "ID_")
    @AccessType("field")
    public Long getId() {
        return id;
    }
    public void setId(final Long id) {
        this.id = id;
    }

    @Version
    @Column(name = "VERSION_")
    @AccessType("field")
    public Integer getVersion() {
        return version;
    }
    // not intended to be used
    @SuppressWarnings("unused")
    private void setVersion(final Integer version) {
        this.version = version;
    }

    @Column(name = "ADDRESS_EXPR_", 
            length = MAX_ADDRESS_EXPR_LENGTH)
    public String getAddressExpression() {
        return addressExpression;
    }
    public void setAddressExpression(final String addressExpression) {
        this.addressExpression = addressExpression;
    }

    @ManyToOne
    @JoinColumn(name = "TEMPLATE_ID_", 
                nullable = false)
    public CharmsEmailTemplate getTemplate() {
        return template;
    }
    public void setTemplate(final CharmsEmailTemplate template) {
        this.template = template;
    }

}
