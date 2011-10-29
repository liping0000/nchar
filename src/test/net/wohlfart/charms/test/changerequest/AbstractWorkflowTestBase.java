package net.wohlfart.charms.test.changerequest;

import java.util.Collection;
import java.util.List;

import javax.faces.application.FacesMessage;

import net.wohlfart.authentication.CharmsUserIdentityStore;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.ChangeRequestAction;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.ChangeRequestFacetState;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.TypedValue;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.mock.SeamTest;
import org.jboss.seam.security.Identity;
import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.cmd.CommandService;
import org.jbpm.pvm.internal.jobexecutor.AcquireJobsCmd;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.jobexecutor.JobParcel;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class AbstractWorkflowTestBase extends SeamTest {

    protected String USERNAME = "testrunner2";
    protected String PASSWORD = "testrunner2";  

    protected Session hibernateSession;

    protected ChangeRequestAction changeRequestAction;

    protected ChangeRequestFacetState changeRequestFacetState;

    protected ProcessEngine processEngine;
    
    protected JobExecutor jobExecutor;

    // the logged in user
    protected CharmsUser charmsUser;

    protected static final String PROCESS_KEY = "ChangeRequest";

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        Identity.setSecurityEnabled(false);
        
        Lifecycle.beginCall();
        Lifecycle.beginSession(session.getAttributes(), null);

        // find the hibernate session
        hibernateSession = (Session) Component.getInstance("hibernateSession");

        // find the admin user
        final CharmsUserIdentityStore charmsUserIdentityStore = (CharmsUserIdentityStore) Component.getInstance(CharmsUserIdentityStore.CHARMS_USER_IDENTITY_STORE);
        if (!charmsUserIdentityStore.userExists(USERNAME)) {
            if (!charmsUserIdentityStore.createUser(USERNAME, PASSWORD)) {
                Assert.fail("test user doesn't exist and can't be created");
            }
            hibernateSession.flush();
        }

        if (!charmsUserIdentityStore.authenticate(USERNAME, PASSWORD)) {
            Assert.fail("can't authenticate test user");
        }

        // find the process engine
        processEngine = (ProcessEngine) Component.getInstance("processEngine");

        // make sure the job executor is offline
        final Object object = processEngine.get("jobExecutor");
        Assert.assertNotNull(object);
        jobExecutor = (JobExecutor) object;
        if (!jobExecutor.isActive()) {
            jobExecutor.stop(true);
        }

        // entity under test
        changeRequestAction = (ChangeRequestAction) Component.getInstance("changeRequestAction");
        Assert.assertNotNull(changeRequestAction, "changeRequestAction is null");
    };

    @SuppressWarnings("unchecked")
    @AfterMethod
    @Override
    public void end() {
        // remove any data we created for the user

        
        final CharmsUserIdentityStore charmsUserIdentityStore = (CharmsUserIdentityStore) Component.getInstance(CharmsUserIdentityStore.CHARMS_USER_IDENTITY_STORE);
        // remove the user
        if (charmsUserIdentityStore.userExists(USERNAME)) {
            
            // remove all ChangeRequestData
            List<ChangeRequestData> list = hibernateSession.createCriteria(ChangeRequestData.class).list();
            for (ChangeRequestData data : list) {
                // FIXME: this deletes all data from the DB ChangeRequest table 
                // we need to check for USERNAME in he user fields... if (data.)
                hibernateSession.delete(data);
            }
                           
            if (!charmsUserIdentityStore.deleteUser(USERNAME)) {
                Assert.fail("can't delete test user");
            }
            hibernateSession.flush();
            
        } else {
            Assert.fail("test user doesn't exist, should have been created in the begin method");
        }
               
        session.invalidate();
        session = null;
        //super.end();
    };
    
    public void removeExecution(String processInstanceId) {
        hibernateSession.flush();
        //processEngine.getExecutionService().deleteProcessInstanceCascade(processInstanceId);
        processEngine.getExecutionService().deleteProcessInstance(processInstanceId);
        hibernateSession.flush();
    }

    protected void runJobs() {
        hibernateSession.flush();
        
        // 13 on openJDK 14 on Sun's JDK it seems...
        //Assert.assertEquals(Thread.activeCount(), 13);
        
        // Command<Collection<Long>> command = jobExecutor.getAcquireJobsCommand();
        AcquireJobsCmd acquireJobsCommand = new AcquireJobsCmd(jobExecutor);
        Assert.assertNotNull(acquireJobsCommand);
        CommandService executor = jobExecutor.getCommandExecutor();
        Collection<Long> result = executor.execute(acquireJobsCommand);

        if (result != null) {
            JobParcel jobs = new JobParcel(executor, result);
            jobs.run();
        }
        hibernateSession.flush();
    }

}
