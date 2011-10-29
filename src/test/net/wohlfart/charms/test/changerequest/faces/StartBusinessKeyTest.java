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
public class StartBusinessKeyTest extends SeamFacesBase {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(StartBusinessKeyTest.class);

    @Test
    public void testBusinessKeyRequest() throws Exception {

        final String loginCid = getDoLoginCid();
        Assert.assertNotNull(loginCid);

        final String startCid = getDoStartPageCid();
        submitRandomRequestToCreateBusinessKey("StartBusinessKeyTest", startCid);

        // the tqm group has two members so there should be two emails
        Assert.assertEquals(super.stopMailserver(), 2);
    } // end testBusinessKeyRequest()

}
