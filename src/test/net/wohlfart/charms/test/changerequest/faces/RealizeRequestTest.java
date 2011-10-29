package net.wohlfart.charms.test.changerequest.faces;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * starting a change request 
 * - pushing it to the toComplete list 
 * - picking it up
 * - pushing it to the TQM 
 * - picking it up 
 * - assigning it to a PE 
 * - picking it up 
 * - decision to realize it by PE 
 * - picking it up 
 * - decision to finish it by PE
 * 
 * @author Michael Wohlfart
 * 
 */
public class RealizeRequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(RealizeRequestTest.class);

    @Test
    public void testRealizeRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToCreateBusinessKey("ProcessRequestTest", startCid);

        final String cid1 = pickupLastTask();
        final String cid2 = assignToProcess(cid1);
        Assert.assertNotNull(cid2);

        final String cid3 = pickupLastTask();
        final String cid4 = processToRealize(cid3);
        Assert.assertNotNull(cid4);

        final String cid5 = pickupLastTask();
        final String cid6 = realizeToFinish(cid5);
        Assert.assertNotNull(cid6);

        // the tqm group has two members so there should be two emails
        // plus one for the PE on the server
        // tqm team: 2
        // assign to pe: 1
        // decission to realize doesn't generate an email
        Assert.assertEquals(super.stopMailserver(), 3);
    } // end testRealizeRequest()

}
