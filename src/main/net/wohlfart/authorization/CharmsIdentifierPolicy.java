package net.wohlfart.authorization;


import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.DEPLOYMENT;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.security.permission.Identifier;
import org.jboss.seam.security.permission.ClassIdentifierStrategy;
import org.jboss.seam.security.permission.EntityIdentifierStrategy;
import org.jboss.seam.security.permission.IdentifierPolicy;
import org.jboss.seam.security.permission.IdentifierStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A policy for the generation of object "identifiers" - unique Strings that
 * identify a specific instance of an object. A policy can consist of numerous
 * identifier strategies, each with the ability to generate identifiers for
 * specific classes of objects.
 * 
 * We override Seam's own Identifier Policy since it is not Serializable
 * 
 * @author Shane Bryzak
 */
@Name("org.jboss.seam.security.identifierPolicy")
@Scope(APPLICATION)
@BypassInterceptors
@Install(precedence = DEPLOYMENT, value = true)
public class CharmsIdentifierPolicy extends IdentifierPolicy implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsIdentifierPolicy.class);

    @SuppressWarnings("rawtypes")
    private final Map<Class, IdentifierStrategy> strategies = new ConcurrentHashMap<Class, IdentifierStrategy>();

    private Set<IdentifierStrategy> registeredStrategies = new HashSet<IdentifierStrategy>();

    @Override
    @Create
    public void create() {
        LOGGER.info("CharmsIdentifierPolicy created...");
        if (registeredStrategies.isEmpty()) {
            registeredStrategies.add(new EntityIdentifierStrategy());
            registeredStrategies.add(new ClassIdentifierStrategy());
        }
    }

    @Override
    public String getIdentifier(final Object target) {
        if (target instanceof String) {
            return (String) target;
        }

        IdentifierStrategy strategy = strategies.get(target.getClass());

        if (strategy == null) {
            if (target.getClass().isAnnotationPresent(Identifier.class)) {
                final Class<? extends IdentifierStrategy> strategyClass = target.getClass().getAnnotation(Identifier.class).value();

                if (strategyClass != IdentifierStrategy.class) {
                    try {
                        strategy = strategyClass.newInstance();
                        strategies.put(target.getClass(), strategy);
                    } catch (final Exception ex) {
                        throw new RuntimeException("Error instantiating IdentifierStrategy for object " + target, ex);
                    }
                }
            }

            for (final IdentifierStrategy s : registeredStrategies) {
                if (s.canIdentify(target.getClass())) {
                    strategy = s;
                    strategies.put(target.getClass(), strategy);
                    break;
                }
            }
        }

        return strategy != null ? strategy.getIdentifier(target) : null;
    }

    @Override
    public Set<IdentifierStrategy> getRegisteredStrategies() {
        return registeredStrategies;
    }

    @Override
    public void setRegisteredStrategies(final Set<IdentifierStrategy> registeredStrategies) {
        this.registeredStrategies = registeredStrategies;
    }
}
