package net.wohlfart.authentication;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.security.management.IdentityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends the seam specific IdentityManager; all work is delegated
 * to the backend classes IdentityStore and RoldIdentityStore with their Charms
 * specific implementations, the only work the IdentityManager is doing is
 * checking for permission on method invocation like this:
 * Identity.instance().checkPermission(USER_PERMISSION_NAME, PERMISSION_UPDATE);
 * or likewise for other actions
 * 
 * This makes this class basically a facade for a configurable authentication
 * backend, note that all methods on IdentityManager are using strings while the
 * backend use the CharmsUser and CharmsRole Objects.
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(ScopeType.EVENT)
@Name("org.jboss.seam.security.identityManager")
@Install(precedence = Install.APPLICATION)
@BypassInterceptors
@AutoCreate
public class CharmsIdentityManager extends IdentityManager {


    private final static Logger LOGGER = LoggerFactory.getLogger(CharmsIdentityManager.class);

    /**
     * parent contains the method:
     * 
     * @Create 
     * public void create() { initIdentityStore(); [...]
     * 
     * so this is called when the bean is created, the main action  
     * happens in the identity stores
     */
    @Override
    protected void initIdentityStore() {
        if (getIdentityStore() == null) {
            LOGGER.error("please configure an IdentityStore for {}", this);
        }
        if (getRoleIdentityStore() == null) {
            LOGGER.error("please configure an RoleIdentityStore for {}", this);
        }
    }

    /**
     * this is the single entry point for user creation there must be no other
     * place to create users for this application whatsoever
     * 
     * synchronized, but should operate in a transaction anyways
     */
    @Override
    synchronized public boolean createUser(
            final String name, 
            final String password, 
            final String firstname, 
            final String lastname) {
        
        return super.createUser(name, password, firstname, lastname);
    }

}
