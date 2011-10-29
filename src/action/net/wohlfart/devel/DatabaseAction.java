package net.wohlfart.devel;

import org.jboss.seam.ScopeType;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import net.wohlfart.AbstractActionBean;
import net.wohlfart.refdata.entities.ChangeRequestProduct;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * see:
 * http://www.coderslog.com/How_to_export_schema_from_a_spring_configured_hibernate
 * see: https://forum.hibernate.org/viewtopic.php?t=943520
 */

@Scope(ScopeType.CONVERSATION)
@Name("databaseAction")
public class DatabaseAction extends AbstractActionBean implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(DatabaseAction.class);

    @In(value = "hibernateSession")
    private Session             hibernateSession;

    @SuppressWarnings("rawtypes")
    @Transactional
    public void dump() {

        final Session dom4jSession = hibernateSession.getSession(EntityMode.DOM4J);
        // org.hibernate.Transaction t = dom4jSession.beginTransaction();

        final Document doc = DocumentHelper.createDocument();
        final Element root = doc.addElement("root");
        // List stores = dom4jSession.createCriteria(CharmsUser.class).list();
        final List stores = dom4jSession.createCriteria(ChangeRequestProduct.class).list();

        // iterate through child elements of root
        for (final Iterator i = stores.iterator(); i.hasNext();) {
            // org.dom4j.Element element = (org.dom4j.Element) i.next();
            final Object object = i.next();
            LOGGER.info("save item: {}", object);
            root.add((Element) object);
            // dom4jSession.replicate(object, ReplicationMode.LATEST_VERSION);
        }

        // LOGGER.debug("committing...");
        // t.commit();
        // LOGGER.warn("committed");

        final OutputFormat format = OutputFormat.createPrettyPrint();
        try {
            final XMLWriter writer = new XMLWriter(System.out, format);
            writer.write(doc);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        // LOGGER.warn("document content: {}", doc.asXML());
        dom4jSession.close();
        LOGGER.warn("Session dom4jSession closed");

    }

    /*
     * 
     * see: http://www.coderslog.com/
     * How_to_export_schema_from_a_spring_configured_hibernate see:
     * https://forum.hibernate.org/viewtopic.php?t=943520
     * 
     * public static void main(String[] args) throws Exception {
     * export(true,"org.hibernate.dialect.HSQLDialect"); }
     * 
     * public static void export(boolean createOnly, String dialect) throws
     * Exception{ GenericApplicationContext context = new
     * GenericApplicationContext(); XmlBeanDefinitionReader xmlReader = new
     * XmlBeanDefinitionReader(context); xmlReader.loadBeanDefinitions(new
     * FileSystemResource("WEB-INF/applicationContext.xml")); context.refresh();
     * export(context,createOnly,dialect); } public static void
     * export(GenericApplicationContext context,boolean createOnly, String
     * dialect) throws Exception{ AnnotationConfiguration configuration=new
     * AnnotationConfiguration(); ArrayList<String>
     * annotatedClasses=(ArrayList)context.getBean("annotatedClasses");
     * configuration.setProperty("hibernate.dialect", dialect); for(String
     * annotatedClass:annotatedClasses)
     * configuration.addAnnotatedClass(Class.forName(annotatedClass));
     * SchemaExport export = new SchemaExport(configuration);
     * export.setOutputFile("sql.ddl"); export.setDelimiter(";");
     * export.execute(false, false,false,createOnly); }
     * 
     * 
     * 
     * 
     * @Transactional public void dumpOlder() { Transaction tx = null ; Session
     * session = null ;
     * 
     * try { Document document = DocumentHelper.createDocument(); Element root =
     * document.addElement ("root");
     * 
     * Configuration configuration = new AnnotationConfiguration() ;
     * configuration.configure() ; SessionFactory factory =
     * configuration.buildSessionFactory();
     * 
     * session = factory.openSession(); Session dom4jSession =
     * session.getSession(EntityMode.DOM4J); tx =
     * dom4jSession.beginTransaction();
     * 
     * List stores = session.createCriteria(Object.class).list(); for ( Iterator
     * i = stores.iterator(); i.hasNext(); ) { org.dom4j.Element element =
     * (org.dom4j.Element) i.next(); LOGGER.debug("exporting item");
     * //dom4jSession.replicate(element, ReplicationMode.LATEST_VERSION);
     * root.add (element) ; }
     * 
     * 
     * 
     * 
     * 
     * document.write (new FileWriter ("test.xml")) ; } catch (Exception e) {
     * e.printStackTrace () ; } finally { if (tx != null) tx.commit () ;
     * 
     * if (session != null) session.close () ; } }
     */

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
