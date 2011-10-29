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
 * up - decission to realize it by PE - picking it up - push task to the
 * implement node by PE
 * 
 * @author Michael Wohlfart
 * 
 */
public class ImplementRequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(ImplementRequestTest.class);

    @Test
    public void testImplementRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToCreateBusinessKey("ImplementRequestTest", startCid);

        final String cid1 = pickupLastTask();
        final String cid2 = assignToProcess(cid1);
        Assert.assertNotNull(cid2);

        final String cid3 = pickupLastTask();
        final String cid4 = processToRealize(cid3);
        Assert.assertNotNull(cid4);

        final String cid5 = pickupLastTask();
        final String cid6 = realizeToImplement(cid5);
        Assert.assertNotNull(cid6);

        Assert.assertEquals(super.stopMailserver(), 4);

    } // end testImplementRequest()

}
