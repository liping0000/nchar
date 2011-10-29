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
public class Cancel2RequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(Cancel2RequestTest.class);

    @Test
    public void testCancel2Request() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToCreateBusinessKey("Cancel2RequestTest", startCid);

        final String cid1 = pickupLastTask();
        final String cid2 = assignToProcess(cid1);
        Assert.assertNotNull(cid2);

        final String cid3 = pickupLastTask();
        final String cid4 = processToCancel2(cid3);
        Assert.assertNotNull(cid4);

        Assert.assertEquals(super.stopMailserver(), 3);

    } // end testCompleteRequest()

}
