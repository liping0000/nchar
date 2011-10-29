package net.wohlfart.charms.test.changerequest.faces;

import java.util.List;

import javax.faces.render.ResponseStateManager;

import net.wohlfart.changerequest.entities.ChangeRequestData;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.core.Expressions;
import org.jboss.seam.core.Manager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * 
 * starting a change request: - pushing into the toComplete list - pushing it to
 * the TQM team right away
 * 
 * @author Michael Wohlfart
 * 
 */
public class BulkStartBusinessKeyTest extends SeamFacesBase {
    
    public static final String START_BUSINESS_KEY_TEST = "StartBusinessKeyTest";

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(BulkStartBusinessKeyTest.class);

    // private final static int SUBMIT_COUNT = 1000;
    private final static int SUBMIT_COUNT = 10;

    private static int       beforeCount;
    private static int       afterCount;

    @Test
    public void testBusinessKeyRequest() throws Exception {

        final String cid = getDoLoginCid();
        Assert.assertNotNull(cid);

        // query the tasklist
        final String list1Cid = new NonFacesRequest("/pages/user/taskList.xhtml") {
            // to call the page actions
        }.run();
        
        deleteAllProcessInstances();


        final String list2Cid = new FacesRequest("/pages/user/taskList.xhtml", list1Cid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void renderResponse() {
                Assert.assertTrue(Manager.instance().isLongRunningConversation());
            };

            @Override
            protected void invokeApplication() throws Exception {
                final Session hibernateSesion = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class).getValue();
                Assert.assertNotNull(hibernateSesion);
                beforeCount = hibernateSesion.createQuery("from " + ChangeRequestData.class.getName()).list().size();
            };

        }.run(); // end user proc list
        Assert.assertNotNull(list2Cid);
        Assert.assertNotNull(list1Cid);

        for (int i = 0; i < SUBMIT_COUNT; i++) {
            final String loginCid = getDoLoginCid();
            Assert.assertNotNull(loginCid);

            final String startCid = getDoStartPageCid();
            submitRandomRequestToCreateBusinessKey(START_BUSINESS_KEY_TEST + " [" + i + "]", startCid);
        }

        // query the tasklist
        final String list3Cid = new NonFacesRequest("/pages/user/taskList.xhtml") {
            // to call the page actions
        }.run();

        final String list4Cid = new FacesRequest("/pages/user/taskList.xhtml", list3Cid) {

            @Override
            protected void beforeRequest() {
                setParameter(ResponseStateManager.VIEW_STATE_PARAM, "postback");
            }

            @Override
            protected void renderResponse() { // no long running conversation here
                Assert.assertTrue(Manager.instance().isLongRunningConversation());
            };

            @SuppressWarnings("unchecked")
            @Override
            protected void invokeApplication() throws Exception {
                final Session hibernateSession = Expressions.instance().createValueExpression("#{hibernateSession}", Session.class).getValue();
                Assert.assertNotNull(hibernateSession);

                hibernateSession.flush();

                afterCount = hibernateSession.createQuery("from " + ChangeRequestData.class.getName()).list().size();

                int counter = 0;
                // check if any of the error descriptions are empty:
                final List<ChangeRequestData> list = hibernateSession.createQuery("from " + ChangeRequestData.class.getName()).list();
                for (final ChangeRequestData data : list) {
                    Assert.assertFalse(StringUtils.isEmpty(data.getProblemDescription()));
                    Assert.assertFalse(StringUtils.isEmpty(data.getTitle()));
                    Assert.assertNotNull(data.getItemIdNumber());
                    Assert.assertNotNull(data.getChangeRequestProduct());
                    // some might actually not be submitted yet, who knows what
                    // kind of dump previous tests got here
                    // so we try to filter out own:

                    // System.err.println("#############################" +
                    // data.getTitle() + "###########################");
                    if (data.getTitle().startsWith("StartBusinessKeyTest")) {
                        counter++;
                        Assert.assertNotNull(data.getSubmitDate());
                        Assert.assertNotNull(data.getSubmitUser());
                    }
                }
                Assert.assertEquals(counter, SUBMIT_COUNT);
            };

        }.run(); // end user proc list
        Assert.assertNotNull(list4Cid);
        Assert.assertNotNull(list3Cid);

        // check the open tasks
        Assert.assertTrue((afterCount - beforeCount) == SUBMIT_COUNT);

        // we get random test failures here: java.lang.AssertionError: expected:<20> but was:<18>
        // for testBusinessKeyRequest();
        Assert.assertEquals(super.stopMailserver(), 2 * SUBMIT_COUNT);
        
        // cleanup the database
        deleteAllProcessInstances();
        
    } // end testBusinessKeyRequest()

}
