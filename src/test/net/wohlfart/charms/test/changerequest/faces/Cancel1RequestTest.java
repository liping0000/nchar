package net.wohlfart.charms.test.changerequest.faces;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * - starting a change request - pushing it to the toComplete list - picking it
 * up - pushing it to the TQM - picking it up - cancel it
 * 
 * @author Michael Wohlfart
 * 
 */
public class Cancel1RequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(Cancel1RequestTest.class);

    @Test
    public void testCancel1Request() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToCreateBusinessKey("Cancel1RequestTest", startCid);

        final String cid1 = pickupLastTask();
        final String cid2 = assignToCancel1(cid1);
        Assert.assertNotNull(cid2);

        Assert.assertEquals(super.stopMailserver(), 2);

    } // end testCompleteRequest()

}
