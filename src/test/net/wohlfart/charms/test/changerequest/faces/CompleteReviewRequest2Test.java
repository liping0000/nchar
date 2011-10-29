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
public class CompleteReviewRequest2Test extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CompleteRequestTest.class);

    @Test
    public void testCompleteReviewRequest2() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToComplete("CompleteReviewRequest2Test", startCid);

        final String cid1 = pickupLastTask();
        Assert.assertNotNull(cid1);
        // request 3 reviews
        final String cid2 = completeToReview(cid1);
        Assert.assertNotNull(cid2);
        final String cid3 = completeToReview(cid2);
        Assert.assertNotNull(cid3);
        final String cid4 = completeToReview(cid3);
        Assert.assertNotNull(cid4);

        // now jump back to the tasklist and check if we can submit again...
        final String cid5 = pickupNthLastTask(3); // counting starts at 0 !
        Assert.assertNotNull(cid5);
        // request 2 more reviews
        final String cid6 = completeToReview(cid5);
        Assert.assertNotNull(cid6);
        final String cid7 = completeToReview(cid6);
        Assert.assertNotNull(cid7);

        Assert.assertEquals(super.stopMailserver(), 5); // one email for the
                                                        // reviewer

    } // end testCompleteRequest()

}
