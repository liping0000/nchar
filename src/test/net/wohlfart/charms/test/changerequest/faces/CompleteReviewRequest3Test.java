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
public class CompleteReviewRequest3Test extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CompleteRequestTest.class);

    @Test
    public void testCompleteReviewRequest3() throws Exception {

        // login
        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        // start a request and save as draft
        final String startCid = getDoStartPageCid();
        submitRandomRequestToComplete("CompleteReviewRequest3Test", startCid);

        // pickup the draft
        final String cid1 = pickupLastTask();
        Assert.assertNotNull(cid1);

        // request one review
        final String cid2 = completeToReview(cid1);
        Assert.assertNotNull(cid2);

        // now jump back to the tasklist and pickup the review
        final String cid3 = pickupNthLastTask(0); // counting starts at 0 !
        Assert.assertNotNull(cid3);

        // finish the review
        final String cid4 = reviewToDone(cid3);
        Assert.assertNotNull(cid4);

        // pickup the draft again
        final String cid5 = pickupLastTask();
        Assert.assertNotNull(cid5);

        // submit it
        final String cid6 = completeToCreateBusinessKey(cid5);
        Assert.assertNotNull(cid6);

        // one for the reviewer and one for each TQM, plus one for the complete
        // mail
        Assert.assertEquals(super.stopMailserver(), 4);

    } // end testCompleteRequest()

}
