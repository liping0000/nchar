package net.wohlfart.charms.test.changerequest.faces;

import net.wohlfart.authentication.entities.CharmsUser;

import org.jboss.seam.mock.SeamTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Testing login validation right/wrong login
 * 
 * see: http://docs.jboss.com/seam/2.2.0.GA/reference/en-US/html/testing.html#
 * d0e30109
 * 
 * @author Michael Wohlfart
 * 
 */
public class LoginTest extends SeamTest {

    @Test
    public void testSuccessLogin() throws Exception {

        /**************************
         * call login page and fill out fields to log in
         */
        new FacesRequest("/login.xhtml") {

            @Override
            protected void updateModelValues() throws Exception {
                setValue("#{credentials.username}", "devel");
                setValue("#{credentials.password}", "devel");
            }

            @Override
            protected void invokeApplication() {
                CharmsUser user = null;
                user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNull(user, "devel user is already authenticated, shouldn't happen before calling the login method");

                Assert.assertEquals(invokeMethod("#{identity.login}"), "loggedIn");

                user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertEquals(user.getName(), "devel", "devel user is not authenticated, login failed!");
            }

        }.run();
    } // end testSuccessLogin

    @Test
    public void testFailedLogin() throws Exception {

        /**************************
         * call login page and fill out, use wrong login/password
         */
        new FacesRequest("/login.xhtml") {

            @Override
            protected void updateModelValues() throws Exception {
                setValue("#{credentials.username}", "unknownUser");
                setValue("#{credentials.password}", "invalidPass");
            }

            @Override
            protected void invokeApplication() {
                CharmsUser user = null;
                user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNull(user, "user is already authenticated, shouldn't happen before calling the login method");

                Assert.assertNull(invokeMethod("#{identity.login}"));

                user = (CharmsUser) getValue("#{authenticatedUser}");
                Assert.assertNull(user, "user is already authenticated, shouldn't happen before calling the login method");
            }

        }.run();
    } // end testFailedLogin

}
