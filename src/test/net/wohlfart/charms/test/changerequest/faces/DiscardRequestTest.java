package net.wohlfart.charms.test.changerequest.faces;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * starting a change request - pushing it to the toComplete list - picking it up
 * - discarding it
 * 
 * @author Michael Wohlfart
 * 
 */
public class DiscardRequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(CompleteRequestTest.class);

    @Test
    public void testDiscardRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToComplete("DiscardRequestTest", startCid);

        final String cid1 = pickupLastTask();
        final String cid2 = completeToDiscard(cid1);
        Assert.assertNotNull(cid2);

        Assert.assertEquals(super.stopMailserver(), 0);

    } // end testCompleteRequest()

}
