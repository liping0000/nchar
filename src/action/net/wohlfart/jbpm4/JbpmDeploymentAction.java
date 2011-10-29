package net.wohlfart.jbpm4;

import org.jboss.seam.ScopeType;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import net.wohlfart.AbstractActionBean;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jbpm.api.JbpmException;
import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.repository.DeploymentImpl;
import org.jbpm.pvm.internal.repository.DeploymentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: this class is not used atm
@Scope(ScopeType.CONVERSATION)
@Name("jbpmDeploymentAction")
public class JbpmDeploymentAction extends AbstractActionBean implements Serializable {

    private final static Logger LOGGER = LoggerFactory.getLogger(JbpmDeploymentAction.class);

    @In(value = "hibernateSession")
    private Session             hibernateSession;

    @In(value = "processEngine")
    private ProcessEngine       processEngine;

    private Long                deploymentId;

    // private List<MailTemplateItemSet> mailTemplates;

    private String              pdId;

    private String              pdKey;

    private Long                pdVersion;

    private String              name;

    // FIXME: add checks here
    public void setJbpmDeploymentId(final Long deploymentId) {
        this.deploymentId = deploymentId;
    }

    // called from a page action
    public void refresh() {
        LOGGER.info("refresh called ");
        setupProperties();
    }

    private void setupProperties() {
        // derby is picky about rereading blobs in the entityManager, so we need
        // to get the blob data again here
        final DeploymentImpl deployment = (DeploymentImpl) hibernateSession.get(DeploymentImpl.class, deploymentId);
        // setup some properties
        final Set<DeploymentProperty> properties = deployment.getObjectProperties();
        final Iterator<DeploymentProperty> iter = properties.iterator();
        while (iter.hasNext()) {
            final DeploymentProperty prop = iter.next();
            if (StringUtils.equals(DeploymentImpl.KEY_PROCESS_DEFINITION_ID, prop.getKey())) {
                pdId = prop.getStringValue();
            } else if (StringUtils.equals(DeploymentImpl.KEY_PROCESS_DEFINITION_KEY, prop.getKey())) {
                pdKey = prop.getStringValue();
            } else if (StringUtils.equals(DeploymentImpl.KEY_PROCESS_DEFINITION_VERSION, prop.getKey())) {
                pdVersion = prop.getLongValue();
            }
        }
    }

    // private void setupMailTemplate() {
    // String definition = jbpmDeploymentAdaptor.getJbpmDeploymentDokument();
    // //mailTemplates = new ProcessDefinitionWrapper(definition)
    // .getMailTemplates();
    // }

    // --- getter and setter

    @BypassInterceptors
    public String getPdId() {
        return pdId;
    }

    @BypassInterceptors
    public String getPdKey() {
        return pdKey;
    }

    @BypassInterceptors
    public Long getPdVersion() {
        return pdVersion;
    }

    // the only setter here, we might want to change the name of the deployment
    @BypassInterceptors
    public void setName(final String name) {
        this.name = name;
    }

    @BypassInterceptors
    public String getName() {
        return name;
    }

    // --- actions

    @Transactional
    public String persist() {
        throw new IllegalArgumentException("persist not supported, use upload");
    }

    // FIXME: the old version is still in the context, we need to remove it
    // somehow...
    @Transactional
    public String update() {
        // we lose all blobs on updating the dployment in DB with the entity manager
        // entityManager.persist(deployment); entityManager.clear();

        // manually update the properties, not sure what is going on with the
        // entityManager at this point
        hibernateSession.createQuery("update " 
                + DeploymentImpl.class.getName() 
                + " set name = :name" 
                + " where dbid = :dbid ")
                .setParameter("name", name)
                .setParameter("dbid", deploymentId)
                .executeUpdate();

        // RepositoryService repositoryService = processEngine.getRepositoryService();
        // repositoryService.
        //
        hibernateSession.flush();
        return "updated";
    }

    // removes just the process keeping the history data
    @Transactional
    public String remove() {
        try {
            // remove if there are no open executions, history information stays
            processEngine.getRepositoryService().deleteDeployment(Long.toString(deploymentId));
            hibernateSession.flush();
            return "removed";
        } catch (final JbpmException ex) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.ERROR, "Error: " + ex.toString());
            ex.printStackTrace();
            return "invalid";
        }
    }

    // removes any history and process data
    @Transactional
    public String removeAll() {
        try {
            // remove brute force including history and running instances
            processEngine.getRepositoryService().deleteDeploymentCascade(Long.toString(deploymentId));
            hibernateSession.flush();
            return "removedAll";
        } catch (final JbpmException ex) {
            final FacesMessages facesMessages = FacesMessages.instance();
            facesMessages.add(Severity.ERROR, "Error: " + ex.toString());
            ex.printStackTrace();
            return "invalid";
        }
    }

    @Transactional
    public String cancel() {
        // FIXME: not sure if we need this for all cancel() calls
        // since we lose the blobs here when not clearing the entityManager :-/
        // entityManager.clear();
        hibernateSession.flush();
        return "canceled";
    }

    /**
     * the faces trace API uses the toString Method to display some information
     * about the components in the UI we need to make sure Seam's Bijection
     * doesn't kick in and gives us an exception
     */
    @Override
    @BypassInterceptors
    public String toString() {
        return super.toString();
    }

}
