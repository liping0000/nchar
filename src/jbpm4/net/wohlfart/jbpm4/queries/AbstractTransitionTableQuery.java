package net.wohlfart.jbpm4.queries;

import java.util.List;

import net.wohlfart.framework.AbstractTableQuery;
import net.wohlfart.jbpm4.entities.TransitionChoice;
import net.wohlfart.jbpm4.entities.TransitionData;
import net.wohlfart.jbpm4.node.TransitionConfig;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransitionTableQuery<R> extends AbstractTableQuery<R> {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractTransitionTableQuery.class);

    protected String            transitionName;
    protected TransitionConfig  transitionConfig;


    @Override
    public List<R> getResultList() {
        LOGGER.debug("getResultList called ");
        if (transitionName == null) {
            // no need to return anything if we don't know the transition yet
            return null;
        } else {
            return super.getResultList();
        }
    }

    @BypassInterceptors
    public void setTransitionName(final String transitionName) {
        LOGGER.debug("setting transitionName: " + transitionName);
        this.transitionName = transitionName;
        final TransitionChoice transitionChoice = (TransitionChoice) Component.getInstance(TransitionChoice.TRANSITION_CHOICE, true);
        LOGGER.debug("found transitionChoice: {}", transitionChoice);
        final TransitionData transitionData = transitionChoice.getTransitions().get(transitionName);
        LOGGER.debug("found transitionData: {}", transitionData);
        if (transitionData == null) {
            transitionConfig = null;
            LOGGER.warn("can't find transition config in transition choice object, can not provide a reliable userlist");
        } else {
            transitionConfig = transitionData.getConfig();
        }
    }

    @BypassInterceptors
    public String getTransitionName() {
        LOGGER.debug("getting transitionName: {}", transitionName);
        final TransitionChoice transitionChoice = (TransitionChoice) Component.getInstance(TransitionChoice.TRANSITION_CHOICE);
        LOGGER.debug("found transitionChoice: {}", transitionChoice);
        if (transitionChoice != null) {
            final TransitionData transitionData = transitionChoice.getTransitions().get(transitionName);
            LOGGER.debug("found transitionData: {} for {}", transitionData, transitionName);
        } else {
            LOGGER.warn("transitionChoice is null, name is {}",transitionName);
        }
        return transitionName;
    }

}
