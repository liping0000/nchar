package net.wohlfart.authorization;

import static org.jboss.seam.ScopeType.STATELESS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.wohlfart.authorization.entities.CharmsPermissionTarget;
import net.wohlfart.authorization.entities.CharmsTargetAction;
import net.wohlfart.authorization.targets.CharmsAdminTargetSetup;
import net.wohlfart.authorization.targets.CharmsChartTargetSetup;
import net.wohlfart.authorization.targets.CharmsDevelTargetSetup;
import net.wohlfart.authorization.targets.CharmsReportTargetSetup;
import net.wohlfart.authorization.targets.CharmsSearchTargetSetup;
import net.wohlfart.authorization.targets.CharmsWorkflowTargetSetup;
import net.wohlfart.authorization.targets.IPermissionTargetFactory;
import net.wohlfart.authorization.targets.SeamRoleTargetSetup;
import net.wohlfart.authorization.targets.SeamUserTargetSetup;

import org.hibernate.Session;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * setup some test accounts
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(STATELESS)
@Name("permissionTargetSetup")
public class PermissionTargetSetup implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(PermissionTargetSetup.class);

    @In(value = "hibernateSession")
    private Session hibernateSession;

    @SuppressWarnings("rawtypes")
    @Transactional
    public void startup() {
        
        for (String classname : IPermissionTargetFactory.permissionTargetFactories) {
            try {
                Class clazz = Class.forName(classname);
                IPermissionTargetFactory targetFactory = (IPermissionTargetFactory) clazz.newInstance();
                addPermissionTarget(targetFactory.getPermissionTarget());
            } catch (ClassNotFoundException ex) {
                LOGGER.warn("PermissionTargetFactory not found", ex);
            } catch (InstantiationException ex) {
                LOGGER.warn("can't instanciate PermissionTargetFactory", ex);
            } catch (IllegalAccessException ex) {
                LOGGER.warn("can't instanciate PermissionTargetFactory", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addPermissionTarget(final CharmsPermissionTarget pTarget) {
        LOGGER.debug("adding permission for traget: {}", pTarget);

        final List<CharmsPermissionTarget> list = hibernateSession
            .getNamedQuery(CharmsPermissionTarget.FIND_BY_TARGET_STRING)
            .setParameter("targetString", pTarget.getTargetString())
            .list();

        if (list.size() == 0) {
            // not yet in the database, we can persist it
            hibernateSession.persist(pTarget);
            for (final CharmsTargetAction action : pTarget.getActions()) {
                hibernateSession.persist(action);
            }
        } else if (list.size() == 1) {
            // already in the DB, just add the actions
            final CharmsPermissionTarget dbTarget = list.get(0);
            // get the names of the already stored actions 
            // FIXME: we could use a set for this I suppose
            final List<String> dbNames = new ArrayList<String>();
            for (final CharmsTargetAction action : dbTarget.getActions()) {
                dbNames.add(action.getName());
            }

            for (final CharmsTargetAction action : pTarget.getActions()) {
                if (!dbNames.contains(action.getName())) {
                    dbTarget.addAction(action);
                    hibernateSession.persist(dbTarget);
                }
            }
        }
    }

}
