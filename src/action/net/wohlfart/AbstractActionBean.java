package net.wohlfart;

import java.io.Serializable;

import net.wohlfart.authentication.CharmsIdentityManager;

import org.apache.lucene.analysis.Tokenizer;
import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.security.management.IdentityManager;

/**
 * base class for all actions, adding some common features once the actions are
 * all implemented and stable
 * 
 * @author Michael Wohlfart
 */
public class AbstractActionBean implements Serializable {

    /*
     * basic tips to implement actions:
     * 
     * seam injection is very slow and kills performance,
     * instead you might be better off with some of these implementation details:
     * 
     * 
     * * instead of injecting faces message, use:
     *   final FacesMessages facesMessages = FacesMessages.instance();
     * 
     * 
     * * instead of injecting hibernate session, use:
     *   session = (Session) Component.getInstance("hibernateSession");
     *   in hibernate home subclasses you can use:
     *   Session session = getSession();
     *
     *
     * * instead of injectiong conversation use:
     *   final Conversation conversation = (Conversation) Component.getInstance("org.jboss.seam.core.conversation");
     *   check for long running
     *   System.out.println(conversation.isLongRunning());
     *   --> conversation might be null though!
     *
     *
     * * identityManager
     *   IdentityManager identityManager = (IdentityManager) Component.getInstance(CharmsIdentityManager.class);
     * 
     * * processEngine
     *   ProcessEngine processEngine = (ProcessEngine) Component.getInstance("processEngine");
     *   
     * * current user:
     *   CharmsUser charmsUser = (CharmsUser) Component.getInstance("authenticatedUser");
     * 
     */

}
