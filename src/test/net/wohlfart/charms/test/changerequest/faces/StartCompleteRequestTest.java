package net.wohlfart.charms.test.changerequest.faces;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * http://seamframework.org/Community/IntegrationTestNotRunningPageAction
 * 
 * 
 * starting a change request: - pushing into the toComplete list - pushing it to
 * the TQM team right away
 * 
 * @author Michael Wohlfart
 * 
 */
public class StartCompleteRequestTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(StartCompleteRequestTest.class);

    @Test
    public void testStartCompleteRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToComplete("CompleteRequestTest", startCid);

        // no email on the complete node
        Assert.assertEquals(super.stopMailserver(), 0);
    } // end testStartCompleteRequest()

}
