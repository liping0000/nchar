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
public class CompleteReviewRequest1Test extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CompleteRequestTest.class);

    @Test
    public void testCompleteReviewRequest1() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToComplete("CompleteReviewRequest1Test", startCid);

        final String cid1 = pickupLastTask();
        Assert.assertNotNull(cid1);
        // request 3 reviews
        final String cid2 = completeToReview(cid1);
        Assert.assertNotNull(cid2);
        final String cid3 = completeToReview(cid2);
        Assert.assertNotNull(cid3);
        final String cid4 = completeToReview(cid3);
        Assert.assertNotNull(cid4);

        Assert.assertEquals(super.stopMailserver(), 3); // one email for the
                                                        // reviewer

    } // end testCompleteRequest()

}
