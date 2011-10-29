package net.wohlfart.charms.test.changerequest.faces;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * starting a change request - pushing it to the toComplete list - picking it up
 * - pushing it to the TQM - picking it up - assigning it to a PE - picking it
 * up - decission to realize it by PE - picking it up - push task to the handle
 * node by PE
 * 
 * @author Michael Wohlfart
 * 
 */
public class HandleRequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(HandleRequestTest.class);

    @Test
    public void testHandleRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);
        jobWasAdded();

        final String startCid = getDoStartPageCid();
        submitRandomRequestToCreateBusinessKey("HandleRequestTest", startCid);
        jobWasAdded();

        final String cid1 = pickupLastTask();
        final String cid2 = assignToProcess(cid1);
        jobWasAdded();
        Assert.assertNotNull(cid2);

        final String cid3 = pickupLastTask();
        final String cid4 = processToRealize(cid3);
        jobWasAdded();
        Assert.assertNotNull(cid4);

        final String cid5 = pickupLastTask();
        final String cid6 = realizeToHandle(cid5);
        jobWasAdded();
        Assert.assertNotNull(cid6);

        // the tqm group has two members so there should be two emails
        // plus one for the PE on the server
        // tqm team: 2
        // assign to pe: 1
        // assign to handler: 1
        Assert.assertEquals(super.stopMailserver(), 4);

    } // end testHandleRequest()

}
