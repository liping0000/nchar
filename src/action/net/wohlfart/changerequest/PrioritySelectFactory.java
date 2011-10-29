package net.wohlfart.changerequest;

import org.jboss.seam.ScopeType;

import java.io.Serializable;

import net.wohlfart.changerequest.entities.Priority;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(ScopeType.APPLICATION)
@Name("prioritySelectFactory")
public class PrioritySelectFactory implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(PrioritySelectFactory.class);

    @Factory(value = "prioritySelects", autoCreate = true, scope = ScopeType.CONVERSATION)
    public Priority[] getPrioritySelects() {
        LOGGER.info("returning priorities");
        // return an enumeration
        return Priority.values();
    }

}
