package net.wohlfart.charms.test.changerequest.faces;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.render.ResponseStateManager;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.changerequest.ChangeRequestAction;
import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.changerequest.entities.Priority;
import net.wohlfart.charms.test.smtp.SmtpServer;
import net.wohlfart.email.entities.CharmsEmailTemplate;
import net.wohlfart.framework.queries.UserTaskTable;
import net.wohlfart.framework.queries.UserTaskTable.Row;
import net.wohlfart.jbpm4.entities.TransitionChoice;

import org.hibernate.Session;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.core.Expressions;
import org.jboss.seam.core.Manager;
import org.jboss.seam.mock.SeamTest;
import org.jbpm.api.ExecutionService;
import org.jbpm.api.ProcessInstance;
import org.jbpm.pvm.internal.cmd.CommandService;
import org.jbpm.pvm.internal.hibernate.DbSessionImpl;
import org.jbpm.pvm.internal.jobexecutor.AcquireJobsCmd;
import org.jbpm.pvm.internal.jobexecutor.JobExecutor;
import org.jbpm.pvm.internal.jobexecutor.JobParcel;
import org.jbpm.pvm.internal.processengine.ProcessEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class SeamFacesBase extends SeamTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(SeamFacesBase.class);

    // this variable is shared by all subclasses!!
    private Long taskDbid;

    private SmtpServer server;
    private JobExecutor jobExecutor;

    /*
     * FIXME: we are supposed to call startSeam before the integration tests
     * however this gives us exceptions check out why
     */

    @BeforeMethod
    @Override
    public void begin() {
        LOGGER.info("begin() called");
        super.begin();
        LOGGER.info("returned super.begin()");
        // start mailserver on port 6789
        // hudson hangs somewhere after this method call
        try {
            startMailServer(); // sets the jobExecutor variable
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //jobExecutor.stop(true);
        Assert.assertTrue(jobExecutor.isActive());
        LOGGER.info("begin() finished");
    };

    @AfterMethod
    @Override
    public void end() {
        super.end(); // to make sure this is called even after a failed assert

        // stop the mailserver in case it is still running:
        if (!server.isStopped()) {
            server.end();
        }
    }
    
    protected void jobWasAdded() throws InterruptedException {
        
        /*
        AcquireJobsCmd acquireJobsCommand = new AcquireJobsCmd(jobExecutor);
        Assert.assertNotNull(acquireJobsCommand);
        CommandService executor = jobExecutor.getCommandExecutor();
        Collection<Long> result = executor.execute(acquireJobsCommand);

        if (result != null) {
            JobParcel jobs = new JobParcel(executor, result);
            jobs.run();
        }
        */
        
        
        Assert.assertTrue(jobExecutor.isActive());
        jobExecutor.jobWasAdded();
        //Thread.sleep(3000);   
    }

    protected void startMailServer() throws InterruptedException {
        server = new SmtpServer(6789);
        server.begin();
        LOGGER.info("returned BetterSmtpServer.start(6789)");

        // Assert.assertTrue(!server.isStopped());
        LOGGER.info("BetterSmtpServer.start");
        jobExecutor = Expressions.instance().createValueExpression("#{jobExecutor}", JobExecutor.class).getValue();

        LOGGER.info("got jobExecutor");
        // make sure the job executor is running
        if (!jobExecutor.isActive()) {
            jobExecutor.start();
        }
    }

    public int stopMailserver() throws InterruptedException {
        // stop mailserver and job executor and check what we got here...

        // Session session = Expressions.instance()
        // .createValueExpression("#{hibernateSession}", Session.class)
        // .getValue();
        //
        // session.flush();

        Assert.assertTrue(jobExecutor.isActive());
        jobExecutor.jobWasAdded();
        //Thread.sleep(3000);
        Assert.assertTrue(jobExecutor.isActive());
        jobExecutor.jobWasAdded();
        //Thread.sleep(3000);
        Date next;
        final Date now = new Date();
        
        do {
            // date of the next job
            next = jobExecutor.getCommandExecutor().execute(jobExecutor.getNextDueDateCommand());
            // if there is no next job assume the job is now (in process)
            if (next == null) {
                next = new Date();
            }
            ;
            // get into the past to make sure
            final Calendar calendar = new GregorianCalendar();
            calendar.setTime(now);
            calendar.add(Calendar.SECOND, -120);
            now.setTime(calendar.getTime().getTime());

            // next job is still in the past, powercylce the job executor
            if (next.before(now)) {
                // powercycle the job executor to get the jobs done
                // jobExecutor.stop(true); // blocking for shutdown
                // jobExecutor.start();
                jobExecutor.jobWasAdded();
            }
            // if there is something more in the past to do we have to wait for
            // this
        } while (next.before(now));
        
        
        jobExecutor.stop(true); // blocking for shutdown
        Assert.assertFalse(jobExecutor.isActive());

        // stop mailserver and check what we got here...
        server.end();
        Assert.assertTrue(server.isStopped());
        return server.getReceivedEmailSize();

    }

    /***********************************************
     * log in
     */
    public String getDoLoginCid() throws Exception {
        final String loginCid = new FacesRequest("/pages/login.xhtml") {

            @Override
            protected void updateModelValues() throws Exception {
                setValue("#{credentials.username}", "devel");
                setValue("#{credentials.password}", "devel");
            }

            @Override
            protected void invokeApplication() {
                Assert.assertEquals(invokeMethod("#{identity.login}"), "loggedIn");
            }

        }.run(); // run login request
        Assert.assertNotNull(loginCid);
        return loginCid;
    }

    /***********************************************
     * call start form and verify long running conversation
     */
    public String getDoStartPageCid() throws Exception {
        final String startCid = new NonFacesRequest("/pages/wfl/changerequest/start.xhtml") {

            @Override
            protected void renderResponse() {
                Assert.assertTrue(Manager.instance().isLongRunningConversation());
            };

        }.run(); // call start page
        Assert.assertNotNull(startCid);
        return startCid;
    }

    /***********************************************
     * fill out start form and push to complete node
     * 
     * @throws Exception
     */
    public void submitRandomRequestToComplete(final String title, final String cid) throws Exception {
        final String newCid = new FacesRequest("/pages/wfl/changerequest/start.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void applyRequestValues() throws Exception {
                setParameter("cid", cid);
            };

            @Override
            protected void updateModelValues() throws Exception {
                setValue("#{changeRequestData.itemIdNumber}", "xxx");
                setValue("#{changeRequestProductSelect.productId}", new Long("1"));
                setValue("#{changeRequestData.title}", title);
                setValue("#{changeRequestData.problemDescription}", title);
            }

            @Override
            protected void invokeApplication() throws Exception {
                Assert.assertEquals(invokeMethod("#{changeRequestAction.initializeProcess('ChangeRequest','toComplete')}"), "toCompleteDone");
                jobExecutor.jobWasAdded();
            };

            @Override
            protected void renderResponse() {
                Assert.assertTrue(Manager.instance().isLongRunningConversation());
            };
        }.run(); // run submit request
        Assert.assertNotNull(newCid);
    }

    /***********************************************
     * fill out start form and push to create business key node
     * 
     * @throws Exception
     */
    public void submitRandomRequestToCreateBusinessKey(final String title, final String cid) throws Exception {
        final String newCid = new FacesRequest("/pages/wfl/changerequest/start.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void applyRequestValues() throws Exception {
                setParameter("cid", cid);
            };

            @Override
            protected void updateModelValues() throws Exception {
                setValue("#{changeRequestData.itemIdNumber}", "xxx");
                setValue("#{changeRequestProductSelect.productId}", new Long("1"));
                setValue("#{changeRequestData.title}", title);
                setValue("#{changeRequestData.problemDescription}", "title was: " + title);
            }

            @Override
            protected void invokeApplication() throws Exception {
                Assert.assertEquals(invokeMethod("#{changeRequestAction.initializeProcess('ChangeRequest','toCreateBusinessKey')}"), "toCreateBusinessKeyDone");
                jobExecutor.jobWasAdded();
            };

            @Override
            protected void renderResponse() {
                Assert.assertTrue(Manager.instance().isLongRunningConversation());
            };
        }.run(); // run submit request
        Assert.assertNotNull(newCid);
    }

    public String pickupLastTask() throws Exception {
        return pickupNthLastTask(0);
    }

    /***********************************************
     * pick up last tasks taskDbid
     * 
     * @throws Exception
     */
    public String pickupNthLastTask(final int position) throws Exception {

        final String list1Cid = new NonFacesRequest("/pages/user/taskList.xhtml") {
            // to call the page actions
        }.run();

        final String list2Cid = new FacesRequest("/pages/user/taskList.xhtml", list1Cid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void renderResponse() { // no long running conversation
                                              // here
                Assert.assertFalse(Manager.instance().isLongRunningConversation());
            };

            @Override
            protected void invokeApplication() throws Exception {
                final Session hibernateSesion = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class).getValue();
                Assert.assertNotNull(hibernateSesion);

                final UserTaskTable userTaskTable = Expressions.instance().createValueExpression("#{userTaskTable}", UserTaskTable.class).getValue();
                Assert.assertNotNull(userTaskTable);

                userTaskTable.setShowDraft(true); // we want to see the drafts
                userTaskTable.setOrderColumn("d.title");
                userTaskTable.setOrderColumn("t.createTime"); // the lastest
                                                              // draft
                final List<Row> list = userTaskTable.getResultList();
                final Row row = list.get(position);

                taskDbid = row.getDbid();
                Assert.assertNotNull(taskDbid);
                // System.err.println("taskDbid: " + taskDbid);

                // click the link for the first user task, start conversation
                invokeAction("/pages/user/taskList.html?taskDbid=" + taskDbid + "&actionOutcome=doTask");
            };

        }.run(); // end user proc list
        Assert.assertNotNull(list1Cid); // no conversation here

        Assert.assertNotNull(taskDbid);

        return list2Cid;
    }

    public String completeToCreateBusinessKey(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/complete.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        /**
         * faces request to invoke the toCreateBusinessKey() method
         */
        final String completeCid2 = new FacesRequest("/pages/wfl/changerequest/complete.xhtml", completeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {
                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");
                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toCreateBusinessKey')}"), "toCreateBusinessKeyDone");
                jobExecutor.jobWasAdded();
            };
        }.run();

        return completeCid2;
    }

    public String completeToDiscard(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/complete.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        /**
         * faces request to invoke the toCreateBusinessKey() method
         */
        final String completeCid2 = new FacesRequest("/pages/wfl/changerequest/complete.xhtml", completeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {
                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");
                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toDiscard')}"), "toDiscardDone");
                jobExecutor.jobWasAdded();
            };
        }.run();

        return completeCid2;
    }

    public String reviewToDone(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/review.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        /**
         * faces request to invoke the toCreateBusinessKey() method
         */
        final String completeCid2 = new FacesRequest("/pages/wfl/changerequest/review.xhtml", completeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {
                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");
                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('reviewed')}"), "reviewedDone");
                jobExecutor.jobWasAdded();
            };
        }.run();

        return completeCid2;
    }

    public String completeToReview(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/complete.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        /**
         * faces request to invoke the toCreateBusinessKey() method
         */
        final String completeCid2 = new FacesRequest("/pages/wfl/changerequest/complete.xhtml", completeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {
                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");
                final TransitionChoice transitionChoice = Expressions.instance().createValueExpression("#{transitionChoice}", 
                        TransitionChoice.class).getValue();
                Assert.assertNotNull(transitionChoice);
                //
                final CharmsUser user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNotNull(user);
                transitionChoice.getData("review").setReceiverUser(user);

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('review')}"), "reviewDone");
                jobExecutor.jobWasAdded();
            };
            
        }.run();
        Assert.assertNotNull(completeCid2); // no conversation here
        return completeCid2;
    }

    public String completeToForward(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/complete.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        /**
         * faces request to invoke the toCreateBusinessKey() method
         */
        final String completeCid2 = new FacesRequest("/pages/wfl/changerequest/complete.xhtml", completeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");
                
                // this is a page action
                // changeRequestAction.setTaskDbid(taskDbid + "");
                final TransitionChoice transitionChoice = Expressions.instance().createValueExpression("#{transitionChoice}", 
                        TransitionChoice.class).getValue();
                Assert.assertNotNull(transitionChoice);
                //
                final CharmsUser user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNotNull(user);
                transitionChoice.getData("forward").setReceiverUser(user);
                
                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('forward')}"), "forwardDone");
                jobExecutor.jobWasAdded();
            };
            
        }.run();
        Assert.assertNotNull(completeCid2); // no conversation here
        return completeCid2;
    }

    /****************************
     * pick up the task from taskDbid this should send out one email to the
     * assignee
     */
    public String assignToProcess(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/assign.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        /**
         * faces request to invoke the toCreateBusinessKey() method
         */
        final String completeCid2 = new FacesRequest("/pages/wfl/changerequest/assign.xhtml", completeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // "changeRequestAction is null");
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");

                final TransitionChoice transitionChoice = Expressions.instance().createValueExpression("#{transitionChoice}", TransitionChoice.class)
                        .getValue();
                org.testng.Assert.assertNotNull(transitionChoice, "transitionChoice is null");

                final ChangeRequestData changeRequestData = Expressions.instance().createValueExpression("#{changeRequestData}", ChangeRequestData.class)
                        .getValue();
                org.testng.Assert.assertNotNull(changeRequestData, "changeRequestData is null");


                // System.err.println("submitted business key: " +
                // changeRequestData.getBusinessKey());

                final CharmsUser user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNotNull(user);
                transitionChoice.getData("toProcess").setReceiverUser(user);

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toProcess')}"), "toProcessDone");
                jobExecutor.jobWasAdded();
            };
            
        }.run();
        Assert.assertNotNull(completeCid2); // no conversation here
        return completeCid2;
    }

    /****************************
     * pick up the task from taskDbid this should send out one email to the
     * assignee
     */
    public String assignToCancel1(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/assign.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        /**
         * faces request to invoke the toCreateBusinessKey() method
         */
        final String completeCid2 = new FacesRequest("/pages/wfl/changerequest/assign.xhtml", completeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction, "changeRequestAction is null");

                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");

                final ChangeRequestData changeRequestData = Expressions.instance().createValueExpression("#{changeRequestData}", ChangeRequestData.class)
                        .getValue();
                Assert.assertNotNull(changeRequestData, "changeRequestData is null");

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toCancel1')}"), "toCancel1Done");
                // Thread.sleep(Constants.delay); // 10 secs for the job
                // executor to finish its work
                jobExecutor.jobWasAdded();
            };
        }.run();

        return completeCid2;
    }

    public String processToRealize(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/process.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        // http://speedy.persman.de/charms/pages/wfl/changerequest/process.html?taskDbid=40&cid=483
        final String processCid = new FacesRequest("/pages/wfl/changerequest/process.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");

                final ChangeRequestData changeRequestData = Expressions.instance().createValueExpression("#{changeRequestData}", ChangeRequestData.class)
                        .getValue();
                Assert.assertNotNull(changeRequestData);

                // ChangeRequestFacetState changeRequestFacetState =
                // Expressions.instance()
                // .createValueExpression("#{changeRequestFacetState}",
                // ChangeRequestFacetState.class)
                // .getValue();

                // System.err.println("submitted business key: " +
                // changeRequestData.getBusinessKey());

                // CharmsUser user = (CharmsUser)
                // getValue("#{authenticatedUser}");

                // changeRequestFacetState.setProcessUserId(user.getId());

                // fIXME: enable this once we got the validation implemented:
                // Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toRealize')}"),
                // "invalid");

                // we have to assign some values in order to proceed
                changeRequestData.setCostA(true);
                changeRequestData.setCostB(false);
                changeRequestData.setPriority(Priority.NORMAL);

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toRealize')}"), /* "toImplementDone" */
                "toRealizeDone");
            };

        }.run(); // complete the task
        Assert.assertNotNull(processCid); // no conversation here

        return processCid;
    } // end testProcessRequest()

    public String processToCancel2(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String completeCid = new NonFacesRequest("/pages/wfl/changerequest/process.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(completeCid); // no conversation here

        // http://speedy.persman.de/charms/pages/wfl/changerequest/process.html?taskDbid=40&cid=483
        final String processCid = new FacesRequest("/pages/wfl/changerequest/process.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");

                final ChangeRequestData changeRequestData = Expressions.instance().createValueExpression("#{changeRequestData}", ChangeRequestData.class)
                        .getValue();
                Assert.assertNotNull(changeRequestData);

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toCancel2')}"), "toCancel2Done");
            };

        }.run(); // complete the task
        Assert.assertNotNull(processCid); // no conversation here

        return processCid;
    } // end testProcessRequest()

    public String realizeToFinish(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String realizeCid = new NonFacesRequest("/pages/wfl/changerequest/realize.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(realizeCid); // no conversation here

        // http://speedy.persman.de/charms/pages/wfl/changerequest/process.html?taskDbid=40&cid=483
        final String realizeCid2 = new FacesRequest("/pages/wfl/changerequest/realize.xhtml", realizeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");

                final ChangeRequestData changeRequestData = Expressions.instance().createValueExpression("#{changeRequestData}", ChangeRequestData.class)
                        .getValue();
                Assert.assertNotNull(changeRequestData);

                // ChangeRequestFacetState changeRequestFacetState =
                // Expressions.instance()
                // .createValueExpression("#{changeRequestFacetState}",
                // ChangeRequestFacetState.class)
                // .getValue();

                // System.err.println("submitted business key: " +
                // changeRequestData.getBusinessKey());

                // CharmsUser user = (CharmsUser)
                // getValue("#{authenticatedUser}");

                // changeRequestFacetState.setProcessUserId(user.getId());

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('toFinish')}"), "toFinishDone");
            };

        }.run(); // complete the task
        Assert.assertNotNull(realizeCid2);
        return realizeCid;
    }

    public String realizeToImplement(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String realizeCid = new NonFacesRequest("/pages/wfl/changerequest/realize.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(realizeCid); // no conversation here

        // http://speedy.persman.de/charms/pages/wfl/changerequest/process.html?taskDbid=40&cid=483
        final String realizeCid2 = new FacesRequest("/pages/wfl/changerequest/realize.xhtml", realizeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");

                final ChangeRequestData changeRequestData = Expressions.instance().createValueExpression("#{changeRequestData}", ChangeRequestData.class)
                        .getValue();
                Assert.assertNotNull(changeRequestData);

                final TransitionChoice transitionChoice = Expressions.instance().createValueExpression("#{transitionChoice}", TransitionChoice.class)
                        .getValue();
                Assert.assertNotNull(transitionChoice, "transitionChoice is null");

                // System.err.println("submitted business key: " +
                // changeRequestData.getBusinessKey());

                final CharmsUser user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNotNull(user);
                transitionChoice.getData("implement").setReceiverUser(user);

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('implement')}"), "implementDone");
                jobExecutor.jobWasAdded();
            };

        }.run(); // complete the task
        Assert.assertNotNull(realizeCid2); // no conversation here
        return realizeCid2;
    }

    public String realizeToHandle(final String cid) throws Exception {
        /**
         * GET request to trigger the page actions
         */
        final String realizeCid = new NonFacesRequest("/pages/wfl/changerequest/realize.xhtml", cid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", cid);
                setParameter("taskDbid", taskDbid + "");
            }

        }.run(); // complete the task
        Assert.assertNotNull(realizeCid); // no conversation here

        // http://speedy.persman.de/charms/pages/wfl/changerequest/process.html?taskDbid=40&cid=483
        final String realizeCid2 = new FacesRequest("/pages/wfl/changerequest/realize.xhtml", realizeCid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void invokeApplication() throws Exception {

                final ChangeRequestAction changeRequestAction = Expressions.instance()
                        .createValueExpression("#{changeRequestAction}", ChangeRequestAction.class).getValue();
                Assert.assertNotNull(changeRequestAction);
                // this is a page action
                changeRequestAction.setTaskDbid(taskDbid + "");

                final ChangeRequestData changeRequestData = Expressions.instance().createValueExpression("#{changeRequestData}", ChangeRequestData.class)
                        .getValue();
                Assert.assertNotNull(changeRequestData);

                // ChangeRequestFacetState changeRequestFacetState =
                // Expressions.instance()
                // .createValueExpression("#{changeRequestFacetState}",
                // ChangeRequestFacetState.class)
                // .getValue();

                final TransitionChoice transitionChoice = Expressions.instance().createValueExpression("#{transitionChoice}", TransitionChoice.class)
                        .getValue();
                Assert.assertNotNull(transitionChoice, "transitionChoice is null");

                // System.err.println("submitted business key: " +
                // changeRequestData.getBusinessKey());

                final CharmsUser user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNotNull(user);
                transitionChoice.getData("handle").setReceiverUser(user);

                Assert.assertEquals(invokeMethod("#{changeRequestAction.signal('handle')}"), "handleDone");
                jobExecutor.jobWasAdded();
            };

        }.run(); // complete the task
        Assert.assertNotNull(realizeCid2); // no conversation here
        return realizeCid2;
    }

    protected int countMailTemplates(final String templateName) {
        final Session hibernateSesion = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class).getValue();
        Assert.assertNotNull(hibernateSesion);

        final int result = hibernateSesion.getNamedQuery(CharmsEmailTemplate.FIND_BY_NAME).setParameter("name", templateName).list().size();

        return result;
    }
    
    
    @SuppressWarnings("unchecked")
    protected void deleteAllProcessInstances() {
        Lifecycle.beginCall();

        ProcessEngineImpl processEngine = (ProcessEngineImpl) Component.getInstance("processEngine");
        Session hibernateSession = (Session) Component.getInstance("hibernateSession");
        ExecutionService executionService = processEngine.getExecutionService();
        
        // delete process instances
        List<ProcessInstance> instances = executionService.createProcessInstanceQuery().list();
        Assert.assertNotNull(instances);
        //Assert.assertTrue(instances.size() > 0);
        for (ProcessInstance processInstance : instances) {        
            executionService.deleteProcessInstanceCascade(processInstance.getId());
        }
        
        // delete changerequest data
        final List<ChangeRequestData> list = hibernateSession.createQuery("from " + ChangeRequestData.class.getName()).list();
        for (final ChangeRequestData data : list) {
            hibernateSession.delete(data);
        }
        hibernateSession.flush();
        
        //org.jboss.seam.web.Session.instance().invalidate();
        Lifecycle.endCall();
    }

}
