package net.wohlfart.charms.test.admin.faces;

import java.util.List;

import net.wohlfart.jbpm4.queries.JbpmDeploymentTable;
import net.wohlfart.jbpm4.queries.JbpmDeploymentTable.Row;

import org.jboss.seam.mock.SeamTest;
import org.jboss.seam.security.Identity;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 * FacesRequest simulates a postback, so it doesn't execute the action. If you
 * want the action to be executed you have to use a NonFacesRequest.
 * 
 * @author Michael Wohlfart
 * 
 */
public class DeploymentListTest extends SeamTest {

    // set in the deployment list view
    Long deploymentId;

    /*
     * 
     * 
     * somehow we are supposed to run these methods on test startup :-/
     * 
     * @Override
     * 
     * @BeforeSuite public void startSeam() throws Exception {
     * super.startSeam(); };
     * 
     * @Override
     * 
     * @AfterSuite protected void stopSeam() throws Exception {
     * super.stopSeam(); }
     */

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        Identity.setSecurityEnabled(false);
    };

    @AfterMethod
    @Override
    public void end() {
        super.end();
    };

    @Test
    public void testDeploymentListView() throws Exception {

        /**********************************
         * login
         */
        new NonFacesRequest("/pages/login.xhtml") {
        }.run(); // prob not really needed

        final String loginCid = new FacesRequest("/pages/login.xhtml") {

            @Override
            protected void updateModelValues() throws Exception {
                setValue("#{credentials.username}", "devel");
                setValue("#{credentials.password}", "devel");
            }

            @Override
            protected void invokeApplication() {
                Assert.assertEquals(invokeMethod("#{identity.login}"), "loggedIn");
            }
        }.run();
        Assert.assertNotNull(loginCid);

        // next stop is the home page
        // http://charms.persman.de/charms/pages/user/home.html?cid=32
        // we don't have any conversation on the home page:
        new FacesRequest("/pages/user/home.xhtml") {
        }.run();

        /**********************************
         * simulate the menu click switch to the deployment list this actually
         * calls the page actions since there are no parameters
         */
        final String rootCid = new NonFacesRequest("/pages/jbpm/deploymentList.xhtml") {

            @Override
            protected void renderResponse() throws Exception {
                final JbpmDeploymentTable jbpmDeploymentTable = (JbpmDeploymentTable) getValue("#{jbpmDeploymentTable}");
                final List<Row> list = jbpmDeploymentTable.getResultList();
                // currently we deploy 4 instances
                // Assert.assertEquals(list.size(), 4);
                final Row deployment = list.get(0);
                deploymentId = deployment.getDbid();
            };
        }.run();
        Assert.assertNotNull(rootCid); // we have a conversation
        Assert.assertNotNull(deploymentId);

        /**********************************
         * switch to the graph view
         */
        // http://localhost:8080/charms/pages/jbpm/deploymentList.html?jbpmDeploymentId=1&actionOutcome=graph&cid=10
        final String pngCid1 = new NonFacesRequest("/pages/jbpm/deploymentPng.xhtml", rootCid) {

            @Override
            protected void beforeRequest() {
                setParameter("cid", rootCid);
                setParameter("jbpmDeploymentId", deploymentId + "");
            }

        }.run();
        Assert.assertEquals(rootCid, pngCid1);

        /*
         * // this gives us a redirect to: final String pngCid2 = new
         * NonFacesRequest("/pages/jbpm/deploymentPng.xhtml", pngCid1) {
         * 
         * @Override protected void beforeRequest() {
         * System.out.println("beforeRequest called");
         * setParameter("jbpmDeploymentId", deploymentId.toString());
         * //setParameter("cid", pngCid1); }; }.run();
         * Assert.assertNotNull(pngCid2); System.out.println("pngCid2:  " +
         * pngCid2);
         * 
         * 
         * final String pngCid3 = new
         * FacesRequest("/pages/jbpm/deploymentPng.xhtml", pngCid2) {
         * 
         * @Override protected void beforeRequest() {
         * setParameter("jbpmDeploymentId", deploymentId.toString());
         * setParameter("cid", pngCid2); }; }.run();
         * Assert.assertNotNull(pngCid3);
         */
        /**********************************
         * switch to the code view
         * 
         * final String xmlCid = new
         * FacesRequest("/pages/jbpm/deploymentXml.html", "102") {
         * 
         * @Override protected void applyRequestValues() throws Exception {
         *           setParameter("jbpmDeploymentId", deploymentId.toString());
         *           setParameter("cid", "102"); };
         * @Override protected void renderResponse() { //
         *           Assert.assertTrue(Manager
         *           .instance().isLongRunningConversation()); };
         * 
         *           }.run(); Assert.assertEquals(xmlCid, "102");
         */
        /**********************************
         * switch to the update/properties view, this is done with a redirect in
         * the deployment list view
         * 
         * final String propertiesCid = new
         * FacesRequest("/pages/jbpm/deploymentProperties.html", "103") {
         * 
         * @Override protected void applyRequestValues() throws Exception {
         *           setParameter("jbpmDeploymentId", deploymentId.toString());
         *           setParameter("cid", "103"); };
         * @Override protected void renderResponse() { //
         *           Assert.assertTrue(Manager
         *           .instance().isLongRunningConversation()); };
         * 
         *           }.run(); Assert.assertEquals(propertiesCid, "103");
         */
    } // end testStartCompleteRequest()

}
