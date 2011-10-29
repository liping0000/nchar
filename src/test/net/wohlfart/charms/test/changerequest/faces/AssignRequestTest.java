package net.wohlfart.charms.test.changerequest.faces;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * - starting a change request - pushing it to the toComplete list - picking it
 * up - pushing it to the TQM - picking it up - assigning it to a PE
 * 
 * this test keeps failing on hudson:
 * 
 * java.lang.AssertionError: expected:<3> but was:<2> at
 * net.wohlfart.charms.test
 * .changerequest.faces.AssignRequestTest.testAssignRequest
 * (AssignRequestTest.java:37)
 * 
 * @author Michael Wohlfart
 * 
 */
public class AssignRequestTest extends SeamFacesBase {

    @Test
    public void testAssignRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        org.testng.Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToCreateBusinessKey("AssignRequestTest", startCid);

        org.testng.Assert.assertEquals(super.stopMailserver(), 2);
        startMailServer();

        final String cid1 = pickupLastTask();
        final String cid2 = assignToProcess(cid1);
        org.testng.Assert.assertNotNull(cid2);

        //Thread.sleep(1000 * 20);

        Assert.assertEquals(super.stopMailserver(), 1); // fails on hudson here

    } // end testAssignRequest()

}
