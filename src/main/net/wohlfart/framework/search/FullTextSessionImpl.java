/*
 * $Id: FullTextSessionImpl.java 19002 2010-03-16 01:28:07Z hardy.ferentschik $
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party
 * contributors as indicated by the @author tags or express copyright
 * attribution statements applied by the authors. All third-party contributions
 * are distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to: Free Software Foundation,
 * Inc. 51 Franklin Street, Fifth Floor Boston, MA 02110-1301 USA
 */
package net.wohlfart.framework.search;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.UnknownProfileException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.engine.NonFlushedChanges;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.query.ParameterMetadata;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.event.EventListeners;
import org.hibernate.event.EventSource;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.FullTextQueryImpl;
import org.hibernate.search.util.ContextHelper;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

/**
 * Lucene full text search aware session, we use this
 * to wrap the hibernate session for lucene when performing index/optimize
 * and search operations
 * 
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
@SuppressWarnings("deprecation")
public class FullTextSessionImpl implements FullTextSession, SessionImplementor {

    private static final long serialVersionUID = -1L;

    private final Session                      session;
    private final SessionImplementor           sessionImplementor;
    private transient SearchFactoryImplementor searchFactory;
    private final TransactionContext           transactionContext;

    public FullTextSessionImpl(final org.hibernate.Session session) {
        this.session = session;
        transactionContext = new EventSourceTransactionContext((EventSource) session);
        sessionImplementor = (SessionImplementor) session;
    }

    /**
     * Execute a Lucene query and retrieve managed objects of type entities (or
     * their indexed subclasses) If entities is empty, include all indexed
     * entities
     * 
     * @param entities
     *            must be immutable for the lifetime of the query object
     */
    @Override
    public FullTextQuery createFullTextQuery(final org.apache.lucene.search.Query luceneQuery, final Class<?>... entities) {
        return new FullTextQueryImpl(luceneQuery, entities, sessionImplementor, new ParameterMetadata(null, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void purgeAll(final Class<T> entityType) {
        purge(entityType, null);
    }

    @Override
    public void flushToIndexes() {
        final SearchFactoryImplementor searchFactoryImplementor = getSearchFactoryImplementor();
        searchFactoryImplementor.getWorker().flushWorks(transactionContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void purge(final Class<T> entityType, final Serializable id) {
        if (entityType == null) {
            return;
        }

        final Set<Class<?>> targetedClasses = getSearchFactoryImplementor().getIndexedTypesPolymorphic(new Class[] { entityType });
        if (targetedClasses.isEmpty()) {
            final String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
            throw new IllegalArgumentException(msg);
        }

        for (final Class<?> clazz : targetedClasses) {
            if (id == null) {
                createAndPerformWork(clazz, null, WorkType.PURGE_ALL);
            } else {
                createAndPerformWork(clazz, id, WorkType.PURGE);
            }
        }
    }

    private <T> void createAndPerformWork(final Class<T> clazz, final Serializable id, final WorkType workType) {
        Work<T> work;
        work = new Work<T>(clazz, id, workType);
        getSearchFactoryImplementor().getWorker().performWork(work, transactionContext);
    }

    /**
     * (Re-)index an entity. The entity must be associated with the session and
     * non indexable entities are ignored.
     * 
     * @param entity
     *            The entity to index - must not be <code>null</code>.
     * 
     * @throws IllegalArgumentException
     *             if entity is null or not an @Indexed entity
     */
    @Override
    public <T> void index(final T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity to index should not be null");
        }

        final Class<?> clazz = Hibernate.getClass(entity);
        // TODO cache that at the FTSession level
        final SearchFactoryImplementor searchFactoryImplementor = getSearchFactoryImplementor();
        // not strictly necessary but a small optimization
        if (searchFactoryImplementor.getDocumentBuilderIndexedEntity(clazz) == null) {
            final String msg = "Entity to index is not an @Indexed entity: " + entity.getClass().getName();
            throw new IllegalArgumentException(msg);
        }
        final Serializable id = session.getIdentifier(entity);
        final Work<T> work = new Work<T>(entity, id, WorkType.INDEX);
        
        // FIXME: fulltext indexing is offline by now:
        searchFactoryImplementor.getWorker().performWork(work, transactionContext);

        // TODO
        // need to add elements in a queue kept at the Session level
        // the queue will be processed by a Lucene(Auto)FlushEventListener
        // note that we could keep this queue somewhere in the event listener in
        // the mean time but that requires
        // a synchronized hashmap holding this queue on a per session basis plus
        // some session house keeping (yuk)
        // another solution would be to subclass SessionImpl instead of having
        // this LuceneSession delegation model
        // this is an open discussion
    }

    @Override
    public MassIndexer createIndexer(final Class<?>... types) {
        if (types.length == 0) {
            return new MassIndexerImpl(getSearchFactoryImplementor(), getSessionFactory(), Object.class);
        } else {
            return new MassIndexerImpl(getSearchFactoryImplementor(), getSessionFactory(), types);
        }
    }

    @Override
    public SearchFactory getSearchFactory() {
        return getSearchFactoryImplementor();
    }

    private SearchFactoryImplementor getSearchFactoryImplementor() {
        if (searchFactory == null) {
            searchFactory = ContextHelper.getSearchFactory(session);
        }
        return searchFactory;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Query createSQLQuery(final String sql, final String returnAlias, final Class returnClass) {
        throw new HibernateException("unsupported method");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Query createSQLQuery(final String sql, final String[] returnAliases, final Class[] returnClasses) {
        throw new HibernateException("unsupported method");
    }

    @Override
    public int delete(final String query) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public int delete(final String query, final Object value, final Type type) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public int delete(final String query, final Object[] values, final Type[] types) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Collection<?> filter(final Object collection, final String filter) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Collection<?> filter(final Object collection, final String filter, final Object value, final Type type) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Collection<?> filter(final Object collection, final String filter, final Object[] values, final Type[] types) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public List<?> find(final String query) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public List<?> find(final String query, final Object value, final Type type) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public List<?> find(final String query, final Object[] values, final Type[] types) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Iterator<?> iterate(final String query) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Iterator<?> iterate(final String query, final Object value, final Type type) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Iterator<?> iterate(final String query, final Object[] values, final Type[] types) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public void save(final String entityName, final Object object, final Serializable id) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public void save(final Object object, final Serializable id) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Object saveOrUpdateCopy(final String entityName, final Object object) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Object saveOrUpdateCopy(final String entityName, final Object object, final Serializable id) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Object saveOrUpdateCopy(final Object object) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Object saveOrUpdateCopy(final Object object, final Serializable id) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public void update(final String entityName, final Object object, final Serializable id) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public void update(final Object object, final Serializable id) throws HibernateException {
        throw new HibernateException("unsupported method");
    }

    @Override
    public Transaction beginTransaction() throws HibernateException {
        return session.beginTransaction();
    }

    @Override
    public void cancelQuery() throws HibernateException {
        session.cancelQuery();
    }

    @Override
    public void clear() {
        session.clear();
    }

    @Override
    public Connection close() throws HibernateException {
        return session.close();
    }

    @Override
    public Connection connection() throws HibernateException {
        return session.connection();
    }

    @Override
    public boolean contains(final Object object) {
        return session.contains(object);
    }

    @Override
    public Criteria createCriteria(final String entityName) {
        return session.createCriteria(entityName);
    }

    @Override
    public Criteria createCriteria(final String entityName, final String alias) {
        return session.createCriteria(entityName, alias);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Criteria createCriteria(final Class persistentClass) {
        return session.createCriteria(persistentClass);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Criteria createCriteria(final Class persistentClass, final String alias) {
        return session.createCriteria(persistentClass, alias);
    }

    @Override
    public Query createFilter(final Object collection, final String queryString) throws HibernateException {
        return session.createFilter(collection, queryString);
    }

    @Override
    public Query createQuery(final String queryString) throws HibernateException {
        return session.createQuery(queryString);
    }

    @Override
    public SQLQuery createSQLQuery(final String queryString) throws HibernateException {
        return session.createSQLQuery(queryString);
    }

    @Override
    public void delete(final String entityName, final Object object) throws HibernateException {
        session.delete(entityName, object);
    }

    @Override
    public void delete(final Object object) throws HibernateException {
        session.delete(object);
    }

    @Override
    public void disableFilter(final String filterName) {
        session.disableFilter(filterName);
    }

    @Override
    public Connection disconnect() throws HibernateException {
        return session.disconnect();
    }

    @Override
    public Filter enableFilter(final String filterName) {
        return session.enableFilter(filterName);
    }

    @Override
    public void evict(final Object object) throws HibernateException {
        session.evict(object);
    }

    @Override
    public void flush() throws HibernateException {
        session.flush();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object get(final Class clazz, final Serializable id) throws HibernateException {
        return session.get(clazz, id);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object get(final Class clazz, final Serializable id, final LockMode lockMode) throws HibernateException {
        return session.get(clazz, id, lockMode);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object get(final Class clazz, final Serializable id, final LockOptions lockOptions) throws HibernateException {
        return session.get(clazz, id, lockOptions);
    }

    @Override
    public Object get(final String entityName, final Serializable id) throws HibernateException {
        return session.get(entityName, id);
    }

    @Override
    public Object get(final String entityName, final Serializable id, final LockMode lockMode) throws HibernateException {
        return session.get(entityName, id, lockMode);
    }

    @Override
    public Object get(final String entityName, final Serializable id, final LockOptions lockOptions) throws HibernateException {
        return session.get(entityName, id, lockOptions);
    }

    @Override
    public CacheMode getCacheMode() {
        return session.getCacheMode();
    }

    @Override
    public LockMode getCurrentLockMode(final Object object) throws HibernateException {
        return session.getCurrentLockMode(object);
    }

    @Override
    public Filter getEnabledFilter(final String filterName) {
        return session.getEnabledFilter(filterName);
    }

    @Override
    public Interceptor getInterceptor() {
        return sessionImplementor.getInterceptor();
    }

    @Override
    public void setAutoClear(final boolean enabled) {
        sessionImplementor.setAutoClear(enabled);
    }

    @Override
    public boolean isTransactionInProgress() {
        return sessionImplementor.isTransactionInProgress();
    }

    @Override
    public void initializeCollection(final PersistentCollection collection, final boolean writing) throws HibernateException {
        sessionImplementor.initializeCollection(collection, writing);
    }

    @Override
    public Object internalLoad(final String entityName, final Serializable id, final boolean eager, final boolean nullable) throws HibernateException {
        return sessionImplementor.internalLoad(entityName, id, eager, nullable);
    }

    @Override
    public Object immediateLoad(final String entityName, final Serializable id) throws HibernateException {
        return sessionImplementor.immediateLoad(entityName, id);
    }

    @Override
    public long getTimestamp() {
        return sessionImplementor.getTimestamp();
    }

    @Override
    public SessionFactoryImplementor getFactory() {
        return sessionImplementor.getFactory();
    }

    @Override
    public Batcher getBatcher() {
        return sessionImplementor.getBatcher();
    }

    @Override
    public List<?> list(final String query, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.list(query, queryParameters);
    }

    @Override
    public Iterator<?> iterate(final String query, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.iterate(query, queryParameters);
    }

    @Override
    public ScrollableResults scroll(final String query, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.scroll(query, queryParameters);
    }

    @Override
    public ScrollableResults scroll(final CriteriaImpl criteria, final ScrollMode scrollMode) {
        return sessionImplementor.scroll(criteria, scrollMode);
    }

    @Override
    public List<?> list(final CriteriaImpl criteria) {
        return sessionImplementor.list(criteria);
    }

    @Override
    public List<?> listFilter(final Object collection, final String filter, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.listFilter(collection, filter, queryParameters);
    }

    @Override
    public Iterator<?> iterateFilter(final Object collection, final String filter, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.iterateFilter(collection, filter, queryParameters);
    }

    @Override
    public EntityPersister getEntityPersister(final String entityName, final Object object) throws HibernateException {
        return sessionImplementor.getEntityPersister(entityName, object);
    }

    @Override
    public Object getEntityUsingInterceptor(final EntityKey key) throws HibernateException {
        return sessionImplementor.getEntityUsingInterceptor(key);
    }

    @Override
    public void afterTransactionCompletion(final boolean successful, final Transaction tx) {
        sessionImplementor.afterTransactionCompletion(successful, tx);
    }

    @Override
    public void beforeTransactionCompletion(final Transaction tx) {
        sessionImplementor.beforeTransactionCompletion(tx);
    }

    @Override
    public Serializable getContextEntityIdentifier(final Object object) {
        return sessionImplementor.getContextEntityIdentifier(object);
    }

    @Override
    public String bestGuessEntityName(final Object object) {
        return sessionImplementor.bestGuessEntityName(object);
    }

    @Override
    public String guessEntityName(final Object entity) throws HibernateException {
        return sessionImplementor.guessEntityName(entity);
    }

    @Override
    public Object instantiate(final String entityName, final Serializable id) throws HibernateException {
        return sessionImplementor.instantiate(entityName, id);
    }

    @Override
    public List<?> listCustomQuery(final CustomQuery customQuery, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.listCustomQuery(customQuery, queryParameters);
    }

    @Override
    public ScrollableResults scrollCustomQuery(final CustomQuery customQuery, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.scrollCustomQuery(customQuery, queryParameters);
    }

    @Override
    public List<?> list(final NativeSQLQuerySpecification spec, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.list(spec, queryParameters);
    }

    @Override
    public ScrollableResults scroll(final NativeSQLQuerySpecification spec, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.scroll(spec, queryParameters);
    }

    @Override
    public Object getFilterParameterValue(final String filterParameterName) {
        return sessionImplementor.getFilterParameterValue(filterParameterName);
    }

    @Override
    public Type getFilterParameterType(final String filterParameterName) {
        return sessionImplementor.getFilterParameterType(filterParameterName);
    }

    @Override
    public Map<?, ?> getEnabledFilters() {
        return sessionImplementor.getEnabledFilters();
    }

    @Override
    public int getDontFlushFromFind() {
        return sessionImplementor.getDontFlushFromFind();
    }

    @Override
    public EventListeners getListeners() {
        return sessionImplementor.getListeners();
    }

    @Override
    public PersistenceContext getPersistenceContext() {
        return sessionImplementor.getPersistenceContext();
    }

    @Override
    public int executeUpdate(final String query, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.executeUpdate(query, queryParameters);
    }

    @Override
    public int executeNativeUpdate(final NativeSQLQuerySpecification specification, final QueryParameters queryParameters) throws HibernateException {
        return sessionImplementor.executeNativeUpdate(specification, queryParameters);
    }

    @Override
    public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
        return sessionImplementor.getNonFlushedChanges();
    }

    @Override
    public void applyNonFlushedChanges(final NonFlushedChanges nonFlushedChanges) throws HibernateException {
        sessionImplementor.applyNonFlushedChanges(nonFlushedChanges);
    }

    @Override
    public EntityMode getEntityMode() {
        return session.getEntityMode();
    }

    @Override
    public String getEntityName(final Object object) throws HibernateException {
        return session.getEntityName(object);
    }

    @Override
    public FlushMode getFlushMode() {
        return session.getFlushMode();
    }

    @Override
    public Serializable getIdentifier(final Object object) throws HibernateException {
        return session.getIdentifier(object);
    }

    @Override
    public Query getNamedQuery(final String queryName) throws HibernateException {
        return session.getNamedQuery(queryName);
    }

    @Override
    public Query getNamedSQLQuery(final String name) {
        return sessionImplementor.getNamedSQLQuery(name);
    }

    @Override
    public boolean isEventSource() {
        return sessionImplementor.isEventSource();
    }

    @Override
    public void afterScrollOperation() {
        sessionImplementor.afterScrollOperation();
    }

    @Override
    public void setFetchProfile(final String name) {
        sessionImplementor.setFetchProfile(name);
    }

    @Override
    public String getFetchProfile() {
        return sessionImplementor.getFetchProfile();
    }

    @Override
    public JDBCContext getJDBCContext() {
        return sessionImplementor.getJDBCContext();
    }

    @Override
    public boolean isClosed() {
        return sessionImplementor.isClosed();
    }

    @Override
    public LoadQueryInfluencers getLoadQueryInfluencers() {
        return sessionImplementor.getLoadQueryInfluencers();
    }

    @Override
    public org.hibernate.Session getSession(final EntityMode entityMode) {
        return session.getSession(entityMode);
    }

    @Override
    public SessionFactory getSessionFactory() {
        return session.getSessionFactory();
    }

    @Override
    public SessionStatistics getStatistics() {
        return session.getStatistics();
    }

    @Override
    public boolean isReadOnly(final Object entityOrProxy) {
        return session.isReadOnly(entityOrProxy);
    }

    @Override
    public Transaction getTransaction() {
        return session.getTransaction();
    }

    @Override
    public boolean isConnected() {
        return session.isConnected();
    }

    @Override
    public boolean isDirty() throws HibernateException {
        return session.isDirty();
    }

    @Override
    public boolean isDefaultReadOnly() {
        return session.isDefaultReadOnly();
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public Object load(final String entityName, final Serializable id) throws HibernateException {
        return session.load(entityName, id);
    }

    @Override
    public Object load(final String entityName, final Serializable id, final LockMode lockMode) throws HibernateException {
        return session.load(entityName, id, lockMode);
    }

    @Override
    public Object load(final String entityName, final Serializable id, final LockOptions lockOptions) throws HibernateException {
        return session.load(entityName, id, lockOptions);
    }

    @Override
    public void load(final Object object, final Serializable id) throws HibernateException {
        session.load(object, id);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object load(final Class theClass, final Serializable id) throws HibernateException {
        return session.load(theClass, id);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object load(final Class theClass, final Serializable id, final LockMode lockMode) throws HibernateException {
        return session.load(theClass, id, lockMode);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object load(final Class theClass, final Serializable id, final LockOptions lockOptions) throws HibernateException {
        return session.load(theClass, id, lockOptions);
    }

    @Override
    public void lock(final String entityName, final Object object, final LockMode lockMode) throws HibernateException {
        session.lock(entityName, object, lockMode);
    }

    @Override
    public LockRequest buildLockRequest(final LockOptions lockOptions) {
        return session.buildLockRequest(lockOptions);
    }

    @Override
    public void lock(final Object object, final LockMode lockMode) throws HibernateException {
        session.lock(object, lockMode);
    }

    @Override
    public Object merge(final String entityName, final Object object) throws HibernateException {
        return session.merge(entityName, object);
    }

    @Override
    public Object merge(final Object object) throws HibernateException {
        return session.merge(object);
    }

    @Override
    public void persist(final String entityName, final Object object) throws HibernateException {
        session.persist(entityName, object);
    }

    @Override
    public void persist(final Object object) throws HibernateException {
        session.persist(object);
    }

    @Override
    public void reconnect() throws HibernateException {
        session.reconnect();
    }

    @Override
    public void reconnect(final Connection connection) throws HibernateException {
        session.reconnect(connection);
    }

    @Override
    public void refresh(final Object object) throws HibernateException {
        session.refresh(object);
    }

    @Override
    public void refresh(final Object object, final LockMode lockMode) throws HibernateException {
        session.refresh(object, lockMode);
    }

    @Override
    public void refresh(final Object object, final LockOptions lockOptions) throws HibernateException {
        session.refresh(object, lockOptions);
    }

    @Override
    public void replicate(final String entityName, final Object object, final ReplicationMode replicationMode) throws HibernateException {
        session.replicate(entityName, object, replicationMode);
    }

    @Override
    public void replicate(final Object object, final ReplicationMode replicationMode) throws HibernateException {
        session.replicate(object, replicationMode);
    }

    @Override
    public Serializable save(final String entityName, final Object object) throws HibernateException {
        return session.save(entityName, object);
    }

    @Override
    public Serializable save(final Object object) throws HibernateException {
        return session.save(object);
    }

    @Override
    public void saveOrUpdate(final String entityName, final Object object) throws HibernateException {
        session.saveOrUpdate(entityName, object);
    }

    @Override
    public void saveOrUpdate(final Object object) throws HibernateException {
        session.saveOrUpdate(object);
    }

    @Override
    public void setCacheMode(final CacheMode cacheMode) {
        session.setCacheMode(cacheMode);
    }

    @Override
    public void setDefaultReadOnly(final boolean readOnly) {
        session.setDefaultReadOnly(readOnly);
    }

    @Override
    public void setFlushMode(final FlushMode flushMode) {
        session.setFlushMode(flushMode);
    }

    @Override
    public void setReadOnly(final Object entity, final boolean readOnly) {
        session.setReadOnly(entity, readOnly);
    }

    @Override
    public void doWork(final org.hibernate.jdbc.Work work) throws HibernateException {
        session.doWork(work);
    }

    @Override
    public void update(final String entityName, final Object object) throws HibernateException {
        session.update(entityName, object);
    }

    @Override
    public void update(final Object object) throws HibernateException {
        session.update(object);
    }

    @Override
    public boolean isFetchProfileEnabled(final String name) throws UnknownProfileException {
        return session.isFetchProfileEnabled(name);
    }

    @Override
    public void enableFetchProfile(final String name) throws UnknownProfileException {
        session.enableFetchProfile(name);
    }

    @Override
    public void disableFetchProfile(final String name) throws UnknownProfileException {
        session.disableFetchProfile(name);
    }




    /*
    // new for Hibernate 3.6
    @Override
    public LobHelper getLobHelper() {
        // TODO Auto-generated method stub
        return null;
    }

    // new for Hibernate 3.6
    @Override
    public TypeHelper getTypeHelper() {
        // TODO Auto-generated method stub
        return null;
    }
    */

    /*
    @Override
    public FullTextQuery createFullTextQuery(org.apache.lucene.search.Query arg0, Class<?>... arg1) {
        // TODO Auto-generated method stub
        return null;
    }
    */
}
