package net.wohlfart.framework;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.framework.EntityNotFoundException;
import org.jboss.seam.framework.HibernateEntityHome;
import org.jboss.seam.international.StatusMessage.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/*
 * The EntityHome class is used for managing the persistent state of single
 * entity instance, providing methods for all CRUD operations.
 * 
 * The generic type E represents the entity class being managed.
 * 
 * A Home object provides the following operations: persist(), remove(),
 * update() and getInstance().
 * 
 * Before you can call the remove(), or update() operations, you must first set
 * the identifier of the object you are interested in, using the setId() method.
 * 
 * see: http://docs.jboss.org/seam/2.1.1.GA/reference/en-US/html_single/ also
 * for JSF examples
 * 
 * see: http://docs.jboss.org/seam/1.1BETA2/reference/en/html/framework.html for
 * the hibernate view of the home pattern
 * 
 * 
 * Seam provides two implementations of Home, one for JPA, EntityHome, and one
 * for Hibernate, HibernateEntityHome.
 * 
 * 
 * 
 * A Home manages a single entity instance, negotiating with the persistence
 * manager to retrieve the instance. It is similar to the DAO pattern the
 * difference is it actually contains the managed entity, like a mix of DAO and
 * mediator pattern.
 * 
 * 
 * recommended reading:
 * http://www.seamframework.org/Community/HowDoIMakeAnEntityHomeSubclassStateful
 * http://www.seamframework.org/Community/ProblemDeletingWithEntityHome
 * http://www.seamframework.org/Community/HowDoIMakeAnEntityHomeSubclassStateful
 */

// see:
// http://seamframework.org/Community/PermissionManagergrantPermissionsReturnsTrueButTheValuesAreNotInTheDatabase
// for permission related stuff


/**
 * utility class for implementing entityHome classes
 * 
 * use Session hibernateSession = getSession(); instead of:
 * 
 * @In(value = "hibernateSession") private Session hibernateSession;
 * 
 * 
 * 
 * @author Michael Wohlfart
 * 
 * @param <E>
 */
public abstract class AbstractEntityHome<E> extends HibernateEntityHome<E> {


    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractEntityHome.class);

    // strangely we get a number format exception for l = new Long("+0");
    // but none for l = new Long("-0"); so we will use a pattern to identity
    // valid numbers
    private final String patternString = "[0-9]{0,10}";

    protected abstract String getNameInContext();

    /**
     * init an entity by its id
     * 
     * - return "invalid" if the id is not a valid Long 
     * - create a new entity and return "valid" if the id is empty or 0 
     * - return "invalid" if the id is valid but the entity can't be found 
     * 
     * note that the entity is in the hibernate session until evict(Entity) is 
     * called the cancel method does this
     * 
     * @param s
     * @return
     */
    public String setEntityId(final String s) {
        final FacesMessages facesMessages = FacesMessages.instance();
        LOGGER.debug("setting id called: >{}< old id is: >{}<", s, getId());
        // check if we can parse the id string to an id number
        if ((StringUtils.isNotEmpty(s)) && (!Pattern.matches(patternString, s))) {
            getSession().evict(instance);
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance();
            facesMessages.addFromResourceBundle(Severity.FATAL, "framework.entityHome.entityNotFoundException");
            LOGGER.info("EntityNotFoundException, the id is empty or doesn't match the pattern, the string was '{}'," 
                    + " returning 'invalid' as view ID, the context was cleared from the last component...", s);
            return "invalid";
        }
        try {
            // parsing the string to a long, using 0 as default
            final Long id = new Long(StringUtils.defaultIfEmpty(s, "0"));
            LOGGER.debug("cleaned parameter is: >{}<", id);
            // remove any old instance from the session so we just have
            // the current entity and nothing else to worry about
            //getSession().evict(instance);
            getSession().clear(); // this is a test for roleUpstream/downstream problems
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance(); // this sets the id to null
            if (id != 0) {
                setId(id);
                initInstance();
                // this is necessary to make sure we don't get a stale instance from the session
                // which used to happen with roles because of the upstream/downstream sets...
                getSession().refresh(instance);
            }
            return "valid";
        } catch (final EntityNotFoundException ex) {
            getSession().evict(instance);
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance();
            LOGGER.info("caught EntityNotFoundException, returning 'invalid' as view ID");
            facesMessages.addFromResourceBundle(Severity.FATAL, "framework.entityHome.entityNotFoundException");
            return "invalid";
        } catch (final NumberFormatException ex) {
            getSession().evict(instance);
            Contexts.getConversationContext().remove(getNameInContext());
            clearInstance();
            LOGGER.info("caught NumberFormatException, returning 'invalid' as view ID");
            facesMessages.addFromResourceBundle(Severity.FATAL, "framework.entityHome.numberFormatException");
            return "invalid";
        }
    }

    /**
     * removes the entity from the session and from the home object
     */
    public String cancel() {
        getPersistenceContext().evict(getInstance());
        clearInstance();
        return "canceled";
    }

}
