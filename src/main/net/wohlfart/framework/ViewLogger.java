package net.wohlfart.framework;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// disabled for now

// @Name("viewHistory")
// @AutoCreate
// @BypassInterceptors
// @Scope(ScopeType.APPLICATION)
public class ViewLogger {

    private final static Logger LOGGER = LoggerFactory.getLogger(ViewLogger.class);

    // @Observer("org.jboss.seam.beforePhase")
    public void before(final PhaseEvent event) {
        if (event.getPhaseId().equals(PhaseId.RENDER_RESPONSE)) {
            LOGGER.warn("phase event: {}, request: {}", event.toString(), event.getFacesContext().getExternalContext().getRequest());
        }
    }
}
