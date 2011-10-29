package net.wohlfart.framework.properties;

import java.io.Serializable;
import java.util.Date;

import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Expressions;
import org.jboss.seam.core.Expressions.ValueExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("propertiesManager")
@Scope(ScopeType.EVENT)
@BypassInterceptors
@AutoCreate
public class PropertiesManager implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(PropertiesManager.class);

    // default key for some properties
    public static final String LAST_LOGIN_PROPERTY_NAME = "lastLogin";
    public static final String DEFAULT_DATE_FORMAT      = "defaultDateFormat";
    
    // application property used for database initialization
    public static final String FIRST_BOOT_PROPERTY_NAME = "firstBoot";

    public static final String LAST_BOOT_PROPERTY_NAME  = "lastBoot";

    // for dynamically resolving the hibernate session without bijection
    private ValueExpression<Session> hibernateSession;

    @Create
    public void init() {
        LOGGER.debug("Created component {}", this);
        initHibernateSession();
    }

    protected void initHibernateSession() {
        // EL expression for runtime evaluation
        hibernateSession = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class);
    }

    protected Session lookupHibernateSession() {
        return hibernateSession.getValue();
    }

    public CharmsPropertySet getApplicationProperties() {

        final Session session = lookupHibernateSession();
        CharmsPropertySet set = (CharmsPropertySet) session
            .getNamedQuery(CharmsPropertySet.FIND_BY_NAME_AND_TYPE)
            .setParameter("name", CharmsPropertySet.APPLICATION_NAME)
            .setParameter("type", CharmsPropertySetType.APPLICATION)
            .uniqueResult();

        if (set == null) {
            // no properties set so far for this user, create and persis one
            set = new CharmsPropertySet();
            set.setName(CharmsPropertySet.APPLICATION_NAME);
            set.setType(CharmsPropertySetType.APPLICATION);
            LOGGER.info("Creating an application properties set in the database, this is normal on first bootup. "
                    + "There might be a problem if this application has been run before with the current database. ");
            session.persist(set);
        }

        return set;
    }

    public CharmsPropertySet getTemplateProperties(final String name) {

        final Session session = lookupHibernateSession();
        CharmsPropertySet set = (CharmsPropertySet) session
            .getNamedQuery(CharmsPropertySet.FIND_BY_NAME_AND_TYPE)
            .setParameter("name", name)
            .setParameter("type", CharmsPropertySetType.USER)
            .uniqueResult();

        if (set == null) {
            // no properties set so far for this user, create and persis one
            set = new CharmsPropertySet();
            set.setName(name);
            set.setType(CharmsPropertySetType.TEMPLATE);
            LOGGER.info("creating template properties set with name {}", name);
            session.persist(set);
        }

        return set;
    }

    /**
     * never return null
     * 
     * @param charmsUser
     * @return
     */
    public CharmsPropertySet getUserProperties(final CharmsUser charmsUser) {

        // the users name might be changed so we use the only
        // parameter that can't be changed in DB, the database primary key of the
        // user row as name for the property lookup:
        final String name = String.valueOf(charmsUser.getId());

        final Session session = lookupHibernateSession();
        CharmsPropertySet set = (CharmsPropertySet) session
            .getNamedQuery(CharmsPropertySet.FIND_BY_NAME_AND_TYPE)
            .setParameter("name", name)
            .setParameter("type", CharmsPropertySetType.USER)
            .uniqueResult();

        if (set == null) {
            // no properties set so far for this user, create and persist one
            set = new CharmsPropertySet();
            set.setName(name);
            set.setType(CharmsPropertySetType.USER);
            LOGGER.info("creating template properties set with id/name {}", name);
            session.persist(set);
        }
        return set;
    }

    // simplifying the persistence work for the caller ----

    public void persistProperty(final CharmsPropertySet set, final String key, final String string) {
        persistProperty(set, key, CharmsPropertyType.STRING, string);
    }

    public void persistProperty(final CharmsPropertySet set, final String key, final Date date) {
        persistProperty(set, key, CharmsPropertyType.DATE, Long.toString(date.getTime()));
    }

    public void persistProperty(final CharmsPropertySet set, final String key, final CharmsMementoState state) {
        persistProperty(set, key, CharmsPropertyType.MEMENTO, state.getValue());
    }

    public void persistProperty(final CharmsProperty prop) {
        persistProperty(prop.getPropertySet(), prop.getName(), prop.getType(), prop.getValue());
    }

    // this method is doing the real work ----
    private void persistProperty(
            final CharmsPropertySet set, 
            final String key, 
            final CharmsPropertyType type,
            final String value) {
        LOGGER.debug("persisting user property: {}, date value is {}", key, value);

        CharmsProperty property;
        if (set.hasKey(key)) {
            property = set.getProperty(key);
            property.setType(type);
            property.setValue(value);
        } else {
            property = new CharmsProperty();
            property.setName(key);
            property.setType(type);
            property.setValue(value);
            
            property.setPropertySet(set);
            set.putProperty(property);
        }

        final Session session = lookupHibernateSession();
        session.persist(property);
        session.persist(set);
    }

}
