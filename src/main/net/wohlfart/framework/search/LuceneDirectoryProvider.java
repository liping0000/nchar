package net.wohlfart.framework.search;

import static net.wohlfart.framework.RuntimeEnvDataProvider.FileSystemLocation.LUCENE_DIRECTORY;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.DirectoryProviderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class implements a directory provider for the lucene seach engine
 * 
 * @author Michael Wohlfart
 * 
 */
public class LuceneDirectoryProvider implements DirectoryProvider<FSDirectory> {

    private final static Logger LOGGER = LoggerFactory.getLogger(LuceneDirectoryProvider.class);

    private FSDirectory         directory;
    private File                indexDir;
    private String              indexName;

    // private String directoryProviderName;
    // private Properties properties;

    
    /*
     * lucene >= 3 method:
     * 

    @Override
    public void initialize(
            final String directoryProviderName, 
            final Properties properties, 
            final BuildContext buildContext) {
        initialize(directoryProviderName, properties, buildContext.getUninitializedSearchFactory());
    }
      */
    
    /**
     * FIXME: this method is called multiple times on startup
     */
    //@Override
    public void initialize(
            final String directoryProviderName, 
            final Properties properties, 
            final SearchFactoryImplementor searchFactoryImplementor) {
        LOGGER.debug("initializing index dir by {}, indexName is {}", this, indexName);

        // this.directoryProviderName = directoryProviderName;
        // this.properties = properties;

        try {
            // in the test environment this ends up as
            // /home/michael/Projects/charms/eclipse_workspace/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/wtpwebapps/charms/var/lucene
            File file = LUCENE_DIRECTORY.getLocation();
            if (file == null) {
                LOGGER.warn("hibernate.search.default.indexBase is null, trying to work with temp dir now");
                File tempFile = File.createTempFile("lucene", ".tmp" );
                file = tempFile.getParentFile();
            } 
            String indexBase = file.getCanonicalPath();
            LOGGER.debug("hibernate.search.default.indexBase is {}", indexBase);
            // on "manual" indexing skip read-write check on index directory
            final boolean verifyIsWritable = true;
            indexDir = getVerifiedIndexDir(indexBase, directoryProviderName, properties, verifyIsWritable);
            indexName = indexDir.getCanonicalPath();

        } catch (final IOException ex) {
            LOGGER.warn("can't initialize Directory Provider", ex);
        }
    }

    /**
     * Verify the index directory exists and is writable, or creates it if not
     * existing.
     * 
     * @param annotatedIndexName
     *            The index name declared on the @Indexed annotation
     * @param properties
     *            The properties may override the indexname.
     * @param verifyIsWritable
     *            Verify the directory is writable
     * @return the File representing the Index Directory
     * @throws SearchException
     */
    private File getVerifiedIndexDir(final String indexBase, final String annotatedIndexName, final Properties properties, final boolean verifyIsWritable) {
        final String indexName = properties.getProperty("indexName", annotatedIndexName);
        final File baseIndexDir = new File(indexBase);
        makeSanityCheckedDirectory(baseIndexDir, indexName, verifyIsWritable);
        final File indexDir = new File(baseIndexDir, indexName);
        makeSanityCheckedDirectory(indexDir, indexName, verifyIsWritable);
        LOGGER.debug("returning verified directory {}", indexDir);
        return indexDir;
    }

    /**
     * @param directory
     *            The directory to create or verify
     * @param indexName
     *            To label exceptions
     * @param verifyIsWritable
     *            Verify the directory is writable
     * @throws SearchException
     */
    private void makeSanityCheckedDirectory(final File directory, final String indexName, final boolean verifyIsWritable) {
        if (!directory.exists()) {
            LOGGER.info("Index directory not found, creating: '" + directory.getAbsolutePath() + "'");
            // if not existing, create the full path
            if (!directory.mkdirs()) {
                throw new SearchException("Unable to create index directory: " + directory.getAbsolutePath() + " for index " + indexName);
            }
        } else {
            // else check it is not a file
            if (!directory.isDirectory()) {
                throw new SearchException("Unable to initialize index: " + indexName + ": " + directory.getAbsolutePath() + " is a file.");
            }
        }
        // and ensure it's writable
        if (verifyIsWritable && (!directory.canWrite())) {
            throw new SearchException("Cannot write into index directory: " + directory.getAbsolutePath() + " for index " + indexName);
        }
    }

    @Override
    public void start() {
        LOGGER.debug("starting search directory provider");
        // everything is done in the initialize method

        try {
            // delete the whole index directory
            if (indexDir.isDirectory()) {
                final File[] files = indexDir.listFiles();
                for (final File file : files) {
                    LOGGER.info("removing file {} from index directory", file);
                    final boolean deleted = file.delete();
                    if (!deleted) {
                        LOGGER.warn("unable to delete file {}", file);
                    }
                }
                LOGGER.info("removing index directory {}, you have to reindex for full text search to work", indexDir);
                final boolean deleted = indexDir.delete();
                if (!deleted) {
                    LOGGER.warn("error deleting file {}", deleted);
                }
            } else {
                LOGGER.info("index location is no directory: {}", indexDir);
            }

            // this is cheap so it's not done in start()
            LOGGER.info("creating file system index at {}", indexDir);
            // for the old search API:
            // directory = DirectoryProviderHelper.createFSIndex(indexDir);
            // for the next hibernate search release:
            final Properties properties = new Properties(); // empty properties
            // to match the
            // signature
            directory = DirectoryProviderHelper.createFSIndex(indexDir, properties);
        } catch (final IOException e) {
            throw new SearchException("Unable to clean index", e);
        }
    }

    @Override
    public void stop() {
        LOGGER.debug("stopping search directory provider");
        try {
            directory.close();
        } catch (final Exception e) {
            LOGGER.error("Unable to properly close Lucene directory {}" + directory.getFile(), e);
        }
    }

    @Override
    public FSDirectory getDirectory() {
        LOGGER.debug("returning index directory {}", directory);
        return directory;
    }

    @Override
    public boolean equals(final Object obj) {
        // this code is actually broken since the value change after initialize
        // call
        // but from a practical POV this is fine since we only call this method
        // after the initialize call
        if (obj == this) {
            return true;
        }
        if ((obj == null) || !(obj instanceof LuceneDirectoryProvider)) {
            return false;
        }

        if (indexName == null) {
            LOGGER.warn("equals() must not be called before the initialize() method, "
                    + "this is a programmers error and should be fixed, returning false for equals()");
            return false;
        }
        final LuceneDirectoryProvider that = (LuceneDirectoryProvider) obj;
        if (that.indexName == null) {
            LOGGER.warn("equals() must not be called before the initialize() method, "
                    + "this is a programmers error and should be fixed, returning false for equals()");
            return false;
        }
        return indexName.equals(((LuceneDirectoryProvider) obj).indexName);
    }

    @Override
    public int hashCode() {
        // this code is actually broken since the value change after initialize
        // call
        // but from a practical POV this is fine since we only call this method
        // after initialize call
        final int hash = 11;
        return 37 * hash + ((indexName == null) ? 0 : indexName.hashCode());
    }
}
