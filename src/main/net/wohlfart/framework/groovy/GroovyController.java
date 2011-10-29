package net.wohlfart.framework.groovy;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Scope(CONVERSATION)
// @Name("terminalController")
// @AutoCreate
// @Controller(name = "terminalController", scope = Scope.REQUEST)
@Scope(ScopeType.CONVERSATION)
@Name("groovyController")
public class GroovyController implements Serializable {


    private final static Logger LOGGER = LoggerFactory.getLogger(GroovyController.class);

    @Create
    public void create() {
        LOGGER.debug("creating GroovyController");
    }

    @Destroy
    public void destroy() {
        LOGGER.debug("destroying GroovyController");
    }

    @WebRemote
    public String handleCommand(final String commandLine) {
        LOGGER.debug("handle command invoked for: >{}<", commandLine);

        try {

            return "nothing";
        } catch (final Exception ex) {
            ex.printStackTrace();
            return ex.toString();
        }
    }

}
