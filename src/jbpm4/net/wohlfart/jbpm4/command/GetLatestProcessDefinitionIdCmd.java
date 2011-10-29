package net.wohlfart.jbpm4.command;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jbpm.api.JbpmException;
import org.jbpm.api.cmd.Command;
import org.jbpm.api.cmd.Environment;
import org.jbpm.pvm.internal.hibernate.DbSessionImpl;
import org.jbpm.pvm.internal.model.ProcessDefinitionImpl;
import org.jbpm.pvm.internal.repository.DeploymentProperty;
import org.jbpm.pvm.internal.session.DbSession;
import org.jbpm.pvm.internal.session.RepositorySession;


public class GetLatestProcessDefinitionIdCmd implements Command<String> {
    
    private String processDefinitionName;

    public GetLatestProcessDefinitionIdCmd(final String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    @Override
    public String execute(Environment environment) throws Exception {
        DbSessionImpl dbSession = environment.get(DbSessionImpl.class);
        Session session = dbSession.getSession();
        
        DeploymentProperty deploymentProperty =
        
        (DeploymentProperty) session.createCriteria(DeploymentProperty.class)
            .add(Restrictions.eq("objectName", processDefinitionName))
            .add(Restrictions.eq("key", "pdid"))
            .createAlias("deployment", "d")
            .addOrder(Order.desc("d.timestamp"))
            .setFirstResult(0)
            .setMaxResults(1)
            .uniqueResult();

        return deploymentProperty.getStringValue();
    }
    
    

    

//  RepositorySession repoSession;        
//  ProcessDefinitionImpl processDefinition = repoSession.findLatestProcessDefinitionByName( processDefinitionName);
//  return processDefinition.getId();


}
