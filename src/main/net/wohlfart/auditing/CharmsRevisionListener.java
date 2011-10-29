package net.wohlfart.auditing;

import java.io.Serializable;
import java.security.Principal;

import net.wohlfart.authentication.CharmsIdentity;

import org.hibernate.envers.RevisionListener;
import org.jboss.seam.Component;

public class CharmsRevisionListener implements RevisionListener, Serializable {


    private static final String NO_SESSION_KEY   = "charms (no session)";
    private static final String NO_IDENTITY_KEY  = "charms (no identity)";
    private static final String NO_PRINCIPAL_KEY = "charms (no principal)";
    private static final String NO_NAME_KEY      = "charms (no name)";

    @Override
    public void newRevision(final Object revEntity) {
        final CharmsRevisionEntity charmsRevisionEntity = (CharmsRevisionEntity) revEntity;

        // check if the session context for the identity component is active
        if (!org.jboss.seam.contexts.Contexts.isSessionContextActive()) {
            charmsRevisionEntity.setUsername(NO_SESSION_KEY);
            return;
        }

        // get the identity
        final CharmsIdentity identity = (CharmsIdentity) Component.getInstance("org.jboss.seam.security.identity");
        if (identity == null) {
            charmsRevisionEntity.setUsername(NO_IDENTITY_KEY);
            return;
        }

        // get the principal
        final Principal principal = identity.getPrincipal();
        if (principal == null) {
            charmsRevisionEntity.setUsername(NO_PRINCIPAL_KEY);
            return;
        }

        // get the principal's name
        final String name = principal.getName();
        if (name == null) {
            charmsRevisionEntity.setUsername(NO_NAME_KEY);
            return;
        }

        charmsRevisionEntity.setUsername(name);
    }

}
