package net.wohlfart.charms.test.changerequest.faces;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * starting a change request - pushing it to the toComplete list - picking it up
 * - pushing it to the TQM
 * 
 * @author Michael Wohlfart
 * 
 */
public class CompleteForwardRequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CompleteForwardRequestTest.class);

    @Test
    public void testCompleteForwardRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToComplete("CompleteReviewRequestTest", startCid);

        final String cid1 = pickupLastTask();
        Assert.assertNotNull(cid1);
        final String cid2 = completeToForward(cid1);
        Assert.assertNotNull(cid2);

        final String cid3 = pickupLastTask();
        Assert.assertNotNull(cid3);
        final String cid4 = completeToForward(cid3);
        Assert.assertNotNull(cid4);

        final String cid5 = pickupLastTask();
        Assert.assertNotNull(cid5);
        final String cid6 = completeToForward(cid5);
        Assert.assertNotNull(cid6);

        Assert.assertEquals(super.stopMailserver(), 3); // one email for each
                                                        // forwardee

    } // end testCompleteRequest()

}
