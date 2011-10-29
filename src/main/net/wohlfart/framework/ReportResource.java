package net.wohlfart.framework;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.query.JRHibernateQueryExecuterFactory;
import net.wohlfart.report.entities.CharmsReport;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// see:
// http://seamframework.org/Community/DirectPDFServing
// http://relation.to/Bloggers/FileDownloadSupport

/**
 * frontend to serving a resource to the user, this is backed up by the
 * AttachmentFile class, the only propose of this class is to lookup the
 * AttachmentFile in the Folder array and delete tempfiles created by the
 * AttachmentFile's getData() method
 * 
 * 
 * TODO: extend this component to server all kind of documents also by id
 */
@Name("reportResource")
@Scope(ScopeType.EVENT)
// seems like logger doesn't work in event context
public class ReportResource {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReportResource.class);

    @In(value = "hibernateSession")
    private Session hibernateSession;

    // @RequestParameter
    // private String reportName;

    @RequestParameter
    private Long reportId;  // alternative to rowKey which is relative to a folder

    // @RequestParameter
    // private String name; // not yet used

    // this method is called in the attachmentResource.page.xml definition
    // during the first rendering
    @Transactional
    public void findResource() {
        LOGGER.debug("findResource");
    }

    @Create
    public void createComponent() {
        LOGGER.debug("createComponent called");
    }

    @Destroy
    public void destroyComponent() {
        LOGGER.debug("destroyComponent called");
    }

    @BypassInterceptors
    public String getFileName() {
        LOGGER.debug("getFileName");
        return "report.pdf";
    }

    @BypassInterceptors
    public String getContentType() {
        LOGGER.debug("getContentType");
        return "application/pdf";
    }

    // @BypassInterceptors we need the entity Manager
    @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
    @Transactional
    public byte[] getData() {
        LOGGER.debug("get data called");

        try {

            // DataSource charmsDatasource = null;
            // Context initialContext = new InitialContext();
            // charmsDatasource = (DataSource)
            // initialContext.lookup("charmsDatasource");

            final CharmsReport report = (CharmsReport) hibernateSession.get(CharmsReport.class, reportId);

            // returns: delegate is class: class
            // org.jboss.seam.persistence.HibernateSessionProxy
            // LOGGER.debug("delegate is class: " +
            // hibernateSession.getDelegate().getClass().toString());

            // Connection conn = charmsDatasource.getConnection();
            // // returns: is class: class
            // org.jboss.resource.adapter.jdbc.jdk6.WrappedConnectionJDK6
            // log.debug("is class: " + conn.getClass().toString());

            // log.debug("is closed: " + conn.isClosed());
            // log.debug("is valid: " + conn.isValid(2000));

            // JasperPrint jasperPrint = JasperManager.fillReport(jasperReport,
            // new HashMap(), conn);
            /*
             * JasperReport report = JasperCompileManager.compileReport(
             * this.getClass().getResourceAsStream("simpleReport.jrxml"));
             */
            LOGGER.debug("compiled report");

            // HibernateSessionProxy sessionProxy =
            // (HibernateSessionProxy)entityManager.getDelegate();
            //
            // Session session = sessionProxy.getSession(EntityMode.MAP);
            // Connection connection = sessionProxy.connection();
            //
            //
            // LOGGER.warn("session is: {}", session);
            // LOGGER.warn("connection is: {}", connection);

            final Map parameters = new HashMap();
            parameters.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, hibernateSession);
            parameters.put(JRParameter.REPORT_CONNECTION, hibernateSession.connection());

            final byte[] bytes = JasperRunManager.runReportToPdf(report.getContentStream(), // can't
                                                                                            // reuse
                                                                                            // the
                                                                                            // stream
                    parameters);

            // needed so we don't reuse the content stream
            hibernateSession.clear();

            /*
             * 
             * Map parameters = new HashMap();
             * parameters.put(JRHibernateQueryExecuterFactory
             * .PARAMETER_HIBERNATE_SESSION, session);
             * parameters.put("ReportTitle", "Sample Report");
             */
            // byte[] bytes = JasperRunManager.runReportToPdf(
            // //report,
            // report.getContentStream(),
            // new HashMap(),
            // charmsDatasource.getConnection());

            /*
             * byte[] bytes = JasperRunManager.runReportToPdf(
             * this.getClass().getResourceAsStream("simpleReport.jasper"), new
             * HashMap(), charmsDatasource.getConnection());
             */
            return bytes;

            // } catch (NamingException e1) {
            // // TODO Auto-generated catch block
            // e1.printStackTrace();
        } catch (final JRException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
