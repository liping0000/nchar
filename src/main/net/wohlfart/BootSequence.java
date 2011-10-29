package net.wohlfart;

import static net.wohlfart.framework.RuntimeEnvDataProvider.FileSystemLocation.BOOT_DIRECTORY;
import static org.jboss.seam.ScopeType.STATELESS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;

import net.wohlfart.authentication.AccountSetup;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.PermissionTargetSetup;
import net.wohlfart.framework.RuntimeEnvDataProvider;
import net.wohlfart.framework.excel.WorkbookDeployer;
import net.wohlfart.framework.logging.CharmsLogEntry;
import net.wohlfart.framework.logging.CharmsLogLevel;
import net.wohlfart.framework.logging.CharmsLogger;
import net.wohlfart.framework.mime.MimeTypeIcons;
import net.wohlfart.framework.mime.MimeTypeUtil;
import net.wohlfart.framework.properties.CharmsPropertySet;
import net.wohlfart.framework.properties.PropertiesManager;
import net.wohlfart.jbpm4.Jbpm4Utils;
import net.wohlfart.jbpm4.JbpmSetup;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is called on startup, it scans the boot directory in the
 * application folder for usable files and calls the modules to provide them
 * with the boot data... 
 * 
 * we also check if the database needs some bootstrapping data
 * 
 * FIXME: leave the database empty upon first request, then query the user if
 * the database should be filled with demo data, we can also set application
 * data like port and hostname on an empty database on the first user request
 * instead of hardcoding this in web.xml
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(STATELESS)
@Name("bootSequence")
public class BootSequence {

    private final static Logger LOGGER = LoggerFactory.getLogger(BootSequence.class);

    @In(create = true)
    private JbpmSetup jbpmSetup;

    @In(create = true)
    private PermissionTargetSetup permissionTargetSetup;

    @In(create = true)
    private AccountSetup accountSetup;

    // @In(create=true)
    // private RefDataSetup refDataSetup;

    @In(create = true)
    private PropertiesManager propertiesManager;

    @In(value = "hibernateSession")
    private Session hibernateSession;

    /**
     * this method is called by seam on application startup note that there is
     * no current instance of a FacesContext yet, so the following won't work:
     * ExternalContext externalContext =
     * FacesContext.getCurrentInstance().getExternalContext();
     */
    @Observer("org.jboss.seam.postInitialization")
    @Transactional
    public void boot() {
        LOGGER.debug("running boot method for org.jboss.seam.postInitialization observer");
        runChecks();

        // report to the internal logging system
        hibernateSession.persist(new CharmsLogEntry(CharmsLogger.SYSTEM, CharmsLogLevel.CONFIG, "starting boot sequence in postInitialization Observer"));

        // get the "first boot property" to see if we might have an empty database
        final CharmsPropertySet appProperties = propertiesManager.getApplicationProperties();

        final Date firstBoot = appProperties.getPropertyAsDate(PropertiesManager.FIRST_BOOT_PROPERTY_NAME, null);
        if (firstBoot == null) {
            LOGGER.info("this seems to be the first boot for this database, checking the sequences...");
            final Long sequenceCount = (Long) hibernateSession
            .getNamedQuery(CharmsUser.COUNT_QUERY)
            .uniqueResult();
            LOGGER.info("found {} user(s)", sequenceCount);
            if (sequenceCount > 0) {
                LOGGER.info("we are not performing database bootstrapping since there is at least one user, " 
                        + "setting parameter '"
                        + PropertiesManager.FIRST_BOOT_PROPERTY_NAME + "'");
                propertiesManager.persistProperty(appProperties, PropertiesManager.FIRST_BOOT_PROPERTY_NAME, new Date());
            } else {
                LOGGER.info("bootstrapping database content with data from the boot directory...");
                bootstrapDatabase();
            }
        } else {
            LOGGER.info("first boot was {}, {} days ago", firstBoot, (new Date().getTime() - firstBoot.getTime()) / (1000 * 60 * 60 * 24));
        }

        final Date lastBoot = appProperties.getPropertyAsDate(PropertiesManager.LAST_BOOT_PROPERTY_NAME, null);
        if (lastBoot != null) {
            LOGGER.info("last boot was {}, {} days ago", lastBoot, (new Date().getTime() - lastBoot.getTime()) / (1000 * 60 * 60 * 24));
        }
        propertiesManager.persistProperty(appProperties, PropertiesManager.LAST_BOOT_PROPERTY_NAME, new Date());

        // setting up the mime icons
        // FIXME: use the tika framework instead of MimUtils here...
        MimeTypeIcons.setupProperties();

        hibernateSession.persist(new CharmsLogEntry(CharmsLogger.SYSTEM, CharmsLogLevel.CONFIG, "finished boot sequence in postInitialization Observer"));
        hibernateSession.flush();
        LOGGER.debug("finished boot method");
    }


    private void bootstrapDatabase() {
        LOGGER.debug("started bootstrapping database content");

        // this only works in Tomcat, the real path is not available in
        // a clustered environment
        final File bootDirectory = BOOT_DIRECTORY.getLocation();
        if ((bootDirectory == null) || (!bootDirectory.isDirectory()) || (!bootDirectory.canRead())) {
            LOGGER.info("bootDir not found or is not a directory or is not readable: {}", bootDirectory);
        } else {
            LOGGER.info("found boot directory: {}, enumerating files...", bootDirectory);
            final File[] files = bootDirectory.listFiles();
            if (files.length == 0) {
                LOGGER.info("no files in boot directory");
            } else {
                for (final File file : files) {
                    LOGGER.info("found file in boot directory: {}", file);
                }
                LOGGER.info("installing boot files...");
                for (final File file : files) {
                    if (file.getName().endsWith("_offline")) {
                        LOGGER.info("skipping: {}", file);
                    } else {
                        LOGGER.info("installing: {}", file);
                        // here we install a file from the boot directory

                        final boolean success = installFile(file, bootDirectory);
                        if (success) {
                            LOGGER.info("file installed: {}", file);
                        } else {
                            LOGGER.info("file not installed: {}", file);
                        }
                    }

                }
            }
        }

        // order is important here since the permissions from the process
        // instance
        // need to be set up before the admin user can get all the permissions
        // jbpmSetup.startup();

        // adding some more application dependent permissions if they don't
        // already exists before continuing
        permissionTargetSetup.startup();

        // we need the permissionTargets from the jbpm before running this
        // setup an admin user and assign all permissions to the admin
        accountSetup.startup();

        // menuSetup.startup();
        // messageBundleSetup.startup();
        // we got the import script for that...
        // refDataSetup.startup();

        // propertiesSetup.startup();
        // droolsSetup.startup();

        LOGGER.debug("finished bootstrapping database content");
    }


    /**
     * this method is called for each file in the boot directory...
     * 
     * @param candidate
     * @param directory
     * @return
     */
    private boolean installFile(final File candidate, final File directory) {
        boolean success = false;

        if ((directory == null) || (!directory.isDirectory()) || (!directory.canRead())) {
            LOGGER.warn("bootDir not found or is not a directory or is not readable: {} can't install file {}", 
                    directory != null ? directory.toURI() : "null", candidate.toURI());
            return success;
        }
        if ((candidate == null) || (!candidate.canRead())) {
            LOGGER.warn("candidate file can't be read or is null: {}, can't install", 
                    candidate != null ? candidate.toURI() : "null");
            return success;
        }
        if (candidate.isDirectory()) {
            LOGGER.info("candidate file for install is directory, won't install '{}'", 
                    candidate.toURI());
            return success;
        }

        // try to analyze the file type and call the appropriate installer
        final String mimeType = MimeTypeUtil.findMimeType(candidate);
        LOGGER.info("found mime type {} for file {} ", mimeType, candidate);

        if (mimeType.equals(MimeTypeUtil.MIMETYPE_APPLICATION_ZIP)) {
            LOGGER.info("found a zip file, trying to deploy it as a process definition");

            // a zip file might be a process definition, so let's try to deploy
            // it
            ZipFile zipFile = null;
            try {
                // get the data from the zip file assuming it is a process
                // definition
                zipFile = new ZipFile(candidate);
                final InputStream processDefinition = findInputStream(zipFile, ".jpdl.xml");
                final InputStream processImage = findInputStream(zipFile, ".png");
                final String processName = findProcessName(zipFile);

                // do we really have a process definition
                if ((processDefinition == null) || (processImage == null) || (processName == null)) {
                    LOGGER.warn("zip file doesn't contain a complete process definition set, "
                            + "processDefinition: {}, processImage: {}, not deploying the file", processDefinition, processImage);
                    return success;
                } else if (jbpmSetup.containsProcessDefinition(processName)) {
                    LOGGER.info("process with name {} is already deployed, we won't deploy it again", processName);
                    return success;
                } else {
                    // deploy the process definition
                    jbpmSetup.deployInputStreams(processDefinition, processImage, processName, "Automatic Startup Deployment (" + processName + ")");
                    LOGGER.info("successfully deployed process definition {} from boot directory", processName);
                    success = true;
                }
            } catch (final ZipException ex) {
                success = false;
                LOGGER.warn("problem with zip file", ex);
            } catch (final IOException ex) {
                success = false;
                LOGGER.warn("problem with file", ex);
            } catch (final DocumentException ex) {
                success = false;
                LOGGER.warn("problem with process definition during deployment", ex);
            } finally {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
            return success;

        } else if (mimeType.equals(MimeTypeUtil.MIMETYPE_APPLICATION_EXCEL)) {
            LOGGER.info("found an excel file, trying to deploy it as reference data");

            // an excel file, might be reference data, FIXME: open the file,
            // loop through the sheets and try to deploy each sheet...
            try {
                new WorkbookDeployer().deployData(new HSSFWorkbook(new FileInputStream(candidate)), hibernateSession);
                success = true;
                LOGGER.info("successfully deployed excel reference data from boot directory, filename was {}", candidate.getName());
            } catch (final FileNotFoundException ex) {
                success = false;
                LOGGER.warn("can't find file for deployment", ex);
            } catch (final IOException ex) {
                success = false;
                LOGGER.warn("problem with file during deployment", ex);
            }

        } else if (mimeType.equals(MimeTypeUtil.MIMETYPE_TEXT_XML)) {
            LOGGER.info("found a xml file, trying to deploy it as a process definition");

            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(candidate);
                final String processName = Jbpm4Utils.findProcessName(inputStream);
                inputStream.close();
                // we need a new input stream the other is alread read...
                inputStream = new FileInputStream(candidate);
                jbpmSetup.deployInputStreams(inputStream, null, processName, "Automatic Startup Deployment (" + processName + ")");                
            } catch (final FileNotFoundException ex) {
                success = false;
                LOGGER.warn("problem with file during deployment", ex);
            } catch (final DocumentException ex) {
                success = false;
                LOGGER.warn("problem with process definition during deployment", ex);
            } catch (final IOException ex) {
                success = false;
                LOGGER.warn("problem with IO during deployment", ex);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ex) {
                        LOGGER.warn("error while closing input stream", ex);
                    }
                }
            }
        } else {
            LOGGER.warn("unknown filetype found in boot directory: {}, no idea what to do with this filetype", mimeType);
        }

        return success;
    }

    private InputStream findInputStream(final ZipFile zipFile, final String fileExtension) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(fileExtension)) {
                try {
                    return zipFile.getInputStream(entry);
                } catch (final IOException ex) {
                    LOGGER.warn("problem reading input stream from zip file", ex);
                }
            }
        }
        return null;
    }


    /**
     * @param zipFile
     * @return
     * @throws DocumentException
     */
    private String findProcessName(final ZipFile zipFile) throws DocumentException {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".jpdl.xml")) {
                try {
                    final InputStream inputStream = zipFile.getInputStream(entry);
                    String name = Jbpm4Utils.findProcessName(inputStream);
                    inputStream.close();
                    return name;
                } catch (final IOException ex) {
                    LOGGER.warn("problem reading input stream from zip file {}", ex);
                }
            }
        }
        return null;
    }




    /**
     * perform some system checking and show warning in the logs so we might not
     * run into problems later
     */
    private void runChecks() {
        // validating the hibernate session hibernateSession

        // check if session is connected
        LOGGER.info("hibernateSession is of type {}", hibernateSession.getClass().getName());
        if (!hibernateSession.isConnected()) {
            LOGGER.warn("hibernate session is not connected");
        } else {
            LOGGER.info("session is connected");
        }

        // check if session is open
        if (!hibernateSession.isOpen()) {
            LOGGER.warn("hibernate session is not open");
        } else {
            LOGGER.info("hibernate session is open");
        }

        // check the transaction
        final Transaction transaction = hibernateSession.getTransaction();
        if (transaction == null) {
            LOGGER.warn("no transaction found");
        } else {
            LOGGER.info("transaction found, transaction type is {}", transaction.getClass().getName());
            if (!transaction.isActive()) {
                LOGGER.warn("transaction is not active");
            } else {
                LOGGER.info("transaction is active");
            }
        }

        final ServletContext servletContext = RuntimeEnvDataProvider.getServletContext();
        // check what server we are running on
        final String serverInfo = servletContext.getServerInfo();
        LOGGER.debug("serverInfo is {}", serverInfo);

        if ((serverInfo != null) && (serverInfo.length() > 0)) {

            if (serverInfo.startsWith("jetty")) {
                // for current Jetty: jetty/6.1.22
                LOGGER.info("running on Jetty");
            } else if (serverInfo.startsWith("Apache Tomcat")) {
                // for current Tomcat: Apache Tomcat/6.0.20
                LOGGER.info("running on Tomcat");
            } else {
                LOGGER.warn("unknown Server String: {}", serverInfo);
            }

        } else {
            LOGGER.warn("Serverinfo is null, this is fine for testing but we should have some serverinfo in production");
        }

        final int RECOMMENDED_MAJOR_VERSION = 2;
        final int RECOMMENDED_MINOR_VERSION = 5;

        final int majorVersion = servletContext.getMajorVersion();
        if (majorVersion == RECOMMENDED_MAJOR_VERSION) {
            // major version is ok
            LOGGER.debug("majorVersion is as expected: {}", majorVersion);

            // check minor version
            final int minorVersion = servletContext.getMinorVersion();
            if (minorVersion == RECOMMENDED_MINOR_VERSION) {
                LOGGER.debug("minorVersion is as expected: {}", minorVersion);
            } else if (minorVersion > 5) {
                LOGGER.warn("minorVersion is {}, higher as recommended version " + RECOMMENDED_MINOR_VERSION, minorVersion);
            } else {
                LOGGER.warn("minorVersion is {} not " + RECOMMENDED_MINOR_VERSION + ", consider upgrading your servlet container"
                        + ", continue on your own risk", minorVersion);
            }

        } else if (majorVersion > RECOMMENDED_MAJOR_VERSION) {
            // major version is higher than the recommended version, just a
            // warning
            LOGGER.warn("majorVersion is {}, higher as recommended version " + RECOMMENDED_MAJOR_VERSION, majorVersion);
        } else {
            // major version is lower than the recommended version
            LOGGER.warn("majorVersion is {} not " + RECOMMENDED_MAJOR_VERSION + ", consider upgrading your servlet container" + ", continue on your own risk",
                    majorVersion);
        }

        // this might not work in anything but tomcat
        final String contextPath = servletContext.getContextPath();
        LOGGER.info("contextPath is: {} ", contextPath);

        try {
            // the following stuff is container specific:
            // .servletContext.getResource(BOOT_DIRECTORY.);
            final URL bootDirectoryUrl = BOOT_DIRECTORY.getLocation().toURI().toURL();
            // Jetty Results:
            // url:
            // file:/home/jboss/server/default/deploy/myapp.ear/myapp.war/boot
            // Tomcat Results:
            // url: jndi:/localhost/warcontext/folder/boot
            LOGGER.debug("bootDirectoryUrl is: {} class is: {}", bootDirectoryUrl, bootDirectoryUrl != null ? bootDirectoryUrl.getClass() : "null");
        } catch (final MalformedURLException ex) {
            LOGGER.warn("malformed url", ex);
        }

        try {
            final String realPath = BOOT_DIRECTORY.getLocation().getCanonicalPath();
            LOGGER.debug("realPath is: {} ", realPath);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        // LOGGER.debug("enumerating servlets...");
        // Enumeration servlets = servletContext.getServlets();
        // if (!servlets.hasMoreElements()) {
        // LOGGER.debug("  no servlet found but that's ok, boot service is running before the faces context is available"
        // );
        // } else {
        // do {
        // Object object = servlets.nextElement();
        // LOGGER.debug("  servlet is: {} class is {}", object,
        // object.getClass() );
        // } while (servlets.hasMoreElements());
        // }

    }

}
