package net.wohlfart.authorization.identifier;

import org.jboss.seam.security.permission.IdentifierStrategy;
import org.jbpm.pvm.internal.task.TaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * see: http://java.dzone.com/articles/acl-security-in-seam?page=0,2#viewSource
 * 
 * this class identifies a task by its name
 * 
 * @author Michael Wohlfart
 * 
 */
public class TaskNameTargetIdentifier implements IdentifierStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(TaskNameTargetIdentifier.class);

    @Override
    @SuppressWarnings("rawtypes")
    public boolean canIdentify(final Class targetClass) {
        return targetClass.equals(TaskImpl.class);
    }

    @Override
    public String getIdentifier(final Object target) {
        if ((target == null) || (!(target instanceof TaskImpl))) {
            LOGGER.warn("invalid identifier call, object is {} of class {}, must be TaskImpl", target, target == null ? "null" : target.getClass());
            return "null";
        }
        final TaskImpl task = (TaskImpl) target;
        final String name = task.getName();
        LOGGER.info("identified task with name: {}", name);
        return name;
    }

}
