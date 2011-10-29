package net.wohlfart.framework.debug;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.util.EnumerationIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: add as hidden div with this values
// to the root template for debugging...

@Name("httpSessionStatistics")
@Scope(ScopeType.EVENT)
// @BypassInterceptors
public class HttpSessionStatistics {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpSessionStatistics.class);

    // component-name to component-size map
    private List<ComponentSize> componentSizes;
    // session id
    private String              sessionId;
    // session id
    private Integer             sessionSize;

    public Integer getSessionSize() {
        return sessionSize;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<ComponentSize> getComponentSizes() {
        return componentSizes;
    }

    @Create
    public void createComponent() {
        LOGGER.debug("createComponent called");
        setupComponentSizes();
        sessionId = getCurrentSession().getId();
    }

    @Destroy
    public void destroyComponent() {
        LOGGER.debug("destroyComponent called");

    }

    private void setupComponentSizes() {
        componentSizes = new LinkedList<ComponentSize>();
        final HttpSession session = getCurrentSession();

        try {

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);

            final Iterator<String> iter = new EnumerationIterator<String>(session.getAttributeNames());
            while (iter.hasNext()) {
                final Integer before = baos.size();
                final String name = iter.next();
                final Object object = session.getAttribute(name);
                try {
                    oos.writeObject(object);
                } catch (final Exception ex) {
                    LOGGER.debug("can't serialize object in session: attribute name is " + name + " exception is " + ex.toString() + " class is: "
                            + object.getClass() + " toString returns: " + object.toString());
                }
                oos.flush();
                final Integer after = baos.size();
                componentSizes.add(new ComponentSize(name, after - before));
            }
            // some more statistic data here:
            sessionSize = baos.size();
            sessionId = session.getId();
        } catch (final Exception ex) {
            LOGGER.error("can't calculate session component sizes", ex);
        }

        // sort the list to look nice...
        Collections.sort(componentSizes, new Comparator<ComponentSize>() {

            @Override
            public int compare(final ComponentSize o1, final ComponentSize o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    private HttpSession getCurrentSession() {
        return (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private List<HttpSession> getAllSessions() {
        final Context applicationContext = Contexts.getApplicationContext();
        return (List<HttpSession>) applicationContext.get(HttpSessionCollector.SESSION_LIST_KEY);
    }

    public static class ComponentSize {

        private final String  name;
        private final Integer size;

        ComponentSize(final String name, final Integer size) {
            this.name = name;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public Integer getSize() {
            return size;
        }
    }

    /*
     * 
     * @Factory("currentSessionSize") public int getCurrentSessionSize() {
     * HttpSession currentSession = getCurrentSession(); if (currentSession ==
     * null) { return 0; } return calculateSessionSize(currentSession); }
     * 
     * @Factory("totalSizeOfSessions") public int getTotalSizeOfSessions() { int
     * totalBytes = 0; for (HttpSession session : getSessions()) { totalBytes +=
     * calculateSessionSize(session); } return totalBytes; }
     * 
     * public void flushBackgroundSessions() { HttpSession currentSession =
     * getCurrentSession(); List<HttpSession> sessions = new
     * CopyOnWriteArrayList<HttpSession>(getSessions()); for (HttpSession
     * session : sessions) { if (!session.equals(currentSession)) {
     * session.invalidate(); } } }
     * 
     * 
     * protected void calculateSizes(HttpSession session) {
     * 
     * 
     * }
     * 
     * 
     * protected int calculateSessionSize(HttpSession session) { int totalBytes
     * = 0; try { ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * ObjectOutputStream oos = new ObjectOutputStream(baos);
     * LOGGER.trace("Calculating session size for session id: " +
     * session.getId()); for (Iterator<String> iter = new
     * EnumerationIterator<String>(session.getAttributeNames());
     * iter.hasNext();) { String name = iter.next(); try {
     * oos.writeObject(session.getAttribute(name)); } catch
     * (NotSerializableException ex) {
     * LOGGER.warn("object is not serializable: {}", name); } int size =
     * baos.size(); LOGGER.trace("Session attribute name: " + name +
     * "; session attribute size: " + size + "b"); } oos.flush(); totalBytes =
     * baos.size(); oos.close();
     * 
     * LOGGER.trace("Total session size: " + totalBytes + "b"); } catch
     * (Exception e) { LOGGER.error("Could not get the session size", e);
     * totalBytes = -1; }
     * 
     * return totalBytes; }
     */

}
